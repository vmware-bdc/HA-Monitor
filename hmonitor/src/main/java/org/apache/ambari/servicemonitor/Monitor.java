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

package org.apache.ambari.servicemonitor;

import org.apache.ambari.servicemonitor.probes.DfsListProbe;
import org.apache.ambari.servicemonitor.probes.DfsSafeModeProbe;
import org.apache.ambari.servicemonitor.probes.HttpProbe;
import org.apache.ambari.servicemonitor.probes.JTClusterStatusProbe;
import org.apache.ambari.servicemonitor.probes.PidLiveProbe;
import org.apache.ambari.servicemonitor.probes.PortProbe;
import org.apache.ambari.servicemonitor.probes.Probe;
import org.apache.ambari.servicemonitor.reporting.Reporter;
import org.apache.ambari.servicemonitor.reporting.ReportingLoop;
import org.apache.ambari.servicemonitor.utils.Exit;
import org.apache.ambari.servicemonitor.utils.ExitMainException;
import org.apache.ambari.servicemonitor.utils.InterruptData;
import org.apache.ambari.servicemonitor.utils.Interrupted;
import org.apache.ambari.servicemonitor.utils.IrqHandler;
import org.apache.ambari.servicemonitor.utils.MonitorUtils;
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class Monitor extends ToolPlusImpl implements MonitorKeys, Interrupted {

  private static final Log LOG = LogFactory.getLog(Monitor.class);

  /**
   * interrupts
   */
  private IrqHandler sigint;
  private IrqHandler sigterm;
  private final String name;

  private ReportingLoop reportingLoop;


  public Monitor(String name) {
    this.name = name;
  }

  /**
   * Add sysprops and env variables to the dump
   * @return
   */
  @Override
  public String dump() {
    StringBuilder dump = new StringBuilder();

    InetAddress hostname = MonitorUtils.getLocalHost();
    if (hostname != null) {
      dump.append("Hostname:").append(hostname.toString()).append("\n");
    } else {
      dump.append("Hostname/address unknown!\n");
    }

    //now locate and print the location of log4j so as to see what is going wrong.
    return dump.toString();
  }


  /**
   * Set up the IRQ handling then start the monitor, which only terminates by throwing an exception
   * @param args unused
   * @return
   * @throws Exception
   */
  @Override
  public int run(String[] args) throws Exception {
    sigint = new IrqHandler(IrqHandler.CONTROL_C, this);
    sigterm = new IrqHandler("TERM", this);
    Reporter reporter = MonitorUtils.createReporter(getConf());
    //this method does not return
    execMonitor(reporter);
    return 0;
  }


  /**
   * Execute the monitor. This method does not exit except by throwing exceptions or by calling System.exit().
   * @throws IOException problems
   * @throws ExitMainException an explicit exit exception
   */
  public void execMonitor(Reporter reporter) throws IOException {

    Configuration conf = getConf();
    int probeInterval =
      conf.getInt(MONITOR_PROBE_INTERVAL, PROBE_INTERVAL_DEFAULT);
    int reportInterval =
      conf.getInt(MONITOR_REPORT_INTERVAL, REPORT_INTERVAL_DEFAULT);
    int probeTimeout = conf.getInt(MONITOR_PROBE_TIMEOUT, PROBE_TIMEOUT_DEFAULT);
    int bootstrapTimeout = conf.getInt(MONITOR_BOOTSTRAP_TIMEOUT, BOOTSTRAP_TIMEOUT_DEFAULT);

    List<Probe> probes = new ArrayList<Probe>();
    if (conf.getBoolean(PORT_PROBE_ENABLED, false)) {

      String probeHost = conf.get(PORT_PROBE_HOST, DEFAULT_PROBE_HOST);


      int probePort = conf.getInt(PORT_PROBE_PORT,
                                  DEFAULT_PROBE_PORT);

      if (probePort == -1) {
        URI fsURI = FileSystem.getDefaultUri(conf);
        probePort = fsURI.getPort();
        validateParam(probePort == -1, "No port value in " + fsURI);
      }

      PortProbe portProbe = PortProbe.createPortProbe(new Configuration(conf),
                                                      probeHost,
                                                      probePort);
      probes.add(portProbe);
    } else {
      LOG.debug("port probe disabled");
    }

    if (conf.getBoolean(PID_PROBE_ENABLED, false)) {
      Probe probe = PidLiveProbe.createProbe(new Configuration(conf));
      probes.add(probe);
      LOG.debug("Pid probe enabled: " + probe.toString());
    } else {
      LOG.debug("Pid probe disabled");
    }


    if (conf.getBoolean(WEB_PROBE_ENABLED, false)) {
      HttpProbe httpProbe = HttpProbe.createHttpProbe(new Configuration(conf));
      probes.add(httpProbe);
    } else {
      LOG.debug("HTTP probe disabled");
    }

    if (conf.getBoolean(LS_PROBE_ENABLED, false)) {
      String path = conf.get(LS_PROBE_PATH, LS_PROBE_DEFAULT);
      DfsListProbe lsProbe = new DfsListProbe(new Configuration(conf), path);
      probes.add(lsProbe);
    } else {
      LOG.debug("ls probe disabled");
    }

    if (conf.getBoolean(JT_PROBE_ENABLED, false)) {
      Probe jtProbe = new JTClusterStatusProbe(new Configuration(conf));
      probes.add(jtProbe);
    } else {
      LOG.debug("JT probe disabled");
    }

    
    List<Probe> dependencyProbes = new ArrayList<Probe>(1);

    if (conf.getBoolean(MONITOR_DEPENDENCY_DFSLIVE, false)) {
      //there's a dependency on DFS
      //add a monitor for it
      LOG.info("Adding a dependency on HDFS being live");
      dependencyProbes.add(new DfsSafeModeProbe(new Configuration(conf), true));
    }

    reportingLoop = new ReportingLoop(name,
                                      reporter,
                                      probes,
                                      dependencyProbes,
                                      probeInterval,
                                      reportInterval,
                                      probeTimeout,
                                      bootstrapTimeout);

    if (!reportingLoop.startReporting()) {
      throw new ExitMainException(
        name + ": failed to start monitoring with reporter " + reporter);
    }
    //start reporting, either in a background thread
    //or here, directly in the main thread
    reportingLoop.run();
  }

  /**
   * Validate a condition that verifies a param is valid
   * @param test condition
   * @param message message to use in errors
   * @throws ExitMainException if the condition was false
   */
  private void validateParam(boolean test, String message) throws ExitMainException {
    if (test) {
      throw new ExitMainException(Exit.EXIT_ERROR, message);
    }
  }

  /**
   * An interrupt triggers a clean shutdown
   *
   * @param aised the signal that was raised
   */
  @Override
  public void interrupted(InterruptData interruptData) {
    //this is the signal handler
    LOG.fatal(interruptData.toString() + " received -shutting down.");
    reportingLoop.close();
    Exit.exitProcess(Exit.EXIT_SUCCESS, name + " terminated");
  }

  public static void main(String[] args) {
    try {
      int res =
        ToolRunnerPlus
          .run(new Configuration(),
               new Monitor("NameNode Monitor"),
               args);
      Exit.exitProcess(res, "");
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }


}
