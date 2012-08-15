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

import org.apache.ambari.servicemonitor.utils.ColorLog;
import org.apache.ambari.servicemonitor.utils.DFSUtils;
import org.apache.ambari.servicemonitor.utils.KillHungProcess;
import org.apache.ambari.servicemonitor.utils.OptionHelper;
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This client is the base class for any monitoring test clients. It is also
 * intended for use in test runs.
 */
public abstract class BaseClient extends ToolPlusImpl
  implements ClientEventListener {
  private static final Log LOG = LogFactory.getLog(BaseClient.class);
  public static final int DEFAULT_SLEEP = 1000;
  protected ColorLog out = new ColorLog(false);

  public int attempts;
  public int attemptLimit = -1;
  public int successCount;
  public int successLimit;
  public int failCount, failLimit = -1;
  public int timeoutCount, timeoutLimit = -1;

  public boolean keepHistory;
  public List<Operation> history = new ArrayList<Operation>();
  public boolean blockingIO;
  public int sleepTime = DEFAULT_SLEEP;
  private ClientEventListener listener = this;
  protected final AtomicBoolean shouldExit = new AtomicBoolean(false);
  private KillHungProcess killHungProcess;

  boolean getShouldExit() {
    return shouldExit.get();
  }

  void setShouldExit(boolean value) {
    shouldExit.set(value);
  }

  @Override
  public int run(String[] args) throws Exception {
    return initAndRun(getCommandLine());
  }

  public int initAndRun(CommandLine commandLine) throws Exception {
    init(commandLine);
    return executeAll() ? 0 : 1;
  }

  /**
   * process the arguments
   */
  protected void init(CommandLine commandLine) throws IOException {
    processBaseOptions(getConf(), commandLine);
  }


  public ClientEventListener getListener() {
    return listener;
  }

  public void setListener(ClientEventListener listener) {
    this.listener = listener;
  }

  public void info(String text) throws ExitClientRunException {
    listener.message(text);
  }

  void info(String text, Throwable throwable) {
    out.info(text, throwable);
  }

  public void debug(String text) throws ExitClientRunException {
    info(text);
  }


  @Override
  public void message(String text) {
    out.info(text);
  }

  @Override
  public void startedEvent(Operation operation) throws ExitClientRunException {
    info("Started: " + operation.toString());
  }

  @Override
  public void waitingEvent(Operation operation) throws ExitClientRunException {
    LOG.debug("Waiting: " + operation);
  }

  @Override
  public void finishedEvent(Operation operation) throws ExitClientRunException {
    info("Finished: " + operation, operation.thrown);
  }


  @Override
  public void endListening() {
  }

  /**
   * this should be called by the client as an operation begins
   *
   * @param operation operation that has started
   */
  public void started(Operation operation) throws ExitClientRunException {
    listener.startedEvent(operation);
  }

  protected void waiting(Operation operation) throws ExitClientRunException {
    listener.waitingEvent(operation);
  }

  public void finished(Operation operation) throws ExitClientRunException {
    listener.finishedEvent(operation);
  }

  /**
   * Increment the counters and check the limits
   *
   * @param operation operation to check
   * @throws LimitsExceededException if a limit was reached
   */
  protected void operationFinished(Operation operation)
    throws LimitsExceededException, ExitClientRunException {
    finished(operation);
    if (operation.getText() != null) {
      LOG.info(operation.getText());
    }
    if (operation.getSucceeded()) {
      successCount++;
    }
    if (operation.getFailed()) {
      failCount++;
    }
    if (operation.timedout()) {
      timeoutCount++;
    }
    checkLimits(operation);
  }

  protected void checkLimits(Operation operation)
    throws LimitsExceededException {
    hasExceeded(operation, "successes", successLimit, successCount);
    hasExceeded(operation, "failures", failLimit, failCount);
    hasExceeded(operation, "timeouts", timeoutLimit, timeoutCount);
  }

  protected void hasExceeded(Operation operation,
                             String limitName,
                             int limitValue,
                             int currentValue)
    throws LimitsExceededException {
    if (limitValue > 0 && currentValue >= limitValue) {
      throw new LimitsExceededException(operation,
                                        "Limit " + limitName + " reached");
    }
  }

  protected void setup() throws IOException {
    Configuration conf = getConf();
    if (blockingIO) {
      LOG.info("DFS client set to block on failures");
      DFSUtils.makeDfsCallsBlocking(conf);
    } else {
      DFSUtils.makeDfsCallsNonBlocking(conf);
    }
  }

  protected void teardown() throws IOException {
    listener.endListening();
  }

  protected abstract Operation executeOneOperation() throws IOException;

  protected FileSystem createUncachedFilesystem() throws IOException {
    Configuration conf = getConf();
    return DFSUtils.createUncachedDFS(conf);
  }

  /**
   * Execute all
   *
   * @return true iff no failures occurred
   * @throws IOException IO issues
   */
  protected boolean executeAll() throws IOException, ExitClientRunException {
    boolean unlimited = attemptLimit < 0;
    setup();
    try {
      if (keepHistory && unlimited) {
        info("Can't keep the history for an unlimited run");
      }
      while (!shouldExit.get()) {
        Operation operation = executeOneOperation();
        operationFinished(operation);
        hasExceeded(operation, "attempts", attemptLimit, attempts++);
        if (keepHistory) {
          history.add(operation);
        }
        waiting(operation);
        if (sleepTime > 0) {
          Thread.sleep(sleepTime);
        }
      }
    } catch (LimitsExceededException limited) {
      info(this + "\n" + limited.getMessage());
    } catch (ExitClientRunException e) {
      //client run exit requested ... fall through into teardown
      shouldExit.set(true);
    } catch (Exception e) {
      info("Operation: failed" + e, e);
    } finally {
      teardown();
    }
    return failCount == 0;
  }

  @Override
  public Options createToolSpecificOptions() {
    Options options = createBaseOptions();
    addToolOptions(options, true);
    return createChildOptions(options);
  }

  /**
   * For overriding
   *
   * @param options options in
   * @return options out
   */
  protected Options createChildOptions(Options options) {
    return options;
  }

  protected Options createBaseOptions() {
    Options options = new Options();
    options.addOption("v", "verbose", false, "verbose output");
    options.addOption("c", "color", false, "color output");
    options.addOption("b", "blocking", false, "blocking operations");
    options
      .addOption("h", "history", false, "build a history of all operations");
    OptionHelper
      .addIntOpt(options, "st", "sleeptime", "sleep time in milliseconds");
    OptionHelper.addIntOpt(options, "al", "attempts",
                           "number of attempts (or -1 for no limit)");
    OptionHelper.addIntOpt(options, "fl", "faillimit",
                           "number of failures before halting (or -1 for no limit)");
    OptionHelper.addIntOpt(options, "sl", "successlimit",
                           "number of successes (or -1 for no limit)");
    OptionHelper.addIntOpt(options, "pt", "processtimeout",
                           "time in milliseconds before the process terminates abnormally if it is still running");
    return options;
  }

  private void addToolOptions(Options options, boolean confRequired) {
    //conf is required
    Option option = OptionHelper
      .addStringArgOpt(options, "cf", "conf", "configuration file");
    option.setType(PatternOptionBuilder.FILE_VALUE);
    option.setRequired(confRequired);
    OptionHelper
      .addStringArgOpt(options, "fs", "filesystem", "filesystem to use");
    OptionHelper.addStringArgOpt(options, "jt", "jobtracker",
                                 "job tracker to connect to");
  }


  private void processBaseOptions(Configuration conf, CommandLine commandLine) {
    if (commandLine.hasOption('v')) {
      out.setVerbose(true);
    }
    if (commandLine.hasOption('c')) {
      out.setColorsSet(true);
    }
    if (commandLine.hasOption('b')) {
      LOG.info("blocking");
      blockingIO = true;
    } else {
      LOG.info("non-blocking");
    }

    if (commandLine.hasOption('h')) {
      keepHistory = true;
    }

    attemptLimit = getIntOption(commandLine, "al");
    int processTimeout = getIntOption(commandLine, "processtimeout");
    if (processTimeout > 0) {
      killHungProcess =
        new KillHungProcess(processTimeout, "Timeout executing process");
    }

    failLimit = getIntOption(commandLine, "fl");
    successLimit = getIntOption(commandLine, "sl");
    sleepTime = getIntOption(commandLine, "st");
    if (sleepTime < 0) {
      sleepTime = DEFAULT_SLEEP;
    }
    if (commandLine.hasOption("fs")) {
      FileSystem.setDefaultUri(conf, commandLine.getOptionValue("fs"));
    }

    if (commandLine.hasOption("jt")) {
      conf.set("mapred.job.tracker", commandLine.getOptionValue("jt"));
    }
    if (commandLine.hasOption("conf")) {
      String[] values = commandLine.getOptionValues("conf");
      for (String value : values) {
        conf.addResource(new Path(value));
      }
    }

  }

  protected int getIntOption(CommandLine commandLine, String key) {

    return OptionHelper.getIntOption(commandLine, key, -1);
  }


  public int getAttempts() {
    return attempts;
  }

  public int getAttemptLimit() {
    return attemptLimit;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getSuccessLimit() {
    return successLimit;
  }

  public int getFailCount() {
    return failCount;
  }

  public int getFailLimit() {
    return failLimit;
  }

  public int getTimeoutCount() {
    return timeoutCount;
  }

  public int getTimeoutLimit() {
    return timeoutLimit;
  }

  public abstract String getDescription();

  @Override
  public String toString() {
    return getDescription()
           + " {"
           + " attempts: " + getAttempts() + " of " + getAttemptLimit()
           + " successes: " + getSuccessCount() + " of " + getSuccessLimit()
           + " failures: " + getFailCount() + " of " + getFailLimit()
           + " }";
  }

  /**
   * Exception thrown when the limits of a run are exceeded
   */
  private static class LimitsExceededException extends IOException {
    private final Operation op;

    private LimitsExceededException(Operation op, String message) {
      super(message, op.thrown);
      this.op = op;
    }

    public Operation getOp() {
      return op;
    }

  }
}
