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

package org.apache.ambari.servicemonitor.probes;

import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.ambari.servicemonitor.utils.MonitorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.mapred.ClusterStatus;
import org.apache.hadoop.mapred.JTClusterOps;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * This probe asks for the cluster status of a JT cluster.
 * It considers a ping() call to succeed if the IPC call
 * succeeds
 */
public class JTClusterStatusProbe extends AbstractJTProbe {
  protected static final Log LOG =
    LogFactory.getLog(JTClusterStatusProbe.class);
  protected ClusterStatus clusterStatus;

  public JTClusterStatusProbe(Configuration conf) throws IOException {
    super("Job Tracker Probe ", conf);
  }


  @Override
  public void init() throws IOException {
    super.init();
    setName("Job Tracker Probe " + host + ":" + port);
    LOG.info(getName());
  }


  public ClusterStatus getClusterStatus() {
    return clusterStatus;
  }

  /**
   * Make a {@link JTClusterOps#getClusterStatus(boolean)} call with 
   * the param set to fals (non detailed)
   * @param livePing is the ping live: true for live; false for boot time
   * @return true iff the cluster status operation returns.
   */
  @Override
  public ProbeStatus ping(boolean livePing) {
    ProbeStatus status = new ProbeStatus();
    JTClusterOps clusterOps = new JTClusterOps();
    try {
      InetSocketAddress addr = MonitorUtils.getURIAddress(jturi);
      clusterOps.connect(addr, conf);
      clusterStatus = clusterOps.getClusterStatus(false);
      if (LOG.isDebugEnabled()) {
        LOG.debug("JT state = " + clusterStatus.getJobTrackerStatus());
        LOG.debug("Active trackers = " + clusterStatus.getTaskTrackers());
        LOG.debug(
          "Blacklisted trackers = " + clusterStatus.getBlacklistedTrackers());
      }
      status.succeed(this);
      status.setMessage(
        getName() + " is in state " + clusterStatus.getJobTrackerStatus());
    } catch (IOException e) {
      status.fail(this,
                  new IOException(getName() + " : " + e, e));
      LOG.debug("Failure to probe " + getName());
    } finally {
      IOUtils.cleanup(LOG, clusterOps);
    }
    return status;
  }

}
