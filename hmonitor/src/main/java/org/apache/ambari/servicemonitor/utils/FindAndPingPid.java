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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Class to find and ping a process -designed for use outside a probe, which can
 * aid testing.
 *
 * The path/file is set up at create time but the file containing the pid must
 * be reloaded every ping(), so that after a service restart the new pid will 
 * be picked up.
 */
public class FindAndPingPid {
  private static final Log LOG = LogFactory.getLog(FindAndPingPid.class);

  private final String pidPath;
  private final File pidFile;

  public FindAndPingPid(File pidFile) {
    this.pidFile = pidFile;
    pidPath = pidFile.getAbsolutePath();
  }

  public String getPidPath() {
    return pidPath;
  }

  public File getPidFile() {
    return pidFile;
  }

  @Override
  public String toString() {
    return pidPath;
  }

  /**
   * Load the pid from a file
   * @return the trimmed string from the file
   * @throws IOException IO problems
   * @throws FileNotFoundException if the pid file is missing
   */
  public String loadPid() throws IOException {
    if (!pidFileExists()) {
      throw new FileNotFoundException(pidFile.toString());
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Loading file " + pidFile);
      }
      FileInputStream inputStream = new FileInputStream(pidFile);
      try {
        String text = IOUtils.toString(inputStream);
        return text.trim();
      } finally {
        inputStream.close();
      }
    }
  }

  public boolean pidFileExists() {
    return pidFile.exists();
  }

  /**
   * Ping the process
   * @return true iff the pid file could be loaded, it contains a pid and that
   * pid responds to a signal request
   * @throws IOException on any execution problem.
   */
  public boolean ping() throws IOException {
    String pid = loadPid();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loaded pid \"" + pid + "\"");
    }
    if (pid.isEmpty()) {
      throw new IOException("No PID in the file");
    }
    int result = signalPid(pid, 0);
    if (LOG.isDebugEnabled()) {
      LOG.debug("return code \"" + result + "\"");
    }
    return result == 0;
  }

  /**
   * Signal the pid, block for completion
   * @param pid process to signal
   * @param signal signal to raise
   * @return the return code
   * @throws IOException any IO problem.
   * @throws InterruptedIOException if the thread was interrupted during execution.
   */
  int signalPid(String pid, int signal) throws IOException {
    String[] cmds = new String[3];
    int c = 0;
    cmds[c++] = "kill";
    cmds[c++] = "-" + signal;
    cmds[c] = pid;


    Process process = Runtime.getRuntime().exec(cmds);
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      throw new InterruptedIOException("While signalling " + pid);
    }

  }

}
