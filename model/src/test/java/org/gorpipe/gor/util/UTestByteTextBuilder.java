/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Locale;
import java.util.Random;

/**
 * Test ByteTextBuilder
 *
 * @version $Id$
 */
public class UTestByteTextBuilder {

    private static final double DELTA = 1e-8;

    /**
     * Test like comparison with the text builder
     */
    @Test
    public void testLike() {
        final ByteTextBuilder builder = new ByteTextBuilder(1000);
        Assert.assertTrue(builder.isEmpty());
        String text = "Paul was alone in the world and the world was alone too!";
        builder.append(text);
        Assert.assertEquals(builder, builder);
        Assert.assertFalse(builder.isEmpty());

        Assert.assertTrue(builder.isLike(false, true, "Paul".getBytes()));
        Assert.assertTrue(builder.isLike(false, true, text.getBytes()));
        Assert.assertTrue(builder.isLike(true, false, text.getBytes()));
        Assert.assertTrue(builder.isLike(true, true, "world".getBytes()));
        Assert.assertTrue(builder.isLike(true, true, " al".getBytes()));
        Assert.assertFalse(builder.isLike(true, true, "worLd".getBytes()));
        Assert.assertTrue(builder.isLike(true, false, "alone too!".getBytes()));
        Assert.assertTrue(builder.isLike(false, false, "Paul".getBytes(), "alone".getBytes(), "world".getBytes(), "too!".getBytes()));

        Assert.assertFalse(builder.isLike(false, true, "pAul".getBytes()));
        Assert.assertFalse(builder.isLike(false, true, text.toLowerCase().getBytes()));
        Assert.assertFalse(builder.isLike(true, false, text.toLowerCase().getBytes()));
        Assert.assertFalse(builder.isLike(true, false, "Alone Too!".getBytes()));

        Assert.assertTrue(builder.isLikeIgnoreCase(false, true, "pAul".getBytes()));
        Assert.assertTrue(builder.isLikeIgnoreCase(false, true, text.toLowerCase().getBytes()));
        Assert.assertTrue(builder.isLikeIgnoreCase(true, false, text.toLowerCase().getBytes()));
        Assert.assertTrue(builder.isLikeIgnoreCase(true, true, "worLd".getBytes()));
        Assert.assertTrue(builder.isLikeIgnoreCase(true, false, "Alone Too!".getBytes()));
        Assert.assertTrue(builder.isLikeIgnoreCase(false, false, "PaUl".getBytes(), "Alone".getBytes(), "World".getBytes(), "tOo!".getBytes()));

        builder.clear();
        final String str = "Kalli og Palli";
        builder.append(str.getBytes());
        Assert.assertEquals(str.length(), builder.size());
        Assert.assertEquals(str, builder.toString());
        builder.set((byte) 'A');
        Assert.assertEquals('A', builder.charAt(0));
    }

    /**
     * Test writing integer values as fixed point floating values
     */
    @Test
    public void testWriteAsFixedPointFloat() {
        final ByteTextBuilder builder = new ByteTextBuilder(1000);
        builder.appendAsFixedPointFloat(Integer.MIN_VALUE, 6);
        Assert.assertEquals("-2147.483648", builder.toString());

        builder.setAsFixedPointFloat(Integer.MAX_VALUE, 6);
        Assert.assertEquals("2147.483647", builder.toString());

        for (int i = -1000; i < 1000; i++) {
            builder.setAsFixedPointFloat(i, 5);
            final String expected = String.format(Locale.ENGLISH, "%.5f", i / 100000d).replace("0.", ".");
            Assert.assertEquals(expected, builder.toString());
        }

        builder.setAsFixedPointFloat(Integer.MAX_VALUE, 2);
        Assert.assertEquals("21474836.47", builder.toString());

        builder.setAsFixedPointFloat(123456, 2);
        Assert.assertEquals("1234.56", builder.toString());

        builder.setAsFixedPointFloat(1234567, 1);
        Assert.assertEquals("123456.7", builder.toString());

        builder.setAsFixedPointFloat(-1234567, 3);
        Assert.assertEquals("-1234.567", builder.toString());
    }

    /**
     * Test that ByteTextBuilder hashing and equals methods work as expected
     */
    @Test
    public void testHashingAndEquals() {
        final ByteTextBuilder builder = new ByteTextBuilder(1000);
        builder.set("Sometext");
        int hash1 = builder.hashCode();
        final ByteTextBuilder builder2 = new ByteTextBuilder(1000);
        builder2.set(new String("Sometext"));
        int hash2 = builder2.hashCode();
        Assert.assertEquals(hash1, hash2);
        hash1 = builder.hashCode();
        hash2 = builder2.hashCode();
        Assert.assertEquals(hash1, hash2);
        Assert.assertEquals(builder, builder2);
        builder2.set(new String("Sometext2"));
        Assert.assertNotEquals(builder, builder2);

        // Test equal-length but different strings
        builder.set("bingo");
        builder2.set("bongo");
        Assert.assertNotEquals(builder, builder2);

        builder.set("A");
        builder2.set(new String("A"));
        hash1 = builder.hashCode();
        hash2 = builder2.hashCode();
        Assert.assertEquals(hash1, hash2);

        final ByteTextBuilder empty = new ByteTextBuilder();
        Assert.assertEquals(0, empty.hashCode());
    }

