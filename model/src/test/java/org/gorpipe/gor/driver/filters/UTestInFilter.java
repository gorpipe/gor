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

import java.util.Collection;
import java.util.HashSet;

public class UTestInFilter {

    @Test
    public void test_basic() {
        final RowFilter rf = new InFilter(2, new HashSet<>());
        Assert.assertFalse(rf.test(new RowBase("chr1\t1\thjalti")));
        Assert.assertTrue(((InFilter) rf).getLegalValues().isEmpty());

        final Collection<String> tags = new HashSet<>();
        tags.add("hjalti");
        final RowFilter rf2 = new InFilter(2, tags);
        Assert.assertTrue(rf2.test(new RowBase("chr1\t1\thjalti")));
        Assert.assertEquals(1, ((InFilter) rf2).getLegalValues().size());
    }

    @Test
    public void test_acceptProgress() {
        final RowFilter rf = new InFilter(10, new HashSet<>());
        Assert.assertTrue(rf.test(RowBase.getProgressRow("chr1", 1)));
    }
}
