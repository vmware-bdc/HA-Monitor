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

package org.apache.ambari.servicemonitor.functional

import org.apache.ambari.servicemonitor.remote.BaseRemoteHadoopTestCase
import org.apache.chaos.remote.RemoteServer
import org.apache.hadoop.conf.Configuration

class DfsOperatorTestCase extends BaseRemoteHadoopTestCase {
  public static final String TEST_REMOTE_NAMENODE_SERVER = "test.remote.namenode.server"
  public static final String TEST_REMOTE_NAMENODE_SERVER2 = "test.remote.namenode.server2"
  public static final String TEST_REMOTE_VSPHERE_ENABLED = "test.remote.vsphere.enabled"
  public static final String TEST_REMOTE_REDHAT_ENABLED = "test.remote.redhat.enabled"
  public static final String TEST_REMOTE_NAMENODE_PIDFILE = "test.remote.namenode.pidfile"
  public static final String TEST_REMOTE_FS_NAME = "test.remote.fs.default.name"
  public static final String TEST_REMOTE_NAMENODE_START_TIME = "test.remote.namenode.start.time"
  public static final String TEST_REMOTE_NAMENODE_MONITORED_STOP_TIME = "test.remote.namenode.monitored.stop.time"
  public static final int OPERATOR_CHECK_INTERVAL = 500
  public static final int NN_MONITORED_STOP_TIME = 6 * 60000
  public static final int NN_START_TIME = 6 * 60000
  public static final int SIGSTOP = 19
  public static final int SIGKILL = 9
  protected DfsOperator dfsOperator


  protected RemoteServer nnserver
  protected int namenodeStartTime = NN_START_TIME
  protected int namenodeMonitoredStopTime = NN_MONITORED_STOP_TIME

  @Override
  protected void setUp() {
    super.setUp()
    bindSSHKey()
    nnserver = rootServer(requiredSysprop(TEST_REMOTE_NAMENODE_SERVER))
    namenodeStartTime = intSysProp(TEST_REMOTE_NAMENODE_START_TIME, namenodeStartTime)
    namenodeMonitoredStopTime = intSysProp(TEST_REMOTE_NAMENODE_MONITORED_STOP_TIME, namenodeMonitoredStopTime)
  }

  @Override
  protected void tearDown() {
    stopDfs()
    super.tearDown()
  }


  synchronized void stopDfs() {
    if (dfsOperator != null) {
      dfsOperator.stopThread()
      dfsOperator = null
    }
  }



  Configuration buildConf() {
    Configuration conf = new Configuration()
    conf.set(org.apache.hadoop.fs.FileSystem.FS_DEFAULT_NAME_KEY,
             requiredSysprop(TEST_REMOTE_FS_NAME))
    conf
  }

  public void assertRestartsHDFS(Closure namenodeKillingAction) {
    dfsOperator = new DfsOperator("/", buildConf(), OPERATOR_CHECK_INTERVAL)
    dfsOperator.startThread()
    log.info("Waiting for DFS online: $dfsOperator");
    dfsOperator.waitForOnline(namenodeStartTime)
    namenodeKillingAction()
    log.info("Waiting for DFS offline: $dfsOperator");
    assert dfsOperator.waitForOffine(namenodeMonitoredStopTime)
    log.info("Waiting for DFS to come back online: $dfsOperator");
    assert dfsOperator.waitForOnline(namenodeStartTime)
    log.info("Back online: $dfsOperator")
  }


}
