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
package org.apache.hadoop.io.ec;

import org.apache.hadoop.hdfs.ec.rawcoder.*;
import org.apache.hadoop.io.ec.rawcoder.*;
import org.apache.hadoop.io.ec.rawcoder.util.GaloisField;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * Ported from HDFS-RAID
 */
public class TestRSRawEncodeDecode {
	final Random RAND = new Random();
  private static GaloisField GF = GaloisField.getInstance();
  private static int symbolSize = 0;
	private static int CHUNK_SIZE = 16 * 1024;

  static {
    symbolSize = (int) Math.round(Math.log(GF.getFieldSize()) / Math.log(2));
  }

	//@Test
	public void testJavaRSPerformance() {
		int dataSize = 10;
		int paritySize = 4;
		RawErasureEncoder encoder = new JavaRSRawEncoder(dataSize, paritySize, CHUNK_SIZE);
		RawErasureDecoder decoder = new JavaRSRawDecoder(dataSize, paritySize, CHUNK_SIZE);
		testRSPerformance(encoder, decoder);
	}

	//@Test
	public void testIsaRSPerformance() {
		int dataSize = 10;
		int paritySize = 4;
		RawErasureEncoder encoder = new IsaRSRawEncoder(dataSize, paritySize, CHUNK_SIZE);
		RawErasureDecoder decoder = new IsaRSRawDecoder(dataSize, paritySize, CHUNK_SIZE);
		testRSPerformance(encoder, decoder);
	}

	private void testRSPerformance(RawErasureEncoder rawEncoder, RawErasureDecoder rawDecoder) {
		int dataSize = 10;
		int paritySize = 4;

		int symbolMax = (int) Math.pow(2, symbolSize);
		ByteBuffer[] message = new ByteBuffer[dataSize];
		byte[][] messageArray = new byte[dataSize][];
		int bufsize = 1024 * 1024 * 10;
		for (int i = 0; i < dataSize; i++) {
			byte[] byteArray = new byte[bufsize];
			for (int j = 0; j < bufsize; j++) {
				byteArray[j] = (byte) RAND.nextInt(symbolMax);
			}
			message[i] = ByteBuffer.wrap(byteArray);
			messageArray[i] = byteArray;
		}
		ByteBuffer[] parity = new ByteBuffer[paritySize];
		for (int i = 0; i < paritySize; i++) {
			parity[i] = ByteBuffer.wrap(new byte[bufsize]);
		}
		long encodeStart = System.currentTimeMillis();

		ByteBuffer[] tmpIn = new ByteBuffer[dataSize];
		ByteBuffer[] tmpOut = new ByteBuffer[paritySize];
		for (int i = 0; i < tmpOut.length; i++) {
			tmpOut[i] = ByteBuffer.wrap(new byte[bufsize]);
		}
		for (int i = 0; i < dataSize; i++) {
			byte[] cpByte = Arrays.copyOfRange(messageArray[i], 0, messageArray[i].length);
			tmpIn[i] = ByteBuffer.wrap(cpByte);
		}
		rawEncoder.encode(tmpIn, tmpOut);
		// Copy parity.
		for (int i = 0; i < paritySize; i++) {
			byte[] cpByte = new byte[bufsize];
			tmpOut[i].get(cpByte);
			parity[i] = ByteBuffer.wrap(cpByte);
		}
		long encodeEnd = System.currentTimeMillis();
		float encodeMSecs = (encodeEnd - encodeStart);
		System.out.println("Time to " + rawEncoder.getClass().getName() + " = " + encodeMSecs + "msec ("
				+ messageArray[0].length / (1000 * encodeMSecs) + " MB/s)");

		int[] erasedLocations = new int[] { 4, 1, 5, 7 };
		ByteBuffer[] erasedValues = new ByteBuffer[4];
		for (int i = 0; i < erasedValues.length; i++) {
			erasedValues[i] = ByteBuffer.wrap(new byte[bufsize]);
		}
		byte[] cpByte = Arrays.copyOfRange(messageArray[0], 0, messageArray[0].length);
		ByteBuffer copy = ByteBuffer.wrap(cpByte);

		ByteBuffer[] data = new ByteBuffer[paritySize + dataSize];
		for (int i = 0; i < paritySize; i++) {
			data[i] = parity[i];
		}
		data[paritySize] = ByteBuffer.wrap(new byte[bufsize]);
		for (int i = 1; i < dataSize; i++) {
			data[i + paritySize] = message[i];
		}

		long decodeStart = System.currentTimeMillis();
		rawDecoder.decode(data, erasedValues, erasedLocations);
		long decodeEnd = System.currentTimeMillis();
		float decodeMSecs = (decodeEnd - decodeStart);
		System.out.println("Time to decode = " + decodeMSecs + "msec ("
				+ messageArray[0].length / (1000 * decodeMSecs) + " MB/s)");
    Assert.assertTrue("Decode failed", copy.equals(erasedValues[0]));
	}

