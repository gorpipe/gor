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
import org.junit.Ignore;
import org.junit.Test;

public class UTestPgor {


    @Test
    public void testPgorSplitEquivalence() {
        String testFile = "../tests/data/gor/genes.gor";
        String expected = TestUtils.runGorPipe("gor " + testFile + " | signature -timeres 1 | group chrom -count");

        String res = TestUtils.runGorPipe("create counts = pgor -split 50 " + testFile + " | signature -timeres 1 | group chrom -count; gor [counts] | group chrom -count -sum -ic allCount | hide allCount | rename sum_allCount allCount");
        Assert.assertEquals("pgor split does not provide correct results", expected, res);

        String res2 = TestUtils.runGorPipe("create counts = pgor -split 200 " + testFile + " | signature -timeres 1 | group chrom -count; gor [counts] | group chrom -count -sum -ic allCount | hide allCount | rename sum_allCount allCount");
        Assert.assertEquals("pgor split does not provide correct results", expected, res2);
    }


    @Test
    public void testPgorSplitChr22() {
        String exp = "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chr22\t16062156\t16063236\tLA16c-4G1.3\n" +
                "chr22\t16076051\t16076172\tLA16c-4G1.4\n";
        String res = TestUtils.runGorPipe("pgor -splitzero <(gor -p chr22 ../tests/data/gor/genes.gor | top 2) ../tests/data/gor/genes.gor | signature -timeres 1");
        Assert.assertEquals("pgor nested split does not provide correct results", exp, res);

        res = TestUtils.runGorPipe("create split = gor -p chr22 ../tests/data/gor/genes.gor | top 2;" +
                "pgor -splitzero <(gor [split]) ../tests/data/gor/genes.gor | signature -timeres 1");
        Assert.assertEquals("pgor nested split does not provide correct results", exp, res);
    }

    @Ignore("pgor should not allow range")
    @Test
    public void testPgorWithRange() {
        String[] lines = TestUtils.runGorPipeLines("pgor -p chr22 ../tests/data/gor/genes.gor | signature -timeres 1");
        Assert.assertEquals(1128, lines.length);
    }

    @Ignore("Not sure if this test is correct")
    @Test
    public void testPgorWithNoWithin() {
        String[] lines = TestUtils.runGorPipeLines("pgor -nowithin <(gorrow chrN,1,2)");
        Assert.assertEquals(27, lines.length);
    }


    @Test
    public void testPgorWithLateExpansion() {
        String query = "create splits = gor ../tests/data/gor/genes.gor | select 1-3 | top 10 | signature -timeres 1;" +
                "create xxx = pgor -splitzero <(gor [splits]) ../tests/data/gor/genes.gor;" +
                "gor [xxx]";

        String[] lines = TestUtils.runGorPipeLines(query);
        Assert.assertEquals(15, lines.length);
    }
}
