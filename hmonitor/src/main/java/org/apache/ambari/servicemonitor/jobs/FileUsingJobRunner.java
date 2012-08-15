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

import org.apache.ambari.servicemonitor.utils.Exit;
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
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextOutputFormat;

public class FileUsingJobRunner extends ToolPlusImpl {

  public static final Log LOG = LogFactory.getLog(FileUsingJobRunner.class);

  public int run(String[] args) throws Exception {
    // Configuration processed by ToolRunner
    Configuration conf = getConf();

    CommandLine commandLine = getCommandLine();
    // Create a JobConf using the processed conf
    JobConf jobConf = new JobConf(conf, FileUsingJobRunner.class);

    //tune the config
    if (jobConf.get(JobKeys.RANGEINPUTFORMAT_ROWS) == null) {
      jobConf.setInt(JobKeys.RANGEINPUTFORMAT_ROWS, 1);
    }

    // Process custom command-line options
    String name =
      OptionHelper.getStringOption(commandLine, "n", "File Using Job");
    if (commandLine.hasOption('x')) {
      //delete the output directory
      String destDir = jobConf.get(JobKeys.MAPRED_OUTPUT_DIR);
      FileSystem fs = FileSystem.get(jobConf);
      fs.delete(new Path(destDir), true);
    }

    // Specify various job-specific parameters     
    jobConf.setMapperClass(FileUsingMapper.class);
    jobConf.setReducerClass(FileUsingReducer.class);
    jobConf.setMapOutputKeyClass(IntWritable.class);
    jobConf.setMapOutputValueClass(IntWritable.class);
    jobConf.setOutputFormat(TextOutputFormat.class);
    jobConf.setInputFormat(RangeInputFormat.class);
    //jobConf.setPartitionerClass(SleepJob.class);
    jobConf.setSpeculativeExecution(false);
    jobConf.setJobName(name);
    jobConf.setJarByClass(this.getClass());
    FileInputFormat.addInputPath(jobConf, new Path("ignored"));

    // Submit the job, then poll for progress until the job is complete
    RunningJob runningJob = JobClient.runJob(jobConf);
    runningJob.waitForCompletion();
    return runningJob.isSuccessful() ? 0 : 1;
  }


  @Override
  public Options createToolSpecificOptions() {
    Options options = super.createToolSpecificOptions();
    options.addOption("x", "x", false, "delete the output directory");
    OptionHelper.addStringArgOpt(options, "n", "name", "name of the job");
    return options;
  }

  public static int exec(String... args) throws Exception {
    Configuration conf = new Configuration();
    return exec(conf, args);
  }

  public static int exec(Configuration conf, String... args) throws Exception {
    return ToolRunnerPlus.run(conf, new FileUsingJobRunner(), args);
  }


  @Override
  public String getToolName() {
    return "FileUsingJobRunner";
  }


  public static void main(String[] args) {
    try {
      int res = exec(args);
      Exit.exitProcess(res, "");
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }
}
