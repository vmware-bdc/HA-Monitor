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

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;
import java.util.Iterator;

/**
 * A reducer that probably implements file operations. Works for shuffles too
 */
public class FileUsingReducer extends MapReduceBase
  implements Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {

  public static final String NAME = "fileusingreducer";

  private ProbableFileOperation operation;
  IntWritable iw = new IntWritable();

  @Override
  public void configure(JobConf job) {
    super.configure(job);
    operation = new ProbableFileOperation(NAME, job);
  }


  /**
   * Reduce: return the first value in the list if present
   *
   * @param key the key.
   * @param values the list of values to reduce.
   * @param output to collect keys and combined values.
   * @param reporter facility to report progress.
   * @throws IOException on a file IO problem
   */
  @Override
  public void reduce(IntWritable key,
                     Iterator<IntWritable> values,
                     OutputCollector<IntWritable, IntWritable> output,
                     Reporter reporter) throws IOException {
    operation.execute(reporter);
    int sum = 0;
    while (values.hasNext()) {
      IntWritable next = values.next();
      sum += next.get();
    }
    iw.set(sum);
    if (values.hasNext()) {
      output.collect(key, iw);
    }
  }
}
