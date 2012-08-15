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
import org.apache.ambari.servicemonitor.utils.ExitMainException;
import org.apache.ambari.servicemonitor.utils.FindAndPingPid;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Probe that looks for a live process
 */
public class PidLiveProbe extends Probe {
  private static final Log LOG = LogFactory.getLog(PidLiveProbe.class);

  private final String pidPath;
  private final FindAndPingPid pidPinger;

  public PidLiveProbe(File pidFile, Configuration conf) {
    super("Find pid ", conf);
    pidPinger = new FindAndPingPid(pidFile);
    pidPath = pidPinger.getPidPath();
    setName("Find Pid file at \"" + pidPath + "\"");
  }

  @Override
  public ProbeStatus ping(boolean livePing) {
    ProbeStatus status = new ProbeStatus();
    try {
      if (LOG.isDebugEnabled()) {
      }
      LOG.debug("Probing process at " + pidPath);
      boolean live = pidPinger.ping();
      if (live) {
        status.succeed(this);
      } else {
        //process is not live, no obvious reason why not
        if (LOG.isDebugEnabled()) {
          LOG.debug("Ping of " + pidPath + " returned false");
        }
        status.fail(this, null);
      }
    } catch (FileNotFoundException e) {
      //pid file is not there yet
      String error = "Probe " + pidPath + " failed as the file is missing: " + e;
      LOG.debug(error, e);
      status.fail(this,
                  new IOException(error, e));

    } catch (IOException e) {
      String error = "Probe " + pidPath + " failed: " + e;
      LOG.debug(error, e);
      status.fail(this,
                  new IOException(error, e));

    }
    return status;
  }

  public static PidLiveProbe createProbe(Configuration conf) throws ExitMainException {
    String pidpath = conf.get(PID_PROBE_PIDFILE, "");
    if (pidpath.isEmpty()) {
      throw new ExitMainException("Required property not set: " + PID_PROBE_PIDFILE);
    }
    File pidfile = new File(pidpath);
    return new PidLiveProbe(pidfile, new Configuration(conf));
  }

}
