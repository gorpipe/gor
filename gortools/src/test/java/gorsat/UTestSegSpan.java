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

package gorsat;

import org.junit.Assert;
import org.junit.Test;

public class UTestSegSpan {
    @Test
    public void segspan() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | top 1 | segspan");
        Assert.assertEquals(2, lines.length);
        Assert.assertEquals("Chrom\tbpStart\tbpStop\tsegCount\n", lines[0]);
        Assert.assertEquals("chr1\t11868\t14412\t1\n", lines[1]);
    }

    @Test
    public void segspanMaxSeg() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | top 1 | segspan -maxseg 1000");
        Assert.assertEquals(5, lines.length);
        Assert.assertEquals("Chrom\tbpStart\tbpStop\tsegCount\n", lines[0]);
        Assert.assertEquals("chr1\t11868\t12504\t1\n", lines[1]);
        Assert.assertEquals("chr1\t12504\t13140\t1\n", lines[2]);
        Assert.assertEquals("chr1\t13140\t13776\t1\n", lines[3]);
        Assert.assertEquals("chr1\t13776\t14412\t1\n", lines[4]);
    }
}
