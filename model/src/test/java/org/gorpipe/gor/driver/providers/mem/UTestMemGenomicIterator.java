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

package org.gorpipe.gor.driver.providers.mem;

import org.gorpipe.gor.model.DefaultChromoLookup;
import org.gorpipe.gor.model.Row;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class UTestMemGenomicIterator {
    @Test
    public void header() {
        MemGenomicIterator iterator = new MemGenomicIterator(new DefaultChromoLookup(), 10);
        String header = iterator.getHeader();
        String expected = "Chromo\tPos\tCol3\tCol4\tCol5";
        Assert.assertEquals(expected, header);
    }

    @Test
    public void basicIteration() {
        MemGenomicIterator iterator = new MemGenomicIterator(new DefaultChromoLookup(), 10);
        StringBuilder accumulator = new StringBuilder();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            accumulator.append(row.toString());
            accumulator.append("\n");
        }
        String expected = "chr1\t0\tdata1\t0\tdata0\n" +
                "chr1\t1\tdata1\t1\tdata1\n" +
                "chr1\t2\tdata1\t2\tdata2\n" +
                "chr1\t3\tdata1\t3\tdata3\n" +
                "chr1\t4\tdata1\t4\tdata4\n" +
                "chr1\t5\tdata1\t0\tdata0\n" +
                "chr1\t6\tdata1\t1\tdata1\n" +
                "chr1\t7\tdata1\t2\tdata2\n" +
                "chr1\t8\tdata1\t3\tdata3\n" +
                "chr1\t9\tdata1\t4\tdata4\n";
        Assert.assertEquals(expected, accumulator.toString());
    }

    @Test
    public void seek() {
        MemGenomicIterator iterator = new MemGenomicIterator(new DefaultChromoLookup(), 10);
        iterator.seek("chr1", 5);
        iterator.hasNext();
        Row row = iterator.next();
        String expected = "chr1\t5\tdata1\t0\tdata0";
        Assert.assertEquals(expected, row.toString());
    }
}