/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.ec.coder.old.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.ec.coder.old.ErasureCode;
import org.apache.hadoop.hdfs.ec.coder.Encoder;
import org.apache.hadoop.hdfs.ec.coder.old.impl.help.RaidUtils;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ReedSolomonEncoder extends Encoder {
  public static final Log LOG = LogFactory.getLog(
      "org.apache.hadoop.raid.ReedSolomonEncoder");
  private ErasureCode reedSolomonCode;

  public ReedSolomonEncoder(
      Configuration conf, int stripeSize, int paritySize) {
    super(conf, stripeSize, paritySize);
    this.reedSolomonCode = new ReedSolomonCode(stripeSize, paritySize);
  }

  protected void encodeStripe(
      InputStream[] blocks,
      long stripeStartOffset,
      long blockSize,
      OutputStream[] outs,
      Progressable reporter) throws IOException {

    int[] data = new int[stripeSize];
    int[] code = new int[paritySize];
    long start, end, readTotal = 0, writeTotal = 0, calTotal = 0, allstart, allend, alltime;

    allstart = System.nanoTime();
    for (long encoded = 0; encoded < blockSize; encoded += bufSize) {
      // Read some data from each block = bufSize.
      //blocks.length = stripeSize
      start = System.nanoTime();
      for (int i = 0; i < blocks.length; i++) {
        RaidUtils.readTillEnd(blocks[i], readBufs[i], true);
      }
      end = System.nanoTime();
      readTotal += end - start;

      start = System.nanoTime();
      // Encode the data read.
      for (int j = 0; j < bufSize; j++) {
        performEncode(readBufs, writeBufs, j, data, code);
      }
      end = System.nanoTime();
      calTotal += end - start;

      start = System.nanoTime();
      // Now that we have some data to write, send it to the temp files.
      for (int i = 0; i < paritySize; i++) {
        outs[i].write(writeBufs[i], 0, bufSize);
      }
      end = System.nanoTime();
      writeTotal += end - start;

      if (reporter != null) {
        reporter.progress();
      }
    }

    allend = System.nanoTime();
    alltime = allend - allstart;
    System.out.println("[RS encode one strip] readTotal:" + readTotal / 1000 + ", writeTotal:" + writeTotal / 1000
        + ", calTotal:" + calTotal / 1000 + ", all:" + alltime / 1000);
    System.out.printf("read:%3.2f%%, write:%3.2f%%, cal:%3.2f%%\n",
        (float) readTotal * 100 / alltime, (float) writeTotal * 100 / alltime,
        (float) calTotal * 100 / alltime);
  }

  void performEncode(byte[][] readBufs, byte[][] writeBufs, int idx,
                     int[] data, int[] code) {
    for (int i = 0; i < paritySize; i++) {
      code[i] = 0;
    }
    for (int i = 0; i < stripeSize; i++) {
      data[i] = readBufs[i][idx] & 0x000000FF;
    }
    reedSolomonCode.encode(data, code);
    for (int i = 0; i < paritySize; i++) {
      writeBufs[i][idx] = (byte) code[i];
    }
  }

  @Override
  public Path getParityTempPath() {

    return null;
    //    return new Path(RaidNode.rsTempPrefix(conf));
  }

}
