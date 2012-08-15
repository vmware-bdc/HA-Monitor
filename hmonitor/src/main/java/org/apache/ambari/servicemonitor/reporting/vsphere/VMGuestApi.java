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

package org.apache.ambari.servicemonitor.reporting.vsphere;

import org.apache.ambari.servicemonitor.utils.Exit;

import java.util.Locale;

public class VMGuestApi {
  public native int enable();

  public native int disable();

  public native int isEnabled();

  public native int markActive();

  /**
   * Get a string value of the app status. Convert this to lower case for
   * comparing with the string constants.
   *
   * @return the current application monitoring status
   */
  public native String getAppStatus();

  static {
    System.loadLibrary("VMGuestAppMonitorNative");
  }

  /**
   * monitoring status {@value}
   */
  public static final String APP_STATUS_GREEN = "green";
  /**
   * monitoring status {@value}
   */
  public static final String APP_STATUS_RED = "red";
  /**
   * monitoring status {@value}
   */
  public static final String APP_STATUS_GRAY = "gray";


  /**
   * < No error.: {@value}
   */
  public static final int VMGUESTAPPMONITORLIB_ERROR_SUCCESS = 0;
  /**
   * < Other error: {@value}
   */
  public static final int VMGUESTAPPMONITORLIB_ERROR_OTHER = 1;
  /**
   * < Not running in a VM: {@value}
   */
  public static final int VMGUESTAPPMONITORLIB_ERROR_NOT_RUNNING_IN_VM = 2;
  /**
   * < Monitoring is not enabled: {@value}
   */
  public static final int VMGUESTAPPMONITORLIB_ERROR_NOT_ENABLED = 3;

  /**
   * < Monitoring is not supported: {@value}
   */
  public static final int VMGUESTAPPMONITORLIB_ERROR_NOT_SUPPORTED = 4;


  /**
   * Get the monitoring status with the status string converted into lower case
   * to be the same as the constants.
   *
   * @return the monitoring status.
   */
  public String getMonitoringStatus() {
    String statStr = getAppStatus();
    return statStr.toLowerCase(Locale.ENGLISH);
  }


  public static String getStatusCodeText(int code) {
    switch (code) {
      case VMGUESTAPPMONITORLIB_ERROR_SUCCESS:
        return "Success";
      case VMGUESTAPPMONITORLIB_ERROR_OTHER:
        return "Other error";
      case VMGUESTAPPMONITORLIB_ERROR_NOT_RUNNING_IN_VM:
        return "Not running in a VM";
      case VMGUESTAPPMONITORLIB_ERROR_NOT_ENABLED:
        return "Monitoring is not enabled";
      case VMGUESTAPPMONITORLIB_ERROR_NOT_SUPPORTED:
        return "Not supported";
      default:
        return "Unknown status code: " + code;
    }
  }


  /**
   * This entry point came from the sample and is retained for use during
   * debug/experimentation
   *
   * @param args command line arguments
   */
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println(
        "Usage: VMGuestAPI enable|disable|markActive|isEnabled|getStatus");
      Exit.exitProcess(false);
    }

    VMGuestApi appMon = new VMGuestApi();

    System.out.println("args 0 " + args[0]);
    int result = -1;
    if (args[0].equals("enable")) {
      result = appMon.enable();
    } else if (args[0].equals("disable")) {
      result = appMon.disable();
    } else if (args[0].equals("markActive")) {
      result = appMon.markActive();
    } else if (args[0].equals("isEnabled")) {
      result = appMon.isEnabled();
    } else if (args[0].equals("getStatus")) {
      String status = appMon.getAppStatus();
      System.out.println(args[0] + " status " + status);
      Exit.exitProcess(true);
    } else {
      System.out.println("Bad command " + args[0]);
      Exit.exitProcess(false);
    }

    System.out.println(args[0] + " result " + result
                       + ": " + getStatusCodeText(result));
  }
}