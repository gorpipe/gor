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

package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A binary search iterator for a text file whose lines are ordered as defined by {@code comparator}.
 *
 * The behavior of the iterator is undefined if the file is not ordered as described above.
 *
 * @author hjaltii
 */
public class SeekableIterator implements AutoCloseable {
    private static final int DEFAULT_BUFFER_SIZE = 64 * 1024; //64K
    private static final int MAXIMUM_LINE_SIZE = 32 * 1024 * 1024; //32M
    static final StringIntKey DEFAULT_COMPARATOR = new StringIntKey(0, 1, StringIntKey.cmpLexico);

    private byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    private final StreamSourceSeekableFile file;
    private final StreamSourceSeekableFile indexFile;
    private PositionCache filePositionCache;
    private final byte[] header;
    private int numberOfBytesInBuffer = 0;
    private long bufferPosInFile = 0;
    private long bufferEndInFile = 0;
    private final long fileSize;
    private final BufferIterator bufferIterator;
    private int offset;

    /**
     * Creates a new instance of an iterator using {@code DEFAULT_COMPARATOR}
     */
    public SeekableIterator(StreamSourceSeekableFile file, boolean hasHeader) throws IOException {
        this(file, null, hasHeader);
    }

    /**
     * Creates a new instance of an iterator using {@code DEFAULT_COMPARATOR}
     */
    public SeekableIterator(StreamSourceSeekableFile file, StreamSourceSeekableFile indexFile, boolean hasHeader) throws IOException {
        this(file, indexFile, DEFAULT_COMPARATOR, hasHeader);
    }

    /**
     * Creates a new instance of an iterator.
     *
     * @param comparator An IKey containing the position of the chromosome and position columns and the comparator to use.
     */
    public SeekableIterator(StreamSourceSeekableFile file, StreamSourceSeekableFile indexFile, StringIntKey comparator, boolean hasHeader) throws IOException {
        this.bufferIterator = new BufferIterator(comparator);
        this.file = file;
        this.indexFile = indexFile;
        this.fileSize = this.file.length();
        if (hasHeader) {
            this.header = readHeader();
            offset = this.bufferIterator.getBufferIdx();
            this.bufferIterator.update(this.buffer, offset, this.numberOfBytesInBuffer, true, this.bufferEndInFile >= this.fileSize);
        } else {
            this.header = null;
            offset = 0;
        }
    }

    /**
     * @return The header of the file as a string.
     */
    public String getHeader() {
        return this.header == null ? null : new String(this.header);
    }

    /**
     * @return The header as a byte array.
     */
    public byte[] getHeaderBytes() {
        return this.header;
    }

    /**
     * Reads the header. Must be called when the buffer is located at the beginning of the file.
     *
     * @return The header as bytes.
     * @throws IOException In case there is a problem accessing the file.
     */
    protected byte[] readHeader() throws IOException {
        while (hasNext()) {
            byte[] nextAsBytes = getNextAsBytes();
            if (nextAsBytes.length < 2) {
                return nextAsBytes;
            }
            if (nextAsBytes[0] != '#' || nextAsBytes[1] != '#') {
                return nextAsBytes;
            }
        }
        return new byte[0];
    }

    /**
     * @return Whether there is some more content in the file behind the current position.
     */
    public boolean hasNext() {
        return this.bufferIterator.hasNext() || this.bufferEndInFile < this.fileSize;
    }

    /**
     * @return The next line of the file as a byte array.
     * @throws IOException If we encounter a problem reading the file.
     */
    public byte[] getNextAsBytes() throws IOException {
        if (!this.bufferIterator.hasNext()) {
            slideBuffer();
        }
        return this.bufferIterator.getNextAsBytes();
    }

    /**
     * @return The next line of the file as string.
     * @throws IOException If we encounter a problem reading the file.
     */
    public String getNextAsString() throws IOException {
        if (!this.bufferIterator.hasNext()) {
            slideBuffer();
        }
        return this.bufferIterator.getNextAsString();
    }

    public void writeNextToStream(OutputStream os) throws IOException {
        if (!this.bufferIterator.hasNext()) {
            slideBuffer();
        }
        this.bufferIterator.writeNextToStream(os);
    }

