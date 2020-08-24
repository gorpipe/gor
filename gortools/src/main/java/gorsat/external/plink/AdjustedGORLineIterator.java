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

package gorsat.external.plink;

import org.gorpipe.gor.model.Row;

import java.util.Iterator;
import java.util.stream.Stream;

public class AdjustedGORLineIterator implements Comparable<AdjustedGORLineIterator> {
    Stream<Row> str;
    Iterator<Row> it;
    Row currentRow;

    public AdjustedGORLineIterator(Stream<Row> str) {
        super();
        this.str = str;
        this.it = str.iterator();
        if( it.hasNext() ) currentRow = it.next();
    }

    public Row getRow() {
        return currentRow;
    }

    public void next() {
        if( it.hasNext() ) {
            currentRow = it.next();
        } else {
            currentRow = null;
            close();
        }
    }

    public void close() {
        str.close();
    }

    @Override
    public int compareTo(AdjustedGORLineIterator o) {
        return currentRow.compareTo(o.currentRow);
    }
}
