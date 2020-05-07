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

import java.util.Collection;
import java.util.Set;

public class TagFilter {
    private final Set<String> includeTags;
    private final Collection<String> excludeTags;
    private final int tagColumnIndex;

    public TagFilter(Set<String> includeTags, int col) {
        this(includeTags, null, col);
    }

    public TagFilter(Set<String> includeTags, Collection<String> excludeTags, int tagColumnIndex) {
        this.includeTags = includeTags;
        this.excludeTags = excludeTags;
        this.tagColumnIndex = tagColumnIndex;
    }

    public boolean isIncluded(Row r) {
        String tag =  r.stringValue(tagColumnIndex);
        return isIncluded(tag);
    }

    boolean isIncluded(String tag) {
        return includeTags.contains(tag) && (excludeTags == null || !excludeTags.contains(tag));
    }

    @Override
    public String toString() {
        return "in(" + String.join(", ", includeTags) + (excludeTags!=null?") and not in(" + String.join(", ", excludeTags)+")":")");
    }
}
