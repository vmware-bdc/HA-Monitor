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
import org.apache.ambari.servicemonitor.utils.Exit;
import org.apache.ambari.servicemonitor.utils.MonitorUtils;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;

public class Reporter implements ProbeReportHandler {


  protected String serviceName;

  protected volatile ProbeStatus lastProbe;
  protected boolean exitOnProbeFailure = true;


  public Reporter(String serviceName) {
    this.serviceName = serviceName;
  }

  public Reporter(String serviceName, boolean exitOnProbeFailure) {
    this.exitOnProbeFailure = exitOnProbeFailure;
    this.serviceName = serviceName;
  }

  /**
   * Initialization operatin, called before use to permit
   * late-binding configuration information to be read in; set-ups performed
   * @param conf configuration
   */
  public void init(Configuration conf) throws IOException {

  }

  @Override
  public boolean start(String name, String description) {
    serviceName = (serviceName != null ? (serviceName + ": ") : "") + name;
    return true;
  }

  /**
   * this triggers a clean shutdown of the register
   */
  @Override
  public void unregister() {

  }

  /**
   * Overrideable method to indicate whether or not system failure triggers
   * a VM shutdown.
   * @return the default: false
   */
  @Override
  public boolean isIntegratedWithHAMonitoringSystem() {
    return false;
  }

  /**
   * Probe failure. Default action: exit if {@link #exitOnProbeFailure is set}
   * @param exception the failure
   */
  @Override
  public void probeFailure(ProbeFailedException exception) {
    lastProbe = exception.status;
    if (exitOnProbeFailure) {
      Exit.exitOnProbeFailure(exception.status);
    }
  }

  /**
   * Relay to the (legacy) methods until they are pulled out
   * @param probePhase the new process phrase
   */
  @Override
  public void probeProcessStateChange(ProbePhase probePhase) {

  }


  @Override
  public void probeBooted(ProbeStatus status) {
  }

  @Override
  public void probeResult(ProbePhase phase, ProbeStatus status) {
    lastProbe = status;
  }

  /**
   * report the probe status. This is invoked 
   * by the reporting loop on a heartbeat
   *
   * @param status probe status
   */
  @Override
  public void heartbeat(ProbeStatus status) {
  }

  /**
   * Probing has timed out
   *
   * @param currentPhase
   * @param probe the probe that is active (and implictly, hung)
   * @param lastStatus the last probe status received
   * @param currentTime time that the timeout occurred
   */
  @Override
  public void probeTimedOut(ProbePhase currentPhase,
                            Probe probe,
                            ProbeStatus lastStatus,
                            long currentTime) {

  }


  @Override
  public void liveProbeCycleCompleted() {
    //do nothing
  }

  protected String getProbeTimeoutMessage(Probe probe,
                                          ProbeStatus lastStatus,
                                          long currentTime) {
    long lastSeen = currentTime - lastStatus.getTimestamp();
    return "Probe " + probe
           + " timeout -last event seen "
           + MonitorUtils.millisToHumanTime(lastSeen)
           + " seconds ago : " + lastStatus
      ;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Get the name of this service
   * @return the name given to this reporter at startup
   */


  public String getServiceName() {
    return serviceName;
  }

  @Override
  public String toString() {
    return "Reporter=\"" + serviceName + "\"";
  }
}
