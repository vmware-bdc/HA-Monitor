/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.servicemonitor.jobs;


import org.apache.ambari.servicemonitor.HadoopKeys;
import org.apache.ambari.servicemonitor.utils.Exit;
import org.apache.ambari.servicemonitor.utils.MonitorUtils;
import org.apache.ambari.servicemonitor.utils.OptionHelper;
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Run FileUsingJobs in parallel according to -a job count (-1 for no limit) -a
 * sleep time
 *
 * Because the ToolRunner class blocks during its execution you really need a
 * thread per task.
 *
 * What happens then, is: At a specific rate, jobs are pushed into the
 * Executor.
 */
public class BulkFileJobSubmitter extends ToolPlusImpl {
  public static final Log LOG = LogFactory.getLog(BulkFileJobSubmitter.class);
  public static final int POOL_SIZE = 5;
  private JobConf templateConf;
  private int jobs;
  private final AtomicInteger completed = new AtomicInteger(0);
  private final AtomicInteger successes = new AtomicInteger(0);
  private final AtomicInteger failures = new AtomicInteger(0);
  private final AtomicLong totalExecDuration = new AtomicLong(0);
  private CountDownLatch doneSignal;

  private Path outputPath;
  private boolean deleteOutputDirectories;

  public BulkFileJobSubmitter() throws Exception {

  }

  @Override
  public String getToolName() {
    return "BulkFileJobSubmitter";
  }

  @Override
  public Options createToolSpecificOptions() {
    Options options = new Options();
    OptionHelper.addIntOpt(options, "j", "jobs", "number of jobs to submit");
    OptionHelper.addIntOpt(options, "l", "delay",
                           "delay in millis between job submissions");
    OptionHelper.addStringArgOpt(options, "o", "output",
                                 "output directory in the target filesystem");
    options.addOption("x", "x", false, "delete the output directory");
    return options;
  }


  @Override
  public int run(String[] args) throws Exception {
    return exec();
  }

  private int exec() throws Exception {
    CommandLine commandLine = getCommandLine();
    Configuration conf = getConf();

    String outputdir =
      OptionHelper.getStringOption(commandLine, "o", "bulkjob");
    outputPath = new Path(outputdir);

    if (commandLine.hasOption('x')) {
      //delete the filesystem dir. This will 
      deleteOutputDirectories = true;
    }
    jobs = OptionHelper.getIntOption(commandLine, "j", 1);
    int delay = OptionHelper.getIntOption(commandLine, "l", 1000);
    doneSignal = new CountDownLatch(jobs);

    templateConf = new JobConf(conf);

    String jtURI = MonitorUtils.extractJobTrackerParameter(templateConf);
    LOG.info("Submitting "
             + (jobs >= 0 ? jobs : "unlimited")
             + " jobs with a delay of " + delay + " millis" +
             " to JT " + jtURI +
             " and filesystem " +
             templateConf.get(FileSystem.FS_DEFAULT_NAME_KEY)
            );


    int jobCount = 0;
    ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(POOL_SIZE);
    int toSubmit = jobs;
    long started, finished;
    started = System.currentTimeMillis();

    while (toSubmit > 0) {
      scheduler.submit(new JobWorker("instance-" + (++jobCount)));
      Thread.sleep(delay);
      toSubmit--;
    }
    LOG.info("All jobs scheduled in local queue");
    //here all the jobs are submitted, await their completion.
    doneSignal.await();
    finished = System.currentTimeMillis();
    int s = successes.get();
    int f = failures.get();
    long execDuration = totalExecDuration.get();
    long elapsedTime = finished - started;
    LOG.info("Completed. Successes = " + s + " out of " + jobs +
             " success rate= " + (s * 100) / (jobs) + "% " +
             " total execTime " +
             MonitorUtils.millisToHumanTime(execDuration) + " "
             + " elapsed Time " +
             MonitorUtils.millisToHumanTime(elapsedTime));

    return f == 0 ? 0 : 1;
  }


  /**
   * inner class to handle the starting of a specific job
   */
  private class JobWorker implements Callable<Integer> {

    public final String name;
    private long started, finished;
    private int returnCode;

    private JobWorker(String name) {
      this.name = name;
    }

    @Override
    public Integer call() throws Exception {

      try {
        List<String> runnerArgs = new ArrayList<String>(2);
        runnerArgs.add("-n");
        runnerArgs.add(name);
        if (deleteOutputDirectories) {
          runnerArgs.add("-x");
        }

        JobConf instanceConf = new JobConf(templateConf);
        instanceConf.set(JobKeys.KEY_JOB_NAME, name);
        instanceConf.set(HadoopKeys.MAPRED_OUTPUT_DIR,
                         new Path(outputPath, name).toString());
        LOG.info("Creating job " + name);
        started = System.currentTimeMillis();
        returnCode = FileUsingJobRunner.exec(instanceConf,
                                             runnerArgs.toArray(new String[runnerArgs.size()]));

      } catch (Exception e) {
        LOG.warn("Job " + name + " failed:" + e, e);
        returnCode = -1;
      } finally {
        finished = System.currentTimeMillis();
        jobCompleted();
      }
      return returnCode;
    }

    /**
     * Handles job completion
     */
    private void jobCompleted() {
      String outcome;
      if (returnCode == 0) {
        outcome = "success";
        successes.incrementAndGet();
      } else {
        outcome = "failure";
        failures.incrementAndGet();
      }
      int count = completed.incrementAndGet();
      doneSignal.countDown();
      long duration = finished - started;
      if (duration > 0) {
        totalExecDuration.addAndGet(duration);
      }
      LOG.info("Job " + name + " completed: outcome=" + outcome + ". " +
               "Duration = " + MonitorUtils.millisToHumanTime(duration));
      if (count == jobs) {
        LOG.info("All jobs are completed");
      }
    }
  }


  public static void main(String[] args) {
    try {
      int res =
        ToolRunnerPlus
          .run(new Configuration(),
               new BulkFileJobSubmitter(),
               args);
      Exit.exitProcess(res, "");
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }

}
