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


package org.apache.chaos.hadoop

import org.apache.hadoop.fs.ContentSummary
import org.apache.hadoop.fs.permission.FsPermission
import org.apache.hadoop.hdfs.protocol.Block
import org.apache.hadoop.hdfs.protocol.ClientProtocol
import org.apache.hadoop.hdfs.protocol.DatanodeInfo
import org.apache.hadoop.hdfs.protocol.DirectoryListing
import org.apache.hadoop.hdfs.protocol.FSConstants
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus
import org.apache.hadoop.hdfs.protocol.LocatedBlock
import org.apache.hadoop.hdfs.protocol.LocatedBlocks
import org.apache.hadoop.hdfs.security.token.delegation.DelegationTokenIdentifier
import org.apache.hadoop.hdfs.server.common.UpgradeStatusReport
import org.apache.hadoop.io.Text
import org.apache.hadoop.security.token.Token

/**
 * First attempt at stubbing out an unreliable namenode -but how to fit it into the wire?
 */
class UnreliableNamenode implements ClientProtocol {

  ClientProtocol clientDest;

  private void preflight() {

  }

  @Override
  LocatedBlocks getBlockLocations(String src, long offset, long length) {
    return null
  }

  @Override
  void create(String src, FsPermission masked, String clientName, boolean overwrite, boolean createParent, short replication, long blockSize) {
    clientDest.create(src, masked, clientName, overwrite, createParent, replication, blockSize)
  }

  @Override
  void create(String src, FsPermission masked, String clientName, boolean overwrite, short replication, long blockSize) {
    clientDest.create(src, masked, clientName, overwrite, replication, blockSize)

  }

  @Override
  LocatedBlock append(String src, String clientName) {
    return clientDest.append(src, clientName)
  }

  @Override
  boolean recoverLease(String src, String clientName) {
    return clientDest.recoverLease(src, clientName)
  }

  @Override
  boolean setReplication(String src, short replication) {
    return clientDest.setReplication(src, replication)

  }

  @Override
  void setPermission(String src, FsPermission permission) {
    clientDest.setPermission(src, permission)
  }

  @Override
  void setOwner(String src, String username, String groupname) {
    clientDest.setOwner(src, username, groupname)
  }

  @Override
  void abandonBlock(Block b, String src, String holder) {
    clientDest.abandonBlock(b, src, holder)

  }

  @Override
  LocatedBlock addBlock(String src, String clientName) {
    return clientDest.addBlock(src, clientName)
  }

  @Override
  LocatedBlock addBlock(String src, String clientName, DatanodeInfo[] excludedNodes) {
    return clientDest.addBlock(src, clientName, excludedNodes)

  }

  @Override
  boolean complete(String src, String clientName) {
    return clientDest.complete(src, clientName)
  }

  @Override
  void reportBadBlocks(LocatedBlock[] blocks) {

  }

  @Override
  boolean rename(String src, String dst) {
    return clientDest.rename(src, dst)

  }

  @Override
  boolean delete(String src) {
    return clientDest.delete(src)

  }

  @Override
  boolean delete(String src, boolean recursive) {
    return clientDest.delete(src, recursive)
  }

  @Override
  boolean mkdirs(String src, FsPermission masked) {
    return false
  }

  @Override
  DirectoryListing getListing(String src, byte[] startAfter) {
    return null
  }

  @Override
  void renewLease(String clientName) {

  }

  @Override
  long[] getStats() {
    return new long[0]
  }

  @Override
  DatanodeInfo[] getDatanodeReport(FSConstants.DatanodeReportType type) {
    return new DatanodeInfo[0]
  }

  @Override
  long getPreferredBlockSize(String filename) {
    return 0
  }

  @Override
  boolean setSafeMode(FSConstants.SafeModeAction action) {
    return false
  }

  @Override
  void saveNamespace() {

  }

  @Override
  void refreshNodes() {

  }

  @Override
  void finalizeUpgrade() {

  }

  @Override
  UpgradeStatusReport distributedUpgradeProgress(FSConstants.UpgradeAction action) {
    return null
  }

  @Override
  void metaSave(String filename) {

  }

  @Override
  void setBalancerBandwidth(long bandwidth) {

  }

  @Override
  HdfsFileStatus getFileInfo(String src) {
    return null
  }

  @Override
  ContentSummary getContentSummary(String path) {
    return null
  }

  @Override
  void setQuota(String path, long namespaceQuota, long diskspaceQuota) {

  }

  @Override
  void fsync(String src, String client) {

  }

  @Override
  void setTimes(String src, long mtime, long atime) {

  }

  @Override
  Token<DelegationTokenIdentifier> getDelegationToken(Text renewer) {
    return null
  }

  @Override
  long renewDelegationToken(Token<DelegationTokenIdentifier> token) {
    return 0
  }

  @Override
  void cancelDelegationToken(Token<DelegationTokenIdentifier> token) {

  }

  @Override
  long getProtocolVersion(String protocol, long clientVersion) {
    return 0
  }
}
