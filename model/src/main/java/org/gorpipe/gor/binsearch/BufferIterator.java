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

package org.gorpipe.gor.binsearch;

import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Provides a seekable iterator interface to ordered lines of text contained in a byte buffer.
 *
 * @author hjaltii
 */
class BufferIterator {

    private byte[] buffer;
    private boolean hasNext;
    private int bufferIdx;
    private int lowerBound; //The beginning of the first line in the buffer. (If there is some)
    private int upperBound; //The beginning of the first line behind the last line in the buffer or (upTo if there is none).
    private final StringIntKey comparator;
    private StringIntKey firstKey;
    private StringIntKey lastKey;

    BufferIterator(StringIntKey comparator) {
        this.comparator = comparator;
    }

    /**
     * A method to reset the buffer on which the iterator is working.
     *
     * @param buffer The buffer under consideration.
     * @param offset Lower bound on where to start seeking, inclusive.
     * @param upTo Upper bound on where to start seeking, exclusive.
     * @param firstPosBeginOfLine Indicates that the buffer starts at the beginning of line.
     * @param lastPosEndOfLine Indicates that the (possibly imagined) byte at upTo is a newline or the beginning of a line.
     */
    void update(byte[] buffer, int offset, int upTo, boolean firstPosBeginOfLine, boolean lastPosEndOfLine) {
        if (offset >= upTo) {
            this.hasNext = false;
            this.buffer = null;
        } else {
            this.lowerBound = getLowerBound(buffer, offset, upTo, firstPosBeginOfLine);
            this.upperBound = getUpperBound(buffer, offset, upTo, lastPosEndOfLine);
            if (this.lowerBound < this.upperBound) {
                this.buffer = buffer;
                this.hasNext = true;
                this.bufferIdx = this.lowerBound;
            } else {
                this.hasNext = false;
                this.buffer = null;
            }
        }
        this.firstKey = null;
        this.lastKey = null;
    }

    void clear() {
        this.buffer = null;
        this.hasNext = false;
    }

    boolean hasNext() {
        return this.hasNext;
    }

    Row getNextAsRow() {
        return new RowBase(getNextAsString());
    }

    String getNextAsString() {
        final int beginOfNextLine = getEndOfNextLine(this.buffer, this.bufferIdx, this.upperBound);
        final int len = this.buffer[beginOfNextLine - 2] == '\r' ? beginOfNextLine - this.bufferIdx - 2 : beginOfNextLine - this.bufferIdx - 1;
        final String toReturn = new String(this.buffer, this.bufferIdx, len, StandardCharsets.UTF_8);
        this.bufferIdx = beginOfNextLine;
        this.hasNext = this.bufferIdx < this.upperBound;
        return toReturn;
    }

    byte[] getNextAsBytes() {
        final int beginOfNextLine = getEndOfNextLine(this.buffer, this.bufferIdx, this.upperBound);
        final int len = this.buffer[beginOfNextLine - 2] == '\r' ? beginOfNextLine - this.bufferIdx - 2 : beginOfNextLine - this.bufferIdx - 1;
        final byte[] toReturn = new byte[len];
        System.arraycopy(this.buffer, this.bufferIdx, toReturn, 0, len);
        this.bufferIdx = beginOfNextLine;
        this.hasNext = this.bufferIdx < this.upperBound;
        return toReturn;
    }

    void writeNextToStream(OutputStream os) throws IOException {
        final int beginOfNextLine = getEndOfNextLine(this.buffer, this.bufferIdx, this.upperBound);
        final int len = this.buffer[beginOfNextLine - 2] == '\r' ? beginOfNextLine - this.bufferIdx - 2 : beginOfNextLine - this.bufferIdx - 1;
        os.write(this.buffer, this.bufferIdx, len);
        this.bufferIdx = beginOfNextLine;
        this.hasNext = this.bufferIdx < this.upperBound;
    }

