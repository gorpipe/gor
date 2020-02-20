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

package org.gorpipe.model.genome.files.gor;

import java.io.IOException;

/**
 * RacFile allows random access read from a file
 *
 * @version $Id$
 */
public abstract class RacFile {
    /**
     * Read from current file position, as many bytes as possible upto len-pos
     *
     * @param buf    The buffer to write the bytes into
     * @param offset The offset in the buffer to write the first byte into
     * @param len    The number of bytes to read from the file and write into the buffer
     * @return The number of bytes read
     */
    public abstract int read(byte[] buf, int offset, int len);

    /**
     * Read from current file position, as many bytes as possible upto length of byte array
     *
     * @param buf The buffer to write the bytes into, first byte at index 0
     * @return The number of bytes read
     */
    public int read(byte[] buf) {
        return read(buf, 0, buf.length);
    }

    /**
     * Set the current file position
     *
     * @param pos The position to set
     */
    public abstract void seek(long pos);

    /**
     * Close the random access file
     */
    public abstract void close();

    /**
     * Length of the random access file
     */
    public abstract long length() throws IOException;
}
