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

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;


public class UTestGorGranno {

    String testFilePath;

    @Before
    public void SetUp() {
        try {
            testFilePath = CreateAtTestFile();
        }
        catch (Exception ex) {

        }
    }

    @Test
    public void testGrannoInNorContextWithErrorAtMax() {
        String query = "nor -h " + testFilePath + " | granno 1000";
        try {
            TestUtils.runGorPipeLines(query);
        } catch (GorParsingException ex) {
            Assert.assertTrue("Should get parsing exception", ex.getMessage().contains("Cannot have binSize"));
        }
    }

    @Test
    public void testUseGroup() {
        final String query = "gor ../tests/data/gor/dbsnp_test.gor | granno chrom -count -gc reference";
        final String result = "Chrom\tPOS\treference\tallele\tdifferentrsIDs\tallCount\n" +
                "chr1\t10179\tC\tCC\trs367896724\t1\n" +
                "chr1\t10250\tA\tC\trs199706086\t1\n" +
                "chr10\t60803\tT\tG\trs536478188\t1\n" +
                "chr10\t61023\tC\tG\trs370414480\t1\n" +
                "chr11\t61248\tG\tA\trs367559610\t1\n" +
                "chr11\t66295\tC\tA\trs61869613\t1\n" +
                "chr12\t60162\tC\tG\trs544101329\t1\n" +
                "chr12\t60545\tA\tT\trs570991495\t1\n" +
                "chr13\t19020013\tC\tT\trs181615907\t1\n" +
                "chr13\t19020145\tG\tT\trs28970552\t1\n" +
                "chr14\t19000009\tG\tC\trs373840300\t1\n" +
                "chr14\t19000060\tC\tG\trs28973059\t1\n" +
                "chr15\t20000018\tT\tC\trs374194708\t1\n" +
                "chr15\t20000043\tA\tG\trs375627562\t1\n" +
                "chr16\t60008\tA\tT\trs374973230\t1\n" +
                "chr16\t60087\tC\tG\trs62028703\t1\n" +
                "chr17\t186\tG\tA\trs547289895\t2\n" +
                "chr17\t460\tG\tA\trs554808397\t2\n" +
                "chr18\t10025\tC\tA\trs140072522\t1\n" +
                "chr18\t10147\tTTAACCCTAACCCTT\tT\trs199766986\t1\n" +
                "chr19\t62155\tA\tG\trs201739106\t2\n" +
                "chr19\t70443\tA\tG\trs373133808\t2\n" +
                "chr2\t10181\tA\tG\trs572458259\t2\n" +
                "chr2\t10200\tA\tT\trs563059835\t2\n" +
                "chr20\t60568\tA\tC\trs533509214\t1\n" +
                "chr20\t60808\tG\tA\trs534548532\t1\n" +
                "chr21\t9411302\tG\tT\trs531010746\t1\n" +
                "chr21\t9411384\tC\tT\trs554702871\t1\n" +
                "chr22\t16050036\tA\tC\trs374742143\t1\n" +
                "chr22\t16050527\tC\tA\trs587769434\t1\n" +
                "chr3\t60197\tG\tA\trs115479960\t1\n" +
                "chr3\t60419\tT\tG\trs558166806\t1\n" +
                "chr4\t10035\tT\tA\trs150076536\t1\n" +
                "chr4\t10130\tC\tCC\trs572745514\t1\n" +
                "chr5\t10058\tC\tA\trs547354230\t2\n" +
                "chr5\t10066\tC\tCAA\trs546237653\t2\n" +
                "chr6\t64163\tT\tC\trs199606246\t2\n" +
                "chr6\t70838\tT\tC\trs111875673\t2\n" +
                "chr7\t10367\tG\tA\trs201460812\t1\n" +
                "chr7\t13591\tT\tG\trs201325637\t1\n" +
                "chr8\t10059\tC\tT\trs371829072\t2\n" +
                "chr8\t10467\tC\tG\trs199753717\t2\n" +
                "chr9\t10047\tC\tT\trs567034784\t1\n" +
                "chr9\t10097\tCCA\tC\trs201803828\t1\n" +
                "chrX\t2699625\tA\tG\trs6655038\t2\n" +
                "chrX\t2699968\tA\tG\trs2306737\t2\n" +
                "chrY\t10003\tA\tC\trs375039031\t1\n" +
                "chrY\t10069\tT\tA\trs111065272\t1\n";
        TestUtils.assertGorpipeResults(result, query);
    }

    private static String CreateAtTestFile() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("valuesnogroups", ".tsv");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tpos\tvalue\tdate");
        outputWriter.println("chr1\t500\t1.0\t2010-10-3");
        outputWriter.println("chr1\t521\t2.0\t2010-10-4");
        outputWriter.println("chr1\t1300\t1.0\t2010-10-5");
        outputWriter.println("chr1\t1902\t2.0\t2010-10-6");
        outputWriter.println("chr1\t22500\t1.0\t2010-10-7");
        outputWriter.println("chr2\t32100\t1.0\t2010-10-8");
        outputWriter.println("chr2\t32101\t2.0\t2010-10-9");
        outputWriter.println("chr2\t32102\t3.0\t2010-10-10");
        outputWriter.println("chr2\t32103\t4.0\t2010-10-11");
        outputWriter.println("chr2\t32104\t5.0\t2010-10-12");
        outputWriter.println("chr2\t32105\t6.0\t2010-10-13");
        outputWriter.println("chr2\t32106\t7.0\t2010-10-14");
        outputWriter.close();

