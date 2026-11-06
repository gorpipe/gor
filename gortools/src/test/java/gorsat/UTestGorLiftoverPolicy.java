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

import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
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
 * The contig policy and failure options of the relocating LIFTOVER (ENGKNOW-4191): -mapped, -passthrough and
 * -failon, with the same parsing, validation and messages as LIFTOVER_ANNOTATE, and the full engine status set in
 * {@code <build>_liftoverStatus}. Calls without these options must give exactly the output they gave before.
 */
public class UTestGorLiftoverPolicy {

    private static final String CHAIN_HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore";
    private static final String SNP_OUT = "Chrom\tPos\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus";
    private static final String VAR_OUT = "Chrom\tPos\tRef\tAlt\thgold_Chrom\thgold_Pos\thgold_qStrand"
            + "\thgold_liftoverStatus";
    private static final String SEG_OUT = "Chrom\tStart\tEnd\tName\thgold_Chrom\thgold_Start\thgold_End"
            + "\thgold_qStrand\thgold_liftoverStatus";
    private static final String POINT_STATUSES = "passthrough,unsupported_contig,ambiguous,unmapped,mapped";
    private static final String SPAN_STATUSES = "passthrough,unsupported_contig,ambiguous,unmapped,start_unmapped,"
            + "stop_unmapped,split_contig,inverted,mapped,mapped_across_gap,size_changed";

    private static Path tmpdir;
    private static String chain;
    private static String snp;
    private static String var;
    private static String seg;

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    private static String write(String name, String content) throws IOException {
        File f = new File(tmpdir.toFile(), name);
        FileUtils.write(f, content, StandardCharsets.UTF_8);
        return f.getAbsolutePath();
    }

    private static java.util.List<String> sortedLines(String s) {
        return java.util.Arrays.stream(s.split("\n")).sorted().collect(java.util.stream.Collectors.toList());
    }

    private static String run(String input, String chainFile, String opts) {
        return TestUtils.runGorPipe("gor " + input + " | liftover " + chainFile + " " + opts);
    }

    private static void assertParsingError(String opts, String expected) {
        assertParsingError(snp, opts, expected);
    }

