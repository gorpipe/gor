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

import org.apache.commons.io.FileUtils;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by sigmar on 19/04/16.
 */
public class UTestGorJoin {

    private File gorFile;
    private File gorDictionaryFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
        gorDictionaryFile = FileTestUtils.createGenericDictionaryFile(workDir.getRoot(), gorFile.getCanonicalPath(), "generic.gord");
    }

    @Test
    public void testLeftjoin() throws IOException {
        int count = TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gor | top 16 | leftjoin -segseg " + gorFile.getCanonicalPath() + "");
        Assert.assertEquals(21, count);
    }

    @Test
    public void testJoinR() {
        var query = "gorrows -p chr1:1-3 | join -snpseg -r <(gorrows -p chr1:1-3 | calc end pos + 2 | calc mapq 10)";

        var res = TestUtils.runGorPipe(query);
        Assert.assertEquals("join r not returning correct results","chrom\tpos\tmapq\n" +
                "chr1\t2\t10\n",res);
    }

    @Test
    public void testJoinBam() {
        var query = "gorrows -p chr8:145577711-145584931 | calc x pos +1 | calc y 'y' | join -segseg -maxseg 10000 <(gor ../tests/data/bvl_min/bam/BVL_INDEX_SLC52A2.bam | select 1-3,mapq) | top 1";

        var res = TestUtils.runGorPipe(query);
        Assert.assertEquals("join r not returning correct results","chrom\tpos\tx\ty\tdistance\tPosx\tEnd\tMapQ\n" +
                "chr8\t145577711\t145577712\ty\t0\t14557771114557781129\n",res);
    }

    @Test
    public void testNewIterator() {
        final String query = "gor ../tests/data/gor/dbsnp_test.gorz | join -snpsnp ../tests/data/gor/dbsnp_test.gorz";
        System.setProperty("gor.iterators.useAdaptiveMTP", "false");
        final String results = TestUtils.runGorPipe(query);
        System.setProperty("gor.iterators.useAdaptiveMTP", "true");
        TestUtils.assertGorpipeResults(results, query);
    }

    @Test
    public void testCalcCaseSenseLowerLower() throws IOException {
        String[] args = new String[]{"gorrow chr8,145584264,145584264 | calc Reference 't' | calc Call 'c' | varjoin -r " + gorDictionaryFile.getCanonicalPath() + ""};
        TestUtils.runGorPipe(args);
    }

    @Test
    public void testCalcCaseSenseLowerUpper() throws IOException {
        String[] args = new String[]{"gorrow chr8,145584264,145584264 | calc Reference 't' | calc Call 'C' | varjoin -r " + gorDictionaryFile.getCanonicalPath() + ""};
        TestUtils.runGorPipe(args);
    }

    @Test
    public void testCalcCaseSenseUpperLower() throws IOException {
        String[] args = new String[]{"gorrow chr8,145584264,145584264 | calc Reference 'T' | calc Call 'c' | varjoin -r " + gorDictionaryFile.getCanonicalPath() + ""};
        TestUtils.runGorPipe(args);
    }

    @Test
    public void testCalcCaseSenseUpperUpper() throws IOException {
        String[] args = new String[]{"gorrow chr8,145584264,145584264 | calc Reference 'T' | calc Call 'C' | varjoin -r " + gorDictionaryFile.getCanonicalPath() + ""};
        TestUtils.runGorPipe(args);
    }

    @Test
    public void testSNPSNPJoin() {
        String[] result = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | join -snpsnp ../tests/data/gor/dbsnp_test.gor");

        Assert.assertEquals(49, result.length);
        Assert.assertEquals(8, result[0].split("\t").length);
        Assert.assertEquals(8, result[1].split("\t").length);
    }

    @Test
    public void testSNPSEGJoin() {
        String[] result = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | join -snpseg ../tests/data/gor/genes.gor");

        Assert.assertEquals(5, result.length);
        Assert.assertEquals(9, result[0].split("\t").length);
        Assert.assertEquals(9, result[1].split("\t").length);
    }

    @Test
    public void testSEGSNPJoin() {
        String[] result = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | join -segsnp ../tests/data/gor/dbsnp_test.gor");

        Assert.assertEquals(5, result.length);
        Assert.assertEquals(9, result[0].split("\t").length);
        Assert.assertEquals(9, result[1].split("\t").length);
    }

    @Test
    public void testSEGSEGJoin() {
        String[] result = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | join -segseg ../tests/data/gor/genes.gor");

        Assert.assertEquals(97555, result.length);
        Assert.assertEquals(8, result[0].split("\t").length);
        Assert.assertEquals(8, result[1].split("\t").length);
    }

    @Test
    public void testVARSEGJoin() {
        String[] result = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | join -varseg ../tests/data/gor/genes.gor");

        Assert.assertEquals(5, result.length);
        Assert.assertEquals(9, result[0].split("\t").length);
        Assert.assertEquals(9, result[1].split("\t").length);
    }

    @Test
    public void testSEGVARJoin() {
        String[] result = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | join -segvar ../tests/data/gor/dbsnp_test.gor");

        Assert.assertEquals(5, result.length);
        Assert.assertEquals(9, result[0].split("\t").length);
        Assert.assertEquals(9, result[1].split("\t").length);
    }

    @Test
    public void testVarJoinOnGorrow() throws IOException {
        final Path tmpDir = Files.createTempDirectory("uTestGorJoin");
        final File file = new File(tmpDir.toFile(), "testFile.gor");
        final FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("#Chrom\tPos\tReference\tCall\tMax_Consequence\tGene\tFeature\tFeature_type\tcDNA_position\tCDS_positionProtein_position\tAmino_acids\tCodons\tExisting_variation\n" +
                "chr8\t145584264\tT\tC\tmissense_variant\tENSG00000185803\tENST00000527078\tTranscript\t14621016\t339\tL/P\tcTg/cCg\t\n" +
                "chr8\t145584264\tT\tC\tmissense_variant\tENSG00000185803\tENST00000530047\tTranscript\t11801016\t339\tL/P\tcTg/cCg\t\n" +
                "chr8\t145584264\tT\tC\tmissense_variant\tENSG00000185803\tENST00000329994\tTranscript\t13171016\t339\tL/P\tcTg/cCg\t\n" +
                "chr8\t145584264\tT\tC\tmissense_variant\tENSG00000185803\tENST00000540505\tTranscript\t1099752\t251\tL/P\tcTg/cCg\t\n" +
                "chr8\t145584264\tT\tC\tmissense_variant\tENSG00000185803\tENST00000402965\tTranscript\t12001016\t339\tL/P\tcTg/cCg\t\n" +
                "chr8\t145584264\tT\tC\tmissense_variant\tENSG00000185803\tENST00000532887\tTranscript\t15991016\t339\tL/P\tcTg/cCg\t");
        fileWriter.close();
        final String test = "gorrow 8,145584264,145584264\n" +
                "                | calc Reference 'T'\n" +
                "                | calc Call 'C'\n" +
                "                | hide #3\n" +
                "                | rename #2 Pos\n" +
                "                | varjoin -r " + file.getAbsolutePath();
        final String result = TestUtils.runGorPipe(test);
        final String test2 = "create #vartable# = gorrow 8,145584264,145584264 \n" +
                "| calc Reference 'T' \n" +
                "| calc Call 'C' \n" +
                "| hide #3 \n" +
                "| rename #2 Pos\n" +
                ";\n" +
                "\n" +
                "gor [#vartable#]\n" +
                "| varjoin -r " + file.getAbsolutePath();
        final String result2 = TestUtils.runGorPipe(test2);
        FileUtils.deleteDirectory(tmpDir.toFile());
        Assert.assertEquals(result, result2);
    }

    @Test
    public void testJoinOnNonStandardContig() {
        String script = "create xxx = nor <(norrows 30 | calc c replace('abc' + form(rownum, 2, 0), ' ', '0') | calc p 1 | select c,p,rownum | rename c chrom | rename p pos);\n" +
                "gor [xxx] | join -snpsnp [xxx]";

        String[] result = TestUtils.runGorPipeLines(script);
        Assert.assertEquals(31, result.length);
    }
}
