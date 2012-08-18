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

package org.apache.ambari.servicemonitor.unit

import junit.framework.TestCase
import org.apache.chaos.remote.Clustat
import org.junit.Test

class ClustatParserTest extends TestCase {

  static def clustatXML = '''
<clustat version="4.1.1">
  <cluster name="rhel6ha" id="57734" generation="162772"/>
  <quorum quorate="1" groupmember="1"/>
  <nodes>
    <node name="rhel6ha01" state="1" local="0" estranged="0" rgmanager="1" rgmanager_master="0" qdisk="0" nodeid="0x00000001"/>
    <node name="rhel6ha02" state="1" local="1" estranged="0" rgmanager="1" rgmanager_master="0" qdisk="0" nodeid="0x00000002"/>
  </nodes>
  <groups>
    <group name="service:NameNodeService" state="112" state_str="started" flags="0" flags_str="" owner="rhel6ha01"
      last_owner="rhel6ha02" restarts="0" last_transition="1345237321" last_transition_str="Fri Aug 17 14:02:01 2012"/>
  </groups>
</clustat>
'''
  protected static final String NN_SERVICE_GROUP = "service:NameNodeService"
  protected static final String SERVER1 = "rhel6ha01"

  Clustat cluster

  @Override
  protected void setUp() {
    cluster = new Clustat(clustatXML)
  }

  @Test
  public void testDumpCluster() throws Throwable {
    Node xml = cluster.xml
    assert xml.name() == "clustat"
  }

  public void testFindClustatChildren() {
    Node clustat = cluster.xml
    assert clustat != null
    assert clustat.@version == "4.1.1"
  }

  public void testFindNodes() {
    NodeList nodes = cluster.clusterNodes()
    Node n1 = nodes[0]
    Node n2 = nodes[1]
    assert nodes.size() == 2
  }

  @Test
  public void testClusterNode() throws Throwable {
    def node = cluster.clusterNode(SERVER1)
    assert node != null
  }

  @Test
  public void testFindService() throws Throwable {
    def serviceGroup = cluster.serviceGroup(NN_SERVICE_GROUP)
    assert serviceGroup != null
    assert serviceGroup.@owner == SERVER1
  }

  @Test
  public void testHostLookup() throws Throwable {
    assert cluster.hostRunningService(NN_SERVICE_GROUP) == SERVER1
  }
}
