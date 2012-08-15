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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An input format that assigns ranges of ints to each mapper. It doesn't need
 * any files, so it's used to consume things
 */
public class RangeInputFormat
  implements InputFormat<IntWritable, IntWritable> {
  public static final Log LOG = LogFactory.getLog(RangeInputFormat.class);

  /**
   * An input split consisting of a range on numbers.
   */
  public static class RangeInputSplit implements InputSplit {
    private int firstRow;
    private int rowCount;


    public RangeInputSplit() {
    }

    public RangeInputSplit(int offset, int length) {
      firstRow = offset;
      rowCount = length;
    }

    public long getLength() throws IOException {
      return 0;
    }

    public String[] getLocations() throws IOException {
      return new String[]{};
    }

    public void readFields(DataInput in) throws IOException {
      firstRow = WritableUtils.readVInt(in);
      rowCount = WritableUtils.readVInt(in);
    }

    public void write(DataOutput out) throws IOException {
      WritableUtils.writeVInt(out, firstRow);
      WritableUtils.writeVInt(out, rowCount);
    }
  }

  /**
   * A record reader that will generate a range of numbers.
   */
  public static class RangeRecordReader
    implements RecordReader<IntWritable, IntWritable> {
    private int startRow;
    private int finishedRows;
    private int totalRows;

    public RangeRecordReader(RangeInputSplit split) {
      startRow = split.firstRow;
      finishedRows = 0;
      totalRows = split.rowCount;
    }

    public void close() throws IOException {
      // NOTHING
    }

    public IntWritable createKey() {
      return new IntWritable();
    }

    public IntWritable createValue() {
      return new IntWritable();
    }

    public long getPos() throws IOException {
      return finishedRows;
    }

    public float getProgress() throws IOException {
      return finishedRows / (float) totalRows;
    }

    public boolean next(IntWritable key,
                        IntWritable value) {
      if (finishedRows < totalRows) {
        key.set(startRow + finishedRows);
        value.set(key.get());
        finishedRows++;
        return true;
      } else {
        return false;
      }
    }

  }

  public RecordReader<IntWritable, IntWritable>
  getRecordReader(InputSplit split, JobConf job,
                  Reporter reporter) throws IOException {
    return new RangeRecordReader((RangeInputSplit) split);
  }

  /**
   * Create the desired number of splits, dividing the number of rows between
   * the mappers.
   */
  public InputSplit[] getSplits(JobConf job,
                                int numSplits) {
    int totalRows = job.getInt(JobKeys.RANGEINPUTFORMAT_ROWS, 0);
    int rowsPerSplit = totalRows / numSplits;
    LOG.info("Generating " + totalRows + " using " + numSplits +
             " maps with step of " + rowsPerSplit);
    InputSplit[] splits = new InputSplit[numSplits];
    int currentRow = 0;
    for (int split = 0; split < numSplits - 1; ++split) {
      splits[split] = new RangeInputSplit(currentRow, rowsPerSplit);
      currentRow += rowsPerSplit;
    }
    splits[numSplits - 1] = new RangeInputSplit(currentRow,
                                                totalRows - currentRow);
    return splits;
  }

}