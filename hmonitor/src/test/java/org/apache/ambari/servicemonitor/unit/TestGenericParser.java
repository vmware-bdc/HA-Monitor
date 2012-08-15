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

/**
 *
 */

package org.apache.ambari.servicemonitor.unit;

import junit.framework.Assert;
import org.apache.ambari.servicemonitor.HadoopKeys;
import org.apache.ambari.servicemonitor.jobs.JobKeys;
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.util.GenericOptionsParser;
import org.junit.Test;

import java.io.IOException;

/**
 * This test is here to understand what the GE parse is really up to, and
 * whether it should be replaced or just tweaked
 */
public class TestGenericParser extends Assert {

  public static final String JT = "jobtracker:8030";
  public static final String FS = "hdfs://filesystem:8020";

  @Test
  public void testCreate() throws Throwable {
    GenericOptionsParser parser = createParser("-jt", JT,
                                               "-fs", FS);
    Configuration configuration = parser.getConfiguration();
    assertConfigHas(configuration, HadoopKeys.MAPRED_JOB_TRACKER, JT);
    assertConfigHas(configuration, FileSystem.FS_DEFAULT_NAME_KEY, FS);
  }

  @Test
  public void testProperty() throws Throwable {
    GenericOptionsParser parser = createParser("-jt", JT,
                                               "-fs", FS,
                                               "-D", "a=b");
    Configuration configuration = parser.getConfiguration();
    assertConfigHas(configuration, "a", "b");
  }

  public void assertConfigHas(Configuration conf, String key, String expectedValue) {
    assertEquals(expectedValue, conf.get(key));
  }

  @Test
  public void testToolRunnerArgs() throws Throwable {
    TestTool tool = runTool("-jt", JT,
                            "-fs", FS,
                            "-D", "a=b");
    Configuration configuration = tool.getConf();
    assertConfigHas(configuration, JobKeys.MAPRED_JOB_TRACKER, JT);
    assertConfigHas(configuration, FileSystem.FS_DEFAULT_NAME_KEY, FS);
    assertConfigHas(configuration, "a", "b");
  }

  protected GenericOptionsParser createParser(String... args)
    throws IOException {
    return new GenericOptionsParser(args);
  }

  protected TestTool runTool(String... args) throws Exception {
    TestTool tool = new TestTool();
    ToolRunnerPlus.exec(new Configuration(), tool, args);
    return tool;
  }

  class TestTool extends ToolPlusImpl {

    String[] args;

    @Override
    public int run(String[] args) throws Exception {
      this.args = args;
      return 0;
    }
  }
}
