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

package gorsat.analysis;

import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.LineIterator;

public class ArrayLineIterator implements LineIterator {

    int index = 0;
    String[] lines;

    public ArrayLineIterator(String[] lines) {
        this.lines = lines;
    }

    @Override
    public String nextLine() {
        if (index >= lines.length)
            return null;

        String line = lines[index];
        index += 1;
        return line;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean hasNext() {
        return this.lines.length - index > 0;
    }

    @Override
    public Row next() {
        return RowObj.apply(nextLine());
    }
}
