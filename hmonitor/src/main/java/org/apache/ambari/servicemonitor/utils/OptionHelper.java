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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Assistant for setting up options. The standard option builder doesn't include
 * support for typed arguments -this adds those.
 */
public final class OptionHelper {

  private OptionHelper() {
  }

  private static final Log LOG = LogFactory.getLog(OptionHelper.class);

  /**
   * Add an option that is followed by a string argument
   * @param options options to add it to
   * @param shortName short option name
   * @param longName long option name
   * @param description description
   * @return an option that is already added to the option set
   */
  public static Option addStringArgOpt(Options options,
                                       String shortName,
                                       String longName,
                                       String description) {
    Option option = new Option(shortName, longName, true, description);
    option.setType(PatternOptionBuilder.STRING_VALUE);
    options.addOption(option);
    return option;
  }

  /**
   * Add an option that is followed by an integer argument
   * @param options options to add it to
   * @param shortName short option name
   * @param longName long option name
   * @param description description
   * @return an option that is already added to the option set
   */
  public static Option addIntOpt(Options options,
                                 String shortName,
                                 String longName,
                                 String description) {
    Option option = new Option(shortName, longName, true, description);
    option.setType(PatternOptionBuilder.NUMBER_VALUE);
    options.addOption(option);
    return option;
  }

  /**
   * Bool support is hacked in by saying "string" and converting at parse time
   * @param options
   * @param shortName
   * @param longName
   * @param description
   * @return
   */
  public static Option addBoolOpt(Options options,
                                  String shortName,
                                  String longName,
                                  String description) {

    return addStringArgOpt(options, shortName, longName, description);
  }

  /**
   * Get a string option
   * @param commandLine command line
   * @param optionName name of the option
   * @param defVal the default value
   * @return the value of the option or the default value
   */
  public static String getStringOption(CommandLine commandLine,
                                       String optionName,
                                       String defVal) {
    if (commandLine.hasOption(optionName)) {
      return commandLine.getOptionValue(optionName);
    } else {
      return defVal;
    }
  }

  /**
   * Get an integer option
   * @param commandLine command line
   * @param optionName name of the option
   * @param defVal the default value
   * @return the value of the option or the default value
   */
  public static int getIntOption(CommandLine commandLine,
                                 String optionName,
                                 int defVal) {
    int val = defVal;
    try {
      if (commandLine.hasOption(optionName)) {
        Number number =
          (Number) commandLine.getParsedOptionValue(optionName);
        if (number != null) {
          val = number.intValue();
        } else {
          LOG.warn("No number associated with option " + optionName);
        }
      }
    } catch (ParseException e) {
      LOG.warn(
        "Not a number:  " + optionName + " = " + commandLine.getOptionValue(optionName));
    }
    return val;
  }


  /**
   * Get a string option
   * @param commandLine command line
   * @param optionName name of the option
   * @param defVal the default value
   * @return the value of the option or the default value
   */
  public static boolean getBoolOption(CommandLine commandLine,
                                      String optionName,
                                      boolean defVal) {
    if (commandLine.hasOption(optionName)) {
      String optVal = commandLine.getOptionValue(optionName);
      return Boolean.valueOf(optVal);
    } else {
      return defVal;
    }
  }


  /**
   * Build an argument list for debugging/exception messages
   * @param separator separator after <i>every</i> entry
   * @return a string of all the entries with the separator between each one
   * and after the last one. For an empty list: empty string.
   */
  public static String buildArgumentList(CommandLine commandLine, String separator) {
    StringBuilder builder = new StringBuilder();
    int count = 0;
    for (String arg : commandLine.getArgs()) {
      if (count > 0) {
        builder.append(separator);
      }
      count++;
      builder.append(arg);
    }
    return builder.toString();
  }


  public static String buildParsedOptionList(CommandLine commandLine) {

    StringBuilder builder = new StringBuilder();
    for (Option option : commandLine.getOptions()) {
      builder.append(option.getOpt());
      if (option.getLongOpt() != null) {
        builder.append("/").append(option.getLongOpt());
      }
      builder.append(": ");
      List valuesList = option.getValuesList();
      builder.append("[");
      int count = 0;
      for (Object elt : valuesList) {
        if (count > 0) {
          builder.append(", ");
        }
        builder.append('"').append(elt.toString()).append("\"");
      }
      builder.append("]\n");
    }
    return builder.toString();
  }

}
