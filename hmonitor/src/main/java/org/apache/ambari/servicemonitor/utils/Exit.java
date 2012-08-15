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

import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This is where we do our exits
 */
public class Exit {
  private static final Log LOG = LogFactory.getLog(Exit.class);
  public static final int EXIT_SUCCESS = 0;
  public static final int EXIT_ERROR = -1;
  public static final int EXIT_MONITORING_FAILURE = -2;

  /**
   * exit on an exception; handle ExitMainException exceptions as planned
   * exits.
   *
   * @param thrown the exception
   */
  public static void exitOnException(Throwable thrown) {
    if (thrown instanceof ExitMainException) {
      ExitMainException e = (ExitMainException) thrown;
      //structured error with an exit code
      exitProcess(e.exitCode, e.getMessage());
    } else {
      exitProcess(EXIT_ERROR, thrown);
    }

    ///this is never reached. If some kind of security exception is thrown, it
    //goes up the stack.
  }

  /**
   * exit the process
   *
   * @param exitCode code
   * @param thrown cause of the exit
   */
  public static void exitProcess(int exitCode, Throwable thrown) {
    if (thrown != null) {
      LOG.warn(thrown.toString(), thrown);
    }
    exitProcess(exitCode, thrown == null ? "Exit" : thrown.toString());
  }

  /**
   * exit the process
   *
   * @param exitCode exit code
   * @param text optional text
   */
  public static void exitProcess(int exitCode, String text) {
    if (text != null && !text.isEmpty()) {
      LOG.fatal(text);
    }
    System.exit(exitCode);
  }

  public static void exitProcess(boolean successful) {
    exitProcess(successful ? EXIT_SUCCESS : EXIT_ERROR, "");
  }

  /**
   * Respond to a probe failure by exiting the system straight away
   *
   * @param status the status
   */
  public static void exitOnProbeFailure(ProbeStatus status) {
    LOG.fatal(status.toString());
    exitProcess(EXIT_MONITORING_FAILURE, status.getThrown());
  }
}
