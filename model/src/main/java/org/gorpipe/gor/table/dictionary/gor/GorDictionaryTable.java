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

package org.gorpipe.gor.table.dictionary.gor;


import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;


/**
 * DictionaryTable.
 */
public class GorDictionaryTable extends DictionaryTable<GorDictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(GorDictionaryTable.class);

    public GorDictionaryTable(String path, FileReader fileReader) {
        super(path, fileReader, new GorDictionaryTableMeta(), new GorDictionaryEntryFactory());
    }

    public GorDictionaryTable(String path) {
        this(path, null);
    }

    public GorDictionaryTable(Path path) {
        this(path.toUri().toString(), null);
    }
    
    public GorDictionaryTable(Builder builder) {
        super(builder);
    }

    @Override
    public GorDictionaryFilter<GorDictionaryEntry> filter() {
        return new GorDictionaryFilter<>(this, tableEntries);
    }

    public static class Builder<B extends Builder<B>> extends DictionaryTable.Builder<B> {
        public Builder(String path) {
            super(path);
        }

        public Builder(Path path) {
            this(path.toString());
        }

        public Builder(URI path) {
            this(path.toString());
        }

        public GorDictionaryTable build() {
            this.header = new GorDictionaryTableMeta();
            if (this.factory == null) {
                this.factory(new GorDictionaryEntryFactory());
            }
            return new GorDictionaryTable(this);
        }
    }
}
