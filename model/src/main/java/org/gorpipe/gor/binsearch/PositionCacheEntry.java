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

package org.gorpipe.gor.binsearch;

import java.util.Objects;

class PositionCacheEntry implements Comparable<PositionCacheEntry> {

    PositionCacheEntry(StringIntKey key, long pos) {
        this.key = key;
        this.chromHash = key.chr.hashCode();
        this.filePosition = pos;
    }

    StringIntKey key;
    int chromHash;
    long filePosition;

    @Override
    public int compareTo(PositionCacheEntry o) {
        return key.compareTo(o.key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionCacheEntry that = (PositionCacheEntry) o;
        return filePosition == that.filePosition &&
                key.equals(that.key);
    }

    @Override
    public String toString() {
        return key.toString() + " -> " + filePosition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, filePosition);
    }
}
