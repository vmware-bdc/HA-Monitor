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

import com.jcraft.jsch.Session
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.Test
import groovy.util.logging.Commons

/**
 *
 */
@Commons
class LocalhostSSHTest extends BaseRemoteTestCase {

  public static final int EXEC_TIMEOUT = 5000

  String username

  RemoteServer server
  Session session

  @Override
  protected void setUp() {
    super.setUp()
    username = sysprop("user.name")
    assert username
    bindSSHKey()
  }


  @Override
  protected void tearDown() {
    session?.disconnect()
  }


  RemoteServer localhost() {
    return createServer("localhost", username)
  }

  @Test
  public void testEcho() throws Throwable {
    server = localhost()
    session = server.connect()
    SshCommands cmds = new SshCommands(session)
    def (rv, out) = cmds.command(["echo", "hello world"])
    assert rv == 0
  }

  @Test
  public void testLsLocalhost() throws Throwable {
    server = localhost()
    session = server.connect()
    SshCommands cmds = new SshCommands(session)
    def (rv, out) = cmds.command(["ls", "/"])
    assert rv == 0
  }

  public void testBadCommand() throws Throwable {
    server = localhost()
    session = server.connect()
    SshCommands cmds = new SshCommands(session)
    int rv
    String out
    (rv, out) = cmds.command("false")
    log.info("result: \"$out\"")
    assert rv != 0
  }

  public void testTouchFile() throws Throwable {
    server = localhost()
    session = server.connect()
    SshCommands cmds = new SshCommands(session)
    int rv
    String out
    (rv, out) = cmds.command(["touch", "/tmp/test-touch-file.txt"])
    log.info("result: \"$out\"")
    assert rv == 0
  }


}
