#!/usr/bin/python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#script to take in a process name, find a pid directory and kill the process there


#This is unused as kill -9 `cat $pid` usually works, but it is retained in case
# it is needed again as a starting point for other things
# import modules used here -- sys is a very standard one
import sys
import subprocess


HADOOP_PID_DIR = '/var/run/hadoop/'

# Gather our code in a main() function
def main() :
  if len(sys.argv) < 2 :
    print 'Usage ', sys.argv[0], ': <service>'
    return -1
  service = sys.argv[1]

  pidfilename = HADOOP_PID_DIR + service + '.pid'
  print 'filename: ', pidfilename
  pidfile = open(pidfilename, 'r')
  lines = pidfile.readlines()
  pid = ''
  for l in lines :
    if len(l) > 0 :
      pid = l
  if pid == '' :
    print "No process ID  in", pidfilename
    return -1
    #now there is a process to kill
  subprocess.call(["kill", "-9", pid])
  return 0

  # Command line args are in sys.argv[1], sys.argv[2] ...
  # sys.argv[0] is the script name itself and can be ignored


if __name__ == '__main__' :
  rv = main()
  sys.exit(rv)

