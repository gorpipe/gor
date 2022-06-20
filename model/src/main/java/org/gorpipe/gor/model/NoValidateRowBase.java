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

/**
 * Created by sigmar on 27/04/2017.
 */
public class NoValidateRowBase extends QuoteSafeRowBase {
    public NoValidateRowBase(CharSequence input, int numColumns) {
        super(input, numColumns);
    }

    CharSequence colString(int n, CharSequence str, int[] sa) {
        var start = n == 0 ? 0 : sa[n - 1] + 1;
        var stop = sa[n];
        return start < stop ? str.subSequence(start, stop) : "";
    }

    @Override
    public CharSequence colAsString(int n) {
        return n < splitArray.length ? colString(n, allCols, splitArray) : "";
    }
}
