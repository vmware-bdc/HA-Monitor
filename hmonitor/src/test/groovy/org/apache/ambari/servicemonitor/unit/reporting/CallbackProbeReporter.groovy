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

package org.apache.ambari.servicemonitor.unit.reporting

import groovy.util.logging.Commons
import org.apache.ambari.servicemonitor.probes.Probe
import org.apache.ambari.servicemonitor.reporting.ProbeFailedException
import org.apache.ambari.servicemonitor.reporting.ProbePhase
import org.apache.ambari.servicemonitor.reporting.ProbeStatus
import org.apache.ambari.servicemonitor.reporting.Reporter
import org.apache.ambari.servicemonitor.reporting.ReportingLoop

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 *
 */
@Commons
class CallbackProbeReporter extends Reporter {

  Closeable target
  private Closure statusUpdateCallback = {};
  private Closure probePhaseCallback = {};
  private Closure liveProbeCycleCompletedCallback = {};
  private Closure probeFailureCallback = this.&failureCallback
  private Closure pollingTimeoutCallback = this.&timeoutCallback

  ProbePhase phase;
  ProbeFailedException probeFailedException;
  boolean timeoutReceived;
  boolean testRunTimedOut
  ProbeStatus lastStatusUpdate
  long[] phaseEntered = new long[ProbePhase.PHASE_COUNT];

  CallbackProbeReporter(Closure statusUpdateCallback,
                        Closure pollingTimeoutCallback,
                        Closure probeFailureCallback,
                        Closure probePhaseCallback) {
    this()
    this.statusUpdateCallback = statusUpdateCallback
    this.pollingTimeoutCallback = pollingTimeoutCallback
    this.probeFailureCallback = probeFailureCallback
    this.probePhaseCallback = probePhaseCallback
  }

  CallbackProbeReporter() {
    super("TestingProbeReporter", false)
  }

  @Override
  public void probeProcessStateChange(ProbePhase probePhase) {
    super.probeProcessStateChange(probePhase)
    org.apache.ambari.servicemonitor.unit.reporting.CallbackProbeReporter.log.info("entering phase $probePhase")
    phase = probePhase
    phaseEntered[probePhase.index] = System.currentTimeMillis();
    probePhaseCallback(probePhase)
  }

  @Override
  void heartbeat(ProbeStatus status) {
    super.heartbeat(status)
    org.apache.ambari.servicemonitor.unit.reporting.CallbackProbeReporter.log.debug("heartbeat: $status")
    lastStatusUpdate = status
    statusUpdateCallback(status)
  }

  @Override
  void probeTimedOut(ProbePhase currentPhase, Probe probe, ProbeStatus lastStatus, long currentTime) {
    super.probeTimedOut(currentPhase, probe, lastStatus, currentTime)
    org.apache.ambari.servicemonitor.unit.reporting.CallbackProbeReporter.log.info("Probe timeout in phase $currentPhase; last status : $probe")
    timeoutReceived = true;
    pollingTimeoutCallback(lastStatus)
  }

  @Override
  void probeFailure(ProbeFailedException exception) {
    super.probeFailure(exception)
    if (exception.status) {
      heartbeat(exception.status)
    }
    org.apache.ambari.servicemonitor.unit.reporting.CallbackProbeReporter.log.info("failure : $exception", exception)
    probeFailedException = exception
    probeFailureCallback(exception)
  }

  @Override
  void probeResult(ProbePhase phase, ProbeStatus status) {
    org.apache.ambari.servicemonitor.unit.reporting.CallbackProbeReporter.log.info("probeResult: $status")
    lastStatusUpdate = status

    super.probeResult(phase, status)
  }



  @Override
  public void liveProbeCycleCompleted() {
    liveProbeCycleCompletedCallback()
  }

  /**
   * Close the target. This is here as the default handler for failures and timeouts
   */
  def closeTarget() {
    target?.close()
  }

  def failureCallback(def param) {
    closeTarget()
  }

  def timeoutCallback(def param) {
    closeTarget()
  }

  def timeout() {
    org.apache.ambari.servicemonitor.unit.reporting.CallbackProbeReporter.log.warn("Test timeout");
    testRunTimedOut = true;
    closeTarget();
  }

  def assertTestDidNotTimeout() {
    assert !testRunTimedOut
  }

  def run(ReportingLoop reportingLoop, int timeout) {
    target = reportingLoop;

    ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1);
    scheduler.schedule((Runnable) this.&timeout, timeout, TimeUnit.MILLISECONDS)
    reportingLoop.run()
    assertTestDidNotTimeout();
  }

  boolean phaseWasEntered(ProbePhase phase) {
    phaseEntered[phase.index] > 0
  }

  static def exitReporting() {
    throw new ExitReportingException();
  }
}
