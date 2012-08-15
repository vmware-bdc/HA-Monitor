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

/**
 * Listener for operations events, these will be called from the thread running
 * the operation.
 *
 * Any of the "live" events are free to signal that the job should finish. This
 * is the only way to exit the run. Even so, long lived threads may have hung.
 */
public interface ClientEventListener {

  void message(String text) throws ExitClientRunException;

  void startedEvent(Operation operation) throws ExitClientRunException;

  void waitingEvent(Operation operation) throws ExitClientRunException;

  void finishedEvent(Operation operation) throws ExitClientRunException;

  void endListening();
}
