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

import org.apache.ambari.servicemonitor.probes.PortProbe;
import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

public class LivePortProbeTest extends BaseLocalClusterTestCase {

  private static final Log log = LogFactory.getLog(LivePortProbeTest.class);

  /**
   * Assert that the port probe starts and stops
   */
  @Test
  public void testProbeMiniNN() throws Throwable {
    createDFSCluster();
    int port = dfsCluster.getNameNodePort();
    PortProbe probe = new PortProbe("127.0.0.1", port, NN_STARTUP_SHUTDOWN_TIME, "startup", new Configuration());
    probe.init();
    SleepTimer sleepTimer = new SleepTimer(100, NN_STARTUP_SHUTDOWN_TIME);
    ProbeStatus status;
    do {
      status = probe.ping(true);
    } while (!status.isSuccess() && sleepTimer.sleep());
    assertSuccess(status);
    //now up, kill the nn
    destroyDFSCluster();
    //now spin waiting for the NN to die
    probe = new PortProbe("127.0.0.1", port, NN_STARTUP_SHUTDOWN_TIME, "shutdown", new Configuration());
    sleepTimer = new SleepTimer(100, NN_STARTUP_SHUTDOWN_TIME);
    log.info("Starting ping cycle waiting for NN to die");
    do {
      status = probe.ping(true);
      log.info("Ping outcome: " + status);
    } while (status.isSuccess() && sleepTimer.sleep());
    //here the NN should have failed
    assertFailure(status);
  }


}
