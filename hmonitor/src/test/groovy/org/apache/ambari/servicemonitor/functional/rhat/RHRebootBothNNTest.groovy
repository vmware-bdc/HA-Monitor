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





package org.apache.ambari.servicemonitor.functional.rhat

import org.apache.chaos.remote.Clustat
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * This test reboots each NN in turn, and verifies that the service moves from one to the other,
 * rather than just hope that the fielsystem comes back fast.
 */
class RHRebootBothNNTest extends RHTestCase {
  protected static final Log log = LogFactory.getLog(RHRebootBothNNTest)


  public void testRebootBothNamenodes() throws Throwable {
    if (!enabled()) {return }


    nnServer.command("true")
    nnServer2.command("true")
    assertRestartsHDFS {
      nnServer.command(REBOOT)
    }

    //here failover is expected
    Clustat clustat = nnServer2.clustat()
    assert nnServer2.host == clustat.hostRunningService(SERVICE_GROUP_NAMENODE)

    //give NN1 30s advantage in boot time
    Thread.sleep(30000)

    assertRestartsHDFS {
      nnServer2.command(REBOOT)
    }
    clustat = nnServer.clustat()
    assert nnServer.host == clustat.hostRunningService(SERVICE_GROUP_NAMENODE)
    nnServer2.waitForServerLive(30000)
  }


}
