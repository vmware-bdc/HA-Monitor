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

package org.apache.chaos.infra.manual

import org.apache.chaos.infra.Infrastructure
import org.apache.chaos.infra.VMReference

import javax.swing.JOptionPane

/**
 *
 */
class ManualInfrastructure extends Infrastructure {

  private void msgBox(String text) {
    JOptionPane.showMessageDialog(null, text);
  }

  @Override
  String getName() {
    return "Manual Infrastructure"
  }

  @Override
  VMReference locate(String vmName) {
    return new VMReference(name: vmName)
  }

  @Override
  boolean PowerOffVM(VMReference vmRef) {
    msgBox("Kill VM ${vmRef.name}")
    true
  }

  @Override
  boolean RestartVM(VMReference vmRef) {
    msgBox("Restart VM ${vmRef.name}")
    true
  }

  @Override
  boolean startVM(VMReference vmRef) {
    msgBox("start VM ${vmRef.name}")
    true
  }

  @Override
  boolean kill(VMReference vmRef, int processId, int signal) {
    msgBox("On ${vmRef.name} kill -$signal ${pidToString(processId)}")
    true
  }
}
