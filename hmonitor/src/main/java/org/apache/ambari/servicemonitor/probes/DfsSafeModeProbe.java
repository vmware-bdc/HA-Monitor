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
import org.apache.ambari.servicemonitor.utils.DFSUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;

import java.io.IOException;
import java.net.URI;

/**
 * Safe mode probe can probe the safe mode status of an NN.
 * It can be set to not care if the system re-enters safe mode after startup,
 * in which case it only checks for safe mode in startup, and can be used
 * to assert that the filesystem exits safe mode after a given period of time.
 * <p/>
 * If set to ignore the safe mode status entirely, then the result of the 
 * safemode call is ignored completely -though errors to talk to the
 * filesystem are considered errors. 
 */
public class DfsSafeModeProbe extends Probe implements SafeModeCheck {

  private final boolean ignoreSafeModeManuallyTriggered;
  private boolean safeModeExitedOnce;
  protected volatile boolean inSafeMode;

  public DfsSafeModeProbe(Configuration conf,
                          boolean ignoreSafeModeManuallyTriggered) throws IOException {
    super("SafeModeProbe", conf);
    URI uri = DFSUtils.getHDFSUri(conf);
    setName("SafeModeProbe" + uri);
    this.ignoreSafeModeManuallyTriggered = ignoreSafeModeManuallyTriggered;

    DFSUtils.makeDfsCallsNonBlocking(conf);
  }

  @Override
  public boolean isInSafeMode() {
    return inSafeMode;
  }

  @Override
  public ProbeStatus ping(boolean livePing) {
    ProbeStatus status = new ProbeStatus();
    try {
      DistributedFileSystem hdfs = DFSUtils.createUncachedDFS(conf);
      inSafeMode = hdfs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_GET);
      boolean live = !inSafeMode;
      if (inSafeMode) {
        if (ignoreSafeModeManuallyTriggered) {
          live = safeModeExitedOnce;
        }
      } else {
        //it's easier to always set this on a live NN than it is to track whether there's just
        //been a state transition.
        safeModeExitedOnce = true;
      }
      status.succeed(this);
      status.setSuccess(live);
      status.setMessage(hdfs.getUri() + " up - safe mode state " + inSafeMode);
    } catch (IOException e) {
      status.fail(this, e);
    }
    return status;
  }

}
