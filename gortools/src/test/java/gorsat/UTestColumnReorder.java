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

import java.io.File;

public class UTestColumnReorder {

    @Test
    public void singleColumnReorder() {
        String query = "norrows 10 | calc x rownum*2 | calc y rownum*3 | columnreorder y";
        var res = TestUtils.runGorPipeLines(query);

        Assert.assertEquals("ChromNOR\tPosNOR\ty\tRowNum\tx\n", res[0]);
        Assert.assertEquals("chrN\t0\t0\t0\t0\n", res[1]);
        Assert.assertEquals("chrN\t0\t3\t1\t2\n", res[2]);
        Assert.assertEquals("chrN\t0\t27\t9\t18\n", res[10]);
    }

    @Test
    public void singleColumnReorderIgnoreNonExisting() {
        String query = "norrows 10 | calc x rownum*2 | calc y rownum*3 | columnreorder y,foo -t";
        var res = TestUtils.runGorPipeLines(query);

        Assert.assertEquals("ChromNOR\tPosNOR\ty\tRowNum\tx\n", res[0]);
        Assert.assertEquals("chrN\t0\t0\t0\t0\n", res[1]);
        Assert.assertEquals("chrN\t0\t3\t1\t2\n", res[2]);
        Assert.assertEquals("chrN\t0\t27\t9\t18\n", res[10]);
    }

    @Test
    public void complexColumnReorder() {
        String query = "gor ../tests/data/external/samtools/test.vcf | columnreorder 1,2,format,info,#3-#5 | top 1";
        var res = TestUtils.runGorPipeLines(query);
        Assert.assertEquals("CHROM\tPOS\tFORMAT\tINFO\tID\tREF\tALT\tQUAL\tFILTER\tNA00001\tNA00002\tNA00003\n", res[0]);
        Assert.assertEquals("chr20\t14370\tGT:GQ:DP:HQ\tNS=3;DP=14;AF=0.5;DB;H2\trs6054257\tG\tA\t29\tPASS\t0|0:48:1:51,51\t1|0:48:8:51,51\t1/1:43:5:.,.\n", res[1]);
    }
}
