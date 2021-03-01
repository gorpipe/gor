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

public class UTestGrep {
    @Test
    public void grep() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | grep brca");
        Assert.assertEquals(3, lines.length);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\n", lines[0]);
        Assert.assertEquals("chr13\t32889610\t32973805\tBRCA2\n", lines[1]);
        Assert.assertEquals("chr17\t41196311\t41322290\tBRCA1\n", lines[2]);
    }

    @Test
    public void grepColumn() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | grep -c Gene_Symbol brca");
        Assert.assertEquals(3, lines.length);
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol\n", lines[0]);
        Assert.assertEquals("chr13\t32889610\t32973805\tBRCA2\n", lines[1]);
        Assert.assertEquals("chr17\t41196311\t41322290\tBRCA1\n", lines[2]);
    }

    @Test
    public void grepInverted() {
        final String result = TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | grep ABHD14A | grep -v ACY1");
        String expected = "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chr3\t52005441\t52015212\tABHD14A\n";
        Assert.assertEquals(expected, result);
    }
}
