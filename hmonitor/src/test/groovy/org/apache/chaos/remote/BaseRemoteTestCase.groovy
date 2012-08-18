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

package org.apache.chaos.remote

import junit.framework.TestCase
import groovy.util.logging.Commons

/**
 * Base class for remote tests
 */
@Commons
class BaseRemoteTestCase extends TestCase {
  public static final String TEST_REMOTE = "test.remote."
  public static final String TEST_REMOTE_ENABLED = TEST_REMOTE + "enabled"
  public static final String TEST_REMOTE_SSH_KEY = TEST_REMOTE + "ssh.key"
  File sshkey

  /**
   * Bind to the SSH key -assert that the file actually exists
   */
  protected void bindSSHKey() {
    sshkey = new File(sysprop("user.home"), ".ssh/id_rsa")
    sshkey = new File(sysprop(TEST_REMOTE_SSH_KEY, sshkey.toString()))
    assert sshkey.exists()
  }

  String sysprop(String key) {
    System.getProperty(key)
  }

  String requiredSysprop(String key) {
    String val = sysprop(key)
    assert key && val != null
    val
  }

  String sysprop(String key, String val) {
    System.getProperty(key, val)
  }

  boolean boolSysProp(String key, boolean defval) {
    Boolean.valueOf(sysprop(key, defval.toString()))
  }

  int intSysProp(String key, int defval) {
    Integer.valueOf(sysprop(key, defval.toString()))
  }


  boolean reqBoolSysProp(String key) {
    Boolean.valueOf(requiredSysprop(key))
  }


  boolean isRemoteTestsEnabled() {
    return boolSysProp(TEST_REMOTE_ENABLED, false)
  }


  RemoteServer createServer(String server, String user) {
    new RemoteServer(
        host: server,
        username: user,
        publicKeyFile: sshkey)
  }

  RemoteServer rootServer(String server) {
    return createServer(server, 'root')
  }

}
