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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import java.io.Closeable;
import java.io.IOException;

/**
 * A class that creates a DFS on demand, and will, if needed, close it.
 * It is therad safe. 
 */
public class OnDemandFS implements Closeable {

  private final Configuration conf;
  private DistributedFileSystem fileSystem;

  public OnDemandFS(Configuration conf) {
    this.conf = conf;
  }


  /**
   * Get the filesystem -or create it.
   * @return a DFS instance
   * @throws IOException if it failed to be created
   */
  public DistributedFileSystem getOrCreate() throws IOException {
    if (fileSystem == null) {
      createFS();
    }
    return fileSystem;
  }

  /**
   * thread safe fs constructor
   * @throws IOException on a failure
   */
  private synchronized void createFS() throws IOException {
    if (fileSystem == null) {
      fileSystem = DFSUtils.createUncachedDFS(conf);
    }
  }


  @Override
  public synchronized void close() throws IOException {
    fileSystem = DFSUtils.closeDFS(fileSystem);
  }

}
