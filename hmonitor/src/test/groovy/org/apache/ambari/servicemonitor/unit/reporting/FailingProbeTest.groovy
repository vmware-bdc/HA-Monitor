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
import org.apache.ambari.servicemonitor.reporting.ReportingLoop

/**
 * Test the base loop logic
 */
class FailingProbeTest extends BaseReportingTestCase {


  FailingProbeTest() {
  }

  void testFailingProbeFails() {
    //the probe is set to fail after bootstrapping
    MockProbe probe = new MockProbe(name: "testFailingProbeFails",
                                    failAfterInterval: 2000)



    ReportingLoop reportingLoop = createReportingLoop([probe],
                                                      [])

    //report expects a failure, which then kills the loop
    CallbackProbeReporter tpr = new CallbackProbeReporter(
        pollingTimeoutCallback: {reportingLoop.close()},
        probeFailureCallback: {reportingLoop.close()})

    exec(reportingLoop, tpr)
    assert !tpr.timeoutReceived
    assert !tpr.lastStatusUpdate.success
    assert tpr.lastStatusUpdate.probePhase == ProbePhase.LIVE
  }


}
