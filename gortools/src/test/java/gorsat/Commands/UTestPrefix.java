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

package gorsat.Commands;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class UTestPrefix {

    @Test
    public void simpleGor() {
        String query = "gorrow 1,1 | calc x 42 | prefix x abc | top 0";
        String result = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\tabc_x\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void nameClash() {
        String query = "gorrow 1,1 | calc x 42 | calc abc_x 'bingo' | prefix 2- abc | top 0";
        String result = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\tabc_x\tabc_abc_x\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void manyColumns() {
        final int n = 100_000;
        String query = "norrows " + n + " | calc x round(random()*1000) | pivot -vf <(norrows " + n + ") rownum | prefix 1- PN | top 0";
        String result = TestUtils.runGorPipe(query);
        String[] columns = result.split("\t");
        Assert.assertEquals(n + 2, columns.length);
    }

    @Test
    public void simpleNor() {
        String query = "norrows 1 | calc x 42 | prefix x abc | top 0";
        String result = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tRowNum\tabc_x\n";
        Assert.assertEquals(expected, result);
    }
}