    StringIntKey getNextKey() {
        final StringIntKey toReturn = this.comparator.createKey(this.buffer, this.upperBound, this.bufferIdx);
        this.bufferIdx = getEndOfNextLine(this.buffer, this.bufferIdx, this.upperBound);
        this.hasNext = this.bufferIdx < this.upperBound;
        return toReturn;
    }

    void seek(StringIntKey key) {
        if (this.hasNext) {
            if (key.compareTo(this.getLastKey()) > 0) {
                this.bufferIdx = this.upperBound;
            } else if (key.compareTo(this.getFirstKey()) > 0) {
                this.bufferIdx = findInBuffer(key, this.buffer, this.getFirstLineEnd(), this.upperBound, this.comparator);
            } else {
                this.bufferIdx = this.lowerBound;
            }
            this.hasNext = this.bufferIdx < this.upperBound;
        }
    }

    StringIntKey getFirstKey() {
        if (this.firstKey == null) {
            computeFirstKey();
        }
        return this.firstKey;
    }

    StringIntKey getLastKey() {
        if (this.lastKey == null) {
            computeLastKey();
        }
        return this.lastKey;
    }

    int getFirstLineEnd() {
        return getEndOfNextLine(this.buffer, this.lowerBound, this.upperBound);
    }

    int getBufferIdx() {
        return this.bufferIdx;
    }

    private void computeFirstKey() {
        this.firstKey = this.comparator.createKey(this.buffer, this.upperBound, this.lowerBound);
    }

    private void computeLastKey() {
        final int lastKeyBeginning = getBeginningOfLastLine(this.buffer, this.lowerBound, this.upperBound);
        this.lastKey = this.comparator.createKey(this.buffer, this.upperBound, lastKeyBeginning);
    }

    int getLowerBound() {
        return this.lowerBound;
    }

    int getUpperBound() {
        return this.upperBound;
    }

    static int getLowerBound(byte[] buffer, int offset, int upTo, boolean firstPosBeginOfLine) {
        if (firstPosBeginOfLine) {
            return offset;
        } else {
            int idx = offset;
            while (idx < upTo && buffer[idx++] != '\n');
            return buffer[idx - 1] == '\n' ? idx : idx + 1;
        }
    }

    static int getUpperBound(byte[] buffer, int offset, int upTo, boolean lastPosEndOfLine) {
        if (lastPosEndOfLine) {
            return upTo;
        } else {
            int idx = upTo;
            while (buffer[--idx] != '\n' && idx > offset);
            return buffer[idx] == '\n' ? idx + 1 : idx;
        }
    }

    static int getEndOfNextLine(byte[] buffer, int lowerBound, int upperBound) {
        final int upTo = Math.min(upperBound, buffer.length);
        int idx = lowerBound;
        while (idx < upTo && buffer[idx++] != '\n');
        return buffer[idx - 1] == '\n' ? idx : idx + 1;
    }

    static int getBeginningOfLastLine(byte[] buffer, int lowerBound, int upperBound) {
        int idx = upperBound - 1;
        while (buffer[--idx] != '\n' && idx > lowerBound);
        return buffer[idx] == '\n' ? idx + 1 : idx;
    }

    /**
     * @return The position in buffer of the first line whose key is >= key.
     */
    static int findInBuffer(StringIntKey key, byte[] buffer, int offset, int upTo, StringIntKey comparator) {
        final int newUpTo = Math.min(buffer.length, upTo);
        int lowerBound = offset, upperBound = upTo;
        while (lowerBound != upperBound) {
            final int pos = (lowerBound + upperBound) / 2;
            int lower = pos;
            while (lower > offset && buffer[--lower] != '\n') ;
            if (buffer[lower] == '\n') ++lower;
            int upper = pos;
            while (upper < newUpTo && buffer[upper++] != '\n');
            final StringIntKey currentKey = comparator.createKey(buffer, upper, lower);
            if (currentKey.compareTo(key) < 0) {
                lowerBound = upper;
            } else {
                upperBound = lower;
            }
        }
        return lowerBound;
    }
}
