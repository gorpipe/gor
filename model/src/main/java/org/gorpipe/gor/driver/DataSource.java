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

package org.gorpipe.gor.driver;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;

import java.io.IOException;

/**
 * Represents a data source
 * <p>
 * A single instance of a data source should be used by a
 * single thread only and closed after use.
 * <p>
 * Created by villi on 21/08/15.
 */
public interface DataSource extends AutoCloseable {
    /**
     * Name of source - should contain enough information to resolve the source - e.g. a url.
     *
     * @throws IOException
     */
    String getName() throws IOException;

    /**
     * Returns full path for DataSource
     */
    default String getFullPath() throws IOException {
        return getName();
    }

    /**
     * Get type/protocol of source (File, Http, ...)
     */
    SourceType getSourceType();

    /**
     * Get date type - e.g. BAM, GOR .. for files.
     */
    DataType getDataType() throws IOException;

    /**
     * Does this source support writing
     */
    default boolean supportsWriting() {
        return false;
    }

    /**
     * Check for existance of source.
     * Currently, only side effect of always returning true is that
     * automatic fallback to link files wont't work with that source
     */
    boolean exists();

    default void delete() throws IOException {
        throw new GorResourceException("Delete is not implemented", getSourceType().getName());
    }

    default boolean isDirectory() {
        throw new GorResourceException("isDirectory is not implemented", getSourceType().getName());
    }

    /**
     * Creates a new directory.
     * Returns: the directory
     */
    default String createDirectory(FileAttribute<?>... attrs) throws IOException {
        throw new GorResourceException("Create directory is not implemented", getSourceType().getName());
    }

    default Stream<String> list() throws IOException {
        throw new GorResourceException("List directory is not implemented", getSourceType().getName());
    }

    @Override
    void close() throws IOException;

    default boolean supportsLinks() {
        return true;
    }

    /**
     * Get the source meta data.
     *
     * @return the source meta data.
     * @throws IOException
     */
    SourceMetadata getSourceMetadata() throws IOException;

    /**
     * Get the source reference (url and context).
     *
     * @return the source reference
     */
    SourceReference getSourceReference();
}
