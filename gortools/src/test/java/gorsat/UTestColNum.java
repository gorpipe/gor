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

package gorsat;

import org.junit.Assert;
import org.junit.Test;

public class UTestColNum {
    @Test
    public void colnumChromPosOnly() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | colnum");
        Assert.assertEquals("chrom\t2(pos)\n", lines[0]);
        Assert.assertEquals("chr1\t1\n", lines[1]);
    }

    @Test
    public void colnumChromPosOneOtherColumn() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data 42 | colnum");
        Assert.assertEquals("chrom\t2(pos)\t3(data)\n", lines[0]);
        Assert.assertEquals("chr1\t1\t3(42)\n", lines[1]);
    }

    @Test
    public void colnumChromPosMoreColumns() {
        String[] lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data1 42 | calc data2 3.14 | calc data3 'this is a test' | colnum");
        Assert.assertEquals("chrom\t2(pos)\t3(data1)\t4(data2)\t5(data3)\n", lines[0]);
        Assert.assertEquals("chr1\t1\t3(42)\t4(3.14)\t5(this is a test)\n", lines[1]);
    }
}
