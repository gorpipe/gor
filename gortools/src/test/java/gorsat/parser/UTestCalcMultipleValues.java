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

package gorsat.parser;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class UTestCalcMultipleValues {
    @Test
    public void testMultipleDouble() {
        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x,y 3.14, 42.0");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\t3.14\t42.0\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x,y,z 3.14, 42.0, 3.17");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tz\nchr1\t1\t1\t3.14\t42.0\t3.17\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc t,u,v,w,x,y,z 1,2,3,4, 3.14, 42.0, 3.17");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tt\tu\tv\tw\tx\ty\tz\n" +
                "chr1\t1\t1\t1\t2\t3\t4\t3.14\t42.0\t3.17\n", result);
    }

    @Test
    public void testMultipleInt() {
        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x,y 3, 42");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\t3\t42\n", result);
    }
}
