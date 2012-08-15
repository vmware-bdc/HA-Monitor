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
import org.apache.ambari.servicemonitor.reporting.ProbeStatus

/**
 * Test the base loop logic
 */
class MockProbeTest extends BaseReportingTestCase {


  MockProbeTest() {
  }


  public void testBlockingProbesTimesOut() {
    MockProbe probe = blockingProbe("testBlockingProbesTimesOut")
    probe.blockDuration = 500;

    def (time, ping) = time {
      probe.ping(true)
    }
    assert time >= probe.blockDuration
  }


  void testFailingProbeFails() {
    //a probe that should always fail
    MockProbe probe = failingProbe("testFailingProbeFails")
    start(probe)
    ProbeStatus ping = probe.ping(true);
    assert !ping.isSuccess()
  }

  def void start(MockProbe probe) {
    probe.init()
    probe.beginBootstrap()
  }


  void testDelayedFailingProbeSucceedsOnInitialBoot() {
    //a probe that should always fail
    MockProbe probe = failingProbe("testDelayedFailingProbeSucceedsOnInitialBoot")
    probe.failAfterInterval = 30000
    start(probe)
    ProbeStatus ping = probe.ping(true);
    assert ping.isSuccess()
  }

  void testFailingProbeFailsBoot() {
    //a probe that should always fail
    MockProbe probe = failingProbe("testFailingProbeFailsBoot")
    start(probe)
    ProbeStatus ping = probe.ping(false);
    assert !ping.isSuccess()
  }


}
