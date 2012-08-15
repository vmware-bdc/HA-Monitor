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
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.TreeSet;

/**
 * This is a replacement for the normal ToolRunner, which queries the target for
 * its own option set- and includes that in the parse process. As a result, they
 * get better parsing.
 */
public final class ToolRunnerPlus {
  private static final Log LOG = LogFactory.getLog(ToolRunnerPlus.class);

  private ToolRunnerPlus() {
  }


  /**
   * Print outside logging
   *
   * @param text text to print
   */
  private static void println(String text) {
    System.out.println(text);
  }

  public static int exec(Configuration conf, ToolPlus tool, String... args)
    throws Exception {
    return run(conf, tool, args);
  }

  /**
   * Run a process then exit with its exit code
   * @param conf configuration 
   * @param tool tool to run
   * @param args arguments
   */
  public static void runAndExit(Configuration conf, ToolPlus tool, String[] args) {
    try {
      int res = run(conf, tool, args);
      Exit.exitProcess(res, "");
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }


  public static int run(Configuration conf, ToolPlus tool, String[] args)
    throws Exception {

    if (LOG.isDebugEnabled()) {
      //dump the options
      for (int i = 0; i < args.length; i++) {
        LOG.debug("arg[" + i + "]=" + args[i]);
      }
    }

    String[] toolArgs = prepareToExecute(conf, tool, args);

    if (toolArgs != null) {
      return tool.run(toolArgs);
    } else {
      return -1;
    }
  }

  /**
   * Get ready to run a tool but don't actually run it. This is for test purposes.
   * @param conf Configuration to start with
   * @param tool tool to set up
   * @param args command line arguments
   * @return either an array of unparsed elements, or null, meaning "preparation failed, do not execute"
   * @throws IOException on problems
   */
  public static String[] prepareToExecute(Configuration conf, ToolPlus tool, String[] args) throws IOException {
    boolean canRun = true;
    if (conf == null) {
      conf = new Configuration();
    }
    Options options = tool.createToolSpecificOptions();
    if (options == null) {
      options = new Options();
    }

    options
      .addOption("p", "dump", false, "dump the current configuration");
    options
      .addOption("u", "usage", false, "Print the Usage");

    GenericOptionsParser parser = new GenericOptionsParser(conf, options, args);

    //process our local values


    //set the configuration back, so that Tool can configure itself
    Configuration configuration = parser.getConfiguration();
    CommandLine commandLine = parser.getCommandLine();

    if (commandLine == null) {
      dumpArguments(args);
      canRun = false;
    } else {
      if (commandLine.hasOption("u")) {
        usage(args, tool, options);
        canRun = false;
      } else {
        tool.setConf(configuration);
        tool.setCommandLine(commandLine);


        if (commandLine.hasOption("p")) {
          //dump the commands
          dumpArguments(args);
          //dump the configuration
          dumpConf(conf);

          dumpSystemState();

          String toolDump = tool.dump();
          if (toolDump != null && !toolDump.isEmpty()) {
            println(toolDump);
          }
        }
      }
    }

    String[] toolArgs;
    if (canRun) {
      toolArgs = parser.getRemainingArgs();
    } else {
      toolArgs = null;
    }
    return toolArgs;
  }

  private static int usage(String[] args, ToolPlus tool, Options options) {
    HelpFormatter hf = new HelpFormatter();
    hf.printHelp(80,
                 tool.getUsageHeader(),
                 tool.getToolName(),
                 options,
                 tool.getUsageFooter(),
                 true);
    dumpArguments(args);

    return -1;
  }

  private static void dumpArguments(String[] args) {
    println("Arguments");
    println(convertArgsToString(args));
  }

  private static void dumpConf(Configuration conf) {
    TreeSet<String> keys = DFSUtils.sortedConfigList(conf);
    for (String key : keys) {
      println(key + "=" + conf.get(key));
    }
  }

  private static void dumpSystemState() {
    StringBuilder dump = new StringBuilder();
    URL log4jURL =
      LOG.getClass().getClassLoader().getResource("log4j.properties");
    dump.append("Using Log4J file: ").append(log4jURL).append("\n");
    dump.append("PWD=").append(new File(".").getAbsolutePath()).append("\n");
    dump.append("Log implementation is ").append(LOG.getClass()).append(
      "\n");

    dump.append("System Properties:\n")
        .append(MonitorUtils.dumpSystemProperties());
    dump.append("Env Variables:\n").append(MonitorUtils.dumpEnv());
    println(dump.toString());
  }

  public static String convertArgsToString(String... args) {
    StringBuilder builder = new StringBuilder();
    for (String arg : args) {
      builder.append(" \"").append(arg).append("\"");
    }
    return builder.toString();
  }
}
