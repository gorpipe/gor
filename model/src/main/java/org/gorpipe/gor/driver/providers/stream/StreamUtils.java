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

package org.gorpipe.gor.driver.providers.stream;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for streams
 * Created by villi on 23/08/15.
 */
public class StreamUtils {
    private static final Logger log = LoggerFactory.getLogger(StreamUtils.class);

    /**
     * Try to close stream, ignoring all exceptions
     */
    public static void tryClose(Closeable str) {
        try {
            str.close();
        } catch (Throwable e) {
            // Ignore
        }
    }

    /**
     * Read data into buffer until length is satisfied or end of stream encounterd
     *
     * @param stream Stream to read from
     * @param buf    Buffer to write into
     * @param offset Offset in bytes in buffer to start writing
     * @param length Number of bytes to be read.
     * @return Length actually read or -1 if no data read and end of stream reached.
     */
    public static int readToBuffer(InputStream stream, byte[] buf, int offset, int length) throws IOException {
        int totalread = 0;
        int read = 0;
        do {
            read = stream.read(buf, offset + totalread, length - totalread);
            if (read > 0) {
                totalread += read;
            }
            if (totalread < length) {
                log.warn("readToBuffer did not read everything - {} left to read", length - totalread);
            }
        } while (read > 0);
        if (totalread > 0) {
            return totalread;
        }
        // Return same value as from underlying stream - 0 or -1
        return read;
    }

    /**
     * Read all data from input stream and write to output
     *
     * @param input
     * @param output
     * @param bufSize
     * @return total bytes read
     */
    public static long readFullyToStream(InputStream input, OutputStream output, int bufSize) throws IOException {
        byte[] buf = new byte[bufSize];
        long totalread = 0;
        int read = 0;
        do {
            read = input.read(buf, 0, bufSize);
            if (read > 0) {
                output.write(buf, 0, read);
                totalread += read;
            }
        } while (read > 0);
        return totalread;
    }

    /**
     * Read specified range from input stream and write to output.
     */
    public static long readRangeToStream(InputStream input, RequestRange range, OutputStream output, int bufSize) throws IOException {
        byte[] buf = new byte[bufSize];
        long totalwritten = 0;
        int read = 0;
        input.skip(range.getFirst());
        do {
            int len = totalwritten + bufSize > range.getLength() ? (int) (range.getLength() - totalwritten) : bufSize;
            read = input.read(buf, 0, len);
            if (read > 0) {
                output.write(buf, 0, read);
                totalwritten += read;
            }
        } while (read > 0);
        return totalwritten;
    }

    /**
     * Read maxLength bytes or until end of stream and return as string.
     */
    public static String readString(InputStream stream, int maxLength) throws IOException {
        byte[] buf = new byte[maxLength];
        int read = readToBuffer(stream, buf, 0, maxLength);
        return new String(buf, 0, read);
    }

    /**
     * Read maxLength bytes or until end of stream from stream source and return as string.
     */
    public static String readString(StreamSource stream, int maxLength) throws IOException {
        try (InputStream is = stream.open()) {
            return readString(is, maxLength);
        }
    }

    /**
     * Try to delete file, ignoring exceptions.
     *
     * @return True if file was deleted, false if not (or uncertain).
     */
    public static boolean tryDelete(File file) {
        try {
            if (file != null && file.exists()) {
                return file.delete();
            }
        } catch (Throwable t) {
            log.warn("Cannot delete file: {}", file, t);
        }
        return false;
    }
}
