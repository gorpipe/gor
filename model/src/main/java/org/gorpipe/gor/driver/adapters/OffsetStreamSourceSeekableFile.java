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

import java.io.IOException;
import java.io.InputStream;

/**
 * SeekableFile implementation using a StreamSource.
 * <p>
 * This is usable in some gor iterators.
 * <p>
 * Created by simmi on 20/12/15.
 */
public class OffsetStreamSourceSeekableFile extends StreamSourceSeekableFile {

    private final long offset;

    public OffsetStreamSourceSeekableFile(StreamSource source, long offset) throws IOException {
        super(source);
        this.offset = offset;
        seek(0);
    }

    @Override
    public long getFilePointer() throws IOException {
        return super.getFilePointer() - offset;
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(offset + pos);
    }

    @Override
    public long length() throws IOException {
        return super.length() - offset;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        long offpos = position - offset;
        if (offpos >= length()) return -1;
        if (len > length() - offpos) {
            // Safe to cast because length-position can only be less than len.
            len = (int) (length() - offpos);
        }
        try (InputStream stream = source.open(position, len)) {
            int read = StreamUtils.readToBuffer(stream, b, off, len);
            if (read > 0) {
                position += read;
            }
            return read;
        }
    }
}
