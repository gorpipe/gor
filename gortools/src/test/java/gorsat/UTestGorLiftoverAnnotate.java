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

import gorsat.process.PipeOptions;
import gorsat.process.TestSessionFactory;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.session.GorSession;
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

    /** Runs both queries, asserts equal output and that the output has at least one data row. */
    private static void assertSameNonEmpty(String query1, String query2) {
        String r1 = TestUtils.runGorPipe(query1);
        String r2 = TestUtils.runGorPipe(query2);
        Assert.assertEquals(r1, r2);
        assertHasDataRows(r1);
    }

    private static void assertHasDataRows(String output) {
        Assert.assertTrue("expected a header and at least one data row, got: " + output,
                output.split("\n").length >= 2);
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
        assertHasDataRows("header\n" + TestUtils.runGorPipeNoHeader(snpA));
        Assert.assertEquals(TestUtils.runGorPipeNoHeader(snpB), TestUtils.runGorPipeNoHeader(snpA));

        String nonSymbolic = "not(Alt ~ '*,*' or Alt ~ '*<*' or Alt ~ '*>*')";
        String varA = "gor " + VAR + " | liftover_annotate " + CHAIN_UNIQUE + " -var | where liftover_status='mapped' "
                + "and " + nonSymbolic + " | select Chrom,Pos,Id,liftover_chrom,liftover_pos,liftover_ref,"
                + "liftover_alt,liftover_strand";
        String varB = "gor " + VAR + " | liftover " + CHAIN_UNIQUE + " -var | where hgold_liftoverStatus='mapped' "
                + "and " + nonSymbolic + " | select hgold_Chrom,hgold_Pos,Id,Chrom,Pos,Ref,Alt,hgold_qStrand "
                + "| sort genome";
        assertHasDataRows("header\n" + TestUtils.runGorPipeNoHeader(varA));
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
        assertSameNonEmpty(q(link, SNP, "-snp -overlap best"), q(CHAIN, SNP, "-snp -overlap best"));
    }

    @Test
    public void gorzChainSameOutput() {
        File dir = new File(tmpdir.toFile(), "gorz");
        Assert.assertTrue(dir.mkdirs());
        String gorz = dir.getAbsolutePath() + "/chain.gorz";
        TestUtils.runGorPipe("gor " + CHAIN + " | write " + gorz);
        Assert.assertTrue(new File(gorz).exists());
        assertSameNonEmpty(q(gorz, SNP, "-snp -overlap best"), q(CHAIN, SNP, "-snp -overlap best"));
    }

    @Test
    public void nestedChainSameOutput() {
        assertSameNonEmpty(q("<(gor " + CHAIN + ")", SNP, "-snp -overlap best"),
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
        assertSameNonEmpty("pgor " + SNP + " | liftover_annotate " + CHAIN + " -snp -overlap best",
                "gor " + SNP + " | liftover_annotate " + CHAIN + " -snp -overlap best");
    }

    // Non-default options, end to end

    private static String SEG_CHAIN;
    private static String SEG_INPUT;

    private static void writeSegFixtures() throws IOException {
        // two blocks with a 10 base gap in the source and none in the target
        SEG_CHAIN = write("seg_chain.gor", lines(CHAIN_HEADER,
                "chr1\t100\t200\tchr1\t1000\t1100\t+\t1000",
                "chr1\t210\t310\tchr1\t1100\t1200\t+\t1000"));
        SEG_INPUT = write("seg_input.gor", lines(SEG_HEADER,
                "chr1\t120\t180\tin",
                "chr1\t150\t260\tgap"));
    }

    @Test
    public void seg1IsOneBasedInclusive() throws IOException {
        writeSegFixtures();
        String input = write("seg1_input.gor", lines(SEG_HEADER, "chr1\t121\t180\tin", "chr1\t151\t260\tgap"));
        // [121,180] 1-based is [120,180) 0-based: block 1 gives 1021..1080; the gap row spans both blocks
        Assert.assertEquals(lines(SEG_HEADER + "\t" + SEG_COLS,
                "chr1\t121\t180\tin\tmapped\tchr1\t1021\t1080\t+\t1",
                "chr1\t151\t260\tgap\tsize_changed\tchr1\t1051\t1150\t+\t2"),
                TestUtils.runGorPipe(q(SEG_CHAIN, input, "-seg1")));
    }

    @Test
    public void diagColumnsOnMappedAndGapRows() throws IOException {
        writeSegFixtures();
        String diagCols = "\tliftover_source_size\tliftover_target_size\tliftover_size_ratio\tliftover_start_chrom"
                + "\tliftover_start_pos\tliftover_start_strand\tliftover_stop_chrom\tliftover_stop_pos"
                + "\tliftover_stop_strand";
        // gap row: source 260-151 = 109 bases, the endpoints lift to 1051 and 1150, so target 99
        String ratio = Double.toString(99.0 / 109.0);
        Assert.assertEquals(lines(SEG_HEADER + "\t" + SEG_COLS + diagCols,
                "chr1\t120\t180\tin\tmapped\tchr1\t1020\t1080\t+\t1\t59\t59\t1.0\tchr1\t1021\t+\tchr1\t1080\t+",
                "chr1\t150\t260\tgap\tsize_changed\tchr1\t1050\t1150\t+\t2\t109\t99\t" + ratio
                        + "\tchr1\t1051\t+\tchr1\t1150\t+"),
                TestUtils.runGorPipe(q(SEG_CHAIN, SEG_INPUT, "-seg -diag")));
    }

    @Test
    public void toleranceFlipsGapRowBetweenSizeChangedAndMappedAcrossGap() throws IOException {
        writeSegFixtures();
        // the gap row has size ratio 99/109 = 0.908
        String gapDefault = TestUtils.runGorPipe(q(SEG_CHAIN, SEG_INPUT, "-seg")).split("\n")[2];
        Assert.assertEquals("chr1\t150\t260\tgap\tsize_changed\tchr1\t1050\t1150\t+\t2", gapDefault);
        String gapLoose = TestUtils.runGorPipe(q(SEG_CHAIN, SEG_INPUT, "-seg -tolerance 0.1")).split("\n")[2];
        Assert.assertEquals("chr1\t150\t260\tgap\tmapped_across_gap\tchr1\t1050\t1150\t+\t2", gapLoose);
        String gapTight = TestUtils.runGorPipe(q(SEG_CHAIN, SEG_INPUT, "-seg -tolerance 0.09")).split("\n")[2];
        Assert.assertEquals(gapDefault, gapTight);
        // the contained row is mapped whatever the tolerance
        Assert.assertEquals("chr1\t120\t180\tin\tmapped\tchr1\t1020\t1080\t+\t1",
                TestUtils.runGorPipe(q(SEG_CHAIN, SEG_INPUT, "-seg -tolerance 0")).split("\n")[1]);
    }

    @Test
    public void failOnThrowsOnMatchingStatusOnly() {
        // the first unmapped row of SNP is chr1:350
        try {
            TestUtils.runGorPipe(q(CHAIN, SNP, "-snp -overlap best -failon unmapped"));
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("LIFTOVER_ANNOTATE: status 'unmapped' at chr1:350"));
            Assert.assertEquals("LIFTOVER_ANNOTATE: status 'unmapped' at chr1:350", firstLine(e.getMessage()));
        }
        // a list: ambiguous is hit first (chr1:875) only when unmapped is not listed
        try {
            TestUtils.runGorPipe(q(CHAIN, SNP, "-snp -overlap best -failon ambiguous"));
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertEquals("LIFTOVER_ANNOTATE: status 'ambiguous' at chr1:875", firstLine(e.getMessage()));
        }
        // no row has a listed status: the query passes and returns every row
        String ok = TestUtils.runGorPipe(q(CHAIN_UNIQUE, SNP, "-snp -failon ambiguous,passthrough,unsupported_contig"));
        Assert.assertEquals(14, ok.split("\n").length);
        // segment statuses are accepted for -seg
        try {
            TestUtils.runGorPipe(q(CHAIN, SEG, "-seg -overlap best -failon inverted"));
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertEquals("LIFTOVER_ANNOTATE: status 'inverted' at chr1:250", firstLine(e.getMessage()));
        }
    }

    private static String firstLine(String message) {
        return message.split("\n")[0];
    }

    @Test
    public void customPrefix() throws IOException {
        String input = write("prefix_in.gor", lines(SNP_HEADER, snpRow("chr1", 150, "s01"), snpRow("chr1", 350, "dup")));
        Assert.assertEquals(lines(SNP_HEADER + "\thg38_status\thg38_chrom\thg38_pos\thg38_strand",
                snpRow("chr1", 150, "s01") + "\tmapped\tchr1\t1150\t+",
                snpRow("chr1", 350, "dup") + "\tunmapped\t\t0\t"),
                TestUtils.runGorPipe(q(CHAIN_UNIQUE, input, "-snp -prefix hg38_")));
        // the default prefix is free to be used by the input then
        String taken = write("prefix_taken.gor", lines(SNP_HEADER + "\tliftover_status", snpRow("chr1", 150, "s01") + "\tx"));
        Assert.assertEquals(lines(SNP_HEADER + "\tliftover_status\thg38_status\thg38_chrom\thg38_pos\thg38_strand",
                snpRow("chr1", 150, "s01") + "\tx\tmapped\tchr1\t1150\t+"),
                TestUtils.runGorPipe(q(CHAIN_UNIQUE, taken, "-snp -prefix hg38_")));
    }

    @Test
    public void customColumnsInGorMode() throws IOException {
        writeSegFixtures();
        String input = write("custom_cols.gor", lines("Chrom\tPos\tid\tc2\tp2\te2",
                "chr1\t1\tx\tchr1\t120\t180",
                "chr1\t2\ty\tchr1\t150\t260"));
        String expected = lines("Chrom\tPos\tid\tc2\tp2\te2\t" + SEG_COLS,
                "chr1\t1\tx\tchr1\t120\t180\tmapped\tchr1\t1020\t1080\t+\t1",
                "chr1\t2\ty\tchr1\t150\t260\tsize_changed\tchr1\t1050\t1150\t+\t2");
        Assert.assertEquals(expected, TestUtils.runGorPipe(q(SEG_CHAIN, input, "-seg -chrom c2 -pos p2 -end e2")));
        // columns by number (1-based)
        Assert.assertEquals(expected, TestUtils.runGorPipe(q(SEG_CHAIN, input, "-seg -chrom 4 -pos 5 -end 6")));
    }

    @Test
    public void norSegWithEnd() throws IOException {
        writeSegFixtures();
        String tsv = write("seg_coords.tsv", "#c\tp\te\nchr1\t120\t180\nchr1\t150\t260\n");
        Assert.assertEquals(lines("ChromNOR\tPosNOR\tc\tp\te\t" + SEG_COLS,
                "chrN\t0\tchr1\t120\t180\tmapped\tchr1\t1020\t1080\t+\t1",
                "chrN\t0\tchr1\t150\t260\tsize_changed\tchr1\t1050\t1150\t+\t2"),
                TestUtils.runGorPipe("nor " + tsv + " | liftover_annotate " + SEG_CHAIN
                        + " -seg -chrom c -pos p -end e"));
    }

    // Parse errors through a query

    private static String parseError(String input, String opts) {
        try {
            TestUtils.runGorPipe(q(CHAIN_UNIQUE, input, opts));
        } catch (GorParsingException e) {
            return e.getMessage();
        }
        Assert.fail("expected GorParsingException for " + opts);
        return null;
    }

    @Test
    public void varNeedsRefAndAltColumns() throws IOException {
        String noAlt = write("no_alt.gor", lines("Chrom\tPos\tRef", "chr1\t150\tA"));
        Assert.assertTrue(parseError(noAlt, "-var").contains("-var requires an ALT column (ALT/CALL/ALLELE or -alt)"));
        String noRef = write("no_ref.gor", lines("Chrom\tPos\tAlt", "chr1\t150\tA"));
        Assert.assertTrue(parseError(noRef, "-var").contains("-var requires a REF column (REF/REFERENCE or -ref)"));
        // -ref/-alt supply them
        String named = write("named_ra.gor", lines("Chrom\tPos\tr\ta", "chr1\t150\tA\tG"));
        Assert.assertEquals(lines("Chrom\tPos\tr\ta\t" + VAR_COLS, "chr1\t150\tA\tG\tmapped\tchr1\t1150\tA\tG\t+"),
                TestUtils.runGorPipe(q(CHAIN_UNIQUE, named, "-var -ref r -alt a")));
    }

    @Test
    public void optionsThatDoNothingInTheModeAreRejected() {
        Assert.assertTrue(parseError(SNP, "-snp -end Id").contains("-end is only valid with -seg or -seg1"));
        Assert.assertTrue(parseError(VAR, "-var -end Id").contains("-end is only valid with -seg or -seg1"));
        Assert.assertTrue(parseError(SNP, "-snp -ref Ref").contains("-ref is only valid with -var"));
        Assert.assertTrue(parseError(SEG, "-seg -alt Name").contains("-alt is only valid with -var"));
    }

    @Test
    public void segWithoutAnEndColumnIsRejected() throws IOException {
        String twoCols = write("two_cols.gor", lines("Chrom\tPos", "chr1\t150"));
        Assert.assertTrue(parseError(twoCols, "-seg").contains("-seg requires an end column"));
        Assert.assertTrue(parseError(twoCols, "-seg1").contains("-seg1 requires an end column"));
    }

    // Chain link retargeted inside one session

    /**
     * N6 check. The file reader resolves a link once per session, so repointing the link inside a session is
     * not seen by any read in that session, plain "gor chain.gor.link" included. The cached chain index must agree
     * with what the reader serves in that session (its unique id is that of the resolved target), and a new
     * session must see the new target.
     */
    @Test
    public void retargetedLinkAgreesWithTheReaderInSessionAndIsPickedUpByNewSessions() throws Exception {
        String chainA = write("retarget_a.gor", lines(CHAIN_HEADER, "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000"));
        String chainB = write("retarget_b.gor", lines(CHAIN_HEADER, "chr1\t100\t200\tchr1\t7100\t7200\t+\t1000"));
        String input = write("retarget_in.gor", lines(SNP_HEADER, snpRow("chr1", 150, "r1")));
        File link = new File(tmpdir.toFile(), "retarget_chain.gor.link");
        String annotate = q(link.getAbsolutePath(), input, "-snp -overlap best");
        String plain = "gor " + link.getAbsolutePath();

        PipeOptions options = new PipeOptions();
        options.parseOptions(new String[]{annotate});
        GorSession session = new TestSessionFactory(options, null, false, null, new String[]{"user_data"}).create();

        FileUtils.write(link, chainA + "\n", StandardCharsets.UTF_8);
        Assert.assertEquals(lines(SNP_HEADER + "\t" + SNP_COLS, snpRow("chr1", 150, "r1") + "\tmapped\tchr1\t1150\t+"),
                TestUtils.runGorPipe(new String[]{annotate}, () -> session));

        FileUtils.write(link, chainB + "\n", StandardCharsets.UTF_8);
        long later = System.currentTimeMillis() + 10_000;
        Assert.assertTrue(link.setLastModified(later));
        Assert.assertTrue(new File(chainB).setLastModified(later));

        // what this session's reader serves for the link now
        String qStart = TestUtils.runGorPipe(new String[]{plain}, () -> session).split("\n")[1].split("\t")[4];
        int expectedPos = Integer.parseInt(qStart) + 50;
        Assert.assertEquals(lines(SNP_HEADER + "\t" + SNP_COLS,
                snpRow("chr1", 150, "r1") + "\tmapped\tchr1\t" + expectedPos + "\t+"),
                TestUtils.runGorPipe(new String[]{annotate}, () -> session));

        // a new session sees chain B
        Assert.assertEquals(lines(SNP_HEADER + "\t" + SNP_COLS, snpRow("chr1", 150, "r1") + "\tmapped\tchr1\t7150\t+"),
                TestUtils.runGorPipe(annotate));
    }
}
