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

public class UTestSegHist {
    @Test
    public void testSegHistRowCount() {
        String[] res = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | group 10000 -count| seghist 100");
        Assert.assertEquals("SEGHIST not producing correct number of lines", 532, res.length);
    }

    @Test
    public void testSegHistColCount() {
        String[] res = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | group chrom -count | seghist 100000 ");
        Assert.assertEquals("SEGHIST not producing correct number of columns", 4, res[1].split("\t").length);
    }

    @Test
    public void testSegHistWholeChr() {
        String[] res = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | group 10000 -count| seghist 100 | top 2");
        Assert.assertEquals("SEGHIST wholechrom not starting on 0", "0", res[1].split("\t")[1]);
    }

    @Test
    public void testSegHistResults() {
        String[] res = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | group 1000 -count| seghist 1000");
        Assert.assertEquals("SEGHIST header malformed", "Chrom\tbpStart\tbpStop\tCount\n", res[0]);
        Assert.assertEquals("SEGHIST bpStart value is unexpected.", 0, Integer.parseInt(res[1].split("\t")[1]));
        Assert.assertEquals("SEGHIST bpStop value is unexpected", 35525001, Integer.parseInt(res[1].split("\t")[2]));
        Assert.assertEquals("SEGHIST Count value is unexpected", 1000, Integer.parseInt(res[1].split("\t")[3].trim()));
    }
}
