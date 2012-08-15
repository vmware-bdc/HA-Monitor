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

package org.apache.ambari.servicemonitor.unit

import org.apache.ambari.servicemonitor.Monitor
import org.apache.ambari.servicemonitor.MonitorKeys
import org.apache.ambari.servicemonitor.reporting.ProbePhase
import org.apache.ambari.servicemonitor.unit.reporting.CallbackProbeReporter
import org.apache.ambari.servicemonitor.unit.reporting.ExitReportingException
import org.apache.ambari.servicemonitor.utils.MonitorConfigHelper
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.MiniDFSCluster
import org.junit.Test

class NNMonitorTest extends BaseLocalClusterTestCase implements MonitorKeys {

  @Test
  public void testNNLifecycle() throws Throwable {

    MiniDFSCluster dfs = createDFSCluster();

    Monitor monitor = createNNMonitor()
    //we now have a monitor ready to run, but need to patch in a new monitor
    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {
          if (it.inPhase(ProbePhase.LIVE)) {CallbackProbeReporter.exitReporting()}
        }
    )

    try {
      monitor.execMonitor(tpr)
    } catch (ExitReportingException ignored) {
      //expected
    }
    assert tpr.phaseWasEntered(ProbePhase.LIVE)
  }



  def Monitor createNNMonitor() {
    Configuration conf = createDFSBondedConfiguration()
    URI fsURI = FileSystem.getDefaultUri(conf);
    int probePort = fsURI.getPort();
    MonitorConfigHelper.enablePortPobe(conf, "localhost", probePort);
    MonitorConfigHelper.enableFSLsProbe(conf, "/")
    MonitorConfigHelper.enableWebProbe(conf, getNamenodeWebURL())

    Monitor monitor = new Monitor("NNTracker Monitor")
    String[] args = []
    String[] leftovers = ToolRunnerPlus.prepareToExecute(conf,
                                                         monitor,
                                                         args);
    assert leftovers != null
    monitor
  }

}
