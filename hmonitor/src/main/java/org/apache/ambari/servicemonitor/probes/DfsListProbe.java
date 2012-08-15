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

package org.apache.ambari.servicemonitor.probes;

import org.apache.ambari.servicemonitor.reporting.ProbeStatus;
import org.apache.ambari.servicemonitor.utils.DFSUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import java.io.IOException;
import java.net.URI;

/**
 * Lists a directory
 */
public class DfsListProbe extends Probe {
  private static final Log LOG = LogFactory.getLog(DfsListProbe.class);

  private final String path;
  protected final URI fsURI;

  public DfsListProbe(Configuration conf, String path) throws IOException {
    super("DfsListProbe " + FileSystem.getDefaultUri(conf) + path, conf);
    //make sure the probe doesn't block, regardless of
    //any site configurations
    DFSUtils.makeDfsCallsNonBlocking(conf);
    fsURI = DFSUtils.getHDFSUri(conf);
    LOG.info(getName());
    this.path = path;
  }

  private DistributedFileSystem createDfs() throws IOException {
    return DFSUtils.createUncachedDFS(conf);
  }

  @Override
  public ProbeStatus ping(boolean livePing) {

    ProbeStatus status = new ProbeStatus();
    DistributedFileSystem hdfs = null;
    try {
      hdfs = createDfs();
      Path dfsPath;
      dfsPath = new Path(path);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Listing " + getName());
      }
      FileStatus[] fileStatuses = hdfs.listStatus(dfsPath);
      if (fileStatuses != null) {
        //successful operation
        status.succeed(this);
        status.setMessage(
          getName() + " contains " + fileStatuses.length + " entries");
      } else {
        //no file
        status.finish(this, false, "Path " + path + " not found", null);
      }
    } catch (IOException e) {
      status.fail(this,
                  new IOException(getName() + " : " + e, e));
      LOG.debug("Failure to probe " + getName());
    } finally {
      DFSUtils.closeDFS(hdfs);
    }
    return status;
  }

}
