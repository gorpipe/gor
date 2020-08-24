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
package org.gorpipe.model.util;

import org.gorpipe.util.collection.ByteArray;
import org.gorpipe.util.collection.extract.Extract;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;


/**
 * Builder class to maintain byte based text (instead of unicode multi byte text in StringBuilder).
 *
 * @version $Id$
 */
public class ByteTextBuilder implements CharSequence, Serializable, Comparable<ByteTextBuilder> {
    private static final long serialVersionUID = 1;
    private int pos;
    private byte[] bytes;

    private static final byte[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /**
     * Construct
     */
    public ByteTextBuilder() {
    }

    /**
     * Construct
     *
     * @param size The initial length of the byte buffer
     */
    public ByteTextBuilder(int size) {
        bytes = new byte[size];
    }

    /**
     * Constrcut a ByteTextBuilder using the specified bytes.
     *
     * @param bytes A byte array that the ByteTextBuilder will take ownership of. The assumption is that the array is full
     */
    public ByteTextBuilder(byte[] bytes) {
        this.bytes = bytes;
        this.pos = bytes.length;
    }

    /**
     * @return The bytes from the array
     */
    public byte[] getBytes() {
        byte[] b = new byte[pos];
        System.arraycopy(bytes, 0, b, 0, pos);
        return b;
    }

    /**
     * Construct from string
     */
    public ByteTextBuilder(CharSequence text) {
        this(text.length());
        appendUTF8(text.toString());
    }

    /**
     * Equivalent to length, but allows better inlining options since its not from an interface declaration
     *
     * @return The length of the buffer used.
     */
    public final int size() {
        return pos;
    }

    /**
     * @return The md5 digest of the current byte data, as HEX string
     */
    public final String md5() {
        return Extract.hex(Util.md5Bytes(bytes, 0, pos));
    }

    /**
     * Calculate the sum of the lengths of the arrays
     *
     * @param ba The arrays
     * @return The summary length
     */
    public static int sumLength(ByteTextBuilder[] ba) {
        int sum = 0;
        for (ByteTextBuilder a : ba) {
            sum += a != null ? a.pos : 0;
        }
        return sum;
    }

    /**
     * Write the content of the ByteTextBuilder into an output stream
     *
     * @param os The output stream
     */
    public void toStream(OutputStream os) throws IOException {
        os.write(bytes, 0, pos);
    }

    /**
     * Allow caller to peek at the buffer. Note that modifying operations on the buffer are reflected in this class
     * since this is the internal buffer.
     *
     * @return The buffer
     */
    public byte[] peekAtBuffer() {
        return bytes;
    }

    @Override
    public String toString() {
        return bytes == null ? "" : new String(bytes, 0, pos);
    }

    @Override
    public final int length() {
        return pos;
    }

    @Override
    public final char charAt(int index) {
        return (char) bytes[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new String(bytes, start, end - start);
    }

    @Override
    public int hashCode() {
        // Clone of java String hashCode for compatibility
        int len = pos;
        if (len > 0) {
            int h = 0;
            byte[] val = bytes;
            for (int i = 0; i < len; i++) {
                h = 31 * h + val[i];
            }
            return h;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ByteTextBuilder) {
            ByteTextBuilder other = (ByteTextBuilder) obj;
            int n = length();
            if (n == other.length()) {
                final byte[] v1 = bytes;
                final byte[] v2 = other.bytes;
                int i = 0;
                int j = 0;
                while (n-- != 0) {
                    if (v1[i++] != v2[j++])
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Empty the buffer
     */
    public void clear() {
        pos = 0;
    }

    /**
     * @return If the builder is empty, i.e. contains no data.
     */
    public boolean isEmpty() {
        return pos == 0;
    }

    /**
     * Get the byte at the specified offset.
     *
     * @param offset Any valid offset
     * @return The byte at the position
     */
    public byte byteAt(int offset) {
        if (offset < pos) {
            return bytes[offset];
        }
        throw new IndexOutOfBoundsException("No data position " + offset);
    }

    /**
     * Append the all the bytes in the source array into the ByteTextBuilder
     *
     * @param src
     */
    public void append(byte[] src) {
        append(src, 0, src.length);
    }

    /**
     * Set the beginning bytes
     *
     * @param src
     */
    public void set(byte[] src) {
        set(src, 0, src.length);
    }

    /**
     * Append the specified byte to the text buffer
     *
     * @param b The byte to add
     */
    public void append(byte b) {
        if (bytes == null) {
            bytes = new byte[10];
        } else if (pos + 1 > bytes.length) {
            ensureSize(pos * 2);
        }

        bytes[pos++] = b;
    }

    /**
     * Set the specified byte as the data byte of the buffer
     *
     * @param b The byte to set
     */
    public void set(byte b) {
        pos = 0;
        append(b);
    }

    /**
     * Copy the content of the specified byte text builder into this one
     *
     * @param b The byte text builder to copy from
     */
    public void set(ByteTextBuilder b) {
        pos = 0;
        append(b);
    }

    /**
     * Append the content of the specified byte text buffer
     *
     * @param b The byte text buffer
     */
    public void append(ByteTextBuilder b) {
        append(b.bytes, 0, b.pos);
    }

    /**
     * Set the beginning bytes
     *
     * @param src
     * @param offset
     * @param len
     */
    public void set(byte[] src, int offset, int len) {
        if (bytes == null) {
            bytes = new byte[len];
        } else if (len > bytes.length) {
            pos = 0;
            ensureSize(len);
        }

        switch (len) {
            case 2:
                bytes[1] = src[offset + 1]; //$FALL-THROUGH$
            case 1:
                bytes[0] = src[offset]; //$FALL-THROUGH$
            case 0:  // Nothing to do since there is no data
                break;
            default:
                System.arraycopy(src, offset, bytes, 0, len);
        }

        pos = len;
    }

    /**
     * Append the specified bytes into the ByteTextBuilder
     *
     * @param src    The source byte buffer
     * @param offset The offset into the buffer to start reading
     * @param len    The length to read
     */
    public void append(byte[] src, int offset, int len) {
        if (bytes == null) {
            bytes = new byte[pos + len];
        } else if (pos + len > bytes.length) {
            ensureSize(len);
        }
        System.arraycopy(src, offset, bytes, pos, len);
        pos += len;
    }

    /**
     * Append the string into the builder
     *
     * @param s The string to append
     */
    public void append(CharSequence s) {
        if (s != null) {
            final int len = s.length();
            if (bytes == null) {
                bytes = new byte[pos + len];
            } else if (pos + len > bytes.length) {
                ensureSize(len);
            }
            for (int i = 0; i < len; i++) {
                bytes[pos++] = (byte) s.charAt(i);
            }
        }
    }

    /**
     * Append the string into the builder, encoding the characters as UTF-8 in bytes
     *
     * @param s The string to append
     */
    public void appendUTF8(String s) {
        if (s != null) {
            append(s.getBytes());
        }
    }

    /**
     * Set the beginning bytes as the content of the provided string
     *
     * @param s The string as a sequence of 7 bit ASCII symbols.
     */
    public void set(CharSequence s) {
        pos = 0;
        append(s);
    }

    /**
     * Set the beginning bytes as the content of the provided string, encoding the characters as UTF-8
     *
     * @param s The string.
     */
    public void setUTF8(String s) {
        pos = 0;
        appendUTF8(s);
    }


    /**
     * Append the integer value to the buffer
     *
     * @param val The value
     */
    public void append(int val) {
        if (bytes == null) {
            bytes = new byte[pos + 11];
        } else if (pos + 11 > bytes.length) {
            ensureSize(11);
        }
        pos += writeInt(bytes, pos, val);
    }

    /**
     * Append the integer value to the buffer
     *
     * @param val                 The value
     * @param cntDigitsAfterComma The number of digits after the floiting point comma
     */
    public void appendAsFixedPointFloat(int val, int cntDigitsAfterComma) {
        if (bytes == null) {
            bytes = new byte[pos + 12];
        } else if (pos + 12 > bytes.length) {
            ensureSize(12);
        }
        pos += writeAsFixedPointFloat(bytes, pos, val, cntDigitsAfterComma);
    }


    /**
     * Append the integer value to the buffer
     *
     * @param val The value
     */
    public void append(double val) {
        // this is not very effective, should be optimized if used a lot
        final String text = String.valueOf(val);
        if (pos + text.length() > bytes.length) {
            ensureSize(text.length());
        }
        append(text);
    }

    /**
     * Append the long value to the buffer
     *
     * @param val The value
     */
    public void append(long val) {
        if (bytes == null) {
            bytes = new byte[pos + 20];
        } else if (pos + 20 > bytes.length) {
            ensureSize(20);
        }
        pos += writeLong(bytes, pos, val);
    }

    /**
     * Set the beginning bytes
     *
     * @param val
     */
    public void set(int val) {
        pos = 0;
        append(val);
    }

    /**
     * Set the beginning bytes
     *
     * @param val
     * @param cntDigitsAfterComma The number of digits after the floiting point comma
     */
    public void setAsFixedPointFloat(int val, int cntDigitsAfterComma) {
        pos = 0;
        appendAsFixedPointFloat(val, cntDigitsAfterComma);
    }


    /**
     * Set the beginning bytes
     *
     * @param val
     */
    public void set(long val) {
        pos = 0;
        append(val);
    }


    /**
     * Write our bytes to the specified output stream
     *
     * @param out The output stream
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException {
        out.write(bytes, 0, pos);
    }

    /**
     * Write an integer into the byte buffer
     *
     * @param dest   The destination buffer, must be large enough
     * @param offset The offset into the buffer to write
     * @param val    The value to write
     * @return The number of bytes written
     */
    public static int writeInt(byte[] dest, int offset, int val) {
        int idx = offset;
        if (val < 0) {
            dest[idx++] = '-';
            if (val == Integer.MIN_VALUE) {
                fillMinValueDigits(dest, idx);
                return 11;
            }
            val = -val;
        }
        final int len = cntDigits(val);
        for (int end = idx + len; end > idx; ) {
            final int mod = val % 10;
            val /= 10;
            dest[--end] = digits[mod];
        }
        return (idx - offset) + len;
    }

    /**
     * Write an integer into the byte buffer as a fixed point floating number
     *
     * @param dest                The destination buffer, must be large enough
     * @param offset              The offset into the buffer to write
     * @param val                 The value to write
     * @param cntDigitsAfterComma The number of digits after the floiting point comma
     * @return The number of bytes written
     */
    public static int writeAsFixedPointFloat(byte[] dest, int offset, int val, int cntDigitsAfterComma) {
        assert cntDigitsAfterComma > 0;
        int idx = offset;
        if (val < 0) {
            dest[idx++] = '-';
            if (val == Integer.MIN_VALUE) {
                idx = fillMinValueDigits(dest, idx);
                System.arraycopy(dest, idx - cntDigitsAfterComma, dest, (idx - cntDigitsAfterComma) + 1, cntDigitsAfterComma);
                dest[idx - cntDigitsAfterComma] = '.';
                return 12;
            }
            val = -val;
        }
        final int len = cntDigits(val);
        int digitsAfterComma = 0;
        if (len < cntDigitsAfterComma) { // Must left pad with .0*
            dest[idx++] = '.';
            for (int i = len; i < cntDigitsAfterComma; i++) {
                dest[idx++] = '0';
                digitsAfterComma++;
            }
        }
        int lastDigit = idx + (len < cntDigitsAfterComma ? len : len + 1); // Position of last digit written into the buffer
        for (int end = lastDigit; end > idx; ) {
            if (len >= cntDigitsAfterComma && (lastDigit - end == cntDigitsAfterComma)) {
                dest[--end] = '.';
            } else {
                if (len < cntDigitsAfterComma || (lastDigit - end < cntDigitsAfterComma)) {
                    digitsAfterComma++;
                }
                final int mod = val % 10;
                val /= 10;
                dest[--end] = digits[mod];
            }
        }
        while (digitsAfterComma++ < cntDigitsAfterComma) { // Must right pad with 0
            dest[lastDigit++] = '0';
        }
        return lastDigit - offset;
    }

    private static int fillMinValueDigits(byte[] dest, int idx) {
        dest[idx++] = '2';
        dest[idx++] = '1';
        dest[idx++] = '4';
        dest[idx++] = '7';
        dest[idx++] = '4';
        dest[idx++] = '8';
        dest[idx++] = '3';
        dest[idx++] = '6';
        dest[idx++] = '4';
        dest[idx++] = '8';
        return idx;
    }


    /**
     * Write a long into the byte buffer
     *
     * @param dest   The destination buffer, must be large enough
     * @param offset The offset into the buffer to write
     * @param val    The value to write
     * @return The number of bytes written
     */
    public static int writeLong(byte[] dest, int offset, long val) {
        int idx = offset;
        if (val < 0) {
            dest[idx++] = '-';
            if (val == Long.MIN_VALUE) {
                dest[idx++] = '9';
                dest[idx++] = '2';
                dest[idx++] = '2';
                dest[idx++] = '3';
                dest[idx++] = '3';
                dest[idx++] = '7';
                dest[idx++] = '2';
                dest[idx++] = '0';
                dest[idx++] = '3';
                dest[idx++] = '6';
                dest[idx++] = '8';
                dest[idx++] = '5';
                dest[idx++] = '4';
                dest[idx++] = '7';
                dest[idx++] = '7';
                dest[idx++] = '5';
                dest[idx++] = '8';
                dest[idx++] = '0';
                dest[idx++] = '8';
                return 20;
            }
            val = -val;
        }
        final int len = cntDigits(val);
        for (int end = idx + len; end > idx; ) {
            final int mod = (int) (val % 10);
            val /= 10;
            dest[--end] = digits[mod];
        }
        return (idx - offset) + len;
    }


    /**
     * Compare the text in the two buffers
     *
     * @param b
     * @return 0 if they are equals, <0 if this is lexicographically less than b and >0 if this is lexicographically greater than b
     */
    public int compareTo(ByteTextBuilder b) {
        final int l = pos - b.pos;
        if (l != 0) {
            return l;
        }

        for (int i = 0; i < pos; i++) {
            if (bytes[i] != b.bytes[i]) {
                return bytes[i] - b.bytes[i];
            }
        }
        return 0;
    }

    /**
     * Check if the content of the text buffer is like the specified patterns
     *
     * @param anyPrefix True if first part can be prefixed by anything, false if text must start exactly with the first part
     * @param anySuffix True if last part can be suffixed by anything, false if text must end exactly with the last part
     * @param parts     The parts to find in the text
     * @return True if the text is like the specified pattern, else false
     */
    public boolean isLike(boolean anyPrefix, boolean anySuffix, byte[]... parts) {
        // Find first part, check for exact beginning or any prefix
        int idx = indexAfter(0, parts[0]);
        if (idx < 0 || (!anyPrefix && idx != parts[0].length)) {
            return false;
        }

        // Find the next parts
        for (int i = 1; i < parts.length; i++) {
            idx = indexAfter(idx, parts[i]);
            if (idx < 0) {
                return false;
            }
        }

        // Check if exact ending or any suffix allowed
        return anySuffix || idx == this.length();
    }

    /**
     * Check if the content of the text buffer is like the specified patterns, ignoring case
     *
     * @param anyPrefix True if first part can be prefixed by anything, false if text must start exactly with the first part
     * @param anySuffix True if last part can be suffixed by anything, false if text must end exactly with the last part
     * @param parts     The parts to find in the text
     * @return True if the text is like the specified pattern, else false
     */
    public boolean isLikeIgnoreCase(boolean anyPrefix, boolean anySuffix, byte[]... parts) {
        // Find first part, check for exact beginning or any prefix
        int idx = indexAfterIgnoreCase(0, parts[0]);
        if (!anyPrefix && idx != parts[0].length) {
            return false;
        }

        // Find the next parts
        for (int i = 1; i < parts.length; i++) {
            idx = indexAfterIgnoreCase(idx, parts[i]);
            if (idx < 0) {
                return false;
            }
        }

        // Check if exact ending or any suffix
        return anySuffix || idx == this.length();
    }


    private int indexAfter(int idx, byte[] match) {
        int pidx = 0;
        while (idx < pos && pidx < match.length) {
            if (bytes[idx] == match[pidx]) {
                pidx++;
            } else {
                pidx = 0;
            }
            idx++;
        }
        return (pidx == match.length) ? idx : -1;
    }

    private int indexAfterIgnoreCase(int idx, byte[] match) {
        int pidx = 0;
        while (idx < pos && pidx < match.length) {
            if (upper(bytes[idx]) == upper(match[pidx])) {
                pidx++;
            } else {
                pidx = 0;
            }
            idx++;
        }
        return (pidx == match.length) ? idx : -1;
    }


    /**
     * Compare the text in the two buffers
     *
     * @param b
     * @return 0 if they are equals, <0 if this is lexicographically less than b and >0 if this is lexicographically greater than b
     */
    public int compareToIgnoreCase(ByteTextBuilder b) {
        final int l = pos - b.pos;
        if (l != 0) {
            return l;
        }

        for (int i = 0; i < pos; i++) {
            if (upper(bytes[i]) != upper(b.bytes[i])) {
                return bytes[i] - b.bytes[i];
            }
        }
        return 0;
    }

    /**
     * Convert the byte text into an integer
     *
     * @return The integer value
     */
    public int toInt(boolean throwex) {
        return ByteArray.toInt(bytes, 0, pos, throwex);
    }

    /**
     * Convert the byte text into an integer
     *
     * @return The integer value
     */
    public int toInt() {
        return ByteArray.toInt(bytes, 0, pos, true);
    }

    /**
     * Convert the byte text into an long
     *
     * @return The long value
     */
    public long toLong() {
        if (pos == 0) return 0L;
        return ByteArray.toLongWithException(bytes, 0, pos);
    }

    /**
     * @return True if the byte text can be converted to long, else false.
     */
    public boolean isLong() {
        return ByteArray.isLong(bytes, 0, pos);
    }

    /**
     * @return True if the byte text can be converted to double, else false.
     */
    public boolean isDouble() {
        return ByteArray.isDouble(bytes, 0, pos);
    }

    /**
     * Convert the byte text into a double, inv,-inv and nan must be recognized for what they are.
     *
     * @return The double value
     */
    public double toDouble() {
        if (pos == 0) return Double.NaN;
        return ByteArray.toDouble(bytes, 0, pos);
    }

    /**
     * Convert the byte text into a double, inv,-inv and nan must be recognized for what they are.
     * Throw NumberFormatException if this is not a double number
     *
     * @return The double value
     */
    public double toDoubleWithException() {
        return ByteArray.jdkToDouble(bytes, 0, pos);
    }


    private static byte upper(byte b) {
        return (byte) (b >= ((byte) 97) && b <= ((byte) 122) ? b - ((byte) 32) : b);
    }


    /**
     * Copy the content of our internal buffers into the destination buffer
     *
     * @param dest   Destination buffer
     * @param offset The offset into the destination buffer of first element
     * @return The number of bytes copied into the buffer
     */
    public int copy(byte[] dest, int offset) {
        assert (offset + pos < dest.length) : "Destination Buffer is to small!";

        for (int i = 0; i < pos; i++) {
            dest[offset++] = bytes[i];
        }
        return pos;
    }

    /**
     * Copy the content of our internal buffers into the destination buffer
     *
     * @param dest Destination buffer
     */
    public void copy(StringBuilder dest) {
        String s = this.toString();
        dest.append(s, 0, s.length());
    }

    private void ensureSize(int len) {
        byte[] b = new byte[Math.max(pos + len, bytes.length * 2)];
        System.arraycopy(bytes, 0, b, 0, pos);
        bytes = b;
    }

    /**
     * @param val A non negative integer value
     * @return The number of digits in the specified integer value
     */
    public static int cntDigits(long val) {
        if (val <= Integer.MAX_VALUE) {
            return cntDigits((int) val);
        }

        if (val <= 99999999999999L) {
            if (val <= 99999999999L) {
                return (val <= 9999999999L ? 10 : 11);
            }
            if (val <= 9999999999999L) {
                return (val <= 999999999999L ? 12 : 13);
            }
            return 14;
        }

        if (val <= 9999999999999999L) {
            return (val <= 999999999999999L ? 15 : 16);
        }

        if (val <= 999999999999999999L) {
            return (val <= 99999999999999999L ? 17 : 18);
        }
        return 19;
    }

    /**
     * @param val A non negative integer value
     * @return The number of digits in the specified integer value
     */
    public static int cntDigits(int val) {
        if (val <= 99999) {
            if (val <= 99) {
                return (val <= 9 ? 1 : 2);
            }
            if (val <= 9999) {
                return (val <= 999 ? 3 : 4);
            }
            return 5;
        }
        if (val <= 9999999) {
            return (val <= 999999 ? 6 : 7);
        }
        if (val <= 999999999) {
            return (val <= 99999999 ? 8 : 9);
        }
        return 10;
    }

}
