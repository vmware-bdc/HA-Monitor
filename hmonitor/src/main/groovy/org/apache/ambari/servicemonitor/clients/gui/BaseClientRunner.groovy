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

package org.apache.ambari.servicemonitor.clients.gui

import groovy.util.logging.Commons
import org.apache.ambari.servicemonitor.clients.BaseClient
import org.apache.ambari.servicemonitor.clients.ClientEventListener
import org.apache.ambari.servicemonitor.clients.ExitClientRunException
import org.apache.ambari.servicemonitor.clients.Operation
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus
import org.apache.hadoop.conf.Configuration

import java.util.concurrent.atomic.AtomicBoolean

/**
 * This runnable can run any of the {@link BaseClient} actions, and support
 * interruptions as well as relaying events to the GUI instance passed in
 * to the constructor.
 */
@Commons
class BaseClientRunner implements Runnable, ClientEventListener {

  private final Ham gui;
  private final BaseClient action;
  private final String[] commands
  private final Configuration conf;
  private final AtomicBoolean shouldExit = new AtomicBoolean()
  private Thread runningThread;


  BaseClientRunner(Ham gui, BaseClient action, Configuration conf, String... commands) {
    this.gui = gui
    this.action = action
    this.conf = conf;
    this.commands = commands
    action.setListener(this)
  }

  public void setShouldExit(boolean value) {
    shouldExit.set(value)
    runningThread?.interrupt()
  }

  public void maybeExit() {
    if (shouldExit.get()) {
      throw new ExitClientRunException();
    }
  }

  BaseClient getAction() {
    return action
  }

  @Override
  void run() {
    assert runningThread == null
    runningThread = Thread.currentThread()
    try {
      ToolRunnerPlus.exec(conf, action, commands)
    } catch (e) {
      log.warn(e.toString(), e);
      gui.endListening(this)
    } finally {
      runningThread = null
    }
  }

  @Override
  void message(String text) {
    log.info(text)
    gui.message(this, text)
    maybeExit()
  }

  @Override
  void startedEvent(Operation operation) {
    gui.startedEvent(this, operation)
    maybeExit()
  }

  @Override
  void waitingEvent(Operation operation) {
    gui.waitingEvent(this, operation)
    maybeExit()

  }

  @Override
  void finishedEvent(Operation operation) {
    gui.finishedEvent(this, operation)
    maybeExit()
  }

  @Override
  void endListening() {
    gui.endListening(this)

  }
}
