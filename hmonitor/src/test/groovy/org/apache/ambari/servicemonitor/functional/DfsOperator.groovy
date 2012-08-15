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

package org.apache.ambari.servicemonitor.functional

import groovy.util.logging.Commons
import org.apache.ambari.servicemonitor.utils.DFSUtils
import org.apache.ambari.servicemonitor.utils.MonitorUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hdfs.DistributedFileSystem

import java.util.concurrent.atomic.AtomicBoolean

@Commons
class DfsOperator implements Runnable {

  private String fs;
  private boolean online
  long outStartTime
  int outages
  long downTime
  String path
  Configuration conf
  int interval
  private volatile boolean stopped
  private boolean started
  AtomicBoolean finished = new AtomicBoolean()
  Exception exception
  private final Thread operatorThread;

  /**
   * Create an instance
   * @param path path to operate on
   * @param conf configuration -this is forced into non-block mode
   * @param interval execution interval
   */
  DfsOperator(String path, Configuration conf, int interval) {
    this.path = path
    DFSUtils.makeDfsCallsNonBlocking(conf)
    this.conf = conf
    this.interval = interval
    this.operatorThread = new Thread(this)
    fs = DFSUtils.getHDFSUri(conf).toString()
  }

  protected synchronized void stateChangeEvent(boolean newOnlineFlag) {
    online = newOnlineFlag
    wake()
  }

  private synchronized void wake() {
    this.notifyAll()
  }

  synchronized boolean waitForOnline(long timeout) {
    if (!online) {
      this.wait(timeout)
    }
    return online;
  }

  synchronized boolean getOnline() {
    return online
  }

  synchronized boolean waitForOffine(long timeout) {
    if (online) {
      this.wait(timeout)
    }
    return !online
  }


  long now() {
    return System.currentTimeMillis()
  }

  @Override
  void run() {

    try {
      while (!stopped) {
        dfsOperation()
        Thread.sleep(interval)
      }
    } catch (InterruptedException e) {
      exception = e
    } catch (Exception e) {
      log.warn("In Operation $e", e)
      exception = e
    }
    finished.set(true);
    wake()
  }

  @Override
  public String toString() {
    return "DfsOperator{" +
           "fs='" + fs + '\'' +
           ", online=" + online +
           ", outages=" + outages +
           ", downTime=" + MonitorUtils.millisToHumanTime(downTime) +
           '}';
  }
/**
 * This is the DFS operation
 */
  void dfsOperation() {
    boolean success = false
    try {
      execute()
      success = true
    } catch (Exception e) {
      log.debug("Operation failure $e", e)
    }

    if (!success && online) {
      //have just gone offline
      outStartTime = now()
      outages++
      //flip the flag to offline
      log.info("filesystem -> offline: $this")
      stateChangeEvent(false)
    }
    if (success && !online) {
      //back online
      if (outStartTime > 0) {
        long end = now()
        long diff = end - outStartTime
        downTime += diff;
      }
      //flip the online bit
      log.info("filesystem -> online: $this")
      stateChangeEvent(true)
    }
  }

  /**
   * Default FS operation creates an FS instance then checks for it.
   * It does not care whether or not the FS is in safe mode, only that the directory
   * lookup successfully found a directory
   * @return true
   * @throws IOException
   */
  protected boolean execute() throws IOException {
    //default FS operation is a create + a read
    DistributedFileSystem hdfs = DFSUtils.createUncachedDFS(conf);
    Path dfsPath;
    dfsPath = new Path(path);
    FileStatus[] fileStatuses = hdfs.listStatus(dfsPath);
    return fileStatuses != null;
  }

  synchronized void startThread() {
    if (started) {
      throw new IOException("Attempted to start a thread when one was already running")
    }
    started = true
    operatorThread.start()
  }

  /**
   * idempotent thread stop operator.
   * Does not wait for the operation to finish
   */
  synchronized void stopThread() {
    if (!started || stopped) {
      return
    }
    stopped = true
    operatorThread.interrupt()
  }

}
