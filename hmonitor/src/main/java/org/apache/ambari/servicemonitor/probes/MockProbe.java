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

package org.apache.ambari.servicemonitor.probes;

import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.ambari.servicemonitor.utils.MonitorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;


/**
 * The basic lifecycle of this probe is that it is always live, except when 
 * you say otherwise. 
 * If in the block phase: it blocks for specified duration on every call.
 * If in the fail phase: fails.
 *
 * Otherwise: success.
 *
 * Bootstrap timeout logic is handled in the parent class, and is independent
 * of this behavior.
 *
 * Safe mode entry/exit is just a boolean that can be manipulated.
 */
public class MockProbe extends Probe implements SafeModeCheck {
  private static final Log LOG = LogFactory.getLog(MockProbe.class);

  private long startTime;
  public long failAfterInterval = -1;
  public long failForInterval = -1;
  public long fail2AfterInterval = -1;
  public long fail2ForInterval = -1;
  public long blockAfterInterval = -1;
  public long blockForInterval = 0;
  public long blockDuration = 0;
  public boolean lastOutcome = false;
  public boolean invoked = false;
  public boolean inSafeMode;
  public boolean enteredBootstrap;
  public boolean exitedBootstrap;


  public MockProbe() {
    this("Mock");
  }

  public MockProbe(String name) {
    super(name, new Configuration());
    startTime = now();
  }

  @Override
  public String getName() {
    return super.getName()
           + " failafter:" + failAfterInterval
           + " failfor:" + failForInterval
           + " fail2after:" + fail2AfterInterval
           + " fail2for:" + fail2ForInterval
           + " blockAfter:" + blockAfterInterval
           + " blockFor:" + blockForInterval
           + " blockDuration:" + blockDuration
           + " deployed for: " + MonitorUtils.millisToHumanTime(uptime())
           + " booted: " + isBooted()
      ;
  }

  @Override
  public boolean isInSafeMode() {
    return inSafeMode;
  }

  @Override
  public void beginBootstrap() {
    super.beginBootstrap();
    startTime = now();
    enteredBootstrap = true;
  }

  @Override
  public void endBootstrap() {
    super.endBootstrap();
    exitedBootstrap = true;
  }

  private boolean inState(long enterInterval, long forInterval) {
    if (enterInterval < 0) {
      return false;
    }
    long uptime = uptime();
    boolean entered = uptime >= enterInterval;
    boolean notExited = uptime <= (enterInterval + forInterval);
    boolean unlimited = forInterval < 0;
    boolean inState = entered && (notExited || unlimited);
    if (LOG.isDebugEnabled()) {
      //this debug detail is here to track down logic issues; retained
      LOG.debug("Probe " + getName() + " uptime " + uptime
                + " -entered: " + entered
                + " notExited: " + notExited
                + " unlimited: " + unlimited
                + " inState: " + inState);
    }

    return inState;
  }

  private long uptime() {
    return now() - startTime;
  }

  @Override
  public ProbeStatus ping(boolean livePing) {
    invoked = true;
    boolean failing = inState(failAfterInterval, failForInterval);
    failing |= inState(fail2AfterInterval, fail2ForInterval);
    boolean blocking = inState(blockAfterInterval, blockForInterval);
    if (blocking) {
      try {
        Thread.sleep(blockDuration);
      } catch (InterruptedException ignored) {

      }
    }
    ProbeStatus status = new ProbeStatus();
    lastOutcome = !failing;
    status.finish(this, !failing, this.getName(), null);
    return status;
  }
}
