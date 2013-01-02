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

package org.apache.ambari.servicemonitor.reporting.vsphere;

import org.apache.ambari.servicemonitor.probes.Probe;
import org.apache.ambari.servicemonitor.reporting.ProbePhase;
import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.ambari.servicemonitor.reporting.Reporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Does the reporting to VSphere
 */
public class VSphereReporter extends Reporter {
  private static final Log LOG = LogFactory.getLog(VSphereReporter.class);

  private final VMGuestApi guestAPI = new VMGuestApi();
  private boolean timeoutReported = false;


  public VSphereReporter() {
    super("vSphere");
  }

  private void logOutcome(String operation, int status) {
    if (LOG.isInfoEnabled()) {
      LOG.info("operation=\"" + operation + "\" outcome=\"" + VMGuestApi.getStatusCodeText(status) + "\"");
    }
  }

  /**
   * Bond to the monitoring service, set the enabled flag
   *
   *
   * @param name service name
   * @param description
   * @return true iff the monitoring is enabled
   */
  @Override
  public boolean start(String name, String description) {
    super.start(name, description);
    LOG.info("Starting reporting " + name + " -- " + description);
    enableMonitoring();
    //do an immediate verify/renew just to print out what is going on before
    //a single heartbeat is raised.
    verifyAndRenewMonitoring();
    return isMonitoringEnabled();
  }

  private boolean enableMonitoring() {
    int outcome = guestAPI.enable();
    logOutcome("Enabling guest monitoring for " + getServiceName(), outcome);
    if (outcome != 0) {
      //no monitoring live, fail
      return false;
    }
    return isMonitoringEnabled();
  }

  @Override
  public void unregister() {
    if (isMonitoringEnabled()) {
      logOutcome("Disabling Monitoring", guestAPI.disable());
    }
  }

  public boolean isMonitoringEnabled() {
    return guestAPI.isEnabled() != 0;
  }

  @Override
  public boolean isIntegratedWithHAMonitoringSystem() {
    return true;
  }

  private void verifyAndRenewMonitoring() {
    boolean renewMonitoring = false;
    String monitoringStatus = guestAPI.getMonitoringStatus();
    if (VMGuestApi.APP_STATUS_GREEN.equals(monitoringStatus)) {
      //all is well
      LOG.debug("VMWare monitoring is green");
      renewMonitoring = false;
    } else if (VMGuestApi.APP_STATUS_RED.equals(monitoringStatus)) {
      //trouble: VM should kill this, but...
      LOG.error("Monitoring status is red -Monitoring not live.");
      renewMonitoring = true;
    } else if (VMGuestApi.APP_STATUS_GRAY.equals(monitoringStatus)) {
      //VM unhooked
      LOG.warn("Monitoring has stopped, restarting");
      renewMonitoring = true;
    } else {
      LOG.warn("Unknown monitoring status");
    }
    if (renewMonitoring) {
      enableMonitoring();
    }
  }

  /**
   * Heartbeat process. Push out to vSphere, and choose to continue monitoring
   * if and only if: 1. the probe was a success. 2. the VMWare API calls all
   * worked.
   *
   * @param status the received probe status
   */
  @Override
  public void heartbeat(ProbeStatus status) {
    if (status.isSuccess()) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Sending Heartbeat " + status);
      }
      int outcome = guestAPI.markActive();
      if (outcome != 0) {
        //failure to monitor
        LOG.warn("Heartbeat failure: returned status of " + outcome
                 + ": " + VMGuestApi.getStatusCodeText(outcome));
      }
    } else {
      //probe failed: do not send a heartbeat
    }
    //reset the timeout monitor
    timeoutReported = false;
    //now check monitoring is live
    verifyAndRenewMonitoring();
  }

  @Override
  public void probeTimedOut(ProbePhase currentPhase, Probe probe,
                            ProbeStatus lastStatus,
                            long currentTime) {
    if (!timeoutReported) {
      timeoutReported = true;
      LOG.warn(getProbeTimeoutMessage(probe, lastStatus, currentTime) + " -- heartbeats suspended");
    }
  }

  @Override
  public void probeProcessStateChange(ProbePhase probePhase) {
    super.probeProcessStateChange(probePhase);
    LOG.info("Probing process changed phase=\"" + probePhase + "\"");
  }

  @Override
  public void probeBooted(ProbeStatus status) {
    LOG.info("Probe booted : " + status);
  }

}
