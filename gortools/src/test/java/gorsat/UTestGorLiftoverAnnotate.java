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
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End-to-end tests of the LIFTOVER_ANNOTATE command (ENGKNOW-4117): the output columns of every mode, the contig
 * policies, nor input, chain sources (link, gorz, nested query) and agreement with LIFTOVER on unique chains.
 */
public class UTestGorLiftoverAnnotate {

    private static final String CHAIN_HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore";
    private static final String SNP_HEADER = "Chrom\tPos\tRef\tAlt\tId";
    private static final String SEG_HEADER = "Chrom\tStart\tEnd\tName";

    private static final String SNP_COLS = "liftover_status\tliftover_chrom\tliftover_pos\tliftover_strand";
    private static final String VAR_COLS = "liftover_status\tliftover_chrom\tliftover_pos\tliftover_ref\t"
            + "liftover_alt\tliftover_strand";
    private static final String SEG_COLS = "liftover_status\tliftover_chrom\tliftover_start\tliftover_stop\t"
            + "liftover_strand\tliftover_chain_blocks";

    private static final String UNMAPPED_SNP = "unmapped\t\t0\t";
    private static final String UNMAPPED_VAR = "unmapped\t\t0\t\t\t";
    private static final String UNMAPPED_SEG = "unmapped\t\t0\t0\t\t0";

    private static String CHAIN;
    private static String CHAIN_UNIQUE;
    private static String SNP;
    private static String VAR;
    private static String SEG;
    private static String EDGE;

    private static Path tmpdir;

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    private static String write(String name, String content) throws IOException {
        File f = new File(tmpdir.toFile(), name);
        FileUtils.write(f, content, StandardCharsets.UTF_8);
        return f.getAbsolutePath();
    }

    private static String snpRow(String chr, int pos, String id) {
        return chr + "\t" + pos + "\tA\tG\t" + id;
    }

    private static String q(String chainFile, String input, String opts) {
        return "gor " + input + " | liftover_annotate " + chainFile + " " + opts;
    }

