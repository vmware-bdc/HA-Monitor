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
import org.apache.ambari.servicemonitor.remote.BaseRemoteHadoopTestCase
import org.apache.ambari.servicemonitor.reporting.ReportingLoop
import org.apache.chaos.remote.BaseRemoteTestCase

class BaseReportingTestCase extends BaseRemoteTestCase {

  /**
   * Create a basic reporting loop -without any reporter; that  must be pushed in later
   * @param monitorProbes list of monitor probes
   * @param dependencies list of live probes
   * @return the reporting loop
   */
  public static final int REPORTING_LOOP_TIMEOUT = 30000

  def ReportingLoop createReportingLoop(ArrayList monitorProbes, ArrayList dependencies) {
    ReportingLoop reportingLoop = new ReportingLoop("test",
                                                    null,
                                                    monitorProbes,
                                                    dependencies,
                                                    100, //probe interval
                                                    100,  //report interval
                                                    1000, //probe timeout
                                                    1000) //boot timeout
    reportingLoop
  }

  def ReportingLoop createReportingLoop(ArrayList monitorProbes, ArrayList dependencies,
                                        int probeTimeout, int bootTimeout) {
    ReportingLoop reportingLoop = new ReportingLoop("test",
                                                    null,
                                                    monitorProbes,
                                                    dependencies,
                                                    100, //probe interval
                                                    100,  //report interval
                                                    probeTimeout, //probe timeout
                                                    bootTimeout) //boot timeout
    reportingLoop
  }


  def long exec(ReportingLoop reportingLoop, CallbackProbeReporter tpr) {
    long started = now()
    reportingLoop.reporter = tpr
    tpr.run(reportingLoop, REPORTING_LOOP_TIMEOUT)
    return now() - started
  }

  MockProbe failingProbe(String name) {
    new MockProbe(name: name, failAfterInterval: 0)
  }

  long now() { System.currentTimeMillis() }

  def time(Closure closure) {
    long start = now();
    def result = closure()
    long finish = now();
    [finish - start, result]
  }

  /**
   * A blocking probe -though it exits after 30s anyway so tests fail more often
   * @return
   */
  MockProbe blockingProbe(String name) {
    new MockProbe(name: name,
                  blockAfterInterval: 0,
                  blockForInterval: (1000 * 15),
                  blockDuration: (1000 * 15))
  }

  MockProbe successProbe(String name) {
    new MockProbe(name)
  }

}
