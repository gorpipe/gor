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

package org.gorpipe.gor.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SelectIterator extends GenomicIteratorAdapterBase {
    private final int[] cols;
    private String selectHeader;

    public SelectIterator(GenomicIterator git, int[] cols) {
        super(git);

        selectHeader(cols);
        this.cols = cols;
    }

    @Override
    public Row next() {
        return this.iterator.next().rowWithSelectedColumns(this.cols);
    }

    @Override
    public String getHeader() {
        return selectHeader;
    }

    protected void selectHeader(int[] cols) {
        String header = iterator.getHeader();
        if (header != null && !header.equals("")) {
            final String[] headerCols = header.split("\t");
            selectHeader = Arrays.stream(cols).mapToObj(i -> headerCols[i]).collect(Collectors.joining("\t"));
        }
    }
}