    /**
     * Seeks to the position of the first line whose key is >= to key.
     *
     * @throws IOException If we encounter a problem seeking to the file and reading its content.
     */
    public void seek(StringIntKey key) throws IOException {
        if (filePositionCache == null) {
            final String uniqueId = file.getMeta().getUniqueId();
            filePositionCache = PositionCache.getFilePositionCache(this, this.file.getCanonicalPath(), uniqueId, offset, this.fileSize);
            if (indexFile != null) {
                this.filePositionCache.loadIndex(indexFile);
            }
        }

        final Position lowerBound = this.filePositionCache.getLowerBound(key);
        final Position upperBound = this.filePositionCache.getUpperBound(key);

        if (this.bufferIterator.hasNext()) {
            checkBuffer(key, lowerBound.fileIdx, upperBound.fileIdx, lowerBound.key, upperBound.key);
        } else {
            seekWithinRange(key, lowerBound.fileIdx, upperBound.fileIdx, lowerBound.key, upperBound.key);
        }
    }

    @Override
    public void close() throws IOException {
        this.file.close();
    }

    private void slideBuffer() throws IOException {
        final int bufferUpperBound = this.bufferIterator.getUpperBound();
        this.bufferPosInFile += bufferUpperBound;
        this.numberOfBytesInBuffer -= bufferUpperBound;
        if (this.numberOfBytesInBuffer > 0) {
            System.arraycopy(this.buffer, bufferUpperBound, this.buffer, 0, this.numberOfBytesInBuffer);
        }
        if (this.file.getFilePointer() != this.bufferEndInFile) {
            this.file.seek(this.bufferEndInFile);
        }
        while (this.bufferEndInFile < this.fileSize) {
            extendBufferToRight();
            if (this.bufferIterator.hasNext()) {
                break;
            } else {
                final byte[] oldBuffer = this.buffer;
                doubleTheBuffer();
                System.arraycopy(oldBuffer, 0, this.buffer, 0, oldBuffer.length);
            }
        }
    }

    private void extendBufferToRight() throws IOException {
        this.numberOfBytesInBuffer += readFully(this.buffer, this.numberOfBytesInBuffer, this.buffer.length - this.numberOfBytesInBuffer);
        this.bufferEndInFile = this.bufferPosInFile + this.numberOfBytesInBuffer;
        this.bufferIterator.update(this.buffer, 0, this.numberOfBytesInBuffer, true, this.bufferEndInFile == this.fileSize);
    }

    /**
     * Tells whether there exists a line with key >= key within a given range in the file. Moves our position
     * to the beginning of the first line with that property if it exists.
     *
     * The seek range is [lowerBound, upperBound[, lowerBound is the beginning of a line and upperBound is either
     * the beginning of a line or the size of the file.
     *
     * The function is recursive. In each recursion step, we either decrease upperBound - lowerBound or increase the
     * size of the buffer so the function will terminate since finally everything of interest is in the buffer.
     */
    private void seekWithinRange(StringIntKey key, long lowerBound, long upperBound, StringIntKey lowerBoundKey, StringIntKey upperBoundKey) throws IOException {
        if (lowerBound < upperBound) {
            seekWithinRange(key, lowerBound, upperBound, lowerBoundKey, upperBoundKey, false);
        } else {
            this.bufferIterator.clear();
            this.bufferPosInFile = this.fileSize;
            this.bufferEndInFile = this.fileSize;
            this.numberOfBytesInBuffer = 0;
        }
    }

    private void seekWithinRange(StringIntKey key, long lowerBound, long upperBound, StringIntKey lowerBoundKey, StringIntKey upperBoundKey, boolean doubleTheBuffer) throws IOException {
        estimateSeekAndReadWholeLineToBuffer(key, lowerBound, upperBound, lowerBoundKey, upperBoundKey, doubleTheBuffer);
        checkBuffer(key, lowerBound, upperBound, lowerBoundKey, upperBoundKey);
    }