        return patientsPath.toString();

    }

    @Test
    public void testGranno() {
        TestUtils.assertGorpipeResults("Chrom\tPOS\treference\tallele\tdifferentrsIDs\tdistance\tgene_start\tgene_end\tGene_Symbol\tallCount\n" +
                "chr19\t62155\tA\tG\trs201739106\t0\t60104\t70966\tAC008993.5\t2\n" +
                "chr19\t70443\tA\tG\trs373133808\t0\t60104\t70966\tAC008993.5\t2\n" +
                "chrX\t2699625\tA\tG\trs6655038\t0\t2670090\t2734539\tXG\t2\n" +
                "chrX\t2699968\tA\tG\trs2306737\t0\t2670090\t2734539\tXG\t2\n", "gor ../tests/data/gor/dbsnp_test.gor | JOIN -snpseg ../tests/data/gor/genes.gor | GRANNO chrom -gc gene_start,gene_end,gene_symbol -count");
        TestUtils.assertGorpipeResults("chrom\tchromstart\tchromend\tdistance\tgene_start\tgene_end\tGene_Symbol\tallCount\n" +
                "chr1\t11868\t12227\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t11871\t12227\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t11873\t12227\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t12009\t12057\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t12178\t12227\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t12594\t12721\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t12612\t12697\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t12612\t12721\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t12612\t12721\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t12974\t13052\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t13220\t13374\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t13220\t14409\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t13220\t14409\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t13224\t14412\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t13224\t14412\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t13402\t13655\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t13452\t13670\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t13660\t14409\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t13660\t14409\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t14362\t14829\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t14362\t14829\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t14362\t14829\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t14362\t14829\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t14362\t14829\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t14362\t14829\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t14403\t14501\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t14403\t14501\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t14410\t14502\t0\t11868\t14412\tDDX11L1\t21\n" +
                "chr1\t14410\t14502\t0\t14362\t29806\tWASH7P\t9\n" +
                "chr1\t14969\t15038\t0\t14362\t29806\tWASH7P\t9\n",
                "gor ../tests/data/gor/ensgenes_exons.gorz | select 1-3 | JOIN -segseg ../tests/data/gor/genes.gorz | top 30 | GRANNO gene -range -gc gene_symbol -count");
    }

    @Test
    public void grannoMinGenome() {
        String query = "gor 1.mem | granno genome -ic Col4 -min";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0\n", lines[0]);
    }

    @Test
    public void grannoMinChrom() {
        String query = "gor 1.mem | granno chrom -ic Col4 -min";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0\n", lines[0]);
    }

    @Test
    public void grannoMinGenomeRange() {
        String query = "gor 1.mem | granno genome -range -ic Col4 -min";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0\n", lines[0]);
    }

    @Test
    public void grannoMinDoubleGenome() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno genome -fc Data -min";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t0.0\n", lines[0]);
    }

    @Test
    public void grannoMinDoubleChrom() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno chrom -fc Data -min";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t0.0\n", lines[0]);
    }

    @Test
    public void grannoMinDoubleGenomeRange() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno genome -range -fc Data -min";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t0.0\n", lines[0]);
    }

    @Test
    public void grannoMaxGenome() {
        String query = "gor 1.mem | granno genome -ic Col4 -max";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t4\n", lines[0]);
    }

    @Test
    public void grannoMaxChrom() {
        String query = "gor 1.mem | granno chrom -ic Col4 -max";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t4\n", lines[0]);
    }

    @Test
    public void grannoMaxGenomeRange() {
        String query = "gor 1.mem | granno genome -range -ic Col4 -max";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t4\n", lines[0]);
    }

    @Test
    public void grannoMaxDoubleGenome() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno genome -fc Data -max";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t6.0\n", lines[0]);
    }

    @Test
    public void grannoMaxDoubleChrom() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno chrom -fc Data -max";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t6.0\n", lines[0]);
    }

    @Test
    public void grannoMaxDoubleGenomeRange() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno genome -range -fc Data -max";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t6.0\n", lines[0]);
    }

    @Test
    public void grannoAvgGenome() {
        String query = "gor 1.mem | granno genome -ic Col4 -avg";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t2.0\n", lines[0]);
    }

    @Test
    public void grannoAvgChrom() {
        String query = "gor 1.mem | granno chrom -ic Col4 -avg";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t2.0\n", lines[0]);
    }

    @Test
    public void grannoAvgGenomeRange() {
        String query = "gor 1.mem | granno genome -range -ic Col4 -avg";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t2.0\n", lines[0]);
    }

    @Test
    public void grannoAvgDoubleGenome() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno genome -fc Data -avg";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t3.0\n", lines[0]);
    }

    @Test
    public void grannoAvgDoubleChrom() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno chrom -fc Data -avg";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t3.0\n", lines[0]);
    }

    @Test
    public void grannoAvgDoubleGenomeRange() {
        String query = "gor 1.mem | calc Data Col4*1.5 | granno genome -range -fc Data -avg";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        assertEquals("chr1\t0\tdata1\t0\tdata0\t0.0\t3.0\n", lines[0]);
    }
}
