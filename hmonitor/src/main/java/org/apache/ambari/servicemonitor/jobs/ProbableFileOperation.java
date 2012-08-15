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


package org.apache.ambari.servicemonitor.jobs;

import org.apache.ambari.servicemonitor.utils.OnDemandFS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;
import java.util.Random;

/**
 * Probable file operations. Execute FS operations based on a probability. Know
 * that the FS is fetched every operation -but is not closed.
 *
 * Probability of 1.0 implies every operation is a probe. Probability of 0.0
 * implies no operations probe the FS.
 *
 * The delay parameter allows for each operation to take a fixed period of time
 * irrespective of whether the file operation takes place. This is to aid
 * testing by simulating long-lived Mappers or Reducers.
 */
public class ProbableFileOperation extends Configured {
  public static final Log LOG = LogFactory.getLog(ProbableFileOperation.class);

  /**
   * Key for the seed: {@value}
   */
  public static final String SEED = ".seed";

  /**
   * Key suffix for the path: {@value}
   */
  public static final String PATH = ".path";

  /**
   * Probability: 0.0-1.0, double: : {@value}
   */
  public static final String PROBABILITY = ".probability";

  /**
   * Delay in milliseconds, 0 for none: : {@value}
   */
  public static final String SLEEPTIME = ".delay";
  public static final String COUNTER = "operations";
  public static final String EXECUTES = "executions";
  private final Path path;
  private final float probability;
  private final Random rng;
  private final int delay;
  public final String name;
  private OnDemandFS cachedFS;


  /**
   * Create an operation
   *
   * @param name prefix for the
   * @param conf configuration to save
   */
  public ProbableFileOperation(String name, Configuration conf) {
    super(conf);
    this.name = name;
    path = new Path(conf.get(name + PATH, "/"));
    probability = conf.getFloat(name + PROBABILITY, 0.0f);
    rng = new Random(conf.getInt(name + SEED, 0));
    delay = conf.getInt(name + SLEEPTIME, 0);
    cachedFS = new OnDemandFS(getConf());
  }

  /**
   * Look at the random number, decide whether or not to query the filestatus of
   * the target path.
   *
   * the counter name+.operations is incremented every time a file operation
   * takes place -this is primarily for testing.
   *
   * @param reporter reporter to increment on file ops
   * @return true iff the file execution took place
   * @throws IOException on any failure to talk to the filesystem.
   */
  public boolean execute(Reporter reporter) throws IOException {
    //first sleep
    if (delay > 0) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException ignored) {

      }
    }
    if (reporter != null) {
      reporter.incrCounter(name, EXECUTES, 1);
    }
    //then the operation
    float random = rng.nextFloat();
    if (random < probability) {
      //execute a file operation
      if (LOG.isDebugEnabled()) {
        LOG.debug("Reading " + path);
      }

      cachedFS.getOrCreate().getFileStatus(path);
      if (reporter != null) {
        reporter.incrCounter(name, COUNTER, 1);
      }
      return true;
    } else {
      return false;
    }
  }
}
