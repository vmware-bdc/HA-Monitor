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

package org.apache.ambari.servicemonitor.clients;

import org.apache.ambari.servicemonitor.utils.Exit;
import org.apache.ambari.servicemonitor.utils.OnDemandFS;
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.Date;


public class LsDir extends BaseClient {
  private static final Log LOG = LogFactory.getLog(LsDir.class);

  public String dirName = "/";
  private Path dir;
  protected OnDemandFS cachedFS;

  @Override
  protected Options createChildOptions(Options options) {
    Option option = new Option("d", "dir", true, "directory");
    option.setRequired(true);
    options.addOption(option);
    return options;
  }

  @Override
  protected void init(CommandLine commandLine) throws IOException {
    super.init(commandLine);
    dirName = commandLine.getOptionValue("dir");
    info("Working with " + FileSystem.getDefaultUri(getConf())
         + " directory: " + dirName);
  }

  @Override
  protected void setup() throws IOException {
    super.setup();
    dir = new Path(dirName);
    cachedFS = new OnDemandFS(getConf());
  }

  @Override
  protected void teardown() throws IOException {
    cachedFS.close();
    super.teardown();
  }


  @Override
  protected Operation executeOneOperation() throws IOException {
    Operation operation = new Operation("ls " + dir);

    started(operation);
    try {
      FileSystem result;
      result = cachedFS.getOrCreate();
      FileSystem fs = result;
      FileStatus dirStatus = fs.getFileStatus(dir);
      StringBuilder builder = new StringBuilder();
      builder.append("File ").append(dirStatus.getPath());
      builder.append(" type: ");
      builder.append(dirStatus.isDir() ? "directory" : "file");
      builder.append(" ");
      builder.append("Last Modified ")
             .append(new Date(dirStatus.getModificationTime()));
      operation.success(builder.toString());
    } catch (ExitClientRunException e) {
      //propagate this up
      throw e;
    } catch (IOException e) {
      //all other outcomes are failures
      operation.failure(e);
    }
    return operation;
  }


  @Override
  public String getDescription() {
    return "ls " + dir;
  }

  public static void main(String[] args) {
    try {
      int res =
        ToolRunnerPlus
          .run(new Configuration(),
               new LsDir(),
               args);
      Exit.exitProcess(true);
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }
}
