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
package org.apache.hadoop.io.ec.coder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.ec.BlockGroup;
import org.apache.hadoop.io.ec.ECBlock;
import org.apache.hadoop.io.ec.ECChunk;
import org.apache.hadoop.io.ec.SubBlockGroup;
import org.apache.hadoop.io.ec.rawcoder.XorRawDecoder;

import java.nio.ByteBuffer;

public class XorDecoder extends AbstractErasureDecoder{
  private static final Log LOG =
      LogFactory.getLog(RSDecoder.class.getName());

  public XorDecoder(int dataSize, int chunkSize) {
    super(new XorRawDecoder(dataSize, chunkSize));
  }

  @Override
  public void decode(BlockGroup blockGroup) {
    SubBlockGroup subBlockGroup = blockGroup.getSubGroups().iterator().next();
    ECBlock[] readBlocks = combineBlocks(subBlockGroup.getDataBlocks(), subBlockGroup.getParityBlocks());
    int[] erasedLocations = getErasedLocations(readBlocks);
    ECBlock[] outputBlocks = getErasedBlocks(readBlocks, erasedLocations);
    beforeCoding(readBlocks, outputBlocks);

    try {
      while (hasNextInputs()) {
        ECChunk[] dataChunks = getNextInputChunks(readBlocks);
        ECChunk[] outputChunks = getNextOutputChunks(outputBlocks);

        ByteBuffer[] readBuffs = convert(dataChunks);
        ByteBuffer[] outputBuffs = convert(outputChunks);
        getRawDecoder().decode(readBuffs, outputBuffs, erasedLocations);

        withCoded(dataChunks, outputChunks);
      }
    } catch(Exception e) {
      LOG.info("Error in decode " + e);
    } finally {
      postCoding(readBlocks, outputBlocks);
    }
  }

  private ECBlock[] combineBlocks(ECBlock[] dataBlocks, ECBlock[] parityBlocks) {
    ECBlock[] result = new ECBlock[dataBlocks.length + parityBlocks.length];
    for (int i = 0; i < dataBlocks.length; ++i) {
      result[i] = dataBlocks[i];
    }
    for (int i = 0; i < parityBlocks.length; ++i) {
      result[i + dataBlocks.length] = parityBlocks[i];
    }
    return result;
  }
}
