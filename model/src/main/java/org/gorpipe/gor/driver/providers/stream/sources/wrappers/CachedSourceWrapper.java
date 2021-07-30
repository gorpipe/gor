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

package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Can wrap source to intercept all file requests, download if necessary and delegate to the cache.
 * <p>
 * Created by villi on 04/10/15.
 */
public class CachedSourceWrapper extends WrappedStreamSource {
    private StreamSource delegate;
    private final FileCache cache;

    public CachedSourceWrapper(FileCache cache, StreamSource source) {
        super(source);
        this.cache = cache;
    }

    @Override
    public InputStream open() throws IOException {
        return delegate().open();
    }

    @Override
    public InputStream open(long start) throws IOException {
        return delegate().open(start);
    }

    @Override
    public InputStream open(long start, long minLength) throws IOException {
        return delegate().open(start, minLength);
    }

    private StreamSource delegate() throws IOException {
        if (delegate == null) {
            String sourceId = getSourceMetadata().getUniqueId();
            if (getWrapped().getSourceType().isRemote() && sourceId != null) {
                File f = cache.get(sourceId);
                if (f == null) {
                    f = cache.store(sourceId, getWrapped());
                }
                if (f != null) {
                    delegate = new FileSource(new SourceReference(f.getAbsolutePath(), getSourceReference()));
                }
            }

            if (delegate == null) {
                // Use local sources directly
                delegate = getWrapped();
            }
        }
        return delegate;
    }

    @Override
    public SourceType getSourceType() {
        return FileSourceType.FILE;
    }

    @Override
    public String getFullPath() throws IOException {
        return delegate().getFullPath();
    }

    @Override
    public String getName() throws IOException {
        return delegate().getName();
    }
}
