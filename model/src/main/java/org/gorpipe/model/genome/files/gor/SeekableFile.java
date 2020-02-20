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
 * SeekableFile defines an interfarce for accessing random access files, weather on the local file system or in the cloud
 *
 * @version $Id$
 */
public abstract class SeekableFile implements AutoCloseable {
    /**
     * @return The current position in the file.
     * @throws IOException
     */
    abstract public long getFilePointer() throws IOException;

    /**
     * Seek to the specified position in the file
     *
     * @param pos The position in the file to set as current
     * @throws IOException
     */
    abstract public void seek(long pos) throws IOException;

    /**
     * @return The length of the file in number of bytes
     * @throws IOException
     */
    abstract public long length() throws IOException;

    /**
     * Read the specified number of bytes from the file and write into the specifed byte arrary starting at the specified offset
     *
     * @param b   The buffer into which the data is read.
     * @param off The start offset in array b at which the data is written.
     * @param len The maximum number of bytes read.
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException
     */
    abstract public int read(byte b[], int off, int len) throws IOException;

    /**
     * Read as many bytes from the file as fit into the specified byte array and write into it starting at index 0
     *
     * @param b The byte array buffer
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException
     */
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Close the file, freeing all system resources associated with this file
     *
     * @throws IOException
     */
    abstract public void close() throws IOException;
}
