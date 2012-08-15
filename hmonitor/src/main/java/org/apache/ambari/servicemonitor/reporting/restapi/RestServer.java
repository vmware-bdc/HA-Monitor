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

package org.apache.ambari.servicemonitor.reporting.restapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.http.HttpServer;

import java.io.Closeable;
import java.io.IOException;

public class RestServer implements Closeable {
  private static final Log LOG = LogFactory.getLog(RestServer.class);
  public static final String OWNER = "owner";

  private HttpServer httpServer;

  private RestReporter owner;


  private Configuration conf;
  private String bindAddress;
  private int port;
  private String name;


  public RestServer(String name, RestReporter owner, Configuration conf, String bindAddress, int port) {
    this.owner = owner;
    this.conf = conf;
    this.bindAddress = bindAddress;
    this.port = port;
    this.name = name;
  }

  public synchronized void start() throws IOException {
    httpServer = new HttpServer(name, bindAddress, port, false, conf, null, null);
    httpServer.addServlet("status", "/status", RestStatusServlet.class);
//    Context ctx = new Context();
//    httpServer.addContext(ctx);
    httpServer.setAttribute(OWNER, owner);
    httpServer.start();
  }

  @Override
  public synchronized void close() throws IOException {
    try {
      if (httpServer != null) {
        httpServer.stop();
      }
    } catch (Exception e) {
      LOG.warn("When stopping http server: " + e, e);
    } finally {
      httpServer = null;
    }
  }

}
