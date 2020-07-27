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

package org.gorpipe.gor.driver.providers.simple;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a simple (no function) data source
 * <p>
 * Created by gisli on 26/02/16.
 */
public class SimpleSource implements DataSource {
    private static final Logger log = LoggerFactory.getLogger(SimpleSource.class);

    private final SourceReference sourceReference;

    public SimpleSource(SourceReference sourceReference) {
        this.sourceReference = sourceReference;
    }

    @Override
    public String getName() {
        return sourceReference.getUrl();
    }

    @Override
    public SourceType getSourceType() {
        return SimpleSourceType.SIMPLE;
    }

    @Override
    public DataType getDataType() {
        return DataType.fromFileName(sourceReference.getUrl());
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public void close() {
        // TODO:
        // No action needed
    }

    @Override
    public SourceMetadata getSourceMetadata() {
        return new SourceMetadata(this, sourceReference.getUrl(), System.currentTimeMillis(), null, false);
    }

    @Override
    public SourceReference getSourceReference() {
        return sourceReference;
    }
}
