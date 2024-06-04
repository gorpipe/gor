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

package org.gorpipe.googlecloudstorage.driver;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;

import java.util.Set;

@AutoService(SourceProvider.class)
public class GoogleCloudStorageSourceProvider extends StreamSourceProvider {
    public GoogleCloudStorageSourceProvider() {}

    public GoogleCloudStorageSourceProvider(GorDriverConfig config, FileCache cache, Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{GoogleCloudStorageSourceType.GOOGLE_CLOUD_STORAGE_SOURCE_TYPE};
    }

    @Override
    public GoogleCloudStorageBlobSource resolveDataSource(SourceReference sourceReference) {
        return new GoogleCloudStorageBlobSource(sourceReference);
    }

    @Override
    protected RetryHandlerBase getRetryHandler() {
        if (retryHandler == null) {
            retryHandler = new GoogleCloudStorageRetryHandler(100, 10000);
        }
        return retryHandler;
    }

}