    /**
     * Test setting byte arrays, different handling for 0, 1, 2, and then more bytes to be copied. Check them all
     */
    @Test
    public void testByteSetting() {
        final ByteTextBuilder builder = new ByteTextBuilder(100);
        final byte[] src = {1, 2, 4};
        builder.set(src, 0, 0);
        Assert.assertEquals(0, builder.getBytes().length);
        builder.set(src, 0, 1);
        Assert.assertEquals(1, builder.getBytes().length);
        Assert.assertEquals(src[0], builder.getBytes()[0]);
        builder.set(src, 0, 2);
        Assert.assertEquals(2, builder.getBytes().length);
        Assert.assertEquals(src[0], builder.getBytes()[0]);
        Assert.assertEquals(src[1], builder.getBytes()[1]);
        builder.set(src, 0, 3);
        Assert.assertEquals(3, builder.getBytes().length);
        Assert.assertEquals(src[0], builder.getBytes()[0]);
        Assert.assertEquals(src[1], builder.getBytes()[1]);
        Assert.assertEquals(src[2], builder.getBytes()[2]);
    }

    /**
     * Test Double parsing
     */
    @Test
    public void testDouble() {
        final ByteTextBuilder builder = new ByteTextBuilder(100);
        builder.set("inf");
        Assert.assertEquals(Double.POSITIVE_INFINITY, builder.toDouble(), DELTA);
        builder.set("iNf");
        Assert.assertEquals(Double.POSITIVE_INFINITY, builder.toDouble(), DELTA);
        builder.set("Infinity");
        Assert.assertEquals(Double.POSITIVE_INFINITY, builder.toDouble(), DELTA);
        builder.set("-inf");
        Assert.assertEquals(Double.NEGATIVE_INFINITY, builder.toDouble(), DELTA);
        builder.set("-inF");
        Assert.assertEquals(Double.NEGATIVE_INFINITY, builder.toDouble(), DELTA);
        builder.set("-Infinity");
        Assert.assertEquals(Double.NEGATIVE_INFINITY, builder.toDouble(), DELTA);
        builder.set("nAn");
        Assert.assertEquals(Double.NaN, builder.toDouble(), DELTA);
        builder.set("42.42");
        Assert.assertEquals(42.42, builder.toDouble(), DELTA);
    }

    /**
     * Basic testing of integers
     */
    @Test
    public void testInt() {
        final ByteTextBuilder builder = new ByteTextBuilder(100);
        builder.append(0);
        Assert.assertEquals("0", builder.toString());
        Assert.assertEquals(1, builder.length());
        Assert.assertEquals(0, builder.toInt());
        Assert.assertEquals(0, builder.toLong());

        builder.set(-1);
        Assert.assertEquals("-1", builder.toString());
        Assert.assertEquals(2, builder.length());
        Assert.assertEquals(-1, builder.toInt());
        Assert.assertEquals(-1, builder.toLong());

        builder.clear();
        builder.append(Integer.MIN_VALUE);
        Assert.assertEquals("-2147483648", builder.toString());
        Assert.assertEquals(Integer.MIN_VALUE, builder.toInt());
        Assert.assertEquals(Integer.MIN_VALUE, builder.toLong());

        builder.clear();
        builder.append(2147483647);
        Assert.assertEquals("2147483647", builder.toString());
        Assert.assertEquals(10, builder.length());
        Assert.assertEquals(2147483647, builder.toInt());
        Assert.assertEquals(2147483647, builder.toLong());
        Assert.assertEquals(10, builder.getBytes().length);

        Random rand = new Random();
        for (int i = 0; i < 1000; i++) {
            final int v = rand.nextInt();
            builder.set(v);
            Assert.assertEquals(v, builder.toInt());
        }
    }

