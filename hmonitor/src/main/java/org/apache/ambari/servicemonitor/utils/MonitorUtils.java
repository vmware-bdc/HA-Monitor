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

import org.apache.ambari.servicemonitor.HadoopKeys;
import org.apache.ambari.servicemonitor.MonitorKeys;
import org.apache.ambari.servicemonitor.reporting.LogReporter;
import org.apache.ambari.servicemonitor.reporting.Reporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.mapred.AdminOperationsProtocol;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Various utils to work with the monitor
 */
public final class MonitorUtils {
  private static final Log LOG = LogFactory.getLog(MonitorUtils.class);

  private MonitorUtils() {
  }

  public static String toPlural(int val) {
    return val != 1 ? "s" : "";
  }

  /**
   * Convert the arguments -including dropping any empty strings that creep in
   * @param args arguments
   * @return a list view with no empty strings
   */
  public static List<String> prepareArgs(String[] args) {
    List<String> argsList = new ArrayList<String>(args.length);
    StringBuilder argsStr = new StringBuilder("Arguments: [");
    for (String arg : args) {
      argsStr.append('"').append(arg).append("\" ");
      if (!arg.isEmpty()) {
        argsList.add(arg);
      }
    }
    argsStr.append(']');
    LOG.debug(argsStr);
    return argsList;
  }

  /**
   * convert the system properties to a buffer for printing
   * @return a multi-line list of key-value pairs
   */
  public static String dumpSystemProperties() {
    TreeSet<String> keys = new TreeSet<String>();
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      keys.add(entry.getKey().toString());
    }
    StringBuilder builder = new StringBuilder();
    for (String key : keys) {
      builder
        .append("  ")
        .append(key)
        .append('=')
        .append(System.getProperty(key))
        .append('\n');
    }
    return builder.toString();
  }

  /**
   * Dump the system environment
   * @return the environment
   */
  public static String dumpEnv() {
    Map<String, String> env = System.getenv();
    TreeSet<String> keys = new TreeSet<String>();
    for (Map.Entry<String, String> entry : env.entrySet()) {
      keys.add(entry.getKey());
    }
    StringBuilder builder = new StringBuilder();
    for (String key : keys) {
      builder
        .append("  ")
        .append(key)
        .append('=')
        .append(env.get(key))
        .append('\n');
    }
    return builder.toString();
  }

  /**
   * Convert milliseconds to human time -the exact format is unspecified
   * @param milliseconds a time in milliseconds
   * @return a time that is converted to human intervals
   */
  public static String millisToHumanTime(long milliseconds) {
    StringBuilder sb = new StringBuilder();
    // Send all output to the Appendable object sb
    Formatter formatter = new Formatter(sb, Locale.US);

    long s = Math.abs(milliseconds / 1000);
    long m = Math.abs(milliseconds % 1000);
    if (milliseconds > 0) {
      formatter.format("%d.%03ds", s, m);
    } else if (milliseconds == 0) {
      formatter.format("0");
    } else {
      formatter.format("-%d.%03ds", s, m);
    }
    return sb.toString();
  }

  /**
   * Get the tracker definition
   *
   * @param conf configuraiton
   * @return the (name,value) pair of the tracker
   * @throws IOException if the tracker is unsupported (local) or undefined
   */
  public static String extractJobTrackerParameter(Configuration conf)
    throws IOException {
    String jtURI = conf.get(HadoopKeys.MAPRED_JOB_TRACKER, "");
    if ("local".equals(jtURI) || jtURI.isEmpty()) {
      throw new IOException(
        "Undefined or unsupported job tracker \"" + jtURI + "\"");
    }
    return jtURI;
  }

  /**
   * Convert a jobtracker name:port string into a URI for parsing 
   * @param jtpath path
   * @return the URI
   * @throws IOException on a syntax error
   */

  public static URI getJTURI(String jtpath) throws IOException {
    try {
      return new URI("jt://" + jtpath);
    } catch (URISyntaxException e) {
      throw new IOException("Bad Jobtracker path \"" + jtpath + "\"");
    }
  }


  public static InetSocketAddress getURIAddress(URI uri) {
    String host = uri.getHost();
    int port = uri.getPort();
    return new InetSocketAddress(host, port);
  }


  /**
   * Create a reporter
   * @param conf the configuration to drive this
   * @return an inited reporter
   * @throws ExitMainException if the creation failed
   */
  public static Reporter createReporter(Configuration conf)
    throws ExitMainException {
    Class<? extends Reporter> reporterClass = conf.getClass(
      MonitorKeys.MONITOR_REPORTER,
      LogReporter.class,
      Reporter.class);
    try {
      Reporter reporter = reporterClass.newInstance();
      reporter.init(conf);
      return reporter;

    } catch (Exception e) {
      LOG.error("Instantiation of " + reporterClass + " failed, cause=" + e, e);
      throw new ExitMainException(Exit.EXIT_ERROR,
                                  "Failed to create an instance of " +
                                  reporterClass
                                  + ": " + e,
                                  e);

    }
  }

  /**
   * Get the localhost -may be null
   * @return the localhost if known
   */
  public static InetAddress getLocalHost() {
    InetAddress localHost;
    try {
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      localHost = null;
    }
    return localHost;
  }

  public static AdminOperationsProtocol createJTAdminProxy(InetSocketAddress addr,
                                                           Configuration conf) throws IOException {
    return (AdminOperationsProtocol) RPC.getProxy(AdminOperationsProtocol.class,
                                                  AdminOperationsProtocol.versionID,
                                                  addr,
                                                  UserGroupInformation.getCurrentUser(),
                                                  conf,
                                                  NetUtils.getSocketFactory(
                                                    conf,
                                                    AdminOperationsProtocol.class));
  }


  public static void closeJobClient(JobClient jobClient) throws IOException {
    if (jobClient != null) {
      jobClient.close();
    }
  }


}
