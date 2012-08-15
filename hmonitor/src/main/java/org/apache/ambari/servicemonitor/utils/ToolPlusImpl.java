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

package org.apache.ambari.servicemonitor.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configured;

public class ToolPlusImpl extends Configured implements ToolPlus {

  private CommandLine commandLine;

  @Override
  public Options createToolSpecificOptions() {
    return new Options();
  }

  @Override
  public String getUsageHeader() {
    return getToolName();
  }

  @Override
  public String getUsageFooter() {
    return "";
  }

  @Override
  public String getToolName() {
    return "Tool";
  }

  @Override
  public String dump() {
    return "";
  }

  public CommandLine getCommandLine() {
    return commandLine;
  }

  /**
   * Build an argument list for debugging/exception messages
   * @param separator separator after <i>every</i> entry
   * @return a string of all the entries with the separator between each one
   * and after the last one. For an empty list: empty string.
   */
  protected String buildArgumentList(String separator) {
    return OptionHelper.buildArgumentList(commandLine, separator);
  }

  @Override
  public void setCommandLine(CommandLine commandLine) {
    this.commandLine = commandLine;
  }

  @Override
  public int run(String[] args) throws Exception {
    return 0;
  }
}