    private static void assertParsingError(String input, String opts, String expected) {
        try {
            run(input, chain, opts);
            Assert.fail("expected GorParsingException for " + opts);
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(expected));
        }
    }

    @BeforeClass
    public static void setUp() throws IOException {
        GorPipeCommands.register();
        GorInputSources.register();
        tmpdir = Files.createTempDirectory("liftover-policy");
        tmpdir.toFile().deleteOnExit();

        // two adjacent blocks, a - strand block, a block to chr2, two equal-score overlapping blocks, and chrM
        chain = write("chain.gor", lines(CHAIN_HEADER,
                "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000",
                "chr1\t200\t300\tchr1\t1200\t1300\t+\t1000",
                "chr1\t400\t500\tchr1\t5000\t5100\t-\t900",
                "chr1\t600\t700\tchr2\t2100\t2200\t+\t800",
                "chr1\t800\t900\tchr1\t8800\t8900\t+\t500",
                "chr1\t850\t950\tchr1\t9850\t9950\t+\t500",
                "chrM\t300\t3100\tchrM\t298\t3098\t+\t100"));
        snp = write("snp.gor", lines("Chrom\tPos\tId",
                "chr1\t150\ta",
                "chr1\t350\tb",
                "chr1\t450\tc",
                "chr1\t650\td",
                "chr1\t870\te",
                "chrM\t1000\tf",
                "chrX\t10\tg"));
        var = write("var.gor", lines("Chrom\tPos\tRef\tAlt",
                "chr1\t150\tA\tG",
                "chr1\t450\tAC\tT",
                "chr1\t650\tC\tG",
                "chrM\t1000\tA\tG,T"));
        seg = write("seg.gor", lines("Chrom\tStart\tEnd\tName",
                "chr1\t120\t180\tin",
                "chr1\t150\t250\tgap",
                "chr1\t250\t450\tinv",
                "chr1\t650\t660\tother",
                "chrM\t1000\t1100\tm"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tmpdir.toFile());
    }

    // Unchanged default behaviour

    @Test
    public void defaultSnpUnchanged() {
        Assert.assertEquals(lines(SNP_OUT,
                "chr1\t0\tb\tchr1\t350\t\tunmapped",
                "chr1\t0\te\tchr1\t870\t\tambiguous",
                "chr1\t1150\ta\tchr1\t150\t+\tmapped",
                "chr1\t5051\tc\tchr1\t450\t-\tmapped",
                "chr2\t2150\td\tchr1\t650\t+\tmapped",
                "chrM\t998\tf\tchrM\t1000\t+\tmapped",
                "chrX\t0\tg\tchrX\t10\t\tunmapped"),
                run(snp, chain, "-snp"));
    }

    @Test
    public void defaultSegUnchanged() {
        // a segment no single block contains is unmapped at (source chrom, 0, 1), whatever the engine status
        Assert.assertEquals(lines(SEG_OUT,
                "chr1\t0\t1\tgap\tchr1\t150\t250\t\tunmapped",
                "chr1\t0\t1\tinv\tchr1\t250\t450\t\tunmapped",
                "chr1\t1120\t1180\tin\tchr1\t120\t180\t+\tmapped",
                "chr2\t2150\t2160\tother\tchr1\t650\t660\t+\tmapped",
                "chrM\t998\t1098\tm\tchrM\t1000\t1100\t+\tmapped"),
                run(seg, chain, "-seg"));
    }

    // -passthrough

    @Test
    public void passthroughSnpKeepsCoordinatesAndReportsFullStatuses() {
        Assert.assertEquals(lines(SNP_OUT,
                "chr1\t0\tb\tchr1\t350\t\tunmapped",
                "chr1\t0\te\tchr1\t870\t\tambiguous",
                "chr1\t1150\ta\tchr1\t150\t+\tmapped",
                "chr1\t5051\tc\tchr1\t450\t-\tmapped",
                "chr2\t2150\td\tchr1\t650\t+\tmapped",
                "chrM\t1000\tf\tchrM\t1000\t\tpassthrough",
                "chrX\t0\tg\tchrX\t10\t\tunmapped"),
                run(snp, chain, "-snp -passthrough chrM"));
    }

    @Test
    public void passthroughVarKeepsAlleles() {
        Assert.assertEquals(lines(VAR_OUT,
                "chr1\t1150\tA\tG\tchr1\t150\t+\tmapped",
                "chr1\t5050\tGT\tA\tchr1\t450\t-\tmapped",
                "chr2\t2150\tC\tG\tchr1\t650\t+\tmapped",
                "chrM\t1000\tA\tG,T\tchrM\t1000\t\tpassthrough"),
                run(var, chain, "-var -passthrough chrM"));
    }

    @Test
    public void passthroughSegKeepsInterval() {
        Assert.assertTrue(run(seg, chain, "-seg -passthrough chrM")
                .contains("chrM\t1000\t1100\tm\tchrM\t1000\t1100\t\tpassthrough\n"));
    }

    @Test
    public void passthroughDoesNotLoadTheChain() throws IOException {
        String unsorted = write("unsorted_chain.gor", lines(CHAIN_HEADER,
                "chr1\t300\t400\tchr1\t1300\t1400\t+\t1000",
                "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000"));
        String chrMOnly = write("chrm_only.gor", lines("Chrom\tPos\tId", "chrM\t100\tm1", "chrM\t200\tm2"));
        Assert.assertEquals(lines(SNP_OUT,
                "chrM\t100\tm1\tchrM\t100\t\tpassthrough",
                "chrM\t200\tm2\tchrM\t200\t\tpassthrough"),
                run(chrMOnly, unsorted, "-snp -passthrough chrM"));

        String withChr1 = write("chr1_and_chrm.gor", lines("Chrom\tPos\tId", "chr1\t150\ts", "chrM\t100\tm1"));
        try {
            run(withChr1, unsorted, "-snp -passthrough chrM");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("not sorted by tStart"));
        }
    }

    @Test
    public void passthroughNoneReportsFullSegStatuses() {
        // gap and inv are classified by their endpoints; mapped_across_gap has a target and is relocated
        Assert.assertEquals(lines(SEG_OUT,
                "chr1\t0\t1\tinv\tchr1\t250\t450\t\tinverted",
                "chr1\t1120\t1180\tin\tchr1\t120\t180\t+\tmapped",
                "chr1\t1150\t1250\tgap\tchr1\t150\t250\t+\tmapped_across_gap",
                "chr2\t2150\t2160\tother\tchr1\t650\t660\t+\tmapped",
                "chrM\t998\t1098\tm\tchrM\t1000\t1100\t+\tmapped"),
                run(seg, chain, "-seg -passthrough none"));
    }

    @Test
    public void passthroughBamKeepsReadsUnchanged() {
        String bam = "../tests/data/bvl_min/bam/BVL_INDEX_SLC52A2.bam";
        String input = TestUtils.runGorPipe("gor " + bam);
        int n = input.split("\n", 2)[0].split("\t").length;
        String lifted = TestUtils.runGorPipe("gor " + bam + " | liftover " + chain
                + " -bam -passthrough chr8 | where hgold_liftoverStatus = 'passthrough' and hgold_qStrand = ''"
                + " and Pos = hgold_Pos and End = hgold_End | select 1-" + n);
        Assert.assertTrue(input.split("\n").length > 1);
        // rows at the same position may come out of the blocking sort in another order
        Assert.assertEquals(sortedLines(input), sortedLines(lifted));
        assertParsingError(bam, "-bam -failon passthrough,mapped_across_gap,bogus",
                "Invalid -failon status 'bogus' for -bam, valid: " + SPAN_STATUSES);
    }

    // -mapped

    @Test
    public void mappedMarksOtherSourceAndTargetContigsUnsupported() {
        Assert.assertEquals(lines(SNP_OUT,
                "chr1\t0\tb\tchr1\t350\t\tunmapped",
                "chr1\t0\td\tchr1\t650\t\tunsupported_contig",
                "chr1\t0\te\tchr1\t870\t\tambiguous",
                "chr1\t1150\ta\tchr1\t150\t+\tmapped",
                "chr1\t5051\tc\tchr1\t450\t-\tmapped",
                "chrM\t1000\tf\tchrM\t1000\t\tpassthrough",
                "chrX\t0\tg\tchrX\t10\t\tunsupported_contig"),
                run(snp, chain, "-snp -mapped chr1 -passthrough chrM"));
    }

    @Test
    public void mappedSeg() {
        Assert.assertEquals(lines(SEG_OUT,
                "chr1\t0\t1\tinv\tchr1\t250\t450\t\tinverted",
                "chr1\t0\t1\tother\tchr1\t650\t660\t\tunsupported_contig",
                "chr1\t1120\t1180\tin\tchr1\t120\t180\t+\tmapped",
                "chr1\t1150\t1250\tgap\tchr1\t150\t250\t+\tmapped_across_gap",
                "chrM\t0\t1\tm\tchrM\t1000\t1100\t\tunsupported_contig"),
                run(seg, chain, "-seg -mapped chr1"));
    }

    // -failon

    @Test
    public void failonStopsOnFirstRowWithStatus() {
        try {
            run(snp, chain, "-snp -failon unmapped");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("LIFTOVER: status 'unmapped' at chr1:350"));
        }
        try {
            run(snp, chain, "-snp -mapped chr1,chrM -failon unsupported_contig");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(),
                    e.getMessage().contains("LIFTOVER: status 'unsupported_contig' at chr1:650"));
        }
        try {
            run(seg, chain, "-seg -failon inverted");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("LIFTOVER: status 'inverted' at chr1:250"));
        }
    }

    @Test
    public void failonPassesWhenNoRowHasStatus() {
        Assert.assertEquals(8, run(snp, chain, "-snp -failon passthrough,unsupported_contig")
                .split("\n").length);
    }

    @Test
    public void failonStatusesAreValidatedPerMode() {
        assertParsingError("-snp -failon size_changed",
                "Invalid -failon status 'size_changed' for -snp, valid: " + POINT_STATUSES);
        assertParsingError(var, "-var -failon inverted",
                "Invalid -failon status 'inverted' for -var, valid: " + POINT_STATUSES);
        assertParsingError("-failon bogus", "Invalid -failon status 'bogus' for -seg, valid: " + SPAN_STATUSES);
    }

    // Parsing shared with LIFTOVER_ANNOTATE

    @Test
    public void contigListErrors() {
        assertParsingError("-snp -passthrough chrM -mapped chr1,chrM", "Contig chrM is in both -passthrough and -mapped");
        assertParsingError("-snp -mapped chr1,", "Empty contig in -mapped list 'chr1,'");
        assertParsingError("-snp -passthrough chrM,chrM", "Duplicate contig chrM in -passthrough");
    }

    @Test
    public void repeatedOptionsAreErrors() {
        assertParsingError("-snp -failon unmapped -failon ambiguous", "-failon given more than once");
        assertParsingError("-snp -mapped chr1 -mapped chr2", "-mapped given more than once");
        assertParsingError("-snp -passthrough chrM -passthrough chrX", "-passthrough given more than once");
    }

    @Test
    public void allCannotBeCombinedWithPolicyOptions() {
        for (String opt : new String[]{"-mapped chr1", "-passthrough chrM", "-failon unmapped"}) {
            assertParsingError("-snp -all " + opt, "-all cannot be combined with -mapped, -passthrough or -failon");
        }
    }
}
