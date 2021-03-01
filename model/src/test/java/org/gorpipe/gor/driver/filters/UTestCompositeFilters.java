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
import org.gorpipe.gor.model.RowBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;

public class UTestCompositeFilters {

    @Test
    public void test_and() {
        final Collection<String> tags1 = new HashSet<>();
        tags1.add("hjalti");
        tags1.add("simmi");
        final Collection<String> tags2 = new HashSet<>();
        tags2.add("hjalti");
        tags2.add("gisli");

        final RowFilter rf1 = new InFilter(2, tags1);
        final RowFilter rf2 = new InFilter(2, tags2);

        final AndFilter rf = (AndFilter) rf1.and(rf2);
        final AndFilter fr = (AndFilter) rf2.and(rf1);

        Assert.assertSame(rf1, rf.getLeftChild());
        Assert.assertSame(rf2, rf.getRightChild());

        Assert.assertSame(rf2, fr.getLeftChild());
        Assert.assertSame(rf1, fr.getRightChild());

        final Row r1 = new RowBase("chr1\t1\thjalti");
        final Row r2 = new RowBase("chr1\t1\tsimmi");
        final Row r3 = new RowBase("chr1\t1\tgisli");

        Assert.assertTrue(rf.test(r1));
        Assert.assertTrue(fr.test(r1));

        Assert.assertFalse(rf.test(r2));
        Assert.assertFalse(fr.test(r2));

        Assert.assertFalse(rf.test(r3));
        Assert.assertFalse(fr.test(r3));
    }

    @Test
    public void test_or() {
        final Collection<String> tags1 = new HashSet<>();
        tags1.add("hjalti");
        tags1.add("simmi");
        final Collection<String> tags2 = new HashSet<>();
        tags2.add("hjalti");
        tags2.add("gisli");

        final RowFilter rf1 = new InFilter(2, tags1);
        final RowFilter rf2 = new InFilter(2, tags2);

        final OrFilter rf = (OrFilter) rf1.or(rf2);
        final OrFilter fr = (OrFilter) rf2.or(rf1);

        final Row r1 = new RowBase("chr1\t1\thjalti");
        final Row r2 = new RowBase("chr1\t1\tsimmi");
        final Row r3 = new RowBase("chr1\t1\tgisli");

        Assert.assertSame(rf1, rf.getLeftChild());
        Assert.assertSame(rf2, rf.getRightChild());

        Assert.assertSame(rf2, fr.getLeftChild());
        Assert.assertSame(rf1, fr.getRightChild());

        Assert.assertTrue(rf.test(r1));
        Assert.assertTrue(fr.test(r1));

        Assert.assertTrue(rf.test(r2));
        Assert.assertTrue(fr.test(r2));

        Assert.assertTrue(rf.test(r3));
        Assert.assertTrue(fr.test(r3));
    }


    @Test
    public void test_not() {
        final Collection<String> tags1 = new HashSet<>();
        tags1.add("hjalti");

        final RowFilter child = new InFilter(2, tags1);
        final NotFilter rf = (NotFilter) child.not();

        Assert.assertSame(child, rf.getChild());

        final Row r1 = new RowBase("chr1\t1\thjalti");
        final Row r2 = new RowBase("chr1\t1\tsimmi");

        Assert.assertFalse(rf.test(r1));
        Assert.assertTrue(rf.test(r2));
    }
}
