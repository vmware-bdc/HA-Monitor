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
 * Test the base loop logic
 */
class BasicLoopTest extends BaseReportingTestCase {


  BasicLoopTest() {
  }

  /**
   * Not sure this is the ideal behaviour, but, well, don't do this.
   */
  public void testNoProbesTimesOut() {
    ReportingLoop reportingLoop = createReportingLoop([], [])

    //report expects a timeout, which then kills the loop
    CallbackProbeReporter tpr = new CallbackProbeReporter({},
                                                          {reportingLoop.close()},
                                                          {reportingLoop.close()},
                                                          {})
    exec(reportingLoop, tpr)
    assert tpr.timeoutReceived
  }

  void testSuccessProbeSucceeds() {
    ReportingLoop reportingLoop = createReportingLoop([new MockProbe("testSuccessProbeSucceeds")],
                                                      [])

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        {if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()},
        {},
        {},
        {})
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert update.success && update.inPhase(ProbePhase.LIVE)
  }

  void testNoBootTimeoutProbeSucceeds() {
    ReportingLoop reportingLoop = createReportingLoop([new MockProbe("testSuccessProbeSucceeds")],
                                                      [], 1000, 0)

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        {if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()},
        {},
        {},
        {})
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert update.success && update.inPhase(ProbePhase.LIVE)
  }


  void testDualSuccessProbes() {
    MockProbe probe1 = new MockProbe("probe1")
    MockProbe probe2 = new MockProbe("probe2")
    ReportingLoop reportingLoop = createReportingLoop([probe1, probe2],
                                                      [])

    CallbackProbeReporter tpr = new CallbackProbeReporter(
        {if (it.inPhase(ProbePhase.LIVE)) reportingLoop.close()},
        {},
        {},
        {})
    exec(reportingLoop, tpr)
    ProbeStatus update = tpr.lastStatusUpdate
    assert update.success
    assert update.inPhase(ProbePhase.LIVE)
    assert probe1.exitedBootstrap
    assert probe2.exitedBootstrap
  }

}
