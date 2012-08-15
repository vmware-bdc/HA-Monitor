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

package org.apache.chaos.tools

import groovy.util.logging.Commons

/**
 * This class simulates a blocked process by opening a socket at a defined port, but just 
 * blocking on all requests, rather than attempting to serve them.
 *
 * This is not a 100% reliable implementation of TCP/IP blocking sockets as there
 * may be quirks in how the kernel acks things before invoking the client application -
 * this socket does receive the hand-off, it just then chooses to keep the port open until 
 * closed
 */
@Commons
class HangingSocket implements Closeable {

  ServerSocket serverSocket;
  List<Socket> clients = []


  HangingSocket(int port) {
    serverSocket = new ServerSocket(port)
  }

  synchronized void releaseClients() {
    clients.each { it.close() }
    clients = []
  }

  synchronized void addClient(Socket client) {
    clients << client
  }

  /**
   * Close the server socket and release all clients.
   * Closing the server socket will (implicitly) break the server loop.
   */
  @Override
  synchronized void close() {
    releaseClients()
    serverSocket?.close()
  }

  public void acceptClients() {
    while (!serverSocket.closed) {
      serverSocket.accept(false) {
        addClient(it)
      }
    }
    //to show we've finished, set the serverSocket to null
    serverSocket = null
  }

  public Thread beginAcceptingClients() {
    Thread.start {
      acceptClients()
    }
  }

}