    /**
     * Basic testing of longs
     */
    @Test
    public void testLong() {
        final ByteTextBuilder builder = new ByteTextBuilder(100);
        builder.append(0L);
        Assert.assertEquals("0", builder.toString());
        Assert.assertEquals(1, builder.length());
        Assert.assertEquals(0, builder.toInt());
        Assert.assertEquals(0, builder.toLong());

        builder.set(-1L);
        Assert.assertEquals("-1", builder.toString());
        Assert.assertEquals(2, builder.length());
        Assert.assertEquals(-1, builder.toInt());
        Assert.assertEquals(-1, builder.toLong());

        builder.clear();
        builder.append((long) Integer.MIN_VALUE);
        Assert.assertEquals("-2147483648", builder.toString());
        Assert.assertEquals(Integer.MIN_VALUE, builder.toInt());
        Assert.assertEquals(Integer.MIN_VALUE, builder.toLong());

        builder.clear();
        builder.append(2147483647L);
        Assert.assertEquals("2147483647", builder.toString());
        Assert.assertEquals(10, builder.length());
        Assert.assertEquals(2147483647, builder.toInt());
        Assert.assertEquals(2147483647, builder.toLong());
        Assert.assertEquals(10, builder.getBytes().length);

        builder.set(Long.MIN_VALUE);
        Assert.assertEquals(Long.MIN_VALUE, builder.toLong());
        builder.set(Long.MAX_VALUE);
        Assert.assertEquals(Long.MAX_VALUE, builder.toLong());

        Random rand = new Random();
        for (int i = 0; i < 1000; i++) {
            final long l = rand.nextLong();
            builder.set(l);
            Assert.assertEquals(l, builder.toLong());
        }
    }

    /**
     * Basic testing of strings
     */
    @Test
    public void testString() {
        final ByteTextBuilder builder = new ByteTextBuilder(100);
        builder.append("kalli");
        Assert.assertEquals("kalli", builder.toString());
        Assert.assertEquals("kalli".length(), builder.length());

        builder.clear();
        Assert.assertEquals(0, builder.length());
        builder.append("kalli");
        builder.append(42);
        Assert.assertEquals("kalli42", builder.toString());
        Assert.assertEquals("lli4", builder.subSequence(2, 6));
        byte[] buf = new byte[100];
        builder.copy(buf, 3);
        Assert.assertEquals("kalli42", new String(buf, 3, 7));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000000; i++) {
            sb.setLength(0);
            sb.append("kalli");
            sb.append("palli");
            sb.append("nalli");
        }

        ByteTextBuilder b = new ByteTextBuilder(100);
        for (int i = 0; i < 1000000; i++) {
            b.set("kalli");
            b.append("palli");
            b.append("nalli");
        }

