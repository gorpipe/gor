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

package org.gorpipe.gor.driver.filters;

import org.gorpipe.gor.model.Row;

public class OrFilter extends RowFilter {
    private final RowFilter rf1;
    private final RowFilter rf2;

    public OrFilter(RowFilter rf1, RowFilter rf2) {
        this.rf1 = rf1;
        this.rf2 = rf2;
    }

    @Override
    public boolean test(Row r) {
        return this.rf1.test(r) || this.rf2.test(r);
    }

    @Override
    public String toString() {
        return "(" + this.rf1.toString() + ") or (" + this.rf2.toString() + ")";
    }

    public RowFilter getLeftChild() {
        return this.rf1;
    }

    public RowFilter getRightChild() {
        return this.rf2;
    }
}
