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
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.mapred.AdminOperationsProtocol;
import org.apache.hadoop.mapred.JobTracker;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Lists a directory
 */
public class JTSafeModeProbe extends AbstractJTProbe implements SafeModeCheck {
  protected static final Log LOG = LogFactory.getLog(JTSafeModeProbe.class);
  protected boolean inSafeMode;

  public JTSafeModeProbe(Configuration conf) throws IOException {
    super("JTSafeModeProbe", conf);
  }


  @Override
  public void init() throws IOException {
    super.init();
    setName("JTSafeModeProbe probe against " + host + ":" + port);
  }

  @Override
  public boolean isInSafeMode() {
    return inSafeMode;
  }

  @Override
  public ProbeStatus ping(boolean livePing) {
    ProbeStatus status = new ProbeStatus();
    AdminOperationsProtocol jtAdmin = null;
    try {
      InetSocketAddress addr = MonitorUtils.getURIAddress(jturi);
      jtAdmin = MonitorUtils.createJTAdminProxy(addr, conf);
      inSafeMode = jtAdmin.setSafeMode(JobTracker.SafeModeAction.SAFEMODE_GET);
      status.succeed(this);
      status.setMessage(getName() + " is up -safe mode flag: " + inSafeMode);
    } catch (IOException e) {
      status.fail(this,
                  new IOException(getName() + " : " + e, e));
      LOG.debug("Failure to probe " + getName());
    } finally {
      RPC.stopProxy(jtAdmin);
    }
    return status;
  }

}
