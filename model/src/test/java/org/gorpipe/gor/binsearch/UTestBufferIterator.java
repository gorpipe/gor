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

package org.gorpipe.gor.binsearch;

import org.apache.commons.lang.ArrayUtils;
import org.gorpipe.gor.binsearch.BufferIterator;
import org.gorpipe.gor.binsearch.StringIntKey;
import org.junit.Assert;
import org.junit.Test;

import static org.gorpipe.gor.binsearch.BufferIterator.findInBuffer;

public class UTestBufferIterator {
    final StringIntKey comparator = new StringIntKey(0, 1, StringIntKey.cmpLexico);

    @Test
    public void test_getLowerBound_True() {
        final byte[] buffer = "aaaaaaaaaaaa\nbbbbbbbbbb\nccccccc".getBytes();
        final int wanted = 0;
        final int actual = BufferIterator.getLowerBound(buffer, 0, buffer.length, true);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getLowerBound_False() {
        final byte[] buffer = "aaaaaaaaaaaa\nbbbbbbbbbb\nccccccc".getBytes();
        final int wanted = ArrayUtils.indexOf(buffer, (byte) '\n') + 1;
        final int actual = BufferIterator.getLowerBound(buffer, 0, buffer.length, false);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getLowerBound_False_NoNewLine() {
        final byte[] buffer = "aaaaaaaaaaaa".getBytes();
        final int wanted = buffer.length + 1;
        final int actual = BufferIterator.getLowerBound(buffer, 0, buffer.length, false);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getUpperBound_True() {
        final byte[] buffer = "aaaaaaaaaaaa\nbbbbbbbbbb\nccccccc".getBytes();
        final int wanted = buffer.length;
        final int actual = BufferIterator.getUpperBound(buffer, 0, buffer.length, true);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getUpperBound_False() {
        final byte[] buffer = "aaaaaaaaaaaa\nbbbbbbbbbb\nccccccc".getBytes();
        final int wanted = ArrayUtils.lastIndexOf(buffer, (byte) '\n') + 1;
        final int actual = BufferIterator.getUpperBound(buffer, 0, buffer.length, false);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getUpperBound_False_NoNewLine() {
        final byte[] buffer = "aaaaaaaaaaaa".getBytes();
        final int wanted = 0;
        final int actual = BufferIterator.getUpperBound(buffer, 0, buffer.length, false);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getEndOfFirstLine() {
        final byte[] buffer = "aaaaaaaaaaaa\nbbbbbbbbbb\nccccccc".getBytes();
        final int wanted = ArrayUtils.indexOf(buffer, (byte) '\n') + 1;
        final int actual = BufferIterator.getEndOfNextLine(buffer, 0, buffer.length);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getEndOfFirstLine_NoNewLine() {
        final byte[] buffer = "aaaaaaaaaaaa".getBytes();
        final int wanted = buffer.length + 1;
        final int actual = BufferIterator.getEndOfNextLine(buffer, 0, buffer.length);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getEndOfFirstLine_NewLineAtEnd() {
        final byte[] buffer = "aaaaaaaaaaaa\n".getBytes();
        final int wanted = ArrayUtils.indexOf(buffer, (byte) '\n') + 1;
        final int actual = BufferIterator.getEndOfNextLine(buffer, 0, buffer.length);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getBeginningOfLastLine() {
        final byte[] buffer = "aaaaaaaaaaaa\nbbbbbbbbbb\nccccccc".getBytes();
        final int wanted = ArrayUtils.lastIndexOf(buffer, (byte) '\n') + 1;
        final int actual = BufferIterator.getBeginningOfLastLine(buffer, 0, buffer.length + 1);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getBeginningOfLastLine_NoNewLine() {
        final byte[] buffer = "aaaaaaaaaaaa".getBytes();
        final int wanted = 0;
        final int actual = BufferIterator.getBeginningOfLastLine(buffer, 0, buffer.length + 1);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_getBeginningOfLastLine_NewLineAtEnd() {
        final byte[] buffer = "\naaaaaaaaaaaa".getBytes();
        final int wanted = 1;
        final int actual = BufferIterator.getBeginningOfLastLine(buffer, 0, buffer.length + 1);
        Assert.assertEquals(wanted, actual);
    }

    @Test
    public void test_NoNewLineInBuffer() {
        final String lines = "chr1\t1\taaa";
        final byte[] buffer = lines.getBytes();
        final BufferIterator bi = new BufferIterator(comparator);
        bi.update(buffer, 0, buffer.length, true, true);
        Assert.assertTrue(bi.hasNext());
    }

    @Test
    public void test_NoLineInBuffer() {
        final String lines = "aaaaaa\naaaaa";
        final byte[] buffer = lines.getBytes();
        final BufferIterator bi = new BufferIterator(comparator);
        bi.update(buffer, 0, buffer.length, false, false);
        Assert.assertFalse(bi.hasNext());
    }

    @Test
    public void test_NoLineInBuffer_NoNewLine() {
        final String lines = "aaaaaaaaaaa";
        final byte[] buffer = lines.getBytes();
        final BufferIterator bi = new BufferIterator(comparator);
        bi.update(buffer, 0, buffer.length, false, false);
        Assert.assertFalse(bi.hasNext());
    }

    @Test
    public void testFindInBuffer_NoNewLineAtEnd() {
        final String line = "chr1\t1\tA\tC";
        final byte[] buffer = line.getBytes();
        final StringIntKey key = new StringIntKey("chr1", 1);

        final int actual = findInBuffer(key, buffer, 0, buffer.length + 1, comparator);
        final int expected = 0;
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFindInBuffer_MultipleLinesSamePos_1() {
        final String lines = "chr1\t1\t0\n" +
                "chr1\t1\t1\n" +
                "chr1\t1\t2\n" +
                "chr1\t1\t1\n" +
                "chr1\t1\t3\n" +
                "chr1\t1\t4\n" +
                "chr1\t1\t5\n";
        final byte[] buffer = lines.getBytes();
        final StringIntKey key = new StringIntKey("chr1", 1);

        final int actual = findInBuffer(key, buffer, 0, buffer.length, comparator);
        final int expected = 0;
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFindInBuffer_MultipleLinesSamePos_2() {
        final String lines = "chr1\t1\t0\n" +
                "chr1\t1\t1\n" +
                "chr1\t2\t0\n" +
                "chr1\t2\t1\n" +
                "chr1\t2\t2\n" +
                "chr1\t2\t3\n" +
                "chr1\t2\t4\n";
        final byte[] buffer = lines.getBytes();
        final StringIntKey key = new StringIntKey("chr1", 2);

        final int idx = findInBuffer(key, buffer, 0, buffer.length, comparator);
        final String lineFound = lines.substring(idx, lines.indexOf('\n', idx));
        Assert.assertEquals("chr1\t2\t0", lineFound);
    }

    @Test
    public void testFindInBuffer_FindLastLineInBuffer() {
        final String lines = "chr1\t1\t0\n" +
                "chr1\t1\t1\n" +
                "chr1\t2\t0\n" +
                "chr1\t2\t1\n" +
                "chr1\t2\t2\n" +
                "chr1\t2\t3\n" +
                "chr1\t3\t0\n";
        final byte[] buffer = lines.getBytes();
        final StringIntKey key = new StringIntKey("chr1", 3);

        final int idx = findInBuffer(key, buffer, 0, buffer.length, comparator);
        final String lineFound = lines.substring(idx, lines.indexOf('\n', idx));
        Assert.assertEquals("chr1\t3\t0", lineFound);
    }

    @Test
    public void testFindInBuffer_FindLineSurroundedByManyLines() {
        final String lines = "chr1\t1\t0\n" +
                "chr1\t1\t1\n" +
                "chr1\t1\t2\n" +
                "chr1\t2\t0\n" +
                "chr1\t3\t0\n" +
                "chr1\t3\t1\n" +
                "chr1\t3\t2\n";
        final byte[] buffer = lines.getBytes();
        final StringIntKey key = new StringIntKey("chr1", 2);

        final int idx = findInBuffer(key, buffer, 0, buffer.length, comparator);
        final String lineFound = lines.substring(idx, lines.indexOf('\n', idx));
        Assert.assertEquals("chr1\t2\t0", lineFound);
    }
}
