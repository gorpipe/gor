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

package org.gorpipe.gor.driver.providers.stream.datatypes.parquet;

import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.ParquetLine;
import org.gorpipe.model.genome.files.gor.Row;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

public class ParquetRowReader implements Comparable<ParquetRowReader>, Iterator<Row>, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ParquetRowReader.class);
    ParquetReader<Group> reader;
    Row row;

    Function<Group, ParquetLine> lineProvider;

    public ParquetRowReader(ParquetReader<Group> reader, GenomicIterator.ChromoLookup lookup) {
        this(reader, (Group group) -> new ParquetLine(group, lookup));
    }

    public ParquetRowReader(ParquetReader<Group> reader, Function<Group, ParquetLine> lineProvider) {
        this.reader = reader;
        this.lineProvider = lineProvider;
        hasNext();
    }

    @Override
    public boolean hasNext() {
        try {
            Group grp = reader.read();
            if (grp != null) {
                row = lineProvider.apply(grp);
                return true;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Row next() {
        return row;
    }

    @Override
    public int compareTo(ParquetRowReader o) {
        return row.compareTo(o.row);
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            // Dont care
        }
    }
}
