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

import org.apache.ambari.servicemonitor.jobs.FileUsingJobRunner;
import org.apache.ambari.servicemonitor.jobs.FileUsingMapper;
import org.apache.ambari.servicemonitor.jobs.FileUsingReducer;
import org.apache.ambari.servicemonitor.jobs.JobKeys;
import org.apache.ambari.servicemonitor.jobs.ProbableFileOperation;
import org.apache.hadoop.mapred.JobConf;
import org.junit.Test;

public class FileUsingJobTest extends BaseLocalClusterTestCase {

  /**
   * Test that a file-using job works
   * @throws Throwable
   */
  @Test
  public void testFileOpJob() throws Throwable {
    createDFSCluster();
    createMRCluster();

    float Pmap = 1.0f;
    float Preduce = 1.0f;
    JobConf jobConf = mrCluster.createJobConf();
    int maps = 1;
    jobConf.setInt(JobKeys.RANGEINPUTFORMAT_ROWS, maps);
    jobConf.setFloat(FileUsingMapper.NAME + ProbableFileOperation.PROBABILITY,
                     Pmap);
    jobConf.setInt(FileUsingMapper.NAME + ProbableFileOperation.SLEEPTIME, 10);

    jobConf.setFloat(FileUsingReducer.NAME + ProbableFileOperation.PROBABILITY,
                     Preduce);
    jobConf.setInt(FileUsingReducer.NAME + ProbableFileOperation.SLEEPTIME, 10);
    bondDataOutputDir(this, "testFileOpJob", jobConf);
    int r = FileUsingJobRunner.exec(jobConf);
    assertEquals("Wrong return code from job", 0, r);

  }

}
