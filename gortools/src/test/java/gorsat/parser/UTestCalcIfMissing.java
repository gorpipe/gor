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

public class UTestCalcIfMissing {
    @Test
    public void addsColumnWhenMissing() {
        final String result = TestUtils.runGorPipe("gorrow 1,1 | calcifmissing data 42");
        final String expected = "chrom\tpos\tdata\n" +
                "chr1\t1\t42\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void addsMultipleColumnsWhenMissing() {
        final String result = TestUtils.runGorPipe("gorrow 1,1 | calcifmissing data1,data2,data3 42,3.14,'bingo'");
        final String expected = "chrom\tpos\tdata1\tdata2\tdata3\n" +
                "chr1\t1\t42\t3.14\tbingo\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void ignoresColumnWhenItExists() {
        final String result = TestUtils.runGorPipe("gorrow 1,1 | calc data 42 | calcifmissing data 3.14");
        final String expected = "chrom\tpos\tdata\n" +
                "chr1\t1\t42\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void addsOnlyMissingColumns() {
        final String result = TestUtils.runGorPipe("gorrow 1,1 | calc data1 64 | calcifmissing data1,data2,data3 42,3.14,'bingo'");
        final String expected = "chrom\tpos\tdata1\tdata2\tdata3\n" +
                "chr1\t1\t64\t3.14\tbingo\n";
        Assert.assertEquals(expected, result);
    }
}
