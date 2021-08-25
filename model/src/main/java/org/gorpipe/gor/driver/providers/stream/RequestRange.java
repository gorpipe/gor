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

package org.gorpipe.gor.driver.providers.stream;

/**
 * Created by villi on 23/08/15.
 * <p>
 * Represents a requested range.
 */
public class RequestRange {
    private final long first;
    private final long last;
    private static final RequestRange EMPTY = new RequestRange();

    private RequestRange(long first, long last) {
        if (first < 0) {
            throw new IllegalArgumentException("First position " + first + " is negative");
        }
        if (last < 0) {
            throw new IllegalArgumentException("Last position " + last + " is negative");
        }
        if (first > last) {
            throw new IllegalArgumentException("First byte " + first + " cannot be larger than last byte " + last);
        }
        this.first = first;
        this.last = last;
    }

    /**
     * Used internally to create empty range
     */
    private RequestRange() {
        first = 0;
        last = -1;
    }

    public long getFirst() {
        return first;
    }

    public long getLast() {
        return last;
    }

    public long getLength() {
        return last - first + 1;
    }

    public static RequestRange fromFirstLast(long first, long last) {
        return new RequestRange(first, last);
    }

    public static RequestRange fromFirstLength(long first, long length) {
        return new RequestRange(first, Math.max(first + length - 1, 0));
    }

    public static RequestRange empty() {
        return EMPTY;
    }

    public String toString() {
        return String.format("%d-%d", first, last);
    }

    /**
     * Check if this range covers fully the other range.
     * If true, all bytes in the other range are included in this range.
     */
    public boolean covers(RequestRange range) {
        return first <= range.getFirst() && last >= range.getLast();
    }


    public boolean isEmpty() {
        return last < first;
    }

    /**
     * Ensure that range does not go outside the maximum length
     *
     * @return range object - possibly empty.
     */
    public RequestRange limitTo(long length) {
        if (getFirst() >= length) {
            return EMPTY;
        } else if (getLast() >= length - 1) {
            return fromFirstLast(getFirst(), length - 1);
        }
        return this;
    }
}
