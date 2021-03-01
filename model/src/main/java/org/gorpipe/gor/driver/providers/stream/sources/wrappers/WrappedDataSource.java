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

package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;

import java.io.IOException;

/**
 * Created by villi on 29/08/15.
 */
public class WrappedDataSource implements DataSource {
    protected DataSource wrapped;

    public WrappedDataSource(DataSource wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String getName() throws IOException {
        return wrapped.getName();
    }

    @Override
    public SourceType getSourceType() {
        return wrapped.getSourceType();
    }

    @Override
    public DataType getDataType() throws IOException {
        return wrapped.getDataType();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public boolean exists() throws IOException {
        return wrapped.exists();
    }

    public DataSource getWrapped() {
        return wrapped;
    }

    @Override
    public SourceMetadata getSourceMetadata() throws IOException {
        return wrapped.getSourceMetadata();
    }

    @Override
    public SourceReference getSourceReference() {
        return wrapped.getSourceReference();
    }
}
