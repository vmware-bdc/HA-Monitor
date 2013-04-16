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

package org.apache.ambari.servicemonitor.remote.namenode

import org.apache.ambari.servicemonitor.remote.BaseRemoteHadoopTestCase
import org.apache.ambari.servicemonitor.utils.DFSUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.hadoop.hdfs.protocol.HdfsConstants
import org.junit.Test

/**
 *
 */
class NNLifecycleTest extends BaseRemoteHadoopTestCase {

  @Test
  public void testNamenodeStartStatusStop() throws Throwable {
    if (!remoteTestsEnabled) return
    namenodeActions.start()
    namenodeActions.status()
    namenodeActions.stop()
  }

  @Test
  public void testNamenodeIsStarted() throws Throwable {
    if (!remoteTestsEnabled) return
    namenodeActions.start()
    Configuration conf = createBondedConfiguration()
    DistributedFileSystem dfs = DFSUtils.createUncachedDFS(conf)
    while (dfs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_GET)) {
      sleep(500)
    }
    //here the system is out of safe mode, all is well
  }


}