    /**
     * Tells whether there exists a line with key >= key within a given range in the file. Moves our position
     * to the beginning of the first line with that property if it exists.
     *
     * The function begins by trying to find the position of interest within the buffer, under the assumption that it
     * contains a whole line.
     */
    private void checkBuffer(StringIntKey key, long lowerBound, long upperBound, StringIntKey lowerBoundKey, StringIntKey upperBoundKey) throws IOException {
        if (lowerBound < upperBound) {
            final long bufferLowerBound = this.bufferPosInFile + this.bufferIterator.getLowerBound();
            final long bufferUpperBound = this.bufferPosInFile + this.bufferIterator.getUpperBound();
            boolean doubleTheBuffer = false;
            if (bufferUpperBound > lowerBound && bufferLowerBound < upperBound) {
                this.bufferIterator.seek(key);
                if (this.bufferIterator.hasNext()) {
                    if (bufferLowerBound <= lowerBound || this.bufferIterator.getFirstKey().compareTo(key) < 0) {
                        return;
                    } else {
                        upperBound = this.bufferPosInFile + this.bufferIterator.getFirstLineEnd();
                        upperBoundKey = this.bufferIterator.getNextKey();
                        if (upperBound == bufferUpperBound) {
                            doubleTheBuffer = true;
                        }
                    }
                } else if (this.bufferEndInFile >= this.fileSize) {
                    this.bufferIterator.clear();
                    return;
                } else {
                    lowerBound = bufferUpperBound;
                    lowerBoundKey = null;
                }
            }
            seekWithinRange(key, lowerBound, upperBound, lowerBoundKey, upperBoundKey, doubleTheBuffer);
        } else {
            this.bufferPosInFile = this.fileSize;
            this.bufferEndInFile = this.fileSize;
            this.numberOfBytesInBuffer = 0;
            this.bufferIterator.clear();
        }
    }

    private void estimateSeekAndReadWholeLineToBuffer(StringIntKey key, long lowerBound, long upperBound, StringIntKey lowerBoundKey, StringIntKey upperBoundKey, boolean doubleTheBuffer) throws IOException {
        final long estimatedPosition = getEstimatedPositionInFile(lowerBound, upperBound, key, lowerBoundKey, upperBoundKey);
        seekAndReadWholeLineToBuffer(estimatedPosition, lowerBound, upperBound, doubleTheBuffer);
    }

    private void seekAndReadWholeLineToBuffer(long estimatedPosition, long lowerBound, long upperBound, boolean doubleTheBuffer) throws IOException {
        final long posToSeekTo = getPosToSeekTo(estimatedPosition, lowerBound, upperBound);
        if (doubleTheBuffer) {
            doubleTheBuffer();
        }

        this.numberOfBytesInBuffer = readToBufferFromPos(posToSeekTo);
        this.bufferPosInFile = posToSeekTo;
        this.bufferEndInFile = this.bufferPosInFile + this.numberOfBytesInBuffer;
        this.bufferIterator.update(this.buffer, 0, this.numberOfBytesInBuffer, this.bufferPosInFile == lowerBound,
                this.bufferEndInFile == upperBound || this.bufferEndInFile == this.fileSize);
        if (!this.bufferIterator.hasNext()) {
            seekAndReadWholeLineToBuffer(estimatedPosition, lowerBound, upperBound, true);
        } else {
            this.filePositionCache.putFilePosition(this.bufferIterator.getFirstKey(), this.bufferPosInFile + this.bufferIterator.getFirstLineEnd());
            this.filePositionCache.putFilePosition(this.bufferIterator.getLastKey(), this.bufferPosInFile + this.bufferIterator.getUpperBound());
        }
    }

    private int readToBufferFromPos(long posToSeekTo) throws IOException {
        this.file.seek(posToSeekTo);
        return readFully(this.buffer, 0, this.buffer.length);
    }

    private long getEstimatedPositionInFile(long lowerBound, long upperBound, StringIntKey key, StringIntKey lowerBoundKey, StringIntKey upperBoundKey) {
        float v = lowerBoundKey != null && upperBoundKey != null ? key.deriveCoefficient(lowerBoundKey, upperBoundKey) : 0.5f;
        return (long) (lowerBound + v * (upperBound - lowerBound));
    }

    private long getPosToSeekTo(long estimatedPosition, long lowerBound, long upperBound) {
        return Math.max(lowerBound, Math.min(estimatedPosition - this.buffer.length / 2, upperBound - this.buffer.length));
    }

    /**
     * Reads len many bytes from the current position in the file and writes them to the buffer, if the number
     * of bytes left is >= len.
     *
     * @param buffer The buffer to write the bytes to.
     * @param offset The offset in the buffer.
     * @param len The number of bytes to write.
     * @return The actual number of bytes written.
     * @throws IOException If we encounter a problem when reading the file.
     */
    private int readFully(byte[] buffer, int offset, int len) throws IOException {
        final int upTo = offset + len;
        int bufferIdx = offset;
        int read;
        while ((read = this.file.read(buffer, bufferIdx, upTo - bufferIdx)) > 0) {
            bufferIdx += read;
        }
        return bufferIdx - offset;
    }

    private void doubleTheBuffer() {
        if (this.buffer.length > MAXIMUM_LINE_SIZE) {
            throw new IllegalStateException();
        } else {
            this.buffer = new byte[2 * this.buffer.length];
        }
    }
}
