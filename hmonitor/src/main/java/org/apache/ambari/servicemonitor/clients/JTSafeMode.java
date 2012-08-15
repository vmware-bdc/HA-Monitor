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

package org.apache.ambari.servicemonitor.clients;

import org.apache.ambari.servicemonitor.utils.DFSUtils;
import org.apache.ambari.servicemonitor.utils.Exit;
import org.apache.ambari.servicemonitor.utils.MonitorUtils;
import org.apache.ambari.servicemonitor.utils.OptionHelper;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.mapred.AdminOperationsProtocol;
import org.apache.hadoop.mapred.JobTracker;

import java.io.IOException;


public class JTSafeMode extends JobTrackerAction {
  private static final Log LOG = LogFactory.getLog(JTSafeMode.class);
  private boolean safeMode;
  private boolean toggle;


  @Override
  protected Options createChildOptions(Options options) {
    OptionHelper.addBoolOpt(options, "sm", "safemode", "safemode <boolean>");
    Option option = new Option("t", "toggle", false, "toggle safe mode");
    options.addOption(option);
    return options;
  }

  @Override
  protected void init(CommandLine commandLine) throws IOException {
    super.init(commandLine);
    safeMode = OptionHelper.getBoolOption(commandLine, "s", true);
    toggle = commandLine.hasOption("t");
  }


  @Override
  protected Operation executeOneOperation() throws IOException {
    Operation operation = new Operation("JT safemode to " + safeMode);

    started(operation);
    AdminOperationsProtocol adminProxy = null;
    try {
      Configuration conf = getConf();
      DFSUtils.makeDfsCallsNonBlocking(conf);
      adminProxy = MonitorUtils.createJTAdminProxy(jtAddr, conf);
      if (toggle) {
        boolean currentSM = adminProxy.setSafeMode(JobTracker.SafeModeAction.SAFEMODE_GET);
        safeMode = !currentSM;
      }
      adminProxy.setSafeMode(safeMode ?
                               JobTracker.SafeModeAction.SAFEMODE_ENTER
                               : JobTracker.SafeModeAction.SAFEMODE_LEAVE);
      operation.success();
    } catch (ExitClientRunException e) {
      //propagate this up
      throw e;
    } catch (IOException e) {
      //all other outcomes are failures
      operation.failure(e);
    } finally {
      RPC.stopProxy(adminProxy);
    }
    return operation;
  }


  @Override
  public String getDescription() {
    return "safe mode= " + safeMode;
  }

  public static void main(String[] args) {
    try {
      int res =
        ToolRunnerPlus
          .run(new Configuration(),
               new JTSafeMode(),
               args);
      Exit.exitProcess(true);
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }
}
