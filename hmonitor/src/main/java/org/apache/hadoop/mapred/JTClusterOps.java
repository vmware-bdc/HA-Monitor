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

package org.apache.hadoop.mapred;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Class pushed into mapred to deal with scope issues on RPC interfaces.
 * While JobClient can do most of this, it's layers of indirection could hide
 * things that the status probes want.
 */
public class JTClusterOps implements Closeable {


  private JobSubmissionProtocol jobAPI;


  public void connect(InetSocketAddress addr,
                      Configuration conf) throws
                                          IOException {
    jobAPI = (JobSubmissionProtocol) RPC.getProxy(JobSubmissionProtocol.class,
                                                  JobSubmissionProtocol.versionID,
                                                  addr,
                                                  UserGroupInformation
                                                    .getCurrentUser(),
                                                  conf,
                                                  NetUtils.getSocketFactory(
                                                    conf,
                                                    JobSubmissionProtocol.class));
  }


  @Override
  public synchronized void close() throws IOException {
    RPC.stopProxy(jobAPI);
    jobAPI = null;
  }

  /**
   * Get the current status of the cluster
   * @param detailed if true then report tracker names and memory usage
   * @return summary of the state of the cluster
   */
  public ClusterStatus getClusterStatus(boolean detailed) throws IOException {
    return jobAPI.getClusterStatus(detailed);
  }

}
