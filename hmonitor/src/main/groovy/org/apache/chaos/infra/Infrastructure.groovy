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

package org.apache.chaos.infra

import groovy.util.logging.Commons
import org.apache.chaos.exceptions.NoSuchVmException
import org.apache.chaos.exceptions.UnsupportedActionException
import org.apache.hadoop.conf.Configurable
import org.apache.hadoop.conf.Configuration

/**
 * Base class for infrastructure implementations
 */
@Commons
class Infrastructure implements Configurable, Closeable {


  Configuration conf

  String getName() { "infrastructure"}

  public static final int PID_NAMENODE = -1
  public static final int PID_JOBTRACKER = -2

  void init() throws IOException {

  }

  @Override
  void close() throws IOException {

  }

  /**
   * Locate a VM by name
   * @param vmName name of the vm
   * @return an infrastructure specific VM reference
   * @throws IOException IO problems
   * @throws NoSuchVmException if the VM is not found
   */
  VMReference locate(String vmName) throws IOException {
    return new VMReference(name: vmName)
  }

  /**
   * Kill a specific VM
   * @param vmRef reference to the VM, VM specific
   * @throws IOException
   */
  boolean PowerOffVM(VMReference vmRef) throws IOException {
    throw new UnsupportedActionException()
  }

  /**
   * Restart a specific VM
   * @param vmRef reference to the VM, VM specific
   * @throws IOException
   */
  boolean RestartVM(VMReference vmRef) throws IOException {
    throw new UnsupportedActionException()
  }

  /**
   * Restart a specific VM
   * @param vmRef reference to the VM, VM specific
   * @throws IOException
   */
  boolean startVM(VMReference vmRef) throws IOException {
    throw new UnsupportedActionException()
  }

  /**
   * Restart a specific process
   * @param vmRef reference to the VM, VM specific
   * @param processId pid or index to type of process
   * @param signal signal value
   * @return true if ps was found and killed
   * @throws IOException
   */
  boolean kill(VMReference vmRef, int processId, int signal) throws IOException {
    throw new UnsupportedActionException()
  }

  String pidToString(int pid) {
    switch (pid) {
      case PID_NAMENODE: return "namenode"
      case PID_JOBTRACKER: return "jobtracker"
      default: return "$pid"
    }
  }
}
