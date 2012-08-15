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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public final class DFSUtils {

  private static final Log LOG = LogFactory.getLog(DFSUtils.class);
  public static final String DFS_CLIENT_RETRY_POLICY_ENABLED =
    "dfs.client.retry.policy.enabled";
  public static final String IPC_CLIENT_CONNECT_MAX_RETRIES =
    "ipc.client.connect.max.retries";

  private DFSUtils() {
  }

  /**
   * Get an HDFS URI
   * @param conf configuration
   * @return the URI
   * @throws IOException
   */
  public static URI getHDFSUri(Configuration conf) throws IOException {
    URI uri = FileSystem.getDefaultUri(conf);
    if (!uri.getScheme().equals("hdfs")) {
      throw new IOException("Filesystem is not HDFS " + uri);
    }
    return uri;
  }

  /**
   * Create a DFS Instance that is not cached
   *
   * @param conf the configuration to work with
   * @return the DFS Instance
   * @throws IOException on any IO problem
   * @throws ExitMainException if the default FS isn't HDFS
   */
  public static DistributedFileSystem createUncachedDFS(Configuration conf)
    throws IOException {
    conf.setBoolean(HadoopKeys.FS_HDFS_IMPL_DISABLE_CACHE, true);
    FileSystem filesys = FileSystem.get(conf);
    URI fsURI = filesys.getUri();
    if (!(filesys instanceof DistributedFileSystem)) {
      throw new ExitMainException(-1, "Filesystem is not HDFS " + fsURI);
    }
    return (DistributedFileSystem) filesys;
  }

  /**
   * Take a configuration and add the parameters to make it blocking
   *
   * @param conf configuration to patch
   */
  public static void makeDfsCallsBlocking(Configuration conf) {
    conf.setBoolean(DFS_CLIENT_RETRY_POLICY_ENABLED, true);
  }

  /**
   * Take a configuration and add the parameters to make it blocking
   *
   * @param conf configuration to patch
   */
  public static void makeDfsCallsNonBlocking(Configuration conf) {
    conf.setBoolean(DFS_CLIENT_RETRY_POLICY_ENABLED, false);
    conf.setInt(IPC_CLIENT_CONNECT_MAX_RETRIES, 0);
  }

  /**
   * create a default cluster
   *
   * @return a cluster
   * @throws IOException
   */
  public static MiniDFSCluster createCluster() throws IOException {
    Configuration conf = new Configuration();
    int numDataNodes = 3;
    String testDataDirPath = "target/test/data" + System.currentTimeMillis();
    return createCluster(conf, testDataDirPath, numDataNodes);
  }

  public static MiniDFSCluster createCluster(Configuration conf,
                                             String dataDirPath,
                                             int numDataNodes)
    throws IOException {
    File testDataDir = new File(dataDirPath);
    System.setProperty("test.build.data", testDataDir.getAbsolutePath());
    return new MiniDFSCluster(conf, numDataNodes, true, null);
  }

  public static MiniDFSCluster destroyCluster(MiniDFSCluster dfsCluster) {
    if (dfsCluster != null) {
      try {
        dfsCluster.shutdown();
      } catch (Exception e) {
        LOG.warn("Exception when destroying cluster: " + e, e);
      }
    }
    return null;
  }

  public static MiniMRCluster createMRCluster(JobConf conf, String fsURI)
    throws IOException {
    String logdir = System.getProperty("java.io.tmpdir") + "/mrcluster/logs";
    System.setProperty("hadoop.log.dir", logdir);
    conf.set("hadoop.job.history.location", "file:///" + logdir + "/history");
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, fsURI);
    return new MiniMRCluster(3, fsURI, 1, null, null, conf);
  }

  public static MiniMRCluster destroyMRCluster(MiniMRCluster mrCluster) {
    if (mrCluster != null) {
      try {
        mrCluster.shutdown();
      } catch (Exception e) {
        LOG.warn("Exception when destroying cluster: " + e, e);
      }
    }
    return null;
  }

  /**
   * Close any non-null FS
   *
   * @param hdfs filesystem
   * @return null, always
   */
  public static DistributedFileSystem closeDFS(FileSystem hdfs) {
    if (hdfs != null) {
      try {
        hdfs.close();
      } catch (IOException ignore) {

      }
    }
    return null;
  }

  public static TreeSet<String> sortedConfigList(Configuration conf) {
    TreeSet<String> keys = new TreeSet<String>();
    Iterator<Map.Entry<String, String>> iterator = conf.iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, String> next = iterator.next();
      keys.add(next.getKey());
    }
    return keys;
  }
}
