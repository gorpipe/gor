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
package org.gorpipe.util.collection;

import org.gorpipe.util.collection.extract.Extract;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/**
 * Test the ByteArray class
 *
 * @version $Id$
 */
public class UTestByteArray extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(UTestByteArray.class);

    private ByteArray ba;

    private final String testDataPlain = "Line1\nLine2\nLine3\nLine4\n";
    private final String testDataMulti = "Line1\n\r\n\rLine2\r\n\r\nLine3\n\r";
    private final String testDataMissingEOL = "Line1\nLine2\nLine3\nLine4";
    private final String testDataNoEOL = "String with no end of line";

    /**
     * Construct UTestByteArray
     *
     * @param test
     */
    public UTestByteArray(String test) {
        super(test);
    }

    /**
     * Test the ByteArray
     */
    public void testByteArray() {
        float floatVal = 42.42f;
        byte[] littleendian = {-42, 42, 0x2, 0x1, 0x2, -1, 0x4, 0x3, 0x2, 0x1, 0x4, 0x3, 0x2, -1, 0x14, (byte) 0xAE, 0x29, 0x42};
//    		   ,(byte)0xD7,(byte)0xA9,0x23,(byte)0xC0,(byte)0xF4,0x10,0x22,0x11};
        byte[] bigendian = {-42, 42, 0x1, 0x2, -1, 0x2, 0x1, 0x2, 0x3, 0x4, -1, 0x2, 0x3, 0x4, 0x42, 0x29, (byte) 0xAE, 0x14,
                0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x11};


        ba = new ByteArray(bigendian, ByteOrder.BIG_ENDIAN);
        assertEquals(-42, ba.readByte());
        assertEquals(42, ba.readUnsignedByte());
        assertEquals(258, ba.readUnsignedShort());
        assertEquals(-254, ba.readShort());
        assertEquals(16909060, ba.readUnsignedInt());
        assertEquals(-16645372, ba.readInt());
        assertEquals(floatVal, ba.readFloat());
        assertEquals(1157442765409226769L, ba.readLong());

        assertEquals(-42, ByteArray.readByte(littleendian, 0));
        assertEquals(42, ByteArray.readUnsignedByte(littleendian, 1));
        assertEquals(258, ByteArray.readUnsignedShort(littleendian, 2, ByteOrder.LITTLE_ENDIAN));
        assertEquals(-254, ByteArray.readShort(littleendian, 4, ByteOrder.LITTLE_ENDIAN));
        assertEquals(16909060, ByteArray.readUnsignedInt(littleendian, 6, ByteOrder.LITTLE_ENDIAN));
        assertEquals(-16645372, ByteArray.readInt(littleendian, 10, ByteOrder.LITTLE_ENDIAN));
        assertEquals(floatVal, ByteArray.readFloat(littleendian, 14, ByteOrder.LITTLE_ENDIAN));
    }

    private void checkParse(double value) {
        final byte[] byteval = String.valueOf(value).getBytes();
        final double newvalue = ByteArray.toDouble(byteval, 0, byteval.length);
        // special checking for NaN
        if (Double.isNaN(value)) {
            if (!Double.isNaN(newvalue)) {
                assertSame(value, newvalue);
            }
            return;
        }
        if (value != newvalue) {
            final double relativeError = Math.abs((newvalue - value) / value);
            assertTrue("Expected " + value + " but was " + newvalue, relativeError <= 0.00001);
        }
    }

    /**
     * Test convertion from text to double
     */
    public void testToDouble() {
        final Random r = new Random();
        checkParse(Double.MIN_VALUE);
        checkParse(0);
        checkParse(1.1111);
        checkParse(-1.1111);
        checkParse(Double.MIN_VALUE + 1);
        checkParse(Double.MAX_VALUE - 1);
        checkParse(Double.MAX_VALUE);
        checkParse(Double.NaN);
        checkParse(Double.POSITIVE_INFINITY);
        checkParse(Double.NEGATIVE_INFINITY);
        for (int i = 0; i < 100000; i++) {
            final double value = r.nextDouble();
            checkParse(value);
        }
        for (int i = 0; i < 100000; i++) {
            final double value = r.nextDouble() * 10000000000000L;
            checkParse(value);
        }

        final byte[] byteval = String.valueOf("1.2333").getBytes();
        for (int k = 0; k < 4; k++) {
            final int COUNT = 1000000;
            final long mystart = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                ByteArray.toDouble(byteval, 0, byteval.length);
            }
            final long mystop = System.currentTimeMillis();
            final long jdkstart = System.currentTimeMillis();
            for (int i = 0; i < COUNT; i++) {
                ByteArray.jdkToDouble(byteval, 0, byteval.length);
            }
            final long jdkstop = System.currentTimeMillis();

            log.info("My = {} jdk = {}", Extract.durationString(mystop - mystart), Extract.durationString(jdkstop - jdkstart));
        }
    }

    /**
     * Test long convertion
     */
    public void testToLong() {
        byte[] l1 = "123456789019292".getBytes();
        byte[] l2 = "123456789c019292".getBytes();
        long value = ByteArray.toLong(l1, 0, l1.length);
        assertEquals(123456789019292L, value);
        value = ByteArray.toLong(l2, 0, l2.length);
        assertEquals(123456789L, value);
        assertFalse(ByteArray.isLong(l2, 0, l2.length));
        try {
            ByteArray.toLongCheckLen(l2, 0, l2.length);
            fail("Expected an exception when converting to long");
        } catch (NumberFormatException ex) {
            // Fallback since this is the expected
        }

        try {
            ByteArray.toLongWithException("20,6".getBytes(), 0);
            fail("Expected Exception when converting to integer");
        } catch (NumberFormatException ex) {
            // Fallback since this is the expected
        }

        try {
            ByteArray.toLongWithException("20.2".getBytes(), 0);
            fail("Expected Exception when converting to integer");
        } catch (NumberFormatException ex) {
            // Fallback since this is the expected
        }
    }

    /**
     * Test finding beginning of line.
     */
    public void testBeginOfLine() {
        // Plain forward
        assertEquals(6, ByteArray.beginOfLine(testDataPlain.getBytes(), 0, true));
        assertEquals(12, ByteArray.beginOfLine(testDataPlain.getBytes(), 9, true));
        assertEquals(24, ByteArray.beginOfLine(testDataPlain.getBytes(), 20, true));
        // Plain backward
        assertEquals(0, ByteArray.beginOfLine(testDataPlain.getBytes(), 0, false));
        assertEquals(18, ByteArray.beginOfLine(testDataPlain.getBytes(), 20, false));

        // Multiple EOL forward
        assertEquals(9, ByteArray.beginOfLine(testDataMulti.getBytes(), 0, true));
        assertEquals(18, ByteArray.beginOfLine(testDataMulti.getBytes(), 12, true));
        assertEquals(18, ByteArray.beginOfLine(testDataMulti.getBytes(), 16, true));
        assertEquals(25, ByteArray.beginOfLine(testDataMulti.getBytes(), 18, true));
        // Multiple EOL backward
        assertEquals(0, ByteArray.beginOfLine(testDataMulti.getBytes(), 4, false));
        assertEquals(9, ByteArray.beginOfLine(testDataMulti.getBytes(), 12, false));
        assertEquals(9, ByteArray.beginOfLine(testDataMulti.getBytes(), 16, false));
        assertEquals(18, ByteArray.beginOfLine(testDataMulti.getBytes(), 18, false));

        // Missing EOL
        assertEquals(23, ByteArray.beginOfLine(testDataMissingEOL.getBytes(), 21, true));

        // No EOL
        assertEquals(testDataNoEOL.length(), ByteArray.beginOfLine(testDataNoEOL.getBytes(), 10, true));
        assertEquals(0, ByteArray.beginOfLine(testDataNoEOL.getBytes(), 10, false));
    }


    /**
     * Test finding end of line.
     */
    public void testEndOfLine() {
        // Plain
        assertEquals(5, ByteArray.endOfLine(testDataPlain.getBytes(), 0));
        assertEquals(11, ByteArray.endOfLine(testDataPlain.getBytes(), 9));
        assertEquals(11, ByteArray.endOfLine(testDataPlain.getBytes(), 11));
        assertEquals(23, ByteArray.endOfLine(testDataPlain.getBytes(), 20));

        // Multiple EOL
        assertEquals(5, ByteArray.endOfLine(testDataMulti.getBytes(), 0));
        assertEquals(14, ByteArray.endOfLine(testDataMulti.getBytes(), 12));
        assertEquals(16, ByteArray.endOfLine(testDataMulti.getBytes(), 16));
        assertEquals(23, ByteArray.endOfLine(testDataMulti.getBytes(), 18));

        // Missing EOL
        assertEquals(23, ByteArray.endOfLine(testDataMissingEOL.getBytes(), 21));

        // No EOL
        assertEquals(testDataNoEOL.length(), ByteArray.endOfLine(testDataNoEOL.getBytes(), 10));
    }


    /**
     * Test encoding byte arrays as 7 bit vector, friendly to ASCII text and decoding back
     */
    public void test7BitEncode() {
        final byte[] bytes = new byte[1024 * 7];
        Random rand = new Random();
        rand.nextBytes(bytes);

        for (int i = 0; i < bytes.length; i++) {
            byte[] values = new byte[i];
            System.arraycopy(bytes, 0, values, 0, values.length);
            byte[] bit7values = ByteArray.to7Bit(values);

            // Check that decode to 8 bits works
            byte[] bit8Values = ByteArray.to7Bit(bit7values);
            Arrays.equals(values, bit8Values);

            // Check that decode to 8 bits, inplace, works too
            ByteArray.to8BitInplace(bit7values, 0, bit7values.length);
            for (int j = 0; j < i; j++) {
                assertEquals(values[j], bit7values[j]);
            }
        }
    }

}
