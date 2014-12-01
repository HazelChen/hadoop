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
package org.apache.hadoop.hdfs.ec.codec;

import org.apache.hadoop.hdfs.ec.ECSchema;
import org.apache.hadoop.hdfs.ec.grouper.BlockGrouper;
import org.apache.hadoop.hdfs.ec.grouper.RSBlockGrouper;

/**
 * Reed-Solomon codec
 */
public abstract class RSErasureCodec extends ErasureCodec {

  @Override
  public void initWith(ECSchema schema) {
	super.initWith(schema);
    //XXX get data size and parity size from schema options
    String dataSize = schema.getOptions().get("k");
    schema.setDataBlocks(Integer.parseInt(dataSize));
    String paritySize = schema.getOptions().get("m");
    schema.setParityBlocks(Integer.parseInt(paritySize));

  }

  @Override
  public BlockGrouper createBlockGrouper() {
    BlockGrouper blockGrouper = new RSBlockGrouper();
    blockGrouper.initWith(getSchema());
    return blockGrouper;
  }

}
