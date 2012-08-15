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

package org.apache.ambari.servicemonitor;

/**
 * Configuration Keys used in Hadoop but not formally exported as constants
 */
public interface HadoopKeys {

  String MAPRED_OUTPUT_DIR = "mapred.output.dir";
  String MAPRED_JOB_TRACKER = "mapred.job.tracker";


  /**
   * key to disable HDFS caching on 1.x {@value}
   */

  String FS_HDFS_IMPL_DISABLE_CACHE = "fs.hdfs.impl.disable.cache";


  String DFS_SECONDARY_INFO_BINDADDRESS = "secondary.info.bindAddress";
  String DFS_SECONDARY_INFO_PORT = "dfs.secondary.info.port";
  String DFS_SECONDARY_HTTP_ADDRESS = "dfs.secondary.http.address";

}
