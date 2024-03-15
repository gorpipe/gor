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
package org.gorpipe.gor.table.dictionary;


import org.apache.commons.lang3.StringUtils;
import org.gorpipe.gor.table.TableHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DictionaryTableMeta extends TableHeader  {

    private static final Logger log = LoggerFactory.getLogger(DictionaryTableMeta.class);

    // Basic properties
    public static final String HEADER_SOURCE_COLUMN_KEY = "SOURCE_COLUMN";
    public static final String HEADER_UNIQUE_TAGS_KEY = "UNIQUE_TAGS";
    public static final String HEADER_BUCKETIZE_KEY = "BUCKETIZE";
    public static final String HEADER_LINE_FILTER_KEY = "LINE_FILTER";    // not(-nf)
    public static final String DEFAULT_SOURCE_COLUMN = "Source";
    public static final String[] DEFAULT_SHORT_TABLE_HEADER = new String[] {"File", DEFAULT_SOURCE_COLUMN};
    public static final String[] DEFAULT_TABLE_HEADER = new String[] {"File", DEFAULT_SOURCE_COLUMN, "Tags"};

    public DictionaryTableMeta() {
        super();
        saveHeaderLine = true;
        setFileHeader(DEFAULT_TABLE_HEADER);
    }

    @Override
    public String getProperty(String key) {
        if (HEADER_SOURCE_COLUMN_KEY.equals(key) && !headerProps.containsKey(HEADER_SOURCE_COLUMN_KEY) && isProperTableHeader()) {
            // Special treatment for source column.  If it is missing from standard probs and the header is good
            // we retrieve it from the standard column heading if it is different from the default.
            return DEFAULT_SHORT_TABLE_HEADER[1].equals(getFileHeader()[1]) ? DEFAULT_SOURCE_COLUMN : getFileHeader()[1];
        } else {
            return super.getProperty(key);
        }
    }

    @Override
    protected void parseHeaderLine(String line) {
        String columnsString = StringUtils.strip(line, "\n #");
        if (columnsString.length() > 0) {
            setFileHeader(columnsString.split("[\t,]", -1));
        }
    }

    /**
     * Clear the header info.
     */
    @Override
    public void clear() {
        super.clear();
        setFileHeader(DEFAULT_TABLE_HEADER);
    }
}

