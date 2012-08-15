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

package org.apache.ambari.servicemonitor.unit

import org.apache.ambari.servicemonitor.utils.DFSUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.junit.Test

class DFSUtilsTest extends GroovyTestCase {

  @Test
  public void testGetHFSURIWorks() throws Throwable {
    URI uri = setAndGet("hdfs://localhost:8020")
    assert uri != null
  }

  @Test
  public void testFileURL() throws Throwable {
    try {
      URI uri = setAndGet("file:///tmp")
      assert null == uri
    } catch (IOException e) {
      //expected
    }
  }

  def URI setAndGet(String url) {
    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, url);
    URI uri = DFSUtils.getHDFSUri(conf)
    uri
  }
}
