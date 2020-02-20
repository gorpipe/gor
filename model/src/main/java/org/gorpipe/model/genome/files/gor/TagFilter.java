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

package org.gorpipe.model.genome.files.gor;

import java.util.Set;

public class TagFilter {
    private final Set<String> tags;
    private final int column;

    public TagFilter(Set<String> tags, int col) {
        this.tags = tags;
        this.column = col;
    }

    public boolean isIncluded(Row r) {
        return tags.contains(r.stringValue(column));
    }

    boolean isIncluded(String pn) {
        return tags.contains(pn);
    }

    @Override
    public String toString() {
        return "in(" + String.join(", ", tags) + ")";
    }
}
