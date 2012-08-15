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

package org.apache.ambari.servicemonitor.clients.gui

import groovy.util.logging.Commons
import org.apache.ambari.servicemonitor.probes.Probe
import org.apache.ambari.servicemonitor.reporting.ProbeFailedException
import org.apache.ambari.servicemonitor.reporting.ProbePhase
import org.apache.ambari.servicemonitor.reporting.ProbeStatus
import org.apache.ambari.servicemonitor.reporting.Reporter

/**
 *
 */
@Commons
class HamProbeReporter extends Reporter {

  private Ham owner;
  private Closure statusUpdateCallback;
  private Closure pollingTimeoutCallback


  HamProbeReporter(String serviceName, Closure statusUpdateCallback, Closure pollingTimeoutCallback) {
    super(serviceName, false)
    this.owner = owner;
    this.statusUpdateCallback = statusUpdateCallback
    this.pollingTimeoutCallback = pollingTimeoutCallback

    //comment the next line out if you want to verify that a probe failure
    //is reported all the way up to the reporter -and that it gets to choose whether
    //or not to exit the process
    this.exitOnProbeFailure = false
  }

  @Override
  public void probeProcessStateChange(ProbePhase probePhase) {
    log.info("Probing process changed state to $probePhase");
  }

  def event(ProbeStatus status) {
    statusUpdateCallback(status)
  }

  @Override
  void heartbeat(ProbeStatus status) {
    event(status)
  }

  @Override
  void probeTimedOut(ProbePhase currentPhase, Probe probe, ProbeStatus lastStatus, long currentTime) {
    log.warn(getProbeTimeoutMessage(probe, lastStatus, currentTime));
    pollingTimeoutCallback(lastStatus)
  }

  @Override
  void probeFailure(ProbeFailedException exception) {
    log.warn("Probe failure :" + exception, exception)
    event(exception.status)
    super.probeFailure(exception)
  }

  @Override
  void probeResult(ProbePhase phase, ProbeStatus status) {
    super.probeResult(phase, status)
    event(status)
  }
}
