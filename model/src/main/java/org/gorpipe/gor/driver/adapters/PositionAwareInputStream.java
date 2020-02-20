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

import htsjdk.samtools.util.LocationAware;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Position aware input stream
 */
public class PositionAwareInputStream extends FilterInputStream implements LocationAware {

    private long position;
    private long mark = -1;

    /**
     * Wraps input stream and maintains position
     *
     * @param in the input stream to be wrapped
     */
    public PositionAwareInputStream(InputStream in) {
        super(in);
    }

    /**
     * Returns the number of bytes read.
     */
    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) position++;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = in.read(b, off, len);
        if (result > 0) {
            position += result;
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = in.skip(n);
        position += skipped;
        return skipped;
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
        mark = position;
        // it's okay to mark even if mark isn't supported, as reset won't work
    }

    @Override
    public synchronized void reset() throws IOException {
        if (!in.markSupported()) {
            throw new IOException("Mark not supported");
        }
        if (mark == -1) {
            throw new IOException("No mark set");
        }

        in.reset();
        position = mark;
    }
}
