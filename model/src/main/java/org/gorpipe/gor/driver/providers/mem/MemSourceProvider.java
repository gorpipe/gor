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

package org.gorpipe.gor.driver.providers.mem;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.model.GenomicIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@AutoService(SourceProvider.class)
public class MemSourceProvider implements SourceProvider {

    private static final Logger log = LoggerFactory.getLogger(MemSourceProvider.class);


    public MemSourceProvider() {}

    public MemSourceProvider(GorDriverConfig config, FileCache cache,
                             Set<StreamSourceIteratorFactory> initialFactories) {
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{MemSourceType.MEM};
    }

    @Override
    public MemSource resolveDataSource(SourceReference sourceReference) {
        if (!sourceReference.getUrl().toLowerCase().endsWith(".mem")) {
            log.debug("Unhandled protocol reference: {}", sourceReference);
            return null;
        }
        return new MemSource(sourceReference);
    }

    @Override
    public DataSource wrap(DataSource source) {
        return source;
    }

    @Override
    public GenomicIterator createIterator(DataSource source) {
        return new MemGenomicIterator(source.getSourceReference().getLookup(), 4000, source.getSourceReference().getColumns());
    }
}