    @BeforeClass
    public static void setUp() throws IOException {
        tmpdir = Files.createTempDirectory("liftover-annotate");
        tmpdir.toFile().deleteOnExit();

        String uniqueRows = lines(CHAIN_HEADER,
                "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000",
                "chr1\t200\t300\tchr1\t1200\t1300\t+\t1000",
                "chr1\t400\t500\tchr1\t5000\t5100\t-\t900",
                "chr1\t600\t700\tchr2\t2100\t2200\t+\t800",
                "chr1\t800\t900\tchr1\t8800\t8900\t+\t500");
        String tail = lines("chrM\t300\t3100\tchrM\t298\t3098\t+\t100");
        CHAIN_UNIQUE = write("chain_unique.gor", uniqueRows + tail);
        CHAIN = write("chain.gor", uniqueRows + "chr1\t850\t950\tchr1\t9850\t9950\t+\t500\n" + tail);

        SNP = write("snp.gor", lines(SNP_HEADER,
                snpRow("chr1", 150, "s01"),
                snpRow("chr1", 200, "s02"),
                snpRow("chr1", 201, "s03"),
                snpRow("chr1", 300, "s04"),
                snpRow("chr1", 350, "dup"),
                snpRow("chr1", 350, "dup"),
                snpRow("chr1", 350, "dup"),
                snpRow("chr1", 401, "s08"),
                snpRow("chr1", 500, "s09"),
                snpRow("chr1", 650, "s10"),
                snpRow("chr1", 875, "s11"),
                snpRow("chr2", 100, "s12"),
                snpRow("chrM", 1000, "s13")));

        VAR = write("var.gor", lines(SNP_HEADER,
                "chr1\t150\tA\tG\tv01",
                "chr1\t160\t\tG\tv02",
                "chr1\t198\tACG\tT\tv03",
                "chr1\t199\tACG\tT\tv04",
                "chr1\t350\tA\tG\tdup",
                "chr1\t350\tA\tG\tdup",
                "chr1\t410\tAC\tG\tv07",
                "chr1\t420\tC\tA,G\tv08",
                "chr1\t425\tAC\tG,TTA\tv09",
                "chr1\t430\tA\t<DEL>\tv10",
                "chr1\t440\t\tT\tv11",
                "chr1\t650\tA\tG\tv12",
                "chr1\t875\tA\tG\tv13",
                "chrM\t1000\tA\tG\tv14"));

        SEG = write("seg.gor", lines(SEG_HEADER,
                "chr1\t120\t180\tg01",
                "chr1\t150\t250\tg02",
                "chr1\t250\t450\tg03",
                "chr1\t350\t360\tdup",
                "chr1\t350\t360\tdup",
                "chr1\t410\t450\tg06",
                "chr1\t610\t690\tg07",
                "chr1\t860\t880\tg08",
                "chrM\t1000\t1100\tg09"));

        EDGE = write("edge.gor", lines(SNP_HEADER,
                "chr1\t100\t\tA\te01",
                "chr1\t100\tA\tG\te02",
                "chr1\t101\t\tA\te03",
                "chr1\t101\tA\tG\te04",
                "chr1\t150\tA\tG\tmdup",
                "chr1\t150\tA\tG\tmdup",
                "chr1\t200\t\tA\te07",
                "chr1\t201\t\tA\te08",
                "chr1\t350\tA\tG\tdup",
                "chr1\t350\tA\tG\tzzz",
                "chr1\t350\tA\tG\tdup",
                "chr1\t500\t\tA\te12",
                "chr1\t501\t\tA\te13"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tmpdir.toFile());
    }

    // -snp

    private static String snpExpected(String s13) {
        return lines(SNP_HEADER + "\t" + SNP_COLS,
                snpRow("chr1", 150, "s01") + "\tmapped\tchr1\t1150\t+",
                snpRow("chr1", 200, "s02") + "\tmapped\tchr1\t1200\t+",
                snpRow("chr1", 201, "s03") + "\tmapped\tchr1\t1201\t+",
                snpRow("chr1", 300, "s04") + "\tmapped\tchr1\t1300\t+",
                snpRow("chr1", 350, "dup") + "\t" + UNMAPPED_SNP,
                snpRow("chr1", 350, "dup") + "\t" + UNMAPPED_SNP,
                snpRow("chr1", 350, "dup") + "\t" + UNMAPPED_SNP,
                snpRow("chr1", 401, "s08") + "\tmapped\tchr1\t5100\t-",
                snpRow("chr1", 500, "s09") + "\tmapped\tchr1\t5001\t-",
                snpRow("chr1", 650, "s10") + "\tmapped\tchr2\t2150\t+",
                snpRow("chr1", 875, "s11") + "\tambiguous\t\t0\t",
                snpRow("chr2", 100, "s12") + "\t" + UNMAPPED_SNP,
                snpRow("chrM", 1000, "s13") + "\t" + s13);
    }

    @Test
    public void snpBest() {
        Assert.assertEquals(snpExpected("mapped\tchrM\t998\t+"), TestUtils.runGorPipe(q(CHAIN, SNP, "-snp -overlap best")));
    }

    @Test
    public void snpPassthrough() {
        Assert.assertEquals(snpExpected("passthrough\tchrM\t1000\t"),
                TestUtils.runGorPipe(q(CHAIN, SNP, "-snp -overlap best -passthrough chrM")));
    }

    @Test
    public void snpPassthroughNone() {
        Assert.assertEquals(snpExpected("mapped\tchrM\t998\t+"),
                TestUtils.runGorPipe(q(CHAIN, SNP, "-snp -overlap best -passthrough none")));
    }

    @Test
    public void snpOverlapErrorIsDefault() {
        try {
            TestUtils.runGorPipe(q(CHAIN, SNP, "-snp"));
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("overlapping blocks"));
        }
    }

    // -var

    @Test
    public void edgeVarMatchesLiftover() {
        // status, position and strand per EDGE row equal LIFTOVER -var (UTestGorLiftoverCharacterization.edgeVar)
        String[] expected = {
                "e01 unmapped 0 ", "e02 unmapped 0 ", "e03 unmapped 0 ", "e04 mapped 1101 +",
                "mdup mapped 1150 +", "mdup mapped 1150 +", "e07 mapped 1200 +", "e08 unmapped 0 ",
                "dup unmapped 0 ", "zzz unmapped 0 ", "dup unmapped 0 ", "e12 mapped 5002 -", "e13 unmapped 0 "};
        String[] out = TestUtils.runGorPipe(q(CHAIN, EDGE, "-var -overlap best")).split("\n");
        Assert.assertEquals(expected.length + 1, out.length);
        Assert.assertEquals(SNP_HEADER + "\t" + VAR_COLS, out[0]);
        for (int i = 0; i < expected.length; i++) {
            String[] c = out[i + 1].split("\t", -1);
            Assert.assertEquals(out[i + 1], expected[i], c[4] + " " + c[5] + " " + c[7] + " " + c[10]);
        }
    }

