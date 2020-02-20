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

public class UTestUnPivot {
    @Test
    public void unpivot() {
        String query = "gorrow 1,1 | calc data1 42 | calc data2 3.14 | calc data3 'test this' | unpivot data1,data2,data3";
        String[] lines = TestUtils.runGorPipeLines(query);
        Assert.assertEquals(4, lines.length);
        Assert.assertEquals("chrom\tpos\tCol_Name\tCol_Value\n", lines[0]);
        Assert.assertEquals("chr1\t1\tdata1\t42\n", lines[1]);
        Assert.assertEquals("chr1\t1\tdata2\t3.14\n", lines[2]);
        Assert.assertEquals("chr1\t1\tdata3\ttest this\n", lines[3]);
    }
}
