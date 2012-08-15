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

import org.apache.ambari.servicemonitor.clients.LsDir;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

public class NonBlockingLsDirTest extends BaseLocalClusterTestCase {
  private static final Log LOG = LogFactory.getLog(NonBlockingLsDirTest.class);

  /**
   * Bring up a mini dfs cluster then assert that the 
   * @throws Throwable
   */
  @Test
  public void testClientRun() throws Throwable {
    createDFSCluster();
    LsDir lsDir = new LsDir();
    Configuration conf = createDFSBondedConfiguration();
    int outcome = ToolRunnerPlus.exec(conf, lsDir,
                                      "--dir", "/",
                                      "--successlimit", "2",
                                      "--attempts", "2",
                                      "-faillimit", "1");
    LOG.info(lsDir.toString());
    assertEquals("expected a return code of 0", outcome, 0);
  }

}
