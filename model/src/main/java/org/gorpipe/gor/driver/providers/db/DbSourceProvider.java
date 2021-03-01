/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.gor.driver.providers.db;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.model.GenomicIterator;

import java.io.IOException;

@AutoService(SourceProvider.class)
public class DbSourceProvider implements SourceProvider {

    public DbSourceProvider() {}

    @Override
    public DataSource resolveDataSource(SourceReference sourceReference) {
        return new DbSource(sourceReference);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{DbSourceType.DB};
    }

    @Override
    public DataSource wrap(DataSource source) {
        return source;
    }

    @Override
    public GenomicIterator createIterator(DataSource source) throws IOException {
        return ((DbSource) source).open();

    }


}
