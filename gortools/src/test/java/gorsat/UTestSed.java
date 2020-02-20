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

public class UTestSed {
    @Test
    public void sed() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data 'test this' | sed test bingo");
        Assert.assertEquals("chrom\tpos\tdata\n", lines[0]);
        Assert.assertEquals("chr1\t1\tbingo this\n", lines[1]);

    }

    @Test
    public void sedNoMatch() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data 'test this' | sed bongo bingo");
        Assert.assertEquals("chrom\tpos\tdata\n", lines[0]);
        Assert.assertEquals("chr1\t1\ttest this\n", lines[1]);

    }

    @Test
    public void sedFirstOnly() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data 'test this and test that' | sed -f test bingo");
        Assert.assertEquals("chrom\tpos\tdata\n", lines[0]);
        Assert.assertEquals("chr1\t1\tbingo this and test that\n", lines[1]);

    }

    @Test
    public void sedSingleColumn() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data1 'test this' | calc data2 'test that' | sed -c data1 test bingo");
        Assert.assertEquals("chrom\tpos\tdata1\tdata2\n", lines[0]);
        Assert.assertEquals("chr1\t1\tbingo this\ttest that\n", lines[1]);

    }
}
