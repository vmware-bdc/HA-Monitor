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

package org.apache.ambari.servicemonitor.unit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.net.URL;

/**
 * This test is just here to see what Log4J resource file is being picked off the classpath
 */
public class Log4JDiagTest {

  @Test
  public void testLog4J() throws Throwable {
    Log log = LogFactory.getLog(this.getClass());
    URL log4jURL = log.getClass().getClassLoader().getResource("log4j.properties");
    System.out.println("Log4J is at " + log4jURL);
    log.info("Log4J is at " + log4jURL);
    LogFactory factory = LogFactory.getFactory();
    log.info("Commons logging factory is " + factory);
  }
}
