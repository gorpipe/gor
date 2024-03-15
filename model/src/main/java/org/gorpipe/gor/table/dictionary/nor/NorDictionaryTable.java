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


import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;


/**
 * NorDictionaryTable.
 */
public class NorDictionaryTable extends DictionaryTable<DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(NorDictionaryTable.class);

    public NorDictionaryTable(String path, FileReader fileReader) {
        super(path, fileReader, new NorDictionaryTableMeta(), new NorDictionaryEntryFactory());

        setValidateFiles(true);

        reload();
    }

    public NorDictionaryTable(String path) {
        this(path, null);
    }

    public NorDictionaryTable(Path path) {
        this(path.toUri().toString(), null);
    }

    public NorDictionaryTable(Builder builder) {
        super(builder);
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

        public NorDictionaryTable build() {
            this.header = new NorDictionaryTableMeta();
            if (this.factory == null) {
                this.factory(new NorDictionaryEntryFactory());
            }
            return new NorDictionaryTable(this);
        }
    }
}
