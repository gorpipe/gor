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
package org.gorpipe.util.collection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.IntStream;
import javax.imageio.stream.FileImageInputStream;

/**
 * ByteArray provides methods to read various data types out of a byte array
 *
 * @version $Id$
 */
public class ByteArray {

    /**
     * The length of the Byte Array
     */
    public final int length;
    private final byte[] bytes;
    private final ByteOrder order;
    private int position;

    /**
     * Construct ByteArray
     *
     * @param bytes     The bytes to read
     * @param byteOrder The ByteOrder to use
     */
    public ByteArray(byte[] bytes, ByteOrder byteOrder) {
        this.bytes = bytes;
        this.length = bytes.length;
        this.order = byteOrder;
        this.position = 0;
    }

    /**
     * Query for the current position in the Byte Array
     *
     * @return The position
     */
    public int position() {
        return position;
    }

    /**
     * Set the current position in the ByteArray
     *
     * @param pos The position
     */
    public void seek(int pos) {
        assert pos >= 0;
        this.position = pos;
    }

    /**
     * Skip the specified number of bytes
     *
     * @param len The number of bytes to skip
     */
    public void skipBytes(int len) {
        this.position += len;
        assert position >= 0;
    }

    /**
     * Read a block from the buffer into the specified ByteBuffer
     *
     * @param dst The destination ByteBuffer
     * @return The number of bytes read
     */
    public int read(ByteBuffer dst) {
        final int remaining = bytes.length - position;
        if (remaining == 0) {
            return -1; // Indicate no more data
        }
        final int len = Math.min(dst.remaining(), remaining);
        dst.put(bytes, position, len);
        position += len;
        return len;
    }

    /**
     * Read the next byte
     *
     * @return The byte
     */
    public byte readByte() {
        return bytes[position++];
    }

    /**
     * Read the next unsigned byte
     *
     * @return The unsigned byte as int
     */
    public int readUnsignedByte() {
        return readUnsignedByte(bytes, position++);
    }

    /**
     * Read the next short
     *
     * @return The short
     */
    public short readShort() {
        short s = readShort(bytes, position, order);
        position += 2;
        return s;
    }

    /**
     * Read the next unsigned short
     *
     * @return The unsigned short as an integer
     */
    public int readUnsignedShort() {
        int us = readUnsignedShort(bytes, position, order);
        position += 2;
        return us;
    }

    /**
     * Read the next int
     *
     * @return The int value
     */
    public int readInt() {
        int i = readInt(bytes, position, order);
        position += 4;
        return i;
    }

    /**
     * Read the next unsigned int
     *
     * @return The unsigned int as a long
     */
    public long readUnsignedInt() {
        long l = readUnsignedInt(bytes, position, order);
        position += 4;
        return l;
    }

    /**
     * Read the next float
     *
     * @return The float value
     */
    public float readFloat() {
        float f = readFloat(bytes, position, order);
        position += 4;
        return f;
    }

    /**
     * Read a block from the specified input stream that is equal in length as
     * the buffer
     *
     * @param pos    The position to start writing the block into
     * @param data   The stream to read from
     * @param offset The starting position of the reading
     * @param len    The number of bytes to read
     * @throws IOException
     */
    public void fillRange(int pos, FileImageInputStream data, int offset, int len) throws IOException {
        data.seek(offset);
        data.readFully(bytes, pos, len);
        position = pos;
    }

    /**
     * Read an long from the current position in the byte array
     *
     * @return The long value
     */
    public long readLong() {
        int i1 = readInt();
        int i2 = readInt();

        if (order == ByteOrder.BIG_ENDIAN) {
            return ((long) i1 << 32) + (i2 & 0xFFFFFFFFL);
        }
        return ((long) i2 << 32) + (i1 & 0xFFFFFFFFL);
    }

    /**
     * Read an long from the current position in the byte array, assuming big endian order
     *
     * @param buf    The buffer to read from
     * @param offset The offset into the byte array to read from
     * @return The long value
     */
    public static long readLongBigEndian(byte[] buf, int offset) {
        return ((long) readIntBigEndian(buf, offset) << 32) + (readIntBigEndian(buf, offset + 4) & 0xFFFFFFFFL);
    }


