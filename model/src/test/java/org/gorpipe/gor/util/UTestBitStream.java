/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.util;

import org.gorpipe.gor.driver.bgenreader.BitStream;
import org.junit.Assert;
import org.junit.Test;

public class UTestBitStream {

    @Test
    public void testBitStream() {
        final int testIntCount = 100;
        for (byte bitCount = 1; bitCount < 64; ++bitCount) {
            final long maxInt = (1L << bitCount) - 1;
            final int byteArrayLen = (testIntCount * bitCount >> 3) + (((testIntCount * bitCount) & 7) == 0 ? 0 : 1);

            //Generate some random integers.
            final long[] testInts = new long[testIntCount];
            for (int i = 0; i < testIntCount; ++i) {
                testInts[i] = (long) (Math.random() * maxInt);
            }

            //Write the random integers to byte array
            final byte[] bytes = new byte[byteArrayLen];
            int offset = 0, idx = 0, bitSum = 0;
            for (int i = 0; i < testIntCount; ++i) {
                long n = testInts[i];
                bytes[idx++] |= (n << offset) & 0xff;
                n >>>= (8 - offset);
                while (n != 0) {
                    bytes[idx++] = (byte) (n & 0xff);
                    n >>>= 8;
                }
                bitSum += bitCount;
                offset = bitSum & 7;
                idx = bitSum >>> 3;
            }
            final BitStream bitStream = new BitStream(bitCount, bytes, 0);
            for (int i = 0; i < testIntCount; ++i) {
                Assert.assertEquals(testInts[i], bitStream.next());
            }
        }
    }
}
