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

import org.apache.ambari.servicemonitor.utils.OptionHelper
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl
import org.apache.chaos.Keys
import org.apache.chaos.infra.Infrastructure
import org.apache.chaos.tools.ToolsOfChaos
import org.apache.commons.cli.Options

/**
 *
 */
class Action extends ToolPlusImpl {

  public static final String O_VM = 'vm'
  public static final String O_SERVICE = 's'
  Infrastructure infrastructure;

  Infrastructure getInfrastructure() {
    return infrastructure ?: loadInfrastructure()
  }

  synchronized Infrastructure loadInfrastructure() {
    if (!infrastructure) {
      infrastructure = (Infrastructure) ToolsOfChaos.loadClass(conf,
                                                               Keys.CHAOS_INFRASTRUCTURE)
    }
    infrastructure
  }

  @Override
  Options createToolSpecificOptions() {
    Options options = new Options();
    OptionHelper.addStringArgOpt(options, O_VM, 'vm', 'the name of a VM')
    OptionHelper.addStringArgOpt(options, O_SERVICE, 'service', 'the name of a service')
    options
  }

  @Override
  int run(String[] args) {
    return execAction() ? 0 : 1
  }

  boolean execAction() {
    true
  }
}