    /**
     * Read the next short
     *
     * @param byteBuf   The byte array
     * @param pos       The position to read from
     * @param byteOrder The byte order to use
     * @return The short
     */
    public static short readShort(byte[] byteBuf, int pos, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short) (((byteBuf[pos] & 0xff) << 8) | (byteBuf[pos + 1] & 0xff));
        }
        return (short) (((byteBuf[pos + 1] & 0xff) << 8) | (byteBuf[pos] & 0xff));
    }

    /**
     * Read the next short
     *
     * @param byteBuf The byte array
     * @param pos     The position to read from
     * @return The short
     */
    public static short readShortBigEndian(byte[] byteBuf, int pos) {
        return (short) (((byteBuf[pos] & 0xff) << 8) | (byteBuf[pos + 1] & 0xff));
    }

    /**
     * Read an unsigned short from the specified position in the byte array
     *
     * @param byteBuf   The byte array
     * @param pos       The position to read from
     * @param byteOrder The byte order to use
     * @return The unsigned short as an integer
     */
    public static int readUnsignedShort(byte[] byteBuf, int pos, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return readUnsignedShortBigEndian(byteBuf, pos);
        }
        return (((byteBuf[pos + 1] & 0xff) << 8) | (byteBuf[pos] & 0xff));
    }

    /**
     * Read an unsigned short from the specified position in the byte array
     *
     * @param byteBuf The byte array
     * @param pos     The position to read from
     * @return The unsigned short as an integer
     */
    public static int readUnsignedShortBigEndian(byte[] byteBuf, int pos) {
        return (((byteBuf[pos] & 0xff) << 8) | (byteBuf[pos + 1] & 0xff));
    }

    /**
     * Read an int from the specified position in the byte array
     *
     * @param byteBuf   The byte array
     * @param pos       The position to read from
     * @param byteOrder The byte order to use
     * @return The int value
     */
    public static int readInt(byte[] byteBuf, int pos, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return readIntBigEndian(byteBuf, pos);
        }
        return (((byteBuf[pos + 3] & 0xff) << 24) | ((byteBuf[pos + 2] & 0xff) << 16) | ((byteBuf[pos + 1] & 0xff) << 8) | (byteBuf[pos] & 0xff));
    }

    /**
     * Read an int from the specified position in the byte array
     *
     * @param byteBuf The byte array
     * @param pos     The position to read from
     * @return The int value
     */
    public static int readIntBigEndian(byte[] byteBuf, int pos) {
        return (((byteBuf[pos] & 0xff) << 24) | ((byteBuf[pos + 1] & 0xff) << 16) | ((byteBuf[pos + 2] & 0xff) << 8) | (byteBuf[pos + 3] & 0xff));
    }

    /**
     * Read an unsigned int from the specified position in the byte array
     *
     * @param byteBuf   The byte array
     * @param pos       The position to read from
     * @param byteOrder The byte order to use
     * @return The unsigned int as a long
     */
    public static long readUnsignedInt(byte[] byteBuf, int pos, ByteOrder byteOrder) {
        return (readInt(byteBuf, pos, byteOrder)) & 0xffffffffL;
    }

    /**
     * Read an unsigned int from the specified position in the byte array, assuming big endian
     *
     * @param byteBuf The byte array
     * @param pos     The position to read from
     * @return The unsigned int as a long
     */
    public static long readUnsignedIntBigEndian(byte[] byteBuf, int pos) {
        return (readIntBigEndian(byteBuf, pos)) & 0xffffffffL;
    }

    /**
     * Read an unsigned byte from the specified position in the byte array
     *
     * @param byteBuf The byte array
     * @param pos     The position to read from
     * @return The unsigned byte
     */
    public static int readUnsignedByte(byte[] byteBuf, int pos) {
        return byteBuf[pos] & 0xFF;
    }

    /**
     * Read a byte from the specified position in the byte array
     *
     * @param byteBuf The byte array
     * @param pos     The position to read from
     * @return The byte
     */
    public static int readByte(byte[] byteBuf, int pos) {
        return byteBuf[pos];
    }

    /**
     * Read a float from the specified position in the byte array
     *
     * @param byteBuf   The byte array
     * @param pos       The position to read from
     * @param byteOrder The byte order to use
     * @return The float value
     */
    public static float readFloat(byte[] byteBuf, int pos, ByteOrder byteOrder) {
        return Float.intBitsToFloat(readInt(byteBuf, pos, byteOrder));
    }

    /**
     * Write the specified integer value into the byte buffer
     *
     * @param byteBuf   The byte buffer
     * @param pos       The position to write to
     * @param byteOrder The ByteOrder to use
     * @param v         The value to write
     */
    public static void writeInt(byte[] byteBuf, int pos, ByteOrder byteOrder, int v) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byteBuf[pos] = (byte) (v >>> 24);
            byteBuf[pos + 1] = (byte) (v >>> 16);
            byteBuf[pos + 2] = (byte) (v >>> 8);
            byteBuf[pos + 3] = (byte) (v);
        } else {
            byteBuf[pos] = (byte) (v);
            byteBuf[pos + 1] = (byte) (v >>> 8);
            byteBuf[pos + 2] = (byte) (v >>> 16);
            byteBuf[pos + 3] = (byte) (v >>> 24);
        }
    }

    /**
     * Write the specified short value into the buffer
     *
     * @param byteBuf
     * @param pos
     * @param byteOrder
     * @param v
     */
    public static void writeShort(byte[] byteBuf, int pos, ByteOrder byteOrder, short v) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byteBuf[pos] = (byte) (v >>> 8);
            byteBuf[pos + 1] = (byte) (v);
        } else {
            byteBuf[pos] = (byte) (v);
            byteBuf[pos + 1] = (byte) (v >>> 8);
        }
    }

    /**
     * Write the specified int value as an unsigned short into the buffer
     *
     * @param byteBuf
     * @param pos
     * @param byteOrder
     * @param v
     */
    public static void writeUnsignedShort(byte[] byteBuf, int pos, ByteOrder byteOrder, int v) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byteBuf[pos] = (byte) (v >>> 8);
            byteBuf[pos + 1] = (byte) (v);
        } else {
            byteBuf[pos] = (byte) (v);
            byteBuf[pos + 1] = (byte) (v >>> 8);
        }
    }

    /**
     * Write the specified int value as an unsigned int into the buffer
     *
     * @param byteBuf
     * @param pos
     * @param byteOrder
     * @param v
     */
    public static void writeUnsignedInt(byte[] byteBuf, int pos, ByteOrder byteOrder, long v) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byteBuf[pos] = (byte) (v >>> 24);
            byteBuf[pos + 1] = (byte) (v >>> 16);
            byteBuf[pos + 2] = (byte) (v >>> 8);
            byteBuf[pos + 3] = (byte) (v);
        } else {
            byteBuf[pos] = (byte) (v);
            byteBuf[pos + 1] = (byte) (v >>> 8);
            byteBuf[pos + 2] = (byte) (v >>> 16);
            byteBuf[pos + 3] = (byte) (v >>> 24);
        }
    }

    /**
     * Write the specified long value into the byte buffer
     *
     * @param byteBuf   The byte buffer
     * @param pos       The position to write to
     * @param byteOrder The ByteOrder to use
     * @param value     The value to write
     */
    public static void writeLong(byte[] byteBuf, int pos, ByteOrder byteOrder, long value) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byteBuf[pos++] = (byte) (value >>> 56);
            byteBuf[pos++] = (byte) (value >>> 48);
            byteBuf[pos++] = (byte) (value >>> 40);
            byteBuf[pos++] = (byte) (value >>> 32);
            byteBuf[pos++] = (byte) (value >>> 24);
            byteBuf[pos++] = (byte) (value >>> 16);
            byteBuf[pos++] = (byte) (value >>> 8);
            byteBuf[pos] = (byte) (value);
        } else {
            byteBuf[pos++] = (byte) (value);
            byteBuf[pos++] = (byte) (value >>> 8);
            byteBuf[pos++] = (byte) (value >>> 16);
            byteBuf[pos++] = (byte) (value >>> 24);
            byteBuf[pos++] = (byte) (value >>> 32);
            byteBuf[pos++] = (byte) (value >>> 40);
            byteBuf[pos++] = (byte) (value >>> 48);
            byteBuf[pos] = (byte) (value >>> 56);
        }
    }

    /**
     * Create a 32 bit hash from the specified byte array starting with the
     * specified hash
     *
     * @param hash   The initial hash to start from
     * @param bytes  The bytes to add to the hash
     * @param offset The offset to start reading from the array at
     * @param len    The length to read from the byte array
     * @return The new hash value
     */
    public static int DJBHash(int hash, byte[] bytes, int offset, int len) {
        final int CNT = offset + len;
        for (int i = offset; i < CNT; i++) {
            hash = ((hash << 5) + hash) + bytes[i];
        }
        return hash;
    }

    /**
     * Convert the byte text into an integer
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @return The integer value
     */
    public static int toInt(byte[] bytes, int begin) {
        return toInt(bytes, begin, bytes.length, false);
    }


    /**
     * Convert the byte text into an integer
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The integer value
     */
    public static int toInt(byte[] bytes, int begin, int end) {
        return toInt(bytes, begin, end, false);
    }

    /**
     * Convert the byte text into an integer
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The integer value
     */
    public static int toInt(byte[] bytes, int begin, int end, boolean throwex) {
        int nr = 0;
        final boolean isNegative = end > begin && bytes[begin] == '-';
        final boolean hasPlus = end > begin && bytes[begin] == '+';
        for (int p = isNegative || hasPlus ? begin + 1 : begin; p < end; p++) {
            final byte ch = bytes[p];
            if (ch < '0' || ch > '9') {
                if (!throwex || ch == '\t') {
                    break;
                } else {
                    String s = new String(bytes, begin, p - begin);
                    throw new NumberFormatException("Unable to parse number: " + s + " len " + s.length());
                }
            }
            nr = 10 * nr + (ch - '0');
        }
        return isNegative ? -nr : nr;
    }

    /**
     * Convert the byte text into an long
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The long value
     */
    public static long toLong(byte[] bytes, int begin, int end) {
        long nr = 0;
        final boolean isNegative = end > begin && bytes[begin] == '-';
        for (int p = isNegative ? begin + 1 : begin; p < end; p++) {
            final byte ch = bytes[p];
            if (ch < '0' || ch > '9') {
                break;
            }
            nr = 10 * nr + (ch - '0');
        }
        return isNegative ? -nr : nr;
    }

    /**
     * Convert the byte text into an long
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @return The long value
     */
    public static long toLongWithException(byte[] bytes, int begin) {
        return toLongWithException(bytes, begin, bytes.length);
    }

    /**
     * Convert the byte text into an long
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The long value
     */
    public static long toLongWithException(byte[] bytes, int begin, int end) {
        long nr = 0;
        final boolean isNegative = end > begin && bytes[begin] == '-';
        int p = isNegative ? begin + 1 : begin;
        while (p < end && bytes[p] >= '0' && bytes[p] <= '9') {
            nr = 10 * nr + (bytes[p++] - '0');
        }
        if (p == (isNegative ? begin + 1 : begin) || p < end) {
            throw new NumberFormatException("Cannot parse " + new String(bytes, begin, end) + " to long");
        }
        return isNegative ? -nr : nr;
    }


    /**
     * Convert the byte text into an long
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The long value
     */
    public static long toLongCheckLen(byte[] bytes, int begin, int end) {
        long nr = 0;
        final boolean isNegative = end > begin && bytes[begin] == '-';
        int p = isNegative ? begin + 1 : begin;
        while (p < end && bytes[p] >= '0' && bytes[p] <= '9') {
            nr = 10 * nr + (bytes[p++] - '0');
        }
        if (end != p) {
            throw new NumberFormatException(new String(bytes, begin, end - begin) + " is not a valid long integer!");
        }
        return isNegative ? -nr : nr;
    }


    /**
     * Convert the byte text into an long
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The long value
     */
    public static boolean isLong(byte[] bytes, int begin, int end) {
        long nr = 0;
        final boolean isNegative = end > begin && bytes[begin] == '-';
        int p = isNegative ? begin + 1 : begin;
        while (p < end && bytes[p] >= '0' && bytes[p] <= '9') {
            nr = 10 * nr + (bytes[p++] - '0');
        }
        return p == end;
    }

    /**
     * Check if the specified bytes form a legal double value
     *
     * @param bytes The byte array to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return True if this is a legal double value, else false
     */
    public static boolean isDouble(byte[] bytes, int begin, int end) {
        try {
            jdkToDouble(bytes, begin, end);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Convert the byte text into a double, inv,-inv and nan must be recognized for what they are.
     *
     * @param bytes The byte array to convert
     * @param begin The begin position, inclusive
     * @return The double value
     */
    public static double toDouble(byte[] bytes, int begin) {
        return toDouble(bytes, begin, bytes.length);
    }

    private static final long MAX_VALUE_DIVIDE_10 = Long.MAX_VALUE / 10;

    private static boolean isWhitespace(byte ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
    }

    /**
     * Convert the byte text into a double, inv,-inv and nan must be recognized for what they are.
     *
     * @param bytes The byte array to convert
     * @param begin The begin position, inclusive
     * @param end   The end position, exclusive
     * @return The double value
     */
    public static double toDouble(byte[] bytes, int begin, int end) {
        while (begin < end && isWhitespace(bytes[begin])) {
            begin++;
        }
        while (end > 0 && isWhitespace(bytes[end - 1])) {
            end--;
        }
        int ibeg = begin;
        if (bytes[ibeg] == '-') {
            ibeg++;
        } else if (isNaN(bytes, begin, end)) {
            return Double.NaN;
        }
        if (isInfinity(bytes, ibeg, end)) {
            return ibeg == begin ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        return bytesToDouble(bytes, begin, end);
    }

    private static double bytesToDouble(byte[] bytes, int begin, int end) {
        long value = 0;
        int exp = 0;
        boolean negative = bytes[begin] == '-';
        boolean hasPlus = bytes[begin] == '+';
        int decimalPlaces = Integer.MIN_VALUE;
        for (int i = negative || hasPlus ? begin + 1 : begin; i < end; i++) {
            byte ch = bytes[i];
            if (ch >= '0' && ch <= '9') {
                while (value >= MAX_VALUE_DIVIDE_10) {
                    value >>>= 1;
                    exp++;
                }
                value = value * 10 + (ch - '0');
                decimalPlaces++;
            } else if (ch == '.') {
                decimalPlaces = 0;
            } else {
                if (ch == 'e' || ch == 'E') {
                    final int decimals = toInt(bytes, i + 1, end, true);
                    if (decimals == -324 && value == 49) { // There is overflow with MIN_VALUE, handle it speciallay
                        return Double.MIN_VALUE;
                    }
                    final double d = asDouble(value, exp, negative, decimalPlaces);
                    return d * Math.pow(10, decimals);
                }
                throw new NumberFormatException("Cannot parse " + new String(bytes, begin, end) + " to double");
            }
        }
        return asDouble(value, exp, negative, decimalPlaces);
    }

    private static boolean isInfinity(final byte[] bytes, int begin, int end) {
        final int invlen = end - begin;
        return invlen >= 3 &&
                (bytes[begin] == 'I' || bytes[begin] == 'i') &&
                (bytes[begin + 1] == 'N' || bytes[begin + 1] == 'n') &&
                (bytes[begin + 2] == 'F' || bytes[begin + 2] == 'f') &&
                (invlen == 3 ||
                        invlen == 8 &&
                                (bytes[begin + 3] == 'I' || bytes[begin + 3] == 'i') &&
                                (bytes[begin + 4] == 'N' || bytes[begin + 4] == 'n') &&
                                (bytes[begin + 5] == 'I' || bytes[begin + 5] == 'i') &&
                                (bytes[begin + 6] == 'T' || bytes[begin + 6] == 't') &&
                                (bytes[begin + 7] == 'Y' || bytes[begin + 7] == 'y'));
    }

    private static boolean isNaN(final byte[] bytes, int begin, int end) {
        return end - begin == 3 &&
                (bytes[begin] == 'N' || bytes[begin] == 'n') &&
                (bytes[begin + 1] == 'A' || bytes[begin + 1] == 'a') &&
                (bytes[begin + 2] == 'N' || bytes[begin + 2] == 'n');
    }

    private static double asDouble(long value, int exp, boolean negative, int decimalPlaces) {
        if (decimalPlaces > 0 && value < Long.MAX_VALUE / 2) {
            if (value < Long.MAX_VALUE / (1L << 32)) {
                exp -= 32;
                value <<= 32;
            }
            if (value < Long.MAX_VALUE / (1L << 16)) {
                exp -= 16;
                value <<= 16;
            }
            if (value < Long.MAX_VALUE / (1L << 8)) {
                exp -= 8;
                value <<= 8;
            }
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
            }
        }
        for (; decimalPlaces > 0; decimalPlaces--) {
            exp--;
            final long mod = value % 5;
            value /= 5;
            int modDiv = 1;
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
                modDiv <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
                modDiv <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
                modDiv <<= 1;
            }
            value += modDiv * mod / 5;
        }
        final double d = Math.scalb((double) value, exp);
        return negative ? -d : d;
    }

    /**
     * Convert the byte text into a double, inv,-inv and nan must be recognized for what they are.
     *
     * @param bytes The byte array to convert
     * @param begin The begin position, inclusive
     * @param end   The end position, exclusive
     * @return The double value
     */
    public static double jdkToDouble(byte[] bytes, int begin, int end) {
        while (begin < end && isWhitespace(bytes[begin])) {
            begin++;
        }
        while (end > 0 && isWhitespace(bytes[end - 1])) {
            end--;
        }
        int ibeg = begin;
        if (bytes[ibeg] == '-') {
            ibeg++;
        } else if (isNaN(bytes, begin, end)) {
            return Double.NaN;
        }
        if (isInfinity(bytes, ibeg, end)) {
            return ibeg == begin ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        int len = end - begin;
        @SuppressWarnings("deprecation") final String value = new String(bytes, 0, begin, len);
        return Double.parseDouble(value);
    }

    /**
     * Parses a chrId from a byte array
     *
     * @param buffer    buffer to parse from
     * @param startPos  start position within buffer
     * @param bufLength The number of valid bytes in the buffer, i.e. the buffer length must be less than or equal buffer.length
     * @return The int value
     */
    public static int parseChrId(byte[] buffer, int startPos, int bufLength) {
        int chrId = -1;
        int pos = startPos + 3;  // skip chr prefix
        byte chr = buffer[pos];
        if (chr == 'M' || chr == 'm') {
            chrId = 0;
        } else if (chr == 'X' || chr == 'x') {
            chrId = (pos + 1 < bufLength && (buffer[pos + 1] == 'Y' || buffer[pos + 1] == 'y')) ? 24 : 23;
        } else if (chr == 'Y' || chr == 'y') {
            chrId = 25;
        } else {
            chrId = toInt(buffer, pos, bufLength);
        }
        return chrId;
    }


    /**
     * Parses a chrId from a byte array
     *
     * @param buffer   buffer to parse from
     * @param startPos start position within buffer
     * @return The int value
     */
    public static int parseChrId(byte[] buffer, int startPos) {
        int chrId = -1;
        int pos = startPos + 3;  // skip chr prefix
        byte chr = buffer[pos];
        if (chr == 'M' || chr == 'm') {
            chrId = 0;
        } else if (chr == 'X' || chr == 'x') {
            chrId = (pos + 1 < buffer.length && (buffer[pos + 1] == 'Y' || buffer[pos + 1] == 'y')) ? 24 : 23;
        } else if (chr == 'Y' || chr == 'y') {
            chrId = 25;
        } else {
            chrId = toInt(buffer, pos);
        }
        return chrId;
    }

    /**
     * Writes the 4 byte array representation of an int to a byte[4] buffer
     *
     * @param number   int value
     * @param intBytes a 4 byte buffer to write the number into
     */
    public static void intToBytes(byte[] intBytes, int number) {
        ByteArray.writeInt(intBytes, 0, ByteOrder.BIG_ENDIAN, number);
    }

    /**
     * Calculate the sum of the lengths of the arrays
     *
     * @param ba The arrays
     * @return The summary length
     */
    public static int sumLength(byte[][] ba) {
        int sum = 0;
        for (byte[] a : ba) {
            sum += a.length;
        }
        return sum;
    }

    public static int beginOfLine(byte[] buffer, int start, boolean forward) {
        return beginOfLine(buffer, start, buffer.length, forward);
    }

    /**
     * @param buffer  data buffer
     * @param start   The start to search for begin of line, or -1 if we have not search before.
     * @param len     All data in the buffer has index less than len.
     * @param forward Whether to move forward or backward for next position
     * @return The start pos of a line within the buffer, 0 if start was outside the buffer or size of buffer if new line not found.
     */
    public static int beginOfLine(byte[] buffer, int start, int len, boolean forward) {
        if (start + 1 >= len || start < 0) { // Todo Check this condition.
            return 0;
        }
        byte eol = buffer[start];
        if (forward) {
            // Find next EOL character, searching forward.
            final int lenminusone = len - 1;
            while (eol != '\n' && eol != '\r') {
                if (start >= lenminusone) {
                    ++start; // No EOL found. Return buffer length.
                    break;
                }
                eol = buffer[++start];
            }
        } else {
            // Skip empty lines
            while ((eol == '\n' || eol == '\r') && start > 0) {
                eol = buffer[--start];
            }
            // Find next EOL character, searching backward.
            while ((eol != '\n' && eol != '\r') && start > 0) {
                eol = buffer[--start];
            }
        }
        // Go to the beginning of line (skipping empty lines).
        while (start < len && (buffer[start] == '\r' || buffer[start] == '\n')) {
            start++;
        }
        return start;
    }

    /**
     * Returns the end of line position of the buffer from start.
     *
     * @param buffer    data buffer
     * @param bufLength The number of valid bytes in the buffer, i.e. the buffer length must be less than or equal buffer.length
     * @param start     The start to search for end of line
     * @return The end position of a line within the buffer
     */
    public static int endOfLine(byte[] buffer, int bufLength, int start) {
        byte eol = buffer[start];
        final int lengthminusone = bufLength - 1;
        while ((eol != '\n' && eol != '\r') && start < lengthminusone) {
            eol = buffer[++start];
        }
        if (eol != '\n' && eol != '\r') { // No EOL found. Return buffer length.
            ++start;
        }
        return start;
    }


    /**
     * Returns the end of line position of the buffer from start.
     *
     * @param buffer data buffer
     * @param start  The start to search for end of line
     * @return The end position of a line within the buffer
     */
    public static int endOfLine(byte[] buffer, int start) {
        byte eol = buffer[start];
        final int lengthminusone = buffer.length - 1;
        while ((eol != '\n' && eol != '\r') && start < lengthminusone) {
            eol = buffer[++start];
        }
        if (eol != '\n' && eol != '\r') { // No EOL found. Return buffer length.
            ++start;
        }
        return start;
    }

    /**
     * Converts 8-bit byte encoding into a 7-bit byte encoding or rather
     * converts a base-256 encoding into a base-128 and offsets the results so that lowest value is 33
     * and highest is 33 + 128
     *
     * @param ba The input 8-bit byte array
     * @return A new byte array encoding the input data in a base-128 encoding
     */
    public static byte[] to7Bit(byte[] ba) {
        return to7Bit(ba, ba.length);
    }

    /**
     * Converts 8-bit byte encoding into a 7-bit byte encoding or rather
     * converts a base-256 encoding into a base-128 and offsets the results so that lowest value is 33
     * and highest is 33 + 128
     *
     * @param ba The input 8-bit byte array
     * @return A new byte array encoding the input data in a base-128 encoding
     */
    public static byte[] to7Bit(byte[] ba, int balength) {
        final int outlen = (balength % 7 == 0) ? (balength * 8) / 7 : (balength * 8) / 7 + 1;
        final byte[] out = new byte[outlen];
        to7Bit(ba, balength, out);
        return out;
    }

    /**
     * Converts 8-bit byte encoding into a 7-bit byte encoding or rather
     * converts a base-256 encoding into a base-128 and offsets the results so that lowest value is 33
     * and highest is 33 + 128
     *
     * @param ba The input 8-bit byte array
     */
    public static void to7Bit(byte[] ba, int baLen, byte[] output) {
        if (ba.length == 0) return;
        int inPos = 0, outPos = 0;
        int b;
        final int upTo = baLen - 7;
        while (inPos < upTo) {
            output[outPos++] = (byte) (((ba[inPos] & 0xff) & 0x7f) + 33);
            b = ba[inPos++] & 0xff;
            output[outPos++] = (byte) (((b >>> 7 | (ba[inPos] << 1)) & 0x7f) + 33);
            b = ba[inPos++] & 0xff;
            output[outPos++] = (byte) (((b >>> 6 | (ba[inPos] << 2)) & 0x7f) + 33);
            b = ba[inPos++] & 0xff;
            output[outPos++] = (byte) (((b >>> 5 | (ba[inPos] << 3)) & 0x7f) + 33);
            b = ba[inPos++] & 0xff;
            output[outPos++] = (byte) (((b >>> 4 | (ba[inPos] << 4)) & 0x7f) + 33);
            b = ba[inPos++] & 0xff;
            output[outPos++] = (byte) (((b >>> 3 | (ba[inPos] << 5)) & 0x7f) + 33);
            b = ba[inPos++] & 0xff;
            output[outPos++] = (byte) (((b >>> 2 | (ba[inPos] << 6)) & 0x7f) + 33);
            b = ba[inPos++] & 0xff;
            output[outPos++] = (byte) ((b >>> 1 | (ba[inPos] << 7) & 0x7f) + 33);
        }
        output[outPos++] = (byte) ((ba[inPos] & 0x7f) + 33);
        b = ba[inPos++] & 0xff;
        if (inPos == baLen) {
            output[outPos] = (byte) ((b >>> 7) + 33);
            return;
        }
        output[outPos++] = (byte) ((((b >>> 7) | (ba[inPos] << 1)) & 0x7f) + 33);
        b = ba[inPos++] & 0xff;
        if (inPos == baLen) {
            output[outPos] = (byte) ((b >>> 6) + 33);
            return;
        }
        output[outPos++] = (byte) ((((b >>> 6) | (ba[inPos] << 2)) & 0x7f) + 33);
        b = ba[inPos++] & 0xff;
        if (inPos == baLen) {
            output[outPos] = (byte) ((b >>> 5) + 33);
            return;
        }
        output[outPos++] = (byte) ((((b >>> 5) | (ba[inPos] << 3)) & 0x7f) + 33);
        b = ba[inPos++] & 0xff;
        if (inPos == baLen) {
            output[outPos] = (byte) ((b >>> 4) + 33);
            return;
        }
        output[outPos++] = (byte) ((((b >>> 4) | (ba[inPos] << 4)) & 0x7f) + 33);
        b = ba[inPos++] & 0xff;
        if (inPos == baLen) {
            output[outPos] = (byte) ((b >>> 3) + 33);
            return;
        }
        output[outPos++] = (byte) ((((b >>> 3) | (ba[inPos] << 5)) & 0x7f) + 33);
        b = ba[inPos++] & 0xff;
        if (inPos == baLen) {
            output[outPos] = (byte) ((b >>> 2) + 33);
            return;
        }
        output[outPos++] = (byte) ((((b >>> 2) | (ba[inPos] << 6)) & 0x7f) + 33);
        b = ba[inPos++] & 0xff;
        if (inPos == baLen) {
            output[outPos] = (byte) ((b >>> 1) + 33);
            return;
        }
        output[outPos++] = (byte) (((((ba[inPos++] & 0xff) << 7) | (b >>> 1)) & 0x7f) + 33);
        if (inPos == baLen) {
            output[outPos] = 33;
        }
    }

    /**
     * Converts 8-bit byte encoding into a 7-bit byte encoding or rather
     * converts a base-256 encoding into a base-128 and offsets the results so that lowest value is 33
     * and highest is 33 + 128
     *
     * @param ba The input 8-bit byte array
     * @return A new byte array encoding the input data in a base-128 encoding
     */
    public static byte[] to7BitParallel(byte[] ba) {
        final int len = ba.length;
        final byte[] out = new byte[(len % 7 == 0) ? (len * 8) / 7 : (len * 8) / 7 + 1];

        IntStream.range(0, out.length).parallel().forEach(outPos -> {
            int inPos = outPos - outPos / 8;
            int i = outPos % 8;
            int lastBits = i == 0 ? 0 : (ba[inPos - 1] & 0xFF) >>> (7 - ((outPos - 1) % 8));
            if (inPos == len) {
                out[outPos] = (byte) (lastBits + 33);
            } else {
                int b = ba[inPos] & 0xFF;
                out[outPos] = (byte) (((b << i) | lastBits) & 0x7F);
                out[outPos] += 33; // offset to avoid control chars
            }
        });
        return out;
    }

    /**
     * Restores a base-128 (7-bits per byte) encoding to the original base-256
     * (8-bits per byte) encoding. Assumes lowest value is 33 and highest is 33 + 128
     *
     * @param ba The input base-128 encoding
     * @return The original 8-bit data
     */
    public static byte[] to8Bit(byte[] ba) {
        final int len = ba.length;
        final byte[] out = new byte[(len * 7) / 8];
        int bit = 0, readPos = 0, writePos = 0;
        while (readPos < len - 1) {
            final byte b1 = (byte) (ba[readPos] - 33);
            final byte b2 = (byte) (ba[++readPos] - 33);
            out[writePos++] =
                    (byte) (((b1 & 0xff) >>> bit)
                            | ((b2 & 0xff) << (7 - bit)));
            if (++bit == 7) {
                bit = 0;
                ++readPos;
            }
        }
        return out;
    }

    /**
     * Restores a base-128 (7-bits per byte) encoding to the original base-256
     * (8-bits per byte) encoding. Assumes lowest value is 33 and highest is 33 + 128
     *
     * @param ba The input base-128 encoding
     * @return The original 8-bit data
     */
    public static int to8Bit(byte[] ba, byte[] out) {
        final int len = ba.length;
        final int outlen = (len * 7) / 8;
        int bit = 0, readPos = 0, writePos = 0;
        while (readPos < len - 1) {
            final byte b1 = (byte) (ba[readPos] - 33);
            final byte b2 = (byte) (ba[++readPos] - 33);
            out[writePos++] =
                    (byte) (((b1 & 0xff) >>> bit)
                            | ((b2 & 0xff) << (7 - bit)));
            if (++bit == 7) {
                bit = 0;
                ++readPos;
            }
        }
        return outlen;
    }

    /**
     * Restores a base-128 (7-bits per byte) encoding to the original base-256
     * (8-bits per byte) encoding. Assumes lowest value is 33 and highest is 33 + 128
     *
     * @param ba The input base-128 encoding
     * @return The original 8-bit data
     */

    public static byte[] to8BitParallel(byte[] ba, int off, int length) {
        final byte[] out = new byte[(length * 7) / 8];
        IntStream.range(0, out.length).parallel().forEach(writePos -> {
            int readPos = off + writePos + writePos / 7;
            final byte b1 = (byte) (ba[readPos] - 33);
            final byte b2 = (byte) (ba[readPos + 1] - 33);

            int bit = writePos % 7;
            out[writePos] = (byte) (((b1 & 0xff) >>> bit) | ((b2 & 0xff) << (7 - bit)));
        });
        return out;
    }

    /**
     * Restores a base-128 (7-bits per byte) encoding to the original base-256
     * (8-bits per byte) encoding. Assumes lowest value is 33 and highest is 33 + 128
     * The results are stored inplace in the incoming byte array, i.e. the content is replaced
     * in the incoming byte array
     *
     * @param ba     The input base-128 encoding
     * @param off    The offset in the buffer to start reading/writing from
     * @param length The length to read from
     * @return The number of bytes written to the byte array
     */
    public static int to8BitInplace(byte[] ba, int off, int length) {
        final int end = off + length - 1;
        int bit = 0, readPos = off, writePos = off;
        while (readPos < end) {
            final byte b1 = (byte) (ba[readPos] - 33);
            final byte b2 = (byte) (ba[++readPos] - 33);
            ba[writePos++] =
                    (byte) (((b1 & 0xff) >>> bit)
                            | ((b2 & 0xff) << (7 - bit)));
            if (++bit == 7) {
                bit = 0;
                ++readPos;
            }
        }
        return writePos - off;
    }
}
