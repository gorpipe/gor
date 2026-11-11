package gorsat.liftover;

import gorsat.TestUtils;
import gorsat.process.GorInputSources;
import gorsat.process.GorPipeCommands;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * CG-805: a position inside a double-sided gap of equal length between two consecutive blocks of the same chain
 * (same qChrom, strand and score) is lifted by its offset from the preceding block. UCSC chains encode runs of
 * substituted bases this way (mostly 1/1), and a chain file that has not been merged left them unmapped.
 * Unequal gaps (real indels), gaps between different chains and gaps that hold another block stay unmapped.
 */
public class UTestLiftEqualGaps {

    private static final String HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore";

    /** + strand chain: a 1/1 gap at 200-201, a 5/5 gap at 300-305, then a 10/5 gap (an indel) at 400-410. */
    private static final List<ChainBlock> CHR1 = List.of(
            new ChainBlock(100, 200, "chr1", 1000, 1100, "+", 1000),
            new ChainBlock(201, 300, "chr1", 1101, 1200, "+", 1000),
            new ChainBlock(305, 400, "chr1", 1205, 1300, "+", 1000),
            new ChainBlock(410, 500, "chr1", 1305, 1395, "+", 1000),
            // 1/1 gaps between blocks that differ in score, strand or target contig, or whose target goes backwards
            new ChainBlock(600, 700, "chr2", 5000, 5100, "+", 50),
            new ChainBlock(701, 800, "chr2", 5101, 5200, "+", 60),
            new ChainBlock(810, 900, "chr2", 6000, 6090, "+", 70),
            new ChainBlock(901, 1000, "chr2", 5900, 5999, "-", 70),
            new ChainBlock(1010, 1100, "chr2", 7000, 7090, "+", 80),
            new ChainBlock(1101, 1200, "chr3", 7091, 7190, "+", 80),
            new ChainBlock(1210, 1300, "chr2", 8000, 8090, "+", 90),
            new ChainBlock(1301, 1400, "chr2", 7000, 7099, "+", 90),
            // a 2/2 gap at 1600-1602 of chain 9 that holds a block of another chain
            new ChainBlock(1500, 1600, "chr5", 0, 100, "+", 9),
            new ChainBlock(1600, 1601, "chr6", 50, 51, "+", 1),
            new ChainBlock(1602, 1700, "chr5", 102, 200, "+", 9));

    /** - strand chain: a 1/1 gap at 200-201 and a 3/3 gap at 301-304. */
    private static final List<ChainBlock> CHR3 = List.of(
            new ChainBlock(100, 200, "chr3", 5000, 5100, "-", 7),
            new ChainBlock(201, 301, "chr3", 4899, 4999, "-", 7),
            new ChainBlock(304, 404, "chr3", 4796, 4896, "-", 7));

    private static final ContigPolicy NO_POLICY = new ContigPolicy(null, null);

    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    private static Supplier<ChainSource> chain;
    private static String chainFile;

    @BeforeClass
    public static void setUp() throws IOException {
        GorPipeCommands.register();
        GorInputSources.register();
        Map<String, ContigIndex> contigs = Map.of(
                "chr1", ContigIndex.of("chr1", OverlapPolicy.ERROR, CHR1),
                "chr3", ContigIndex.of("chr3", OverlapPolicy.ERROR, CHR3));
        ChainSource source = c -> contigs.getOrDefault(c, ContigIndex.empty(c));
        chain = () -> source;
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (List<ChainBlock> blocks : Arrays.asList(CHR1, CHR3)) {
            String contig = blocks == CHR1 ? "chr1" : "chr3";
            for (ChainBlock b : blocks) {
                sb.append(contig).append('\t').append(b.tStart()).append('\t').append(b.tEnd()).append('\t')
                        .append(b.qChrom()).append('\t').append(b.qStart()).append('\t').append(b.qEnd()).append('\t')
                        .append(b.strand()).append('\t').append((long) b.score()).append('\n');
            }
        }
        chainFile = write("chain.gor", sb.toString());
    }

    private static String write(String name, String content) throws IOException {
        Path p = TMP.getRoot().toPath().resolve(name);
        Files.writeString(p, content);
        return p.toString();
    }

    private static String snp(String chrom, int pos) {
        LiftEngine.PointResult r = LiftEngine.liftSnp(chain, chrom, pos, NO_POLICY);
        return r.status() + " " + r.chrom() + ":" + r.pos() + r.strand();
    }

    private static final String UNMAPPED = "unmapped :0";

