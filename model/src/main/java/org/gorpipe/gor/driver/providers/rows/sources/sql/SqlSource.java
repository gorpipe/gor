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

package org.gorpipe.gor.driver.providers.rows.sources.sql;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.rows.RowIteratorSource;
import org.gorpipe.gor.model.DbConnection;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.StreamWrappedGenomicIterator;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SqlSource extends RowIteratorSource {
    private static final Logger log = LoggerFactory.getLogger(SqlSource.class);

    public SqlSource(SourceReference sourceReference) {
        super(sourceReference);

        // Validate sql statement
        if (!this.sourceReference.getUrl().toLowerCase().contains("select")) {
            throw new GorResourceException("Invalid sql source, expected data on the form sql://[sql statement], but got " + sourceReference.toString(), sourceReference.toString());
        }
    }

    @Override
    public GenomicIterator open() {
        String resolvedUrl = PathUtils.fixDbSchema(sourceReference.getUrl());

        if (!SqlSourceType.SQL.match(resolvedUrl)) {
            throw new GorResourceException("SQLSource: content must start with a valid sql source type", resolvedUrl);
        }

        var sql = resolvedUrl.substring(6);

        var dataStream = DbConnection.getDBLinkStream(sql, new Object[]{}, null);
        return new StreamWrappedGenomicIterator(dataStream, getHeaderfromQuery(resolvedUrl), true, true);
    }

    @Override
    public GenomicIterator open(String filter) {
        return null;
    }

    @Override
    public boolean supportsFiltering() {
        return false;
    }

    @Override
    public String getName() {
        return sourceReference.getUrl();
    }

    @Override
    public SourceType getSourceType() {
        return SqlSourceType.SQL;
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
        // No action needed
    }

    @Override
    public boolean supportsLinks() {
        return false;
    }

    @Override
    public SourceMetadata getSourceMetadata() {
        long timestamp = System.currentTimeMillis();
        /*if (tableName != null) {
            final DbConnection dbsource = DbConnection.lookup(databaseSource);
            if (dbsource != null) {
                timestamp = dbsource.queryDefaultTableChange(tableName);
            }
        }*/
        return new SourceMetadata(this, sourceReference.getUrl(), timestamp, null, false);
    }

    @Override
    public SourceReference getSourceReference() {
        return sourceReference;
    }

    private String getHeaderfromQuery(String url) {
        final int idxSelect = url.indexOf("select ");
        final int idxFrom = url.indexOf(" from ");
        if (idxSelect < 0 || idxFrom < 0) { // Must find columns
            return null;
        }
        final ArrayList<String> fields = StringUtil.split(url, idxSelect + 7, idxFrom, ',');
        final StringBuilder header = new StringBuilder(200);
        for (String f : fields) {
            if (header.length() > 0) {
                header.append('\t');
            } else {
                header.append('#');
            }
            final int idxAs = f.indexOf(" as ");
            if (idxAs > 0) {
                header.append(f.substring(idxAs + 4).trim());
            } else {
                final int idxPoint = f.indexOf('.');
                header.append(f.substring(idxPoint > 0 ? idxPoint + 1 : 0).trim());
            }
        }
        return header.toString();
    }
}
