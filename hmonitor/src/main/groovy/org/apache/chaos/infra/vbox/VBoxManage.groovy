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

package org.apache.chaos.infra.vbox

/**
 * issue vbox manage operations
 * see <a href="http://www.virtualbox.org/manual/ch08.html">http://www.virtualbox.org/manual/ch08.html</a>
 *
 */
class VBoxManage {
/**
 * VBoxManage controlvm        <uuid>|<name>
 pause|resume|reset|poweroff|savestate|
 acpipowerbutton|acpisleepbutton|
 keyboardputscancode <hex> [<hex> ...]|
 setlinkstate<1-N> on|off |
 nic<1-N> null|nat|bridged|intnet|generic
 [<devicename>] |
 nictrace<1-N> on|off
 nictracefile<1-N> <filename>
 nicproperty<1-N> name=[value]
 natpf<1-N> [<rulename>],tcp|udp,[<hostip>],
 <hostport>,[<guestip>],<guestport>
 natpf<1-N> delete <rulename>
 guestmemoryballoon <balloonsize in MB>]
 gueststatisticsinterval <seconds>]
 usbattach <uuid>|<address> |
 usbdetach <uuid>|<address> |
 vrde on|off |
 vrdeport <port> |
 vrdeproperty <name=[value]> |
 vrdevideochannelquality <percent>
 setvideomodehint <xres> <yres> <bpp> [display] |
 screenshotpng <file> [display] |
 setcredentials <username> <password> <domain>
 [--allowlocallogon <yes|no>] |
 teleport --host <name> --port <port>
 [--maxdowntime <msec>] [--password password]
 plugcpu <id>
 unplugcpu <id>
 cpuexecutioncap <1-100>
 */
  static final String CMD_CONTROL_VM = "controlvm"
  /**
   * VBoxManage list [--long|-l] vms|runningvms|ostypes|hostdvds|hostfloppies|
   bridgedifs|dhcpservers|hostinfo|
   hostcpuids|hddbackends|hdds|dvds|floppies|
   usbhost|usbfilters|systemproperties|extpacks

   */
  static final String CMD_LIST = "list"
  /**
   * VBoxManage startvm          <uuid>|<name>...
   * [--type gui|sdl|headless]
   */
  static final String CMD_STARTVM = "startvm"
  static final String pause = "pause"
  static final String resume = "resume"
  static final String reset = "reset"
  static final String poweroff = "poweroff"
  static final String savestate = "savestate"



  private final ProcessBuilder processBuilder;

  /**
   * Run a management operation
   * @param operation operation to exec
   * @param params optional list of parameters
   */
  VBoxManage(String operation, List<String> params) {
    processBuilder = new ProcessBuilder()
    List command = ["VBoxManage", operation]
    if (params) {
      command.addAll(params)
    }
    processBuilder.command(command)

  }

  Process start() {
    processBuilder.start()
  }

  ProcessBuilder getProcessBuilder() {
    return processBuilder
  }

/**
 *
 Create a control VM command
 * @param uuid VM UUID/name
 * @param action action
 * @return the command ready to start
 */
  public static VBoxManage controlVM(String uuid, String action) {
    new VBoxManage(CMD_CONTROL_VM, [uuid, action])
  }
}
