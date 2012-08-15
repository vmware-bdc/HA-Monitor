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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;

/**
 * New tool runner that supports options from the target class.
 */
public interface ToolPlus extends Tool {

  /**
   * Create the tool specific options or null if there are none
   * Remember that these options MUST NOT clash with the shortnames of the GenericOptionsParser
   * @return the options
   */
  Options createToolSpecificOptions();


  /**
   * Set the (parsed) command line. This is done before {@link
   * Tool#run(String[])} is invoked.
   *
   * @param commandLine command line passed to the Tool
   */
  void setCommandLine(CommandLine commandLine);

  /**
   * Get a string suitable for the usage header
   *
   * @return the usage header
   */
  String getUsageHeader();

  /**
   * Get the name of the tool for use in messages to the user
   *
   * @return the tool name
   */
  String getToolName();

  String getUsageFooter();

  /**
   * Get any extra information to invoke on a --dump call. This will be invoked
   * after {@link #setConf(Configuration)} has set the configuration
   * @return a possibly empty string
   */
  String dump();
}