    @Test
    public void snpInOneBaseGapMapsByOffsetOnPlusStrand() {
        Assert.assertEquals("mapped chr1:1100+", snp("chr1", 200));
        Assert.assertEquals("mapped chr1:1101+", snp("chr1", 201));
        Assert.assertEquals("mapped chr1:1102+", snp("chr1", 202));
    }

    @Test
    public void snpInLongerEqualGapMapsByOffsetOnPlusStrand() {
        for (int p = 301; p <= 305; p++) {
            Assert.assertEquals("mapped chr1:" + (900 + p) + "+", snp("chr1", p));
        }
        Assert.assertEquals("mapped chr1:1206+", snp("chr1", 306));
    }

    @Test
    public void snpInEqualGapsMapsByOffsetOnMinusStrand() {
        Assert.assertEquals("mapped chr3:5001-", snp("chr3", 200));
        Assert.assertEquals("mapped chr3:5000-", snp("chr3", 201));
        Assert.assertEquals("mapped chr3:4999-", snp("chr3", 202));
        Assert.assertEquals("mapped chr3:4900-", snp("chr3", 301));
        Assert.assertEquals("mapped chr3:4899-", snp("chr3", 302));
        Assert.assertEquals("mapped chr3:4898-", snp("chr3", 303));
        Assert.assertEquals("mapped chr3:4897-", snp("chr3", 304));
        Assert.assertEquals("mapped chr3:4896-", snp("chr3", 305));
    }

    @Test
    public void unequalGapStaysUnmapped() {
        for (int p = 401; p <= 410; p++) {
            Assert.assertEquals("pos " + p, UNMAPPED, snp("chr1", p));
        }
    }

    @Test
    public void gapBetweenDifferentChainsStaysUnmapped() {
        Assert.assertEquals("score differs", UNMAPPED, snp("chr1", 701));
        Assert.assertEquals("strand differs", UNMAPPED, snp("chr1", 901));
        Assert.assertEquals("target contig differs", UNMAPPED, snp("chr1", 1101));
        Assert.assertEquals("target goes backwards", UNMAPPED, snp("chr1", 1301));
    }

    @Test
    public void aGapHoldingABlockOfAnotherChainIsNotFilled() {
        // the other chain's block maps; the rest of the gap is not a plain run of substitutions and stays unmapped
        Assert.assertEquals("mapped chr6:51+", snp("chr1", 1601));
        Assert.assertEquals(UNMAPPED, snp("chr1", 1602));
        Assert.assertEquals("mapped chr5:103+", snp("chr1", 1603));
    }

    @Test
    public void varInGapMapsAndVarAcrossGapAndBlockDoesNot() {
        LiftEngine.PointResult in = LiftEngine.liftVar(chain, "chr1", 303, 2, NO_POLICY);
        Assert.assertEquals("mapped chr1:1203+", in.status() + " " + in.chrom() + ":" + in.pos() + in.strand());
        // a REF that runs from the gap into the next block is not contained in one block, as across two blocks today
        Assert.assertEquals(LiftEngine.UNMAPPED, LiftEngine.liftVar(chain, "chr1", 305, 2, NO_POLICY).status());
    }

    private static String seg(String chrom, int s1, int e1) {
        LiftEngine.SpanResult r = LiftEngine.classifySpan(chain, chrom, s1, e1, NO_POLICY, 0.05);
        return r.status() + " " + r.chrom() + ":" + r.start1() + "-" + r.stop1() + r.strand();
    }

    @Test
    public void segmentsWithAnEndpointInAGapAreClassifiedThroughIt() {
        // start in the 1/1 gap: lifted by offset instead of start_unmapped
        Assert.assertEquals("mapped_across_gap chr1:1101-1150+", seg("chr1", 201, 250));
        // stop in the 5/5 gap
        Assert.assertEquals("mapped_across_gap chr1:1150-1203+", seg("chr1", 250, 303));
        // inside the gap
        Assert.assertEquals("mapped chr1:1202-1204+", seg("chr1", 302, 304));
        // across blocks and gaps of one chain, as across abutting blocks
        Assert.assertEquals("mapped_across_gap chr1:1050-1250+", seg("chr1", 150, 350));
        // minus strand, start in the 3/3 gap
        Assert.assertEquals("mapped_across_gap chr3:4847-4898-", seg("chr3", 303, 354));
        // an endpoint in an unequal gap is still unmapped
        Assert.assertEquals(LiftEngine.START_UNMAPPED + " :0-0", seg("chr1", 405, 450));
    }

