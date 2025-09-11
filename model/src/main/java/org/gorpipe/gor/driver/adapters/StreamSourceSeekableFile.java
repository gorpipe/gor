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

import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;
import org.gorpipe.gor.model.SeekableFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * SeekableFile implementation using a StreamSource.
 * <p>
 * This is usable in some gor iterators.
 * <p>
 * Created by villi on 22/08/15.
 */
public class StreamSourceSeekableFile extends SeekableFile {
    private static final Logger log = LoggerFactory.getLogger(StreamSourceSeekableFile.class);

    final StreamSource source;

    long position = 0;
    private StreamSourceMetadata meta;

    public StreamSourceSeekableFile(StreamSource source) {
        this.source = source;
    }

    @Override
    public long getFilePointer() throws IOException {
        return position;
    }

    @Override
    public void seek(long pos) throws IOException {
        log.trace("Seek: {}", pos);
        this.position = pos;
    }

    @Override
    public long length() throws IOException {
        return getMeta().getLength();
    }

    public int read() throws IOException {
        try (InputStream stream = source.open(position++)) {
            return stream.read();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        log.trace("Read: {}", len);
        if (position >= length()) return -1;
        if (len > length() - position) {
            // Safe to cast because length-position can only be less than len.
            len = (int) (length() - position);
        }

        try (InputStream stream = source.open(position, len)) {
            int read = StreamUtils.readToBuffer(stream, b, off, len);
            if (read > 0) {
                position += read;
            }
            log.trace("Actually read: {}", read);
            return read;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        try(var os = source.getOutputStream(position)) {
            os.write(b);
            position += b.length;
        }
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    public StreamSourceMetadata getMeta() throws IOException {
        if (meta == null) {
            meta = source.getSourceMetadata();
        }
        return meta;
    }

    public StreamSource getDataSource() {
        return source;
    }

    public long lastModified() throws IOException {
        return getMeta().getLastModified();
    }

    public String getCanonicalPath() throws IOException {
        return this.getMeta().getCanonicalName();
    }
}
