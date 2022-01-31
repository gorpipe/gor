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

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;

public class GorzSeekableIterator extends GenomicIteratorBase {
    private static final Logger log = LoggerFactory.getLogger(GorzSeekableIterator.class);
    private static final String ITERATOR_CLOSED_MESSAGE = "Iterator is closed";
    private static final String GORZ_CORRUPTED_MESSAGE = "Corrupt gorz file: ";

    private final SeekableIterator seekableIterator; //The iterator on the underlying file.
    private final String filePath;
    private GorHeader header;
    private final Unzipper unzipper;
    private final RowBuffer bufferIterator; //An iterator to iterate a block once unzipped.
    private boolean isClosed = false;

    public GorzSeekableIterator(StreamSourceSeekableFile file) {
        this(file, null);
    }

    public GorzSeekableIterator(StreamSourceSeekableFile file, StreamSourceSeekableFile index) {
        try {
            this.filePath = file.getCanonicalPath();
            this.seekableIterator = new SeekableIterator(file, index,true);
        } catch (IOException e) {
            throw wrapIOException(e);
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
        var headerSplit = headerAsString.split("\t");
        this.header = new GorHeader(headerSplit);
        this.bufferIterator = new RowBuffer();
        this.bufferIterator.setColumnCount(headerSplit.length);
    }

    @Override
    public String getHeader() {
        return String.join("\t", this.header.getColumns());
    }

    @Override
    public boolean seek(String chr, int pos) {
        if (isClosed) {
            throw new GorSystemException(ITERATOR_CLOSED_MESSAGE, null);
        }
        final Row key = new RowBase(chr+"\t"+pos,2);
        this.bufferIterator.seek(key);
        if (this.bufferIterator.available() && this.bufferIterator.get(0).compareTo(key) < 0) {
            return true;
        } else {
            try {
                return seekFile(key);
            } catch (IOException e) {
                throw wrapIOException(e);
            } catch (DataFormatException e) {
                throw new GorResourceException(GORZ_CORRUPTED_MESSAGE + e.getMessage(), this.filePath, e);
            }
        }
    }

    private GorException wrapIOException(IOException e) {
        if (e.getMessage().equals("Stale file handle")) {
            return new GorSystemException("Stale file handle reading " + this.filePath, e);
        } else {
            return new GorResourceException("Error reading gorz file: " + e.getMessage(), this.filePath, e);
        }
    }

    private boolean seekFile(Row key) throws IOException, DataFormatException {
        this.seekableIterator.seek(new StringIntKey(key.chr, key.pos));
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
        if (isClosed) {
            throw new GorSystemException(ITERATOR_CLOSED_MESSAGE, null);
        }
        return this.bufferIterator.available() || this.seekableIterator.hasNext();
    }

    @Override
    public Row next() {
        if (isClosed) {
            throw new GorSystemException(ITERATOR_CLOSED_MESSAGE, null);
        }
        if (!this.bufferIterator.available()) {
            try {
                loadBufferIterator();
            } catch (IOException e) {
                throw wrapIOException(e);
            } catch (DataFormatException e) {
                throw new GorResourceException(GORZ_CORRUPTED_MESSAGE + e.getMessage(), this.filePath, e);
            }
        }
        return this.bufferIterator.next();
    }

    @Override
    public void close() {
        isClosed = true;
        try {
            this.seekableIterator.close();
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    private void loadBufferIterator() throws IOException, DataFormatException {
        this.unzipper.rawDataHolder.position(0);
        this.seekableIterator.writeNextToBuffer(this.unzipper.rawDataHolder);

        final int unzippedLen = unzipper.unzipBlock();
        this.bufferIterator.update(unzipper.out.array(), unzippedLen);
    }
}
