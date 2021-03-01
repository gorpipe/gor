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

package org.gorpipe.gor.driver.filters;

import org.gorpipe.gor.model.RowBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class UTestEqFilter {

    @Test
    public void test_basic() {
        final EqFilter rf = new EqFilter(2, "simmi");
        Assert.assertEquals(2, rf.getColIdx());
        Assert.assertEquals("simmi", rf.getWantedValue());
        Assert.assertFalse(rf.test(new RowBase("chr1\t1\thjalti")));

        final EqFilter rf2 = new EqFilter(2, "hjalti");
        Assert.assertEquals(2, rf2.getColIdx());
        Assert.assertEquals("hjalti", rf2.getWantedValue());
        Assert.assertTrue(rf2.test(new RowBase("chr1\t1\thjalti")));
    }

    @Test
    public void test_acceptProgress() {
        final RowFilter rf = new EqFilter(10, "hjalti");
        Assert.assertTrue(rf.test(RowBase.getProgressRow("chr1", 1)));
    }
}
