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
package org.gorpipe.gor.table.dictionary.nor;


import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.table.dictionary.DictionaryTableMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NorDictionaryTableMeta extends DictionaryTableMeta {

    private static final Logger log = LoggerFactory.getLogger(NorDictionaryTableMeta.class);

    // Basic properties
    public static final String[] DEFAULT_TABLE_HEADER = new String[] {"File", DEFAULT_SOURCE_COLUMN, "Tags"};

    public NorDictionaryTableMeta() {
        super();
        saveHeaderLine = true;
        setFileHeader(DEFAULT_TABLE_HEADER);
    }

    /**
     * Clear the header info.
     */
    @Override
    public void clear() {
        super.clear();
        setFileHeader(DEFAULT_TABLE_HEADER);
    }

    @Override
    public TableHeader newLineHeader() {
        return new NorDictionaryTableMeta();
    }
}

