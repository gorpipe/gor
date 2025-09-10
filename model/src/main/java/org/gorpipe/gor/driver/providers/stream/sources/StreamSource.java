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

package org.gorpipe.gor.driver.providers.stream.sources;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents all sources that can return byte streams.
 * <p>
 * A stream source instance is not thread safe and supports only a single InputStream used at a time.
 * <p>
 * Calling any of the open(..) methods invalidates any previous streams returned from the same instance and
 * calling any methods on earlier streams can result in unpredictable behaviour.
 * <p>
 * Created by villi on 22/08/15
 */
public interface StreamSource extends DataSource {


    /**
     * Open stream that reads through whole source.
     */
    default InputStream openClosable()  {
        return open();
    }

    /**
     * Close the stream source and free any resources associated with it.
     * <p>
     */
    default void forceClose() throws IOException {
        close();
    }

    /**
     * Open stream that reads through whole source.
     */
    InputStream open();

    /**
     * Open stream that reads from the position specified to the end of the stream.
     */
    InputStream open(long start);

    /**
     * Open stream that reads from the start position and provides at least minLength bytes.
     */
    InputStream open(long start, long minLength);

    default OutputStream getOutputStream(long start) {
        return getOutputStream(false);
    }

    /**
     * Create stream to write to the source
     */
    default OutputStream getOutputStream() {
        return getOutputStream(false);
    }

    /**
     * Create stream to write to the source
     */
    default OutputStream getOutputStream(boolean append) {
        throw new GorResourceException("Writing to this stream is not supported",this.getClass().toString());
    }

    /**
     * Get source meta data (length, timestamp) etc.
     */
    StreamSourceMetadata getSourceMetadata();

    /**
     * Copy between two stream sources.
     */
    default String copy(DataSource dest) {
        if (!(dest instanceof StreamSource)) {
            throw new GorResourceException(String.format("Can only copy between stream sources, but between (%s to %s)",
                    getFullPath(), dest.getFullPath()), null);
        }
        String sourcePath = getFullPath();
        if (!sourcePath.equals(dest)) {
            try (InputStream inputStream = open();
                 OutputStream outputStream = ((StreamSource)dest).getOutputStream()) {
                inputStream.transferTo(outputStream);
            } catch (IOException ioe) {
                throw GorResourceException.fromIOException(ioe, getPath());
            }
        }
        return dest.getFullPath();
    }

}
