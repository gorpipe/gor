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

import com.github.luben.zstd.ZstdInputStream;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Line;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.util.collection.ByteArray;
import org.gorpipe.util.collection.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.InflaterOutputStream;

public class GorzSeekableIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(GorzSeekableIterator.class);

    private final SeekableIterator seekableIterator; //The iterator on the underlying file.
    private final String filePath;
    private GorHeader header;
    private final Unzipper unzipper;
    private byte beginOfBlockByte;
    private byte[] buffer;
    private final BufferIterator bufferIterator = new BufferIterator(SeekableIterator.DEFAULT_COMPARATOR); //An iterator to iterate a block once unzipped.
    private final ByteArrayWrapper rawDataHolder = new ByteArrayWrapper();
    private boolean firstBlock = true;

    public GorzSeekableIterator(StreamSourceSeekableFile file) {
        this(file, null);
    }

    public GorzSeekableIterator(StreamSourceSeekableFile file, StreamSourceSeekableFile index) {
        try {
            this.filePath = file.getCanonicalPath();
            this.seekableIterator = new SeekableIterator(file, index,true);
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
        final byte[] headerBytes = this.seekableIterator.getHeaderBytes();
        int idx = 0;
        while (idx < headerBytes.length && headerBytes[idx++] != 0);
        final String headerAsString;
        if (idx != headerBytes.length) {
            this.unzipper = new ColumnCompressedUnzipper(Arrays.copyOfRange(headerBytes, idx, headerBytes.length));
            headerAsString = new String(headerBytes, 0, idx - 1);
        } else {
            this.unzipper = new Unzipper();
            headerAsString = new String(headerBytes);
        }
        this.header = new GorHeader(headerAsString.split("\t"));
        this.buffer = new byte[32 * 1024];
    }

    @Override
    public String getHeader() {
        return String.join("\t", this.header.getColumns());
    }

    @Override
    public boolean seek(String chr, int pos) {
        final StringIntKey key = new StringIntKey(chr, pos);
        this.bufferIterator.seek(key);
        if (this.bufferIterator.hasNext() && this.bufferIterator.getFirstKey().compareTo(key) < 0) {
            return true;
        } else {
            try {
                return seekFile(key);
            } catch (IOException e) {
                throw new GorResourceException("Error reading gorz file: " + e.getMessage(), this.filePath, e);
            } catch (DataFormatException e) {
                throw new GorResourceException("Corrupt gorz file: " + e.getMessage(), this.filePath, e);
            }
        }
    }

    private boolean seekFile(StringIntKey key) throws IOException, DataFormatException {
        this.seekableIterator.seek(key);
        if (this.seekableIterator.hasNext()) {
            loadBufferIterator();
            this.bufferIterator.seek(key);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasNext() {
        return this.bufferIterator.hasNext() || this.seekableIterator.hasNext();
    }

    @Override
    public Row next() {
        if (!this.bufferIterator.hasNext()) {
            try {
                loadBufferIterator();
            } catch (IOException e) {
                throw new GorResourceException("Error reading gorz file: " + e.getMessage(), this.filePath, e);
            } catch (DataFormatException e) {
                throw new GorResourceException("Corrupt gorz file: " + e.getMessage(), this.filePath, e);
            }
        }
        return RowObj.apply(this.bufferIterator.getNextAsString());
    }

    @Override
    public boolean next(Line line) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        try {
            this.seekableIterator.close();
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    private void loadBufferIterator() throws IOException, DataFormatException {
        this.rawDataHolder.reset();
        this.seekableIterator.writeNextToStream(this.rawDataHolder);
        final byte[] in = this.rawDataHolder.getBuffer();
        final int len = this.rawDataHolder.size();
        final int blockIdx = getBeginningOfBlock(in);

        final int unzippedLen = unzipBlock(in, len, blockIdx);
        this.bufferIterator.update(this.buffer, 0, unzippedLen, true, true);
    }


    private int getBeginningOfBlock(byte[] in) {
        int idx = 0;
        while (idx < in.length && in[idx++] != '\t');
        while (idx < in.length && in[idx++] != '\t');

        if (idx == in.length || idx + 1 == in.length) {
            String msg = String.format("Could not find zipped block in %s%nBuffer contains %d bytes", this.filePath, in.length);
            throw new GorDataException(msg);
        }

        if (this.firstBlock) {
            this.beginOfBlockByte = in[idx];
            final CompressionType type = (this.beginOfBlockByte & 0x02) == 0 ? CompressionType.ZLIB : CompressionType.ZSTD;
            this.unzipper.setType(type);
            this.firstBlock = false;
        }

        return idx + 1;
    }

    private int unzipBlock(byte[] in, int len, int blockIdx) throws DataFormatException, IOException {
        this.unzipper.setInput(in, blockIdx, len - blockIdx);
        int totalRead = 0;
        do {
            int read;
            while ((read = this.unzipper.decompress(this.buffer, totalRead, this.buffer.length - totalRead)) > 0) {
                totalRead += read;
            }
            if (totalRead == this.buffer.length) {
                this.buffer = Arrays.copyOf(this.buffer, 2 * this.buffer.length);
            } else {
                break;
            }
        } while (true);
        return totalRead;
    }

    class ColumnCompressedUnzipper extends Unzipper {
        private byte[] buffer;
        private byte[] lookupBytesCompressed7Bit;
        private final Map<Integer, Map<Integer, byte[]>> mapExtTable;
        private boolean lookupTableParsed = false;

        ColumnCompressedUnzipper(byte[] lookupBytesCompressed7Bit) {
            super();
            this.buffer = new byte[32 * 1024];
            this.mapExtTable = new HashMap<>();
            this.lookupBytesCompressed7Bit = lookupBytesCompressed7Bit;
        }

        private byte[] getLookupTable() {
            final byte[] lookupBytesCompressed = ByteArray.to8Bit(lookupBytesCompressed7Bit);
            final byte[] toReturn;
            try {
                toReturn = inflate(lookupBytesCompressed);
            } catch (IOException e) {
                throw new GorDataException("Could not uncompress the lookup table in " + filePath, e);
            }
            return toReturn;
        }

        private byte[] inflate(byte[] lookupBytesCompressed) throws IOException {
            if (this.type == CompressionType.ZLIB) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (final InflaterOutputStream infOS = new InflaterOutputStream(baos)) {
                    infOS.write(lookupBytesCompressed);
                }
                return baos.toByteArray();
            } else {
                final ByteArrayInputStream baos = new ByteArrayInputStream(lookupBytesCompressed);
                final byte[] toReturn;
                try (final ZstdInputStream zstdInputStream = new ZstdInputStream(baos)) {
                    byte[] array = new byte[16];
                    int read;
                    int totalRead = 0;
                    while ((read = zstdInputStream.read(array, totalRead, array.length - totalRead)) > 0) {
                        totalRead += read;
                        if (totalRead == array.length) {
                            array = Arrays.copyOf(array, 2 * array.length);
                        }
                    }
                    toReturn = Arrays.copyOfRange(array, 0, totalRead);
                }
                return toReturn;
            }
        }

        @Override
        public int decompress(byte[] out, int offset, int len) throws DataFormatException, IOException {
            if (!this.lookupTableParsed) {
                final byte[] lookupTable = getLookupTable();
                BlockPacker.lookupMapFromBytes(this.mapExtTable, lookupTable);
                this.lookupTableParsed = true;
                this.lookupBytesCompressed7Bit = null;
            }
            if (this.done) {
                return 0;
            } else {
                super.decompress(this.buffer, 0, this.buffer.length);
                return BlockPacker.decode(this.buffer, 0, out, offset, this.mapExtTable);
            }
        }
    }
}