    @Test
    public void chainWalkCrossesAbuttingBlocksAndEqualGapsOnly() {
        ContigIndex ci = ContigIndex.of("chr9", OverlapPolicy.ERROR, List.of(
                new ChainBlock(0, 10, "chr9", 100, 110, "+", 5),
                new ChainBlock(10, 20, "chr9", 110, 120, "+", 5),
                new ChainBlock(21, 30, "chr9", 121, 130, "+", 5),
                new ChainBlock(40, 50, "chr9", 135, 145, "+", 5)));
        Assert.assertEquals(4, ci.size());
        Assert.assertEquals(1, ci.gapCount());
        int gap = ci.size();
        Assert.assertTrue(ci.isGap(gap));
        Assert.assertEquals(20, ci.tStart(gap));
        Assert.assertEquals(21, ci.tEnd(gap));
        Assert.assertEquals(120, ci.qStart(gap));
        // three real blocks and the 1/1 gap form one run; the 10/5 gap breaks it
        Assert.assertEquals(1, ci.nextInChain(0));
        Assert.assertEquals(gap, ci.nextInChain(1));
        Assert.assertEquals(2, ci.nextInChain(gap));
        Assert.assertEquals(-1, ci.nextInChain(2));
        Assert.assertEquals(-1, ci.prevInChain(3));
        Assert.assertEquals(gap, ci.prevInChain(2));
        Assert.assertEquals(1, ci.prevInChain(gap));
        Assert.assertEquals(0, ci.prevInChain(1));
        Assert.assertEquals(-1, ci.prevInChain(0));
        Assert.assertEquals(gap, ci.findPoint(21));
        // gap blocks are not chain blocks of a segment
        Assert.assertEquals(0, ci.countBlocks(21, 21));
    }

    @Test
    public void liftoverOperatorMapsGapPositions() throws IOException {
        String input = write("snp.gor", "Chrom\tPos\tId\nchr1\t201\ta\nchr1\t303\tb\nchr1\t405\tc\nchr3\t302\td\n");
        String out = TestUtils.runGorPipe("gor " + input + " | liftover " + chainFile + " -snp"
                + " | where hgold_liftoverStatus = 'mapped'");
        Assert.assertEquals("Chrom\tPos\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus\n"
                + "chr1\t1101\ta\tchr1\t201\t+\tmapped\n"
                + "chr1\t1203\tb\tchr1\t303\t+\tmapped\n"
                + "chr3\t4899\td\tchr3\t302\t-\tmapped\n", out);
    }

    @Test
    public void nestedQueryChainMapsGapPositions() throws IOException {
        String input = write("snp_nested.gor", "Chrom\tPos\tId\nchr1\t201\ta\nchr3\t302\td\n");
        String out = TestUtils.runGorPipe("gor " + input + " | liftover_annotate <(gor " + chainFile + ") -snp");
        Assert.assertEquals("Chrom\tPos\tId\tliftover_status\tliftover_chrom\tliftover_pos\tliftover_strand\n"
                + "chr1\t201\ta\tmapped\tchr1\t1101\t+\n"
                + "chr3\t302\td\tmapped\tchr3\t4899\t-\n", out);
    }

    @Test
    public void liftoverSegMapsASegmentStartingInAGap() throws IOException {
        String input = write("seg.gor", "Chrom\tStart\tEnd\tName\nchr1\t200\t250\ts\n");
        String out = TestUtils.runGorPipe("gor " + input + " | liftover_annotate " + chainFile + " -seg");
        // [200,250): first base 201 is the 1/1 gap base, last base 250 is in the next block
        Assert.assertTrue(out, out.contains("chr1\t200\t250\ts\tmapped_across_gap\tchr1\t1100\t1150\t+"));
    }

    @Test
    public void liftoverAnnotateOperatorMapsGapPositions() throws IOException {
        String input = write("snp_annotate.gor",
                "Chrom\tPos\tId\nchr1\t201\ta\nchr1\t303\tb\nchr1\t405\tc\nchr3\t302\td\n");
        String out = TestUtils.runGorPipe("gor " + input + " | liftover_annotate " + chainFile + " -snp");
        Assert.assertEquals("Chrom\tPos\tId\tliftover_status\tliftover_chrom\tliftover_pos\tliftover_strand\n"
                + "chr1\t201\ta\tmapped\tchr1\t1101\t+\n"
                + "chr1\t303\tb\tmapped\tchr1\t1203\t+\n"
                + "chr1\t405\tc\tunmapped\t\t0\t\n"
                + "chr3\t302\td\tmapped\tchr3\t4899\t-\n", out);
    }
}
