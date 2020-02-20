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

import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * GorServer implementation of RacFile
 */
public class GCRacFile extends RacFile {

    private static final Logger log = LoggerFactory.getLogger(GorFileReaderContext.class);
    final RandomAccessFile file;
    final String filename;
    boolean open = false;

    /**
     * Construct GCRacFile that can read the specified file
     *
     * @param filename The file
     * @throws FileNotFoundException
     */
    public GCRacFile(String filename) throws FileNotFoundException {
        this.filename = filename;
        this.file = new RandomAccessFile(filename, "r");
        this.open = true;
    }

    @Override
    public int read(byte[] buf, int offset, int len) {
        try {
            return file.read(buf, offset, len);
        } catch (IOException e) {
            log.warn("GCRacFile read from {} failed", filename);
            throw new GorSystemException("Error during file read: " + filename, e);
        }
    }

    /**
     * Set the current file position
     *
     * @param pos The position to set
     */
    @Override
    public void seek(long pos) {
        try {
            file.seek(pos);
        } catch (IOException ex) {
            log.warn("GCRacFile seek in {} failed", filename);
            throw new GorSystemException("Cant seek in file " + filename, ex);
        }
    }


    @Override
    public void close() {
        try {
            file.close();
            this.open = false;
        } catch (Exception ex) {
            log.warn("GCRacFile close on {} failed", filename);
            throw new GorSystemException("Could not close file " + filename, ex);
        }
    }

    @Override
    public long length() throws IOException {
        return file.length();
    }

    @Override
    @SuppressWarnings("squid:ObjectFinalizeOverridenCheck")
    protected void finalize() throws Throwable {
        if (open) {
            String msg = "GCRacFile(" + filename + ") not closed on finalize - auto closing";
            log.warn(msg);
            try {
                close();
            } catch (Exception e) {
                // Ignore here
            }
        }
        super.finalize();
    }
}
