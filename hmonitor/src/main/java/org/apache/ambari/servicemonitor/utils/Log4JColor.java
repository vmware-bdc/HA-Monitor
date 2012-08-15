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

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * from Ant's AnsiColorLogger - a logger that tries to log in color. 
 * Not currently working.
 */
public class Log4JColor extends PatternLayout implements AnsiColors {


  private static final String errColor
    = PREFIX + ATTR_BRIGHT + SEPARATOR + FG_RED + SUFFIX;
  private static final String warnColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_MAGENTA + SUFFIX;
  private static final String infoColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_CYAN + SUFFIX;
  private static final String traceColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_GREEN + SUFFIX;
  private static final String debugColor
    = PREFIX + ATTR_DIM + SEPARATOR + FG_BLUE + SUFFIX;

  @Override
  public String format(LoggingEvent event) {
    String message = super.format(event);
    String prefix = getColorPrefix(event.getLevel());

    return prefix + message + END_COLOR;
  }

  public String getColorPrefix(Level level) {
    if (Level.FATAL.equals(level)) {
      return errColor;
    }
    if (Level.ERROR.equals(level)) {
      return errColor;
    }
    if (Level.WARN.equals(level)) {
      return warnColor;
    }
    if (Level.INFO.equals(level)) {
      return infoColor;
    }
    if (Level.DEBUG.equals(level)) {
      return debugColor;
    }
    if (Level.TRACE.equals(level)) {
      return traceColor;
    }

    return infoColor;
  }
}
