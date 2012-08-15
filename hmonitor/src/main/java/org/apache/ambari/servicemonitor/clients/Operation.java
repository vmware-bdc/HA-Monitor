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

package org.apache.ambari.servicemonitor.clients;

import org.apache.ambari.servicemonitor.utils.MonitorUtils;

public final class Operation {

  public long started, finished;
  public final String name;
  public Outcome outcome;
  public Throwable thrown;
  public String text;

  public Operation(String name) {
    this.name = name;
    started = System.currentTimeMillis();
  }

  public void end(Outcome finalOutcome) {
    this.outcome = finalOutcome;
    finished = System.currentTimeMillis();
  }


  public void success() {
    end(Outcome.success);
  }

  public void success(String text) {
    this.text = text;
    end(Outcome.success);
  }

  public void failure(Throwable t) {
    thrown = t;
    end(Outcome.failure);
  }

  public void timeout(Throwable t) {
    thrown = t;
    end(Outcome.timeout);
  }

  public long duration() {
    return finished > 0 ? (finished - started) : 0;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return "Operation " + name + " "
           + (outcome != null ?
                (outcome.getText() + ": duration: " +
                 MonitorUtils.millisToHumanTime(duration())
                 + (thrown != null ? ("\n  " + thrown.toString()) : "")
                ) : "");
  }

  public boolean getSucceeded() {
    return outcome == Outcome.success;
  }

  public boolean getFailed() {
    return outcome == Outcome.failure;
  }

  public boolean timedout() {
    return outcome == Outcome.timeout;
  }

  public enum Outcome {
    success("success"),
    failure("failure"),
    timeout("timed out");

    private final String text;

    Outcome(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }
  }
}
