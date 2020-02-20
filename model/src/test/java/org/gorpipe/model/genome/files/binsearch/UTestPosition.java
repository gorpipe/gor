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

package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.model.genome.files.binsearch.Position;
import org.gorpipe.model.genome.files.binsearch.StringIntKey;
import org.junit.Assert;
import org.junit.Test;

public class UTestPosition {

    @Test
    public void testCompare_1() {
        final Position p0 = new Position(new StringIntKey("chr1", 1), 0);
        final Position p1 = new Position(new StringIntKey("chr1", 2), 100);

        Assert.assertTrue(p0.compareTo(p1) < 0);
        Assert.assertTrue(p1.compareTo(p0) > 0);
        Assert.assertEquals(0, p0.compareTo(p0));
        Assert.assertEquals(0, p1.compareTo(p1));
    }

    //We do want the file position compare method to only check fileIdx.
    @Test
    public void testCompare_2() {
        final Position p0 = new Position(new StringIntKey("chr2", 1), 0);
        final Position p1 = new Position(new StringIntKey("chr10", 1), 100);

        Assert.assertTrue(p0.compareTo(p1) < 0);
        Assert.assertTrue(p1.compareTo(p0) > 0);
        Assert.assertEquals(0, p0.compareTo(p0));
        Assert.assertEquals(0, p1.compareTo(p1));
    }

    @Test
    public void testCompareEqualPositions() {
        final Position p0 = new Position(new StringIntKey("", 0), 0);
        final Position p1 = new Position(new StringIntKey("", 0), 0);
        Assert.assertEquals(0, p0.compareTo(p1));
        Assert.assertEquals(0, p1.compareTo(p0));
    }
}
