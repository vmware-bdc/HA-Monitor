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

package org.apache.ambari.servicemonitor.reporting;

import org.apache.ambari.servicemonitor.probes.Probe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class LogReporter extends Reporter {

  private static final Log LOG = LogFactory.getLog(LogReporter.class);

  public LogReporter() {
    super("Log", true);
  }

  @Override
  public boolean start(String name, String description) {
    super.start(name, description);
    LOG.info("Started reporting " + name + " -- " + description);
    return true;
  }

  @Override
  public void unregister() {
    LOG.info("Stopped reporting " + serviceName);
  }

  @Override
  public void heartbeat(ProbeStatus status) {
    LOG.info(serviceName + ": " + status);
  }

  @Override
  public void probeProcessStateChange(ProbePhase probePhase) {
    LOG.info("Probing process changed phase=" + probePhase + "\"");
  }


  @Override
  public void probeBooted(ProbeStatus status) {
    LOG.info("Probe booted : " + status);
  }

  @Override
  public void probeFailure(ProbeFailedException exception) {
    LOG.warn("Probe failure: " + exception);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Probe " + exception.status, exception);
    }
    super.probeFailure(exception);
  }

  @Override
  public void probeTimedOut(ProbePhase currentPhase, Probe probe,
                            ProbeStatus lastStatus,
                            long currentTime) {
    LOG.error(getProbeTimeoutMessage(probe, lastStatus, currentTime));
    super.probeTimedOut(currentPhase, probe, lastStatus, currentTime);
  }
}
