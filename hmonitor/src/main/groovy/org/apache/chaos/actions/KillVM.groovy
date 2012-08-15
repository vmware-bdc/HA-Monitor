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

package org.apache.chaos.actions

import groovy.util.logging.Commons
import org.apache.ambari.servicemonitor.utils.Exit
import org.apache.ambari.servicemonitor.utils.ExitMainException
import org.apache.ambari.servicemonitor.utils.OptionHelper
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus
import org.apache.chaos.infra.VMReference
import org.apache.hadoop.conf.Configuration

/**
 *
 */
@Commons
class KillVM extends Action {

  @Override
  boolean execAction() {
    String vm = OptionHelper.getStringOption(commandLine, O_VM, "")
    if (!vm) {
      throw new ExitMainException("No vm specified")
    }
    VMReference targetVM = infrastructure.locate(vm)
    return infrastructure.PowerOffVM(targetVM)
  }

  public static void main(String[] args) {
    try {
      int res =
        ToolRunnerPlus
            .run(new Configuration(),
                 new KillVM(),
                 args);
      Exit.exitProcess(res, "");
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }

}
