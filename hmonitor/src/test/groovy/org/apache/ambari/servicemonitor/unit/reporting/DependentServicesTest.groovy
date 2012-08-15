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
import org.apache.ambari.servicemonitor.reporting.ReportingLoop

/**
 * Test the dependency logic
 */
class DependentServicesTest extends BaseReportingTestCase {


  DependentServicesTest() {
  }

  /**
   * This dependency succeeds immediately
   */
  public void testMockDependencySucceeds() {
    MockProbe probe = successProbe("depends")
    ReportingLoop reportingLoop = createReportingLoop(
        [successProbe("live")],
        [probe])

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {
          if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()
        },
        pollingTimeoutCallback: {reportingLoop.close()})
    reportingLoop.reporter = tpr
    reportingLoop.run()

    assert tpr.phaseWasEntered(ProbePhase.DEPENDENCY_CHECKING)
    assert tpr.phaseWasEntered(ProbePhase.LIVE)
  }

  public void testDependencyFirst() {
    MockProbe depends = successProbe("depends")
    ProbeStatus depStat = null
    MockProbe live = successProbe("live")
    ReportingLoop reportingLoop = createReportingLoop(
        [live],
        [depends])

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {
          if (it.inPhase(ProbePhase.DEPENDENCY_CHECKING)) depStat = it;
          if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()
        },
        pollingTimeoutCallback: {reportingLoop.close()})
    reportingLoop.reporter = tpr
    reportingLoop.run()

    //now assert that the probe is the depends one
    ProbeStatus event = tpr.lastStatusUpdate
    assert event.originator == live
    assert event.probePhase == ProbePhase.LIVE
    assert depStat != null;
    assert depStat.originator == depends
  }


  public void testDependencyTimeIndependentOfOtherTimeouts() {
    //create a failing probe w/ 2s worth of fail
    MockProbe probe = failingProbe("depTime")
    probe.failForInterval = 2000
    ReportingLoop reportingLoop = createReportingLoop(
        [successProbe("live")],
        [probe])

    //report expects a timeout, which then kills the loop
    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {
          if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()
        },
        pollingTimeoutCallback: {reportingLoop.close()})
    long time = exec(reportingLoop, tpr)
    assert tpr.phaseWasEntered(ProbePhase.LIVE)

    //it should have take 2s or longer, not the normal timeouts
    assert time >= 2000
  }


}
