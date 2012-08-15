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

package org.apache.ambari.servicemonitor.unit;

import org.apache.ambari.servicemonitor.probes.JTClusterStatusProbe;
import org.apache.ambari.servicemonitor.probes.Probe;
import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;
import org.junit.Test;

public class JTClusterStatusProbeTest extends BaseLocalClusterTestCase {

  private static final Log log =
    LogFactory.getLog(JTClusterStatusProbeTest.class);

  /**
   * Test the cluster status probe
   * @throws Throwable
   */
  @Test
  public void testProbeMiniNN() throws Throwable {
    createDFSCluster();
    MiniMRCluster miniMRCluster = createMRCluster();
    JobConf conf = new JobConf();
    //once the cluster is up 
    miniMRCluster.waitUntilIdle();
    int port = miniMRCluster.getJobTrackerPort();
    assertTrue("MiniMR Cluster port not yet set, current value" + port,
               port > 0);
    injectMRClusterEntry(conf, miniMRCluster);
    Probe probe = new JTClusterStatusProbe(conf);
    probe.init();
    int startupShutdownTime = NN_STARTUP_SHUTDOWN_TIME;
    SleepTimer sleepTimer = new SleepTimer(100, startupShutdownTime);
    ProbeStatus status;
    do {
      status = probe.ping(true);
    } while (!status.isSuccess() && sleepTimer.sleep());
    assertSuccess(status);
    //now up, kill the cluster
    destroyMRCluster();
    //now spin waiting for the cluster to die

    log.info("Starting ping cycle waiting for cluster shutdown");
    do {
      status = probe.ping(true);
      log.info("Ping outcome: " + status);
    } while (status.isSuccess() && sleepTimer.sleep());
    //here the NN should have failed
    assertFailure(status);
  }


}