        Assert.assertEquals(sb.toString(), b.toString());
    }

    /**
     * Basic testing of byte and byte arrays
     */
    @Test
    public void testByte() {
        final ByteTextBuilder builder = new ByteTextBuilder(100);
        builder.append((byte) 0);
        Assert.assertEquals(1, builder.length());
        Assert.assertEquals(0, builder.toInt(false));

        builder.clear();
        builder.append((byte) '1');
        builder.append((byte) '2');
        builder.append((byte) '3');
        builder.append((byte) '4');
        builder.append((byte) '5');
        Assert.assertEquals(5, builder.length());
        Assert.assertEquals(12345, builder.toInt());
        builder.set(builder.getBytes());
        Assert.assertEquals(12345, builder.toInt());
    }

    @Test
    public void testbyteAt() {
        final ByteTextBuilder b = new ByteTextBuilder("bingo");
        Assert.assertEquals('b', b.byteAt(0));
        Assert.assertEquals('o', b.byteAt(4));
    }

    @Test
    public void byteAtThrowsWhenOutOfBounds() {
        final ByteTextBuilder b = new ByteTextBuilder("bingo");
        Assert.assertThrows(IndexOutOfBoundsException.class, () -> b.byteAt(-1));
        Assert.assertThrows(IndexOutOfBoundsException.class, () -> b.byteAt(5));
    }

    /**
     * Test various stuff
     */
    @Test
    public void testCntDigits() {
        Assert.assertEquals(1, ByteTextBuilder.cntDigits(1));
        Assert.assertEquals(2, ByteTextBuilder.cntDigits(18));
        Assert.assertEquals(3, ByteTextBuilder.cntDigits(100));
        Assert.assertEquals(3, ByteTextBuilder.cntDigits(199));
        Assert.assertEquals(3, ByteTextBuilder.cntDigits(301));
        Assert.assertEquals(4, ByteTextBuilder.cntDigits(1000));
        Assert.assertEquals(5, ByteTextBuilder.cntDigits(10001));
        Assert.assertEquals(6, ByteTextBuilder.cntDigits(910001));
        Assert.assertEquals(7, ByteTextBuilder.cntDigits(3910001));
        Assert.assertEquals(8, ByteTextBuilder.cntDigits(39100019));
        Assert.assertEquals(9, ByteTextBuilder.cntDigits(391000119));
        Assert.assertEquals(10, ByteTextBuilder.cntDigits(1910001393));

        Assert.assertEquals(1, ByteTextBuilder.cntDigits(1L));
        Assert.assertEquals(2, ByteTextBuilder.cntDigits(18L));
        Assert.assertEquals(3, ByteTextBuilder.cntDigits(100L));
        Assert.assertEquals(3, ByteTextBuilder.cntDigits(199L));
        Assert.assertEquals(3, ByteTextBuilder.cntDigits(301L));
        Assert.assertEquals(4, ByteTextBuilder.cntDigits(1000L));
        Assert.assertEquals(5, ByteTextBuilder.cntDigits(10001L));
        Assert.assertEquals(6, ByteTextBuilder.cntDigits(910001L));
        Assert.assertEquals(7, ByteTextBuilder.cntDigits(3910001L));
        Assert.assertEquals(8, ByteTextBuilder.cntDigits(39100019L));
        Assert.assertEquals(9, ByteTextBuilder.cntDigits(391000119L));
        Assert.assertEquals(10, ByteTextBuilder.cntDigits(1910001393L));

        Assert.assertEquals(19, ByteTextBuilder.cntDigits(Long.MAX_VALUE));
    }

    @Test
    public void sumLength() {
        Assert.assertEquals(0, ByteTextBuilder.sumLength(new ByteTextBuilder[0]));

        final ByteTextBuilder b1 = new ByteTextBuilder("bingo");
        final ByteTextBuilder b2 = new ByteTextBuilder("bongo");
        final ByteTextBuilder b3 = new ByteTextBuilder("foo");
        final ByteTextBuilder b4 = new ByteTextBuilder("bar");
        int expected = b1.length() + b2.length() + b3.length() + b4.length();
        Assert.assertEquals(expected, ByteTextBuilder.sumLength(new ByteTextBuilder[]{b1, b2, b3, b4}));
    }

    @Test
    public void lengthAtStart() {
        final ByteTextBuilder b0 = new ByteTextBuilder();
        Assert.assertEquals(0, b0.length());

        final ByteTextBuilder b1 = new ByteTextBuilder(20);
        Assert.assertEquals(0, b0.length());

        final ByteTextBuilder b2 = new ByteTextBuilder("bingo".getBytes());
        Assert.assertEquals(5, b2.length());
    }

    @Test
    public void md5() {
        final ByteTextBuilder b = new ByteTextBuilder("bingo");
        Assert.assertEquals("3A3795BB61D5377545B4F345FF223E3D", b.md5());
    }

    @Test
    public void setAsCopy() {
        final ByteTextBuilder b0 = new ByteTextBuilder();
        final ByteTextBuilder b1 = new ByteTextBuilder("bingo");

        b0.set(b1);
        Assert.assertEquals("bingo", b0.toString());
    }

    @Test
    public void appendByteToEmpty() {
        final ByteTextBuilder b0 = new ByteTextBuilder();
        b0.append((byte)33);
        Assert.assertEquals("!", b0.toString());
    }

    @Test
    public void appendIntToEmpty() {
        final ByteTextBuilder b0 = new ByteTextBuilder();
        b0.append(33);
        Assert.assertEquals("33", b0.toString());
    }

    @Test
    public void compareToWhenEqual() {
        final ByteTextBuilder b1 = new ByteTextBuilder("bingo");
        final ByteTextBuilder b2 = new ByteTextBuilder("bingo");

        Assert.assertEquals(0, b1.compareTo(b2));
    }

    @Test
    public void compareToWhenNotEqual() {
        final ByteTextBuilder b1 = new ByteTextBuilder("bingo");
        final ByteTextBuilder b2 = new ByteTextBuilder("bongo");

        Assert.assertTrue(b1.compareTo(b2) < 0);
        Assert.assertTrue(b2.compareTo(b1) > 0);
    }

    @Test
    public void compareToIgnoreCaseWhenEqual() {
        final ByteTextBuilder b1 = new ByteTextBuilder("bingo");
        final ByteTextBuilder b2 = new ByteTextBuilder("BINGO");

        Assert.assertEquals(0, b1.compareToIgnoreCase(b2));
    }

    @Test
    @Ignore("Case is not ignored when return value is calculated for non-matching strings")
    public void compareToIgnoreCaseWhenNotEqual() {
        final ByteTextBuilder b1 = new ByteTextBuilder("bingo");
        final ByteTextBuilder b2 = new ByteTextBuilder("BONGO");

        Assert.assertTrue(b1.compareToIgnoreCase(b2) < 0);
        Assert.assertTrue(b2.compareToIgnoreCase(b1) > 0);
    }

    @Test
    @Ignore("Lexicographic order is not observed")
    public void compareToWhenDifferentLengths() {
        final ByteTextBuilder b1 = new ByteTextBuilder("aaaaaaaaaa");
        final ByteTextBuilder b2 = new ByteTextBuilder("bongo");

        Assert.assertTrue(b1.compareTo(b2) < 0);
    }
}
