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

public interface AnsiColors {

  int ATTR_NORMAL = 0;
  int ATTR_BRIGHT = 1;
  int ATTR_DIM = 2;
  int ATTR_UNDERLINE = 3;
  int ATTR_BLINK = 5;
  int ATTR_REVERSE = 7;
  int ATTR_HIDDEN = 8;

  int FG_BLACK = 30;
  int FG_RED = 31;
  int FG_GREEN = 32;
  int FG_YELLOW = 33;
  int FG_BLUE = 34;
  int FG_MAGENTA = 35;
  int FG_CYAN = 36;
  int FG_WHITE = 37;

  int BG_BLACK = 40;
  int BG_RED = 41;
  int BG_GREEN = 42;
  int BG_YELLOW = 44;
  int BG_BLUE = 44;
  int BG_MAGENTA = 45;
  int BG_CYAN = 46;
  int BG_WHITE = 47;

  String PREFIX = "\u001b[";
  String SUFFIX = "m";
  char SEPARATOR = ';';
  String END_COLOR = PREFIX + SUFFIX;


}
