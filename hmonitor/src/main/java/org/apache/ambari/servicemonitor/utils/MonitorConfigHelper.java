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

import org.apache.ambari.servicemonitor.MonitorKeys;
import org.apache.hadoop.conf.Configuration;

public class MonitorConfigHelper implements MonitorKeys {

  public static void setMonitorTimeoutProps(Configuration conf,
                                            int probeInterval,
                                            int reportInterval,
                                            int probeTimeout,
                                            int bootstrapTimeout) {
    conf.setInt(MONITOR_PROBE_INTERVAL, probeInterval);
    conf.setInt(MONITOR_REPORT_INTERVAL, reportInterval);
    conf.setInt(MONITOR_PROBE_TIMEOUT, probeTimeout);
    conf.setInt(MONITOR_BOOTSTRAP_TIMEOUT, bootstrapTimeout);
  }

  public static void enableWebProbe(Configuration conf, String url) {
    conf.setBoolean(WEB_PROBE_ENABLED, true);
    conf.set(MonitorKeys.WEB_PROBE_URL, url);
  }

  public static void enablePortPobe(Configuration conf, String host, int port) {
    conf.setBoolean(PORT_PROBE_ENABLED, true);
    conf.set(PORT_PROBE_HOST, host);
    conf.setInt(PORT_PROBE_PORT, port);
  }

  /**
   * assumes filesystem is set
   * @param conf
   * @param path
   */
  public static void enableFSLsProbe(Configuration conf, String path) {
    conf.setBoolean(LS_PROBE_ENABLED, true);
    conf.set(LS_PROBE_PATH, path);
  }

  public static void enableJTProbe(Configuration conf, String jtURI) {
    conf.setBoolean(JT_PROBE_ENABLED, true);
    conf.set(MAPRED_JOB_TRACKER, jtURI);
  }


}
