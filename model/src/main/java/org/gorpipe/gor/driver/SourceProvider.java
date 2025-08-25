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

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.util.DynamicRowIterator;
import org.gorpipe.gor.model.SourceRef;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;

public interface SourceProvider {

    default void setConfig(GorDriverConfig config) {}
    default void setCache(FileCache cache) {}

    /**
     * Get sources handled by this provider
     */
    SourceType[] getSupportedSourceTypes();

    /**
     * Resolve direct data source from source reference. This does not wrap
     * nor follow links.
     *
     * @param context the context the data source will be created in.
     * @return Data source
     * @throws GorResourceException if the resource is not handled.
     */
    DataSource resolveDataSource(SourceReference context) throws IOException;

    /**
     * Wrap up a datasource. The resulting source should be ready to be used by
     * gor driver clients.
     */
    DataSource wrap(DataSource source) throws IOException;

    /**
     * Create a genomic iterator from a source.
     */
    GenomicIterator createIterator(DataSource source) throws IOException;

    /**
     * Create a meta iterator from a source.
     */
    default GenomicIteratorBase createMetaIterator(DataSource source, FileReader reader) throws IOException {
        return new DynamicRowIterator();
    };

    default Stream<SourceRef> prepareSources(Stream<SourceRef> sources) {
        return sources;
    }
}
