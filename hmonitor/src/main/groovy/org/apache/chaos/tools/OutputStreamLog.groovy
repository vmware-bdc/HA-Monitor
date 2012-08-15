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
 *
 */
@Commons
class OutputStreamLog extends OutputStream {


  private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  OutputStreamLog() {
  }

  @Override
  void write(int b) {
    buffer.write(b)
  }

  @Override
  void flush() {
    super.flush()
    printAndClear()
  }

  private synchronized void printAndClear() {
    String text = buffer.toString("UTF-8")
    log.info(text)
    buffer = new ByteArrayOutputStream();
  }


}