	@Test
	public void testEncodeDecode() {
		// verify the production size.
		//verifyJavaRSRawEncodeDecode(10, 4);
		verifyIsaRSRawEncodeDecode(10, 4);

		// verify a test size
		//verifyJavaRSRawEncodeDecode(3, 3);
		//verifyIsaRSRawEncodeDecode(3, 3);
	}

	private void verifyJavaRSRawEncodeDecode(int dataSize, int paritySize) {
		RawErasureEncoder rawEncoder = new JavaRSRawEncoder(dataSize, paritySize, CHUNK_SIZE);
		RawErasureDecoder rawDecoder = new JavaRSRawDecoder(dataSize, paritySize, CHUNK_SIZE);
		verifyRSEncodeDecode(rawEncoder, rawDecoder, dataSize, paritySize);
	}

	private void verifyIsaRSRawEncodeDecode(int dataSize, int paritySize) {
		RawErasureEncoder rawEncoder = new IsaRSRawEncoder(dataSize, paritySize, CHUNK_SIZE);
		RawErasureDecoder rawDecoder = null;//new IsaRSRawDecoder(dataSize, paritySize, CHUNK_SIZE);
		verifyRSEncodeDecode(rawEncoder, rawDecoder, dataSize, paritySize);
	}

	private void verifyRSEncodeDecode(RawErasureEncoder rawEncoder, RawErasureDecoder rawDecoder, int dataSize, int paritySize) {
		int symbolMax = (int) Math.pow(2, symbolSize);
		ByteBuffer[] message = new ByteBuffer[dataSize];
		byte[][] messageArray = new byte[dataSize][];
		ByteBuffer[] cpMessage = new ByteBuffer[dataSize];
		int bufsize = /*1024 * 1024 */ 16;
		for (int i = 0; i < dataSize; i++) {
			byte[] byteArray = new byte[bufsize];
			for (int j = 0; j < bufsize; j++) {
				byteArray[j] = (byte) RAND.nextInt(symbolMax);
			}
			messageArray[i] = byteArray;
			message[i] = ByteBuffer.allocate(bufsize);
			message[i].put(byteArray);
			message[i].flip();

			cpMessage[i] = ByteBuffer.allocate(bufsize);
			cpMessage[i].put(byteArray);
			cpMessage[i].flip();
		}
		
		ByteBuffer[] parity = new ByteBuffer[paritySize];
		for (int i = 0; i < paritySize; i++) {
			parity[i] = ByteBuffer.allocate(bufsize);
		}

		// encode.
		rawEncoder.encode(cpMessage, parity);

		int erasedLocation = RAND.nextInt(dataSize);
		
		byte[] copyByte = Arrays.copyOfRange(messageArray[erasedLocation], 0, messageArray[erasedLocation].length);
		ByteBuffer copy = ByteBuffer.wrap(copyByte);
		message[erasedLocation] = ByteBuffer.wrap(new byte[bufsize]);
		
		// test decode
		ByteBuffer[] data = new ByteBuffer[dataSize + paritySize];
		for (int i = 0; i < paritySize; i++) {
			data[i] = parity[i];
		}

		for (int i = 0; i < dataSize; i++) {
			data[i + paritySize] = message[i];
		}
		ByteBuffer[] writeBufs = new ByteBuffer[1];
		writeBufs[0] = ByteBuffer.wrap(new byte[bufsize]);
		//rawDecoder.decode(data, writeBufs, new int[] {erasedLocation + paritySize });
		Assert.assertTrue("Decode failed", copy.equals(writeBufs[0]));
	}
}