    @Test
    public void varBest() {
        String expected = lines(SNP_HEADER + "\t" + VAR_COLS,
                "chr1\t150\tA\tG\tv01\tmapped\tchr1\t1150\tA\tG\t+",
                "chr1\t160\t\tG\tv02\tmapped\tchr1\t1160\t\tG\t+",
                "chr1\t198\tACG\tT\tv03\tmapped\tchr1\t1198\tACG\tT\t+",
                "chr1\t199\tACG\tT\tv04\t" + UNMAPPED_VAR,
                "chr1\t350\tA\tG\tdup\t" + UNMAPPED_VAR,
                "chr1\t350\tA\tG\tdup\t" + UNMAPPED_VAR,
                "chr1\t410\tAC\tG\tv07\tmapped\tchr1\t5090\tGT\tC\t-",
                "chr1\t420\tC\tA,G\tv08\tmapped\tchr1\t5081\tG\tT,C\t-",
                "chr1\t425\tAC\tG,TTA\tv09\tmapped\tchr1\t5075\tGT\tC,TAA\t-",
                "chr1\t430\tA\t<DEL>\tv10\tmapped\tchr1\t5071\tT\t<DEL>\t-",
                "chr1\t440\t\tT\tv11\tmapped\tchr1\t5062\t\tA\t-",
                "chr1\t650\tA\tG\tv12\tmapped\tchr2\t2150\tA\tG\t+",
                "chr1\t875\tA\tG\tv13\tambiguous\t\t0\t\t\t",
                "chrM\t1000\tA\tG\tv14\tmapped\tchrM\t998\tA\tG\t+");
        Assert.assertEquals(expected, TestUtils.runGorPipe(q(CHAIN, VAR, "-var -overlap best")));
    }

    // -seg

    @Test
    public void segBest() {
        String expected = lines(SEG_HEADER + "\t" + SEG_COLS,
                "chr1\t120\t180\tg01\tmapped\tchr1\t1120\t1180\t+\t1",
                "chr1\t150\t250\tg02\tmapped_across_gap\tchr1\t1150\t1250\t+\t2",
                "chr1\t250\t450\tg03\tinverted\t\t0\t0\t\t2",
                "chr1\t350\t360\tdup\t" + UNMAPPED_SEG,
                "chr1\t350\t360\tdup\t" + UNMAPPED_SEG,
                "chr1\t410\t450\tg06\tmapped\tchr1\t5050\t5090\t-\t1",
                "chr1\t610\t690\tg07\tmapped\tchr2\t2110\t2190\t+\t1",
                "chr1\t860\t880\tg08\tambiguous\t\t0\t0\t\t2",
                "chrM\t1000\t1100\tg09\tmapped\tchrM\t998\t1098\t+\t1");
        Assert.assertEquals(expected, TestUtils.runGorPipe(q(CHAIN, SEG, "-seg -overlap best")));
    }

    // Parity with LIFTOVER

    @Test
    public void parityWithLiftoverOnUniqueRows() {
        String snpA = "gor " + SNP + " | liftover_annotate " + CHAIN_UNIQUE + " -snp | where liftover_status='mapped' "
                + "| select Chrom,Pos,Id,liftover_chrom,liftover_pos,liftover_strand";
        String snpB = "gor " + SNP + " | liftover " + CHAIN_UNIQUE + " -snp | where hgold_liftoverStatus='mapped' "
                + "| select hgold_Chrom,hgold_Pos,Id,Chrom,Pos,hgold_qStrand | sort genome";
        Assert.assertEquals(TestUtils.runGorPipeNoHeader(snpB), TestUtils.runGorPipeNoHeader(snpA));

        String nonSymbolic = "not(Alt ~ '*,*' or Alt ~ '*<*' or Alt ~ '*>*')";
        String varA = "gor " + VAR + " | liftover_annotate " + CHAIN_UNIQUE + " -var | where liftover_status='mapped' "
                + "and " + nonSymbolic + " | select Chrom,Pos,Id,liftover_chrom,liftover_pos,liftover_ref,"
                + "liftover_alt,liftover_strand";
        String varB = "gor " + VAR + " | liftover " + CHAIN_UNIQUE + " -var | where hgold_liftoverStatus='mapped' "
                + "and " + nonSymbolic + " | select hgold_Chrom,hgold_Pos,Id,Chrom,Pos,Ref,Alt,hgold_qStrand "
                + "| sort genome";
        Assert.assertEquals(TestUtils.runGorPipeNoHeader(varB), TestUtils.runGorPipeNoHeader(varA));
    }

    // Contig policies

