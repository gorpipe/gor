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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestPgor {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();


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

    @Test
    public void testPgorRangeReplace() {
        String result = TestUtils.runGorPipe("create xxx = pgor -split <(gor ../tests/data/gor/genes.gor | top 2) ../tests/data/gor/genes.gor | calc f0 '##WHERE_SPLIT_WINDOW##' | calc f1 '#{CHROM}:#{BPSTART}-#{BPSTOP}' | calc f2 '#{RANGETAG}'; gor [xxx] | top 1",
                ".", workDir.getRoot().getPath(), false, null, null);
        Assert.assertEquals("Wrong result from pgor range replace query","Chrom\tgene_start\tgene_end\tGene_Symbol\tf0\tf1\tf2\n" +
                "chr1\t14362\t29806\tWASH7P\t11869 <= #2i and #2i <= 14412\tchr1:11869-14412\tDDX11L1\n",result);
    }

    @Test
    public void testPgorOutOfBoundsRangeReplace() {
        String result = TestUtils.runGorPipe("create xxx = pgor -split <(gor ../tests/data/gor/genes.gor | replace gene_end gene_end+300000000 | top 2) ../tests/data/gor/genes.gor | calc f0 '##WHERE_SPLIT_WINDOW##' | calc f1 '#{CHROM}:#{BPSTART}-#{BPSTOP}'; gor [xxx] | top 1");
        Assert.assertEquals("Wrong result from pgor range replace query","Chrom\tgene_start\tgene_end\tGene_Symbol\tf0\tf1\n" +
                "chr1\t14362\t29806\tWASH7P\t11869 <= #2i and #2i <= 300014412\tchr1:11869-300014412\n",result);
    }

    @Test
    public void testPgorOutOfBounds() {
        String result = TestUtils.runGorPipe("create xxx = pgor <(gorrow chr15,34260920,34357291) | where ##WHERE_SPLIT_WINDOW## | calc f0 '##WHERE_SPLIT_WINDOW##' | calc f1 '#{CHROM}:#{BPSTART}-#{BPSTOP}'; gor [xxx]");
        Assert.assertEquals("Wrong result from pgor range replace query","chrom\tbpStart\tbpStop\tf0\tf1\n" +
                "chr15\t34260920\t34357291\t0 <= #2i\tchr15:0-1000000000\n",result);
    }

    @Test
    public void testPgorServerMode() throws IOException {
        Path projectDir = workDir.getRoot().toPath();
        Files.copy(Path.of("../tests/data/gor/genes.gor"), projectDir.resolve("genes.gor"));
        Files.createDirectory(projectDir.resolve("result_cache"));

        String query = "pgor genes.gor | top 10";

        String lines = TestUtils.runGorPipeServer(query, projectDir.toString(), "");
        Assert.assertEquals(11938, lines.length());
    }
}
