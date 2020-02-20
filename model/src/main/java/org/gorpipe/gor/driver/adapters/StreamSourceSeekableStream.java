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

package org.gorpipe.gor.driver.adapters;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;

/**
 * SeekableFile implementation using a StreamSource.
 * <p>
 * This allows a stream source be used in Samtools libraries
 * <p>
 * Created by villi on 23/08/15.
 */
public class StreamSourceSeekableStream extends SeekableStream {
    private final StreamSourceSeekableFile delegate;

    public StreamSourceSeekableStream(StreamSource source) {
        this.delegate = new StreamSourceSeekableFile(source);
    }

    @Override
    public long length() {
        try {
            return delegate.length();
        } catch (IOException e) {
            throw new RuntimeException("Cannot get length of delegate", e);
        }
    }

    @Override
    public long position() throws IOException {
        return delegate.getFilePointer();
    }

    @Override
    public void seek(long position) throws IOException {
        delegate.seek(position);
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return delegate.read(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean eof() throws IOException {
        return position() >= length();
    }

    @Override
    public String getSource() {
        try {
            return delegate.getMeta().getNamedUrl();
        } catch (IOException e) {
            throw new RuntimeException("Cannot get canonical getName of delegate");
        }
    }
}
