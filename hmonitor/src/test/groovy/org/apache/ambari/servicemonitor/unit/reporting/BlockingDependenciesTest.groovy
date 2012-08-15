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
 * Test the dependency logic
 */
class BlockingDependenciesTest extends BaseReportingTestCase {

  public void testDependencyBlockTimeIndependentOfOtherTimeouts() {
    //create a failing probe w/ 2s worth of fail
    MockProbe probe = new MockProbe(name: "depBlockTime",
                                    blockAfterInterval: 0,
                                    blockForInterval: 2000,
                                    blockDuration: 2000)
    ReportingLoop reportingLoop = createReportingLoop(
        [successProbe("live")],
        [probe])

    boolean wentLive = false;
    //report to enter the live phase and exit
    CallbackProbeReporter tpr = new CallbackProbeReporter(
        statusUpdateCallback: {
          if (it.inPhase(ProbePhase.LIVE)) {
            wentLive = true
            reportingLoop.close()
          }
        },
        pollingTimeoutCallback: {reportingLoop.close()})
    long time = exec(reportingLoop, tpr)
    //no timeout
    assert !tpr.timeoutReceived
    //live was entered
    assert tpr.phaseWasEntered(ProbePhase.LIVE)
    //it should have take 2s or longer
    assert time >= 2000
  }

}
