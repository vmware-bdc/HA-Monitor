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

package org.apache.ambari.servicemonitor.remote

import org.apache.ambari.servicemonitor.HadoopKeys
import org.apache.chaos.remote.BaseRemoteTestCase
import org.apache.chaos.remote.RemoteDaemonOperations
import org.apache.chaos.remote.RemoteServer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hdfs.DistributedFileSystem

/**
 *
 */
class BaseRemoteHadoopTestCase extends BaseRemoteTestCase {

  /**
   * key for the remote server
   */
  public static final String JT_SERVER = TEST_REMOTE + "jobtracker.server"
  public static final String JT_SERVER_PORT = TEST_REMOTE + "jobtracker.port"
  public static final String NN_SERVER = TEST_REMOTE + "namenode.server"
  public static final String NN_SERVER_PORT = TEST_REMOTE + "namenode.port"

  protected static final String REBOOT = "shutdown -r 0"

  protected RemoteServer nnServer
  protected RemoteServer jtServer
  protected RemoteDaemonOperations namenode;
  protected RemoteDaemonOperations jobtracker;


  @Override
  protected void setUp() {
    super.setUp()
    bindSSHKey()
    nnServer = rootServer(requiredSysprop(NN_SERVER))
    jtServer = rootServer(requiredSysprop(JT_SERVER))
    namenode = new RemoteDaemonOperations(nnServer, "namenode")
    jobtracker = new RemoteDaemonOperations(jtServer, "jobtracker")
  }

  Configuration createBondedConfiguration() {
    Configuration conf = new Configuration()
    int nnport = Integer.parseInt(requiredSysprop(NN_SERVER_PORT))
    conf.set(DistributedFileSystem.FS_DEFAULT_NAME_KEY, "hfds://${nnServer.host}:$nnport")
    int jtport = Integer.parseInt(requiredSysprop(JT_SERVER_PORT))
    conf.set(HadoopKeys.MAPRED_JOB_TRACKER, "${jtServer.host}:$jtport")
    conf
  }

}
