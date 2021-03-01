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

package org.gorpipe.gor.driver.providers.mem;

import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.Line;
import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;

/**
 * Simple memory based line generator for testing purposes.
 */
public class MemGenomicIterator extends GenomicIterator {
    int posit = 0;
    int chromo = 1;
    final int lines;
    static final String HEADER = "Chromo\tPos\tCol3\tCol4\tCol5";
    final GenomicIterator.ChromoLookup lookup;

    public MemGenomicIterator(GenomicIterator.ChromoLookup lookup, int lines) {
        this.lookup = lookup;
        this.lines = lines;
    }

    @Override
    public String getHeader() {
        return HEADER;
    }

    @Override
    public boolean seek(String chr, int pos) {
        chromo = lookup.chrToId(chr);
        assert chromo >= 0;
        posit = pos;
        return true;
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public boolean hasNext() {
        return posit < lines;
    }

    @Override
    public Row next() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(lookup.idToName(chromo));
        stringBuilder.append("\t");
        stringBuilder.append(posit);
        stringBuilder.append("\t");
        stringBuilder.append("data1");
        stringBuilder.append("\t");
        stringBuilder.append(posit % 5);
        stringBuilder.append("\t");
        stringBuilder.append("data");
        stringBuilder.append(posit % 5);

        posit++;

        return RowObj.apply(stringBuilder);
    }

    @Override
    public boolean next(Line line) {
        throw new UnsupportedOperationException();
    }
}
