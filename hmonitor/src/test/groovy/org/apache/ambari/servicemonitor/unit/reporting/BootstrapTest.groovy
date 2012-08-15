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

import org.apache.ambari.servicemonitor.probes.MockProbe
import org.apache.ambari.servicemonitor.reporting.ProbePhase
import org.apache.ambari.servicemonitor.reporting.ProbeStatus
import org.apache.ambari.servicemonitor.reporting.ProbeWorker
import org.apache.ambari.servicemonitor.reporting.ReportingLoop

/**
 * Test the base loop logic
 */
class BootstrapTest extends BaseReportingTestCase {


  BootstrapTest() {
  }

/**
 * a failing bootstrap probe may be converted into a timeout
 */
  void testFailingProbeFailsToBoot() {
    //a probe that should always fail
    MockProbe probe = failingProbe("testFailingProbeFails")


    ReportingLoop reportingLoop = createReportingLoop([probe],
                                                      [])

    //report expects a timeout, which then kills the loop
    CallbackProbeReporter tpr = new CallbackProbeReporter(
        pollingTimeoutCallback: {},
        probeFailureCallback: {reportingLoop.close()})

    exec(reportingLoop, tpr)
    assert !tpr.phaseWasEntered(ProbePhase.LIVE)
    assert !tpr.lastStatusUpdate.success
    assert !probe.booted

  }

  /**
   * A booting probe
   */
  void testSlowBootProbeSucceeds() {

    MockProbe probe = new MockProbe(name: "testSlowBootProbeSucceeds",
                                    failAfterInterval: 0,
                                    failForInterval: 500)

    ReportingLoop reportingLoop = createReportingLoop([probe],
                                                      [])

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        liveProbeCycleCompletedCallback: {reportingLoop.close()}
    )
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert update.success && update.inPhase(ProbePhase.LIVE)
    assert probe.booted
  }

  /**
   * A booting probe
   */
  void testSlowBootAndDependency() {

    MockProbe live = new MockProbe(name: "testSlowBootAndDependency-LIVE",
                                   failAfterInterval: 0,
                                   failForInterval: 500)

    MockProbe depends = new MockProbe(name: "testSlowBootAndDependency-DEPENDS",
                                      failAfterInterval: 0,
                                      failForInterval: 500)

    ReportingLoop reportingLoop = createReportingLoop([live],
                                                      [depends])
    boolean nowlive = false;
    CallbackProbeReporter tpr = new CallbackProbeReporter(
        probePhaseCallback: {phase ->
          if (phase == ProbePhase.LIVE) {
            nowlive = true;
            reportingLoop.close()
          }
        }
    )
    exec(reportingLoop, tpr)
    assert live.enteredBootstrap;
    assert live.exitedBootstrap;
    ProbeStatus update = tpr.lastStatusUpdate
    assert update.success && update.inPhase(ProbePhase.BOOTSTRAPPING)
    assert depends.invoked
    assert live.invoked
    assert nowlive
  }


  void testProbe2() {

    MockProbe p1 = new MockProbe(name: "p1")
    MockProbe p2 = new MockProbe(name: "p2")

    ReportingLoop reportingLoop = createReportingLoop([p1, p2],
                                                      [],
                                                      1000,
                                                      2000)
    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()})
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert update.success
    assert update.inPhase(ProbePhase.LIVE)
    assert p1.booted
    assert p2.booted
  }


  void testProbe2Fails() {

    MockProbe p1 = new MockProbe(name: "p1")
    MockProbe p2 = new MockProbe(name: "p2",
                                 failAfterInterval: 0,
                                 failForInterval: 5000)

    ReportingLoop reportingLoop = createReportingLoop([p1, p2],
                                                      [],
                                                      1000,
                                                      2000)

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()})
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert !update.success
    assert update.inPhase(ProbePhase.BOOTSTRAPPING)
    assert tpr.probeFailedException
    assert !p2.booted
    assert p1.booted
  }

  void testProbe2Failsafter() {

    MockProbe p1 = new MockProbe(name: "p1",
                                 failAfterInterval: 0,
                                 failForInterval: 5000)
    MockProbe p2 = new MockProbe(name: "p2",
                                 failAfterInterval: 500,
                                 failForInterval: 5000)

    ReportingLoop reportingLoop = createReportingLoop([p1, p2],
                                                      [])

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()})
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert !update.success
    assert update.inPhase(ProbePhase.BOOTSTRAPPING)
    //assert tpr.probeFailedException
    assert p2.booted
    assert !p1.booted
  }

  void testProbe2LiveThenFailsafter() {

    //stays down for 10s
    MockProbe p1 = new MockProbe(name: "p1",
                                 failAfterInterval: 0,
                                 failForInterval: 10000)

    //down for 1s, then up for 2s, then fails again
    MockProbe p2 = new MockProbe(name: "p2",
                                 failAfterInterval: 0,
                                 failForInterval: 300,
                                 fail2AfterInterval: 600,
                                 fail2ForInterval: 5000
    )

    ReportingLoop reportingLoop = createReportingLoop([p1, p2],
                                                      [],
                                                      1000,
                                                      3000)

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()})
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert !update.success
    assert update.inPhase(ProbePhase.BOOTSTRAPPING)
    assert tpr.probeFailedException

    assert tpr.lastStatusUpdate
    assert tpr.lastStatusUpdate.originator == p2

    assert p2.booted
    assert !p1.booted
    assert tpr.probeFailedException.message.contains(ProbeWorker.FAILURE_OF_A_LIVE_PROBE_DURING_BOOTSTRAPPING)
  }

}
