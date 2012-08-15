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

public class ColorLog implements AnsiColors {


  private String errColor
    = PREFIX + ATTR_BRIGHT + SEPARATOR + FG_RED + SUFFIX;
  private String warnColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_MAGENTA + SUFFIX;
  private String infoColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_CYAN + SUFFIX;
  private String verboseColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_GREEN + SUFFIX;
  private String debugColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_BLUE + SUFFIX;

  private boolean colorsSet = false;
  private boolean verbose = false;

  public ColorLog(boolean colorsSet) {
    this.colorsSet = colorsSet;
  }

  public boolean isColorsSet() {
    return colorsSet;
  }

  public void setColorsSet(boolean colorsSet) {
    this.colorsSet = colorsSet;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  private String prefix(String color) {
    return colorsSet ?
      (PREFIX + color)
      : "";
  }

  public void info(String text) {
    println(infoColor, text);
  }

  private void println(String color, String text) {
    if (colorsSet) {
      System.out.println(prefix(color) + text + END_COLOR);
    } else {
      System.out.println(text);
    }
  }

  public void info(String text, Throwable throwable) {
    if (throwable == null) {
      println(infoColor, text);
    } else {
      println(errColor, text);
      if (verbose) {
        println(warnColor, throwable.getMessage());
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        for (StackTraceElement elt : stackTrace) {
          println(warnColor, elt.toString());
        }
      }
    }
  }

}
