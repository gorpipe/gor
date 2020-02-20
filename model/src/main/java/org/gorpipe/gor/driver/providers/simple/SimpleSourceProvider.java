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

import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;

public class SimpleSourceProvider implements SourceProvider {

    @Override
    public DataSource resolveDataSource(SourceReference sourceReference) {
        return new SimpleSource(sourceReference);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{SimpleSourceType.SIMPLE};
    }

    @Override
    public DataSource wrap(DataSource source) {
        return source;
    }

    @Override
    public GenomicIterator createIterator(DataSource source) {
        throw new GorSystemException("SimpleSource does not support gor iterator", null);
    }
}