    @Test
    public void mixedContigs() throws IOException {
        String chain = write("mixed_chain.gor", lines(CHAIN_HEADER,
                "chr12\t100000\t200000\tchr12\t90000\t190000\t+\t1000"));
        String input = write("mixed_in.gor", lines("Chrom\tPos",
                "chr12\t150000", "chrM\t100", "chrUn_x\t5"));
        Assert.assertEquals(lines("Chrom\tPos\t" + SNP_COLS,
                "chr12\t150000\tmapped\tchr12\t140000\t+",
                "chrM\t100\tpassthrough\tchrM\t100\t",
                "chrUn_x\t5\tunsupported_contig\t\t0\t"),
                TestUtils.runGorPipe(q(chain, input, "-snp -passthrough chrM -mapped chr12")));
        Assert.assertEquals(lines("Chrom\tPos\t" + SNP_COLS,
                "chr12\t150000\tmapped\tchr12\t140000\t+",
                "chrM\t100\tpassthrough\tchrM\t100\t",
                "chrUn_x\t5\t" + UNMAPPED_SNP),
                TestUtils.runGorPipe(q(chain, input, "-snp -passthrough chrM")));
    }

    // nor

    @Test
    public void norMode() throws IOException {
        String tsv = write("coords.tsv", "#c\tp\nchr1\t150\nchr1\t401\n");
        // nor output keeps the ChromNOR/PosNOR columns in the test harness
        Assert.assertEquals("ChromNOR\tPosNOR\tc\tp\t" + SNP_COLS + "\nchrN\t0\tchr1\t150\tmapped\tchr1\t1150\t+\n"
                        + "chrN\t0\tchr1\t401\tmapped\tchr1\t5100\t-\n",
                TestUtils.runGorPipe("nor " + tsv + " | liftover_annotate " + CHAIN_UNIQUE
                        + " -snp -chrom c -pos p"));
    }

    @Test
    public void collisionFailsLoudly() throws IOException {
        String input = write("collision.gor", lines("Chrom\tPos\tliftover_status", "chr1\t150\tx"));
        try {
            TestUtils.runGorPipe(q(CHAIN_UNIQUE, input, "-snp"));
            Assert.fail("expected GorParsingException");
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("liftover_status"));
        }
    }

    // Chain sources

    @Test
    public void chainLoadsLazilyThroughCommand() throws IOException {
        String chain = write("unsorted_chain.gor", lines(CHAIN_HEADER,
                "chr1\t300\t400\tchr1\t1300\t1400\t+\t1000",
                "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000"));
        String chrMOnly = write("chrm_only.gor", lines(SNP_HEADER, snpRow("chrM", 100, "m1"), snpRow("chrM", 200, "m2")));
        Assert.assertEquals(lines(SNP_HEADER + "\t" + SNP_COLS,
                snpRow("chrM", 100, "m1") + "\tpassthrough\tchrM\t100\t",
                snpRow("chrM", 200, "m2") + "\tpassthrough\tchrM\t200\t"),
                TestUtils.runGorPipe(q(chain, chrMOnly, "-snp -passthrough chrM")));

        String withChr1 = write("chr1_and_chrm.gor", lines(SNP_HEADER, snpRow("chr1", 150, "s"), snpRow("chrM", 100, "m1")));
        try {
            TestUtils.runGorPipe(q(chain, withChr1, "-snp -passthrough chrM"));
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("not sorted by tStart"));
        }
    }

    @Test
    public void linkChainSameOutput() throws IOException {
        String link = write("chain_link.gor.link", CHAIN + "\n");
        TestUtils.assertTwoGorpipeResults(q(link, SNP, "-snp -overlap best"), q(CHAIN, SNP, "-snp -overlap best"));
    }

    @Test
    public void gorzChainSameOutput() {
        File dir = new File(tmpdir.toFile(), "gorz");
        Assert.assertTrue(dir.mkdirs());
        String gorz = dir.getAbsolutePath() + "/chain.gorz";
        TestUtils.runGorPipe("gor " + CHAIN + " | write " + gorz);
        Assert.assertTrue(new File(gorz).exists());
        TestUtils.assertTwoGorpipeResults(q(gorz, SNP, "-snp -overlap best"), q(CHAIN, SNP, "-snp -overlap best"));
    }

    @Test
    public void nestedChainSameOutput() {
        TestUtils.assertTwoGorpipeResults(q("<(gor " + CHAIN + ")", SNP, "-snp -overlap best"),
                q(CHAIN, SNP, "-snp -overlap best"));
        try {
            TestUtils.runGorPipe(q("<(nor " + CHAIN + ")", SNP, "-snp -overlap best"));
            Assert.fail("expected GorParsingException");
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("gor query"));
        }
    }

    @Test
    public void pgorEqualsGor() {
        TestUtils.assertTwoGorpipeResults("pgor " + SNP + " | liftover_annotate " + CHAIN + " -snp -overlap best",
                "gor " + SNP + " | liftover_annotate " + CHAIN + " -snp -overlap best");
    }
}
