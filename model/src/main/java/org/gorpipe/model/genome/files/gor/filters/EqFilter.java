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

package org.gorpipe.model.genome.files.gor.filters;

import org.gorpipe.model.genome.files.gor.Row;

public class EqFilter extends RowFilter {
    private final String wanted;
    private final int colIdx;

    public EqFilter(int colIdx, String wanted) {
        this.wanted = wanted;
        this.colIdx = colIdx;
    }

    @Override
    public boolean test(Row r) {
        return this.wanted.equals(r.colAsString(this.colIdx).toString());
    }

    @Override
    public String toString() {
        return "eq(" + this.wanted + ")";
    }

    public String getWantedValue() {
        return this.wanted;
    }

    public int getColIdx() {
        return this.colIdx;
    }
}
