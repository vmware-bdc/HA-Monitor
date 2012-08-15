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

package org.apache.ambari.servicemonitor.unit

import junit.framework.TestCase
import org.apache.ambari.servicemonitor.utils.OptionHelper
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.hadoop.conf.Configuration
import org.junit.Test

/**
 * Verifies that the option helpers do what they should.
 */
class ToolPlusTest extends TestCase {
  public static final int STATUS_CHECK_TIMEOUT = 60000;
  public static final String TIMEOUT = "t";
  public static final String TIMEOUT_LONG = "timeout";
  public static final String PATH = "";
  public static final String PATH_LONG = "file";
  public static final String PID = "i";
  public static final String PID_LONG = "pid";
  public static final String BOOT = "boottimeout";
  public static final String BOOT_LONG = "boottimeout";



  def Options createOptions(ToolPlusTestTool owner, Options options) {
    OptionHelper.addIntOpt(options, TIMEOUT, TIMEOUT_LONG, "timeout in milliseconds; -1 for no timeout");
    OptionHelper.addIntOpt(options, BOOT, BOOT_LONG, "Boot timeout in milliseconds; 0 (or less) for no timeout");
    OptionHelper.addStringArgOpt(options, PATH, PATH_LONG, "path to query");
    OptionHelper.addStringArgOpt(options, PID, PID_LONG, "path to query");
    return options;
  }

  def ToolPlusTestTool createTool() {
    ToolPlusTestTool tool = new ToolPlusTestTool(
        buildOptions: this.&createOptions
    )
    tool
  }

  int run(ToolPlusTestTool tool, String... args) {
    ToolRunnerPlus.run(new Configuration(), tool, args)
  }

  ToolPlusTestTool exec(String... args) {
    ToolPlusTestTool tool = createTool()
    run(tool, args)
    tool
  }

  @Test
  public void testGetStringOpt() throws Throwable {
    ToolPlusTestTool tool = exec("-${PID}", "/")
    CommandLine line = tool.commandLine;
    String pid = OptionHelper.getStringOption(line, PID, null);
    assert pid != null
    assert pid == "/"
  }

  @Test
  public void testGetStringOptLong() throws Throwable {
    ToolPlusTestTool tool = exec("--${PID_LONG}", "/")
    CommandLine line = tool.commandLine;
    String pid = OptionHelper.getStringOption(line, PID, null);
    assert pid != null
    assert pid == "/"
  }

  @Test
  public void testGetIntOpt() throws Throwable {
    ToolPlusTestTool tool = exec("-${BOOT}", "456")
    CommandLine line = tool.commandLine;
    int boot = OptionHelper.getIntOption(line, BOOT, -1);
    assert boot == 456
  }


}
