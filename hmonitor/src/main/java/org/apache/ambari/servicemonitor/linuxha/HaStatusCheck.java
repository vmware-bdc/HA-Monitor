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

package org.apache.ambari.servicemonitor.linuxha;

import org.apache.ambari.servicemonitor.MonitorKeys;
import org.apache.ambari.servicemonitor.probes.DfsListProbe;
import org.apache.ambari.servicemonitor.probes.DfsSafeModeProbe;
import org.apache.ambari.servicemonitor.probes.HttpProbe;
import org.apache.ambari.servicemonitor.probes.PidLiveProbe;
import org.apache.ambari.servicemonitor.probes.Probe;
import org.apache.ambari.servicemonitor.reporting.ProbeFailedException;
import org.apache.ambari.servicemonitor.reporting.ProbePhase;
import org.apache.ambari.servicemonitor.reporting.ProbeReportHandler;
import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.ambari.servicemonitor.reporting.ReportingLoop;
import org.apache.ambari.servicemonitor.utils.Exit;
import org.apache.ambari.servicemonitor.utils.ExitMainException;
import org.apache.ambari.servicemonitor.utils.OptionHelper;
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Status check for Linux HA clustering.
 *
 * -timeout: timeout in milliseconds
 * -path: path; default /
 * -pid processID check
 * -boot booting flag -indicates timeout should include boot time
 *
 * This process works by 
 */
public class HaStatusCheck extends ToolPlusImpl implements ProbeReportHandler {
  private static final Log LOG = LogFactory.getLog(HaStatusCheck.class);
  public static final int STATUS_CHECK_TIMEOUT = 60000;
  public static final String TIMEOUT = "t";
  public static final String TIMEOUT_LONG = "timeout";
  public static final String PATH = "f";
  public static final String PATH_LONG = "file";
  public static final String PID = "i";
  public static final String PID_LONG = "pid";
  public static final String BOOT = "b";
  public static final String BOOT_LONG = "boottimeout";
  public static final String URL_LONG = "url";
  public static final String URL = "r";
  public static final String WAITFS = "w";
  public static final String WAITFS_LONG = "waitfs";

  private boolean shutdown = false;
  private int shutdowntime = 0;


  public static void main(String[] args) {

    ToolRunnerPlus.runAndExit(new Configuration(),
                              new HaStatusCheck(),
                              args);
  }


  @Override
  public Options createToolSpecificOptions() {
    Options options = super.createToolSpecificOptions();
    OptionHelper.addIntOpt(options, TIMEOUT, TIMEOUT_LONG, "timeout in milliseconds; -1 for no timeout");
    OptionHelper.addIntOpt(options, BOOT, BOOT_LONG, "Boot timeout in milliseconds; 0 (or less) for no timeout");
    OptionHelper.addStringArgOpt(options, PATH, PATH_LONG, "path to query");
    OptionHelper.addStringArgOpt(options, PID, PID_LONG, "path to PID file; null means 'no pid'");
    OptionHelper.addStringArgOpt(options, URL, URL_LONG, "URL to block for");
    options.addOption(WAITFS, WAITFS_LONG, false, "wait for the filesystem at boot before beginning probes");

    return options;
  }

  @Override
  public int run(String[] args) throws Exception {
    Configuration conf = getConf();
    CommandLine commandLine = getCommandLine();
    int timeout = OptionHelper.getIntOption(commandLine, TIMEOUT, STATUS_CHECK_TIMEOUT);
    int boottimeout = OptionHelper.getIntOption(commandLine, BOOT, -1);

    String pid = OptionHelper.getStringOption(commandLine, PID, null);

    String path = OptionHelper.getStringOption(commandLine, PATH, null);

    String url = OptionHelper.getStringOption(commandLine, URL, null);

    boolean waitFs = commandLine.hasOption(WAITFS);

    if (pid == null && path == null && url == null) {
      //reject bad args
      String message = "No parameter --" + PID_LONG
                       + ", " + URL_LONG
                       + " or --" + PATH_LONG + " supplied in arguments:"
                       + "[ \"" + buildArgumentList("\" \"") + "\" ]";
      LOG.warn(message);
      LOG.warn("Options\n" + OptionHelper.buildParsedOptionList(commandLine));
      LOG.warn("Unprocessed options " + args.length);
      for (String arg : args) {
        LOG.warn(arg);
      }
      throw new ExitMainException(message);
    }

    //build the probes
    List<Probe> probes = new ArrayList<Probe>();
    if (!IsNullParam(pid)) {
      probes.add(new PidLiveProbe(new File(pid), conf));
    }

    if (!IsNullParam(path)) {
      probes.add(new DfsListProbe(conf, path));
    }

    if (!IsNullParam(url)) {
      URL target = new URL(url);
      HttpProbe probe = new HttpProbe(target, timeout,
                                      MonitorKeys.WEB_PROBE_DEFAULT_CODE,
                                      MonitorKeys.WEB_PROBE_DEFAULT_CODE,
                                      conf);
      probes.add(probe);
    }

    if (probes.isEmpty()) {
      //bail out with success if no probes specified
      LOG.info("No active probes");
      return 0;
    }

    //add a dependency on HDFS if supplied
    List<Probe> depends = null;
    if (waitFs) {
      depends = new ArrayList<Probe>();
      depends.add(new DfsSafeModeProbe(new Configuration(conf), true));
    }

    ReportingLoop reportingLoop = new ReportingLoop("HAStatusCheck",
                                                    this,
                                                    probes,
                                                    depends,
                                                    1000,
                                                    1000,
                                                    timeout,
                                                    boottimeout);
    //this spins off

    if (!reportingLoop.startReporting()) {
      throw new ExitMainException("failed to start monitoring");
    }
    //start reporting, directly in the main thread
    reportingLoop.run();

    LOG.info("Run completed");
    return 0;
  }

  private boolean IsNullParam(String param) {
    return param == null || "null".equals(param) || param.isEmpty();
  }

  @Override
  public void probeProcessStateChange(ProbePhase probePhase) {

  }

  @Override
  public void probeResult(ProbePhase phase, ProbeStatus status) {
  }

  @Override
  public void probeFailure(ProbeFailedException exception) {
    //a probe failed, fail the process
    Exit.exitOnProbeFailure(exception.status);
  }

  @Override
  public void probeBooted(ProbeStatus status) {
  }

  @Override
  public boolean start(String name, String description) {
    return true;
  }

  @Override
  public void unregister() {
  }

  @Override
  public boolean isIntegratedWithHAMonitoringSystem() {
    return false;
  }

  @Override
  public void heartbeat(ProbeStatus status) {
  }

  @Override
  public void probeTimedOut(ProbePhase currentPhase, Probe probe, ProbeStatus lastStatus, long currentTime) {
    //a probe failed, fail the process
    Exit.exitProcess(-1, "Timeout of probe " + probe + " in phase " + currentPhase);
  }

  @Override
  public void liveProbeCycleCompleted() {
    //all is well
    LOG.info("Probes are all live");
    Exit.exitProcess(0, "");
  }

  @Override
  public String toString() {
    return "HAStatusCheck";
  }
}
