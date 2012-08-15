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

import org.apache.ambari.servicemonitor.reporting.ProbePhase;
import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.ambari.servicemonitor.reporting.Reporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * There's a singleton rest reporter here which is architecturally very wrong, but
 * avoids complex servlet context initialization, inserting serializable
 * references into the context, etc, etc.
 */
public final class RestReporter extends Reporter {
  private static final Log LOG = LogFactory.getLog(RestReporter.class);

  private List<ProbeStatus> events = Collections.synchronizedList(new LinkedList<ProbeStatus>());

  private volatile ProbeStatus lastEvent;

  private static RestReporter singletonReporter;
  private RestServer server;

  /**
   * Create the instance and set the {@link #singletonReporter} field to this value. 
   * This is dangerous as it allows the "this" value to leak during construction, and
   * singletons are always bad practise. 
   * @param serviceName name of the service
   */
  public RestReporter() {
    super("RestReporter");
    if (singletonReporter != null) {
      LOG.warn("Singleton reporter is being redefined");
    }
    singletonReporter = this;
  }

  @Override
  public void probeResult(ProbePhase phase, ProbeStatus status) {
    addEvent(status);
  }

  private synchronized void addEvent(ProbeStatus event) {
    lastEvent = event;
    events.add(event);
    if (events.size() > 512) {
      events.remove(0);
    }
  }


  public ProbeStatus getLastEvent() {
    return lastEvent;
  }

  public static RestReporter getSingletonReporter() {
    return singletonReporter;
  }

  @Override
  public boolean start(String name, String description) {
    super.start(name, description);
    server = new RestServer("status", this, new Configuration(), "0.0.0.0", 50088);
    try {
      server.start();
      return true;
    } catch (IOException e) {
      LOG.error("Failed to start Rest Server, cause=\"" + e + "\"", e);
      return false;
    }
  }

  @Override
  public void unregister() {
    super.unregister();
    if (server != null) {
      try {
        server.close();
      } catch (IOException ignored) {
        //
      } finally {
        server = null;
      }
    }
  }
}
