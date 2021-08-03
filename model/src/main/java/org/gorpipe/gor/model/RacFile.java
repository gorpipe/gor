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

package org.gorpipe.gor.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * RacFile allows random access read from a file
 *
 * @version $Id$
 */
public abstract class RacFile implements Closeable {
    /**
     * Set the current file position
     *
     * @param pos The position to set
     */
    public abstract void seek(long pos);

    /**
     * Reads from current position
     * @return
     * @throws IOException
     */
    public abstract int read() throws IOException;

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads from current position
     */
    public abstract int read(byte[] b, int start, int length) throws IOException;

    /**
     * Writes to the current postition
     */
    public abstract void write(byte[] b) throws IOException;

    /**
     * Length of the random access file
     */
    public abstract long length() throws IOException;
}
