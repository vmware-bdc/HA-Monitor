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

import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public class RestStatusServlet extends HttpServlet {
  private static final Log LOG = LogFactory.getLog(RestServer.class);
  public static final String LIVE = "live";

  private ServletContext ctx;
  private RestReporter owner;
  ProbeStatus probeStatus;

  public String status() {
    return "Monitor is up";
  }

  private RestReporter getReporter() {
    return null;
  }

  private ProbeStatus getLastEvent() {
    return null;
  }

  @Override
  protected long getLastModified(HttpServletRequest req) {
    updateProbeStatus();
    return probeStatus == null ? -1 : probeStatus.getTimestamp();
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      super.init(config);
      ctx = config.getServletContext();
      owner = (RestReporter) ctx.getAttribute(RestServer.OWNER);
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private synchronized void updateProbeStatus() {
    probeStatus = owner.getLastEvent();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    updateProbeStatus();
    if (probeStatus == null) {
      doGetNullProbeStatus(req, resp);
    } else {
      doGetProbeStatus(req, resp, probeStatus);
    }
  }

  private void doGetNullProbeStatus(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put(LIVE, false);
    writeJSON(resp, map, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  private void writeJSON(HttpServletResponse resp, HashMap<String, Object> map, int status) throws IOException {
    resp.setStatus(status);
    ObjectMapper json = new ObjectMapper();
    json.writeValue(resp.getOutputStream(), map);
  }

  private void doGetProbeStatus(HttpServletRequest req, HttpServletResponse resp, ProbeStatus status)
    throws ServletException, IOException {

    boolean live = status.isSuccess();
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put(LIVE, live);
    map.put("timestamp", status.getTimestamp());
    map.put("phase", status.getProbePhase());
    map.put("message", status.getMessage());
    Throwable thrown = status.getThrown();
    if (thrown != null) {
      HashMap<String, Object> tmap = new HashMap<String, Object>();
      tmap.put("message", thrown.toString());
      StackTraceElement[] stackTrace = thrown.getStackTrace();
      int index = 0;
      for (StackTraceElement elt : stackTrace) {
        tmap.put(Integer.toString(index++), elt.toString());
      }
      map.put("exception", tmap);
    }
    writeJSON(resp, map, HttpServletResponse.SC_OK);

  }

}
