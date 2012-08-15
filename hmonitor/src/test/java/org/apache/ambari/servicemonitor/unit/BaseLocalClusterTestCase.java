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

import junit.framework.Assert;
import org.apache.ambari.servicemonitor.HadoopKeys;
import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.ambari.servicemonitor.utils.DFSUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Base test class for cluster tests
 */
public class BaseLocalClusterTestCase extends Assert {

  public static final int NN_STARTUP_SHUTDOWN_TIME = 60000;
  protected MiniDFSCluster dfsCluster;
  protected MiniMRCluster mrCluster;

  @Before
  public void setUp() throws Exception {
  }

  /**
   * Create a
   *
   * @param testname
   * @return
   */
  protected String getDataDir(BaseLocalClusterTestCase tc, String testname) {
    return "test/data/" + tc.getClass().getSimpleName() + "/" + testname;
  }


  /**
   * Bond the job configuration to the directory set up for this class for data
   * @param tc test case
   * @param testname test name
   * @param jobConf job conf to configure
   */
  protected void bondDataOutputDir(BaseLocalClusterTestCase tc,
                                   String testname,
                                   JobConf jobConf) {
    Path datadir = new Path(getDataDir(tc, testname));
    jobConf.setWorkingDirectory(new Path(datadir, "working"));
    jobConf
      .set(HadoopKeys.MAPRED_OUTPUT_DIR, new Path(datadir, "output").toString());
  }

  /**
   * Creat a new DFS cluster with a path driven by the system time
   * @return a new cluster
   * @throws IOException if the clsuter could not be created
   */
  protected MiniDFSCluster createDFSCluster() throws IOException {
    Configuration conf = new Configuration();
    assertNoDFSCluster();
    String testDataDirPath = "target/test/data" + System.currentTimeMillis();
    return dfsCluster = DFSUtils.createCluster(conf, testDataDirPath, 3);
  }

  /**
   * Create a MiniMR cluster bonded to whatever DFS cluster has been brought up 
   * @return a miniMR cluster
   * @throws IOException cluster instantiation problems
   */
  protected MiniMRCluster createMRCluster() throws IOException {
    assertNoMRCluster();
    assertDFSCluster();
    JobConf conf = new JobConf();
    mrCluster = DFSUtils.createMRCluster(conf, getDFSClusterURI());
    return mrCluster;
  }

  /**
   * Get the configuration entry for job trackers in a miniMR cluster
   * @param miniMRCluster the mini mr cluster
   * @return the "localhost:port" string used to point to the JT
   */
  protected String getMRClusterConfEntry(MiniMRCluster miniMRCluster) {
    return "localhost:" + miniMRCluster.getJobTrackerPort();
  }

  /**
   * Inject the address of the mini MR cluster into the supplied configuration
   * @param conf configuration to patch
   * @param miniMRCluster the cluster to set the conf to
   */
  protected void injectMRClusterEntry(Configuration conf,
                                      MiniMRCluster miniMRCluster) {
    String val = getMRClusterConfEntry(miniMRCluster);
    conf.set(HadoopKeys.MAPRED_JOB_TRACKER, val);
  }

  /**
   * Assert that there is no DFS cluster
   */
  protected void assertNoDFSCluster() {
    assertNull("DFSCluster already live", dfsCluster);
  }


  /**
   * Assert that there is a DFS cluster
   */
  protected void assertDFSCluster() {
    assertNotNull("No DFSCluster", dfsCluster);
  }

  /**
   * Assert that there is no MiniMR cluster
   */
  protected void assertNoMRCluster() {
    assertNull("MRCluster already live", mrCluster);
  }

  /**
   * Create a Conf bonded to the DFS entry
   * @return a new conf file with fs.default.name set to the mini DFS cluster
   */
  protected Configuration createDFSBondedConfiguration() {
    assertDFSCluster();
    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, getDFSClusterURI());
    return conf;
  }

  /**
   * Get a URI to the cluster
   * @return a dfs string
   * @throws NullPointerException if there isn't a DFS cluster
   */
  protected String getDFSClusterURI() {
    return "hdfs://localhost:" + dfsCluster.getNameNodePort();
  }

  /**
   * Get a URL to the cluster web
   * @return a URL String
   * @throws NullPointerException if there isn't a DFS cluster
   */
  protected String getNamenodeWebURL() {
    InetSocketAddress httpAddress = dfsCluster.getNameNode().getHttpAddress();
    return "http://localhost:" + httpAddress.getPort() + "/";
  }

  /**
   * Teardown will tear down any dfs or MR cluster
   * @throws Exception on a failure
   */

  @After
  public void tearDown() throws Exception {
    destroyMRCluster();
    destroyDFSCluster();
  }

  protected void destroyDFSCluster() {
    dfsCluster = DFSUtils.destroyCluster(dfsCluster);
  }

  protected void destroyMRCluster() {
    mrCluster = DFSUtils.destroyMRCluster(mrCluster);
  }

  protected void assertFailure(ProbeStatus status) {
    assertFalse("Expected a failure but got successful result: " + status,
                status.isSuccess());
  }

  protected void assertSuccess(ProbeStatus status) {
    assertTrue("Expected got success but got failure: " + status,
               status.isSuccess());
  }
}
