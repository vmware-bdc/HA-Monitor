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

package org.apache.ambari.servicemonitor.main;

import org.apache.ambari.servicemonitor.reporting.Reporter;
import org.apache.ambari.servicemonitor.utils.Exit;
import org.apache.ambari.servicemonitor.utils.MonitorUtils;
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

/**
 * Triggers suicide action by starting up logging then doing an unplanned exit
 * Uses the reporter defined in the configuration
 */
public class Suicide extends ToolPlusImpl {

  private static final Log LOG = LogFactory.getLog(Suicide.class);

  @Override
  public int run(String[] args) throws Exception {
    Reporter reporter = MonitorUtils.createReporter(getConf());
    reporter.init(getConf());
    reporter.start("suicide", "Planned Suicide");
    if (reporter.isIntegratedWithHAMonitoringSystem()) {
      LOG.info("About to shut down. If this does not happen then the underlying HA" +
               " cluster infrastructure is not live/not monitoring this host.");
      return 0;
    } else {
      LOG.error("This configuration is not bonded to a reporter that triggers HA-monitored shutdown");
      return -1;
    }
  }

  public static void main(String[] args) {
    try {
      int res =
        ToolRunnerPlus
          .run(new Configuration(),
               new Suicide(),
               args);
      Exit.exitProcess(res, "");
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }

}
