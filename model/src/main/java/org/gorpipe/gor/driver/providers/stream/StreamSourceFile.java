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

package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.SourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StreamSourceFile implements SourceFile {

    private final StreamSource fileSource;
    private StreamSource indexSource;
    private StreamSource referenceSource;

    public StreamSourceFile(StreamSource fileSource) {
        this.fileSource = fileSource;
    }

    public StreamSourceFile(StreamSource fileSource, StreamSource indexSource) {
        this.fileSource = fileSource;
        this.indexSource = indexSource;
    }

    public StreamSourceFile(StreamSource fileSource, StreamSource indexSource, StreamSource referenceSource) {
        this.fileSource = fileSource;
        this.indexSource = indexSource;
        this.referenceSource = referenceSource;
    }

    @Override
    public String getName() throws IOException { return fileSource.getName(); }

    @Override
    public StreamSource getFileSource() {
        return fileSource;
    }

    @Override
    public StreamSource getIndexSource() {
        return indexSource;
    }

    @Override
    public StreamSource getReferenceSource() { return referenceSource; }

    @Override
    public void setIndexSource(DataSource index) {
        this.indexSource = (StreamSource) index;
    }

    @Override
    public void setReferenceSource(DataSource reference) { this.referenceSource = (StreamSource) reference; }

    @Override
    public List<String> possibleIndexNames() throws IOException {
        List<String> result = new ArrayList<>();
        String indexUrl = getFileSource().getSourceMetadata().getIndexFileUrl();
        if (indexUrl != null) {
            result.add(indexUrl);
        }
        return result;
    }

    @Override
    public boolean supportsIndex() {
        return false;
    }

    @Override
    public boolean supportsReference() {
        return false;
    }

    @Override
    public String getReferenceFileName() { return null; }

    @Override
    public DataType getType() throws IOException {
        return fileSource.getDataType();
    }


}
