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

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;

import java.io.IOException;
import java.nio.file.attribute.FileAttribute;
import java.util.stream.Stream;

/**
 * Created by villi on 29/08/15.
 */
public class WrappedDataSource implements DataSource {
    protected DataSource wrapped;

    public WrappedDataSource(DataSource wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public String getFullPath() {
        return wrapped.getFullPath();
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
    public boolean exists() {
        return wrapped.exists();
    }

    @Override
    public String move(DataSource dest) throws IOException {
        return wrapped.move(dest);
    }

    @Override
    public String copy(DataSource dest) throws IOException {
        return wrapped.copy(dest);
    }

    @Override
    public boolean supportsWriting() {
        return wrapped.supportsWriting();
    }

    @Override
    public void delete() throws IOException {
        wrapped.delete();
    }

    @Override
    public boolean isDirectory() {
        return wrapped.isDirectory();
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) throws IOException {
        return wrapped.createDirectory(attrs);
    }

    @Override
    public String createDirectoryIfNotExists(FileAttribute<?>... attrs) throws IOException {
        return wrapped.createDirectoryIfNotExists(attrs);
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) throws IOException {
        return wrapped.createDirectories(attrs);
    }

    @Override
    public Stream<String> list() throws IOException {
        return wrapped.list();
    }

    @Override
    public boolean supportsLinks() {
        return wrapped.supportsLinks();
    }

    @Override
    public boolean forceLink() {
        return wrapped.forceLink();
    }

    @Override
    public String getProjectLinkFileContent() {
        return wrapped.getProjectLinkFileContent();
    }

    @Override
    public String getProjectLinkFile() {
        return wrapped.getProjectLinkFile();
    }

    @Override
    public String[] getAccessValidationPaths() {
        return wrapped.getAccessValidationPaths();
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
