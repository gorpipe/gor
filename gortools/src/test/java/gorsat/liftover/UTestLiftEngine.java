package gorsat.liftover;

import gorsat.liftover.LiftEngine.Endpoint;
import gorsat.liftover.LiftEngine.EndpointLift;
import gorsat.liftover.LiftEngine.PointResult;
import gorsat.liftover.LiftEngine.ScoredTarget;
import gorsat.liftover.LiftEngine.SpanResult;
import gorsat.liftover.LiftEngine.Target;
import gorsat.liftover.LiftFixtures.NaiveLift;
import gorsat.liftover.LiftFixtures.SpanCase;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import static gorsat.liftover.LiftEngine.AMBIGUOUS;
import static gorsat.liftover.LiftEngine.INVERTED;
import static gorsat.liftover.LiftEngine.MAPPED;
import static gorsat.liftover.LiftEngine.MAPPED_ACROSS_GAP;
import static gorsat.liftover.LiftEngine.NO_ENDPOINT;
import static gorsat.liftover.LiftEngine.PASSTHROUGH;
import static gorsat.liftover.LiftEngine.SIZE_CHANGED;
import static gorsat.liftover.LiftEngine.SPLIT_CONTIG;
import static gorsat.liftover.LiftEngine.START_UNMAPPED;
import static gorsat.liftover.LiftEngine.STOP_UNMAPPED;
import static gorsat.liftover.LiftEngine.UNMAPPED;
import static gorsat.liftover.LiftEngine.UNSUPPORTED_CONTIG;
import static gorsat.liftover.LiftFixtures.ENGINE_CHAIN;
import static gorsat.liftover.LiftFixtures.MAPPED_CHR1_CHR2;
import static gorsat.liftover.LiftFixtures.NO_POLICY;
import static gorsat.liftover.LiftFixtures.PASSTHROUGH_CHRM;
import static gorsat.liftover.LiftFixtures.THROWING_CHAIN;

public class UTestLiftEngine {

    private static final double TOL = 0.05;
    private static final Supplier<ChainSource> ENGINE = () -> ENGINE_CHAIN;
    /** A supplier that must never be called: the policy checks come before any chain access. */
    private static final Supplier<ChainSource> NEVER = () -> {
        throw new IllegalStateException("chain supplier called");
    };

    // ---- helpers ----

    private static PointResult p(String status, String chrom, int pos, String strand) {
        return new PointResult(status, chrom, pos, strand);
    }

    private static PointResult p(String status) {
        return new PointResult(status, "", 0, "");
    }

    private static PointResult snp(String chrom, int pos) {
        return LiftEngine.liftSnp(ENGINE, chrom, pos, NO_POLICY);
    }

    private static PointResult var(String chrom, int pos, int refLen) {
        return LiftEngine.liftVar(ENGINE, chrom, pos, refLen, NO_POLICY);
    }

    private static Endpoint ep(String chrom, int pos, String strand) {
        return new Endpoint(chrom, pos, strand);
    }

    private static double r(int tgt, int src) {
        return (double) tgt / src;
    }

    /** A row with a target. */
    private static SpanResult hit(String status, String chrom, int start1, int stop1, String strand, int n, int src,
                                  int tgt, double ratio, Endpoint s, Endpoint e) {
        return new SpanResult(status, chrom, start1, stop1, strand, n, src, tgt, ratio, s, e);
    }

    /** A row without a target: "", 0, 0, "". */
    private static SpanResult none(String status, int n, int src, int tgt, double ratio, Endpoint s, Endpoint e) {
        return new SpanResult(status, "", 0, 0, "", n, src, tgt, ratio, s, e);
    }

    private static SpanResult classify(SpanCase c) {
        return LiftEngine.classifySpan(c::chain, c.chrom(), c.s1(), c.e1(), c.policy(), TOL);
    }

    private static SpanResult classify(String chrom, int s1, int e1, ContigPolicy policy) {
        return LiftEngine.classifySpan(ENGINE, chrom, s1, e1, policy, TOL);
    }

    /** Expected classifySpan result for each worked case (brief rows 1-27, 29-36). */
    private static final Map<Integer, SpanResult> EXPECTED = new TreeMap<>();

    static {
        Endpoint no = NO_ENDPOINT;
        EXPECTED.put(1, hit(MAPPED, "chr1", 1051, 1080, "+", 1, 29, 29, 1.0,
                ep("chr1", 1051, "+"), ep("chr1", 1080, "+")));
        EXPECTED.put(2, hit(MAPPED_ACROSS_GAP, "chr1", 1051, 1150, "+", 2, 99, 99, 1.0,
                ep("chr1", 1051, "+"), ep("chr1", 1150, "+")));
        EXPECTED.put(3, hit(SIZE_CHANGED, "chr1", 1151, 1300, "+", 2, 99, 149, r(149, 99),
                ep("chr1", 1151, "+"), ep("chr1", 1300, "+")));
        EXPECTED.put(4, hit(SIZE_CHANGED, "chr1", 30051, 30156, "+", 2, 100, 105, r(105, 100),
                ep("chr1", 30051, "+"), ep("chr1", 30156, "+")));
        EXPECTED.put(5, hit(MAPPED_ACROSS_GAP, "chr1", 30156, 30260, "+", 2, 100, 104, r(104, 100),
                ep("chr1", 30156, "+"), ep("chr1", 30260, "+")));
        EXPECTED.put(6, hit(SIZE_CHANGED, "chr1", 40051, 40146, "+", 2, 100, 95, r(95, 100),
                ep("chr1", 40051, "+"), ep("chr1", 40146, "+")));
        EXPECTED.put(7, hit(MAPPED_ACROSS_GAP, "chr1", 41051, 41147, "+", 2, 100, 96, r(96, 100),
                ep("chr1", 41051, "+"), ep("chr1", 41147, "+")));
        EXPECTED.put(8, hit(MAPPED, "chr1", 5041, 5090, "-", 1, 49, 49, 1.0,
                ep("chr1", 5090, "-"), ep("chr1", 5041, "-")));
        EXPECTED.put(9, hit(MAPPED_ACROSS_GAP, "chr1", 49950, 50050, "-", 2, 100, 100, 1.0,
                ep("chr1", 50050, "-"), ep("chr1", 49950, "-")));
        EXPECTED.put(10, none(UNMAPPED, 0, 99, 0, 0.0, no, no));
        EXPECTED.put(11, none(UNMAPPED, 1, 200, 0, 0.0, no, no));
        EXPECTED.put(12, none(STOP_UNMAPPED, 1, 70, 0, 0.0, ep("chr1", 1330, "+"), no));
        EXPECTED.put(13, none(START_UNMAPPED, 1, 70, 0, 0.0, no, ep("chr1", 5081, "-")));
        EXPECTED.put(14, none(SPLIT_CONTIG, 2, 99, 0, 0.0, ep("chr1", 5050, "-"), ep("chr2", 150, "+")));
        EXPECTED.put(15, none(UNSUPPORTED_CONTIG, 1, 39, 39, 1.0,
                ep("chrUn_x", 11, "+"), ep("chrUn_x", 50, "+")));
        EXPECTED.put(16, none(INVERTED, 2, 100, 999, r(999, 100), ep("chr1", 9050, "-"), ep("chr1", 8051, "+")));
        EXPECTED.put(17, none(INVERTED, 2, 100, 1000, r(1000, 100),
                ep("chr1", 20051, "+"), ep("chr1", 19051, "+")));
        EXPECTED.put(18, hit(MAPPED, "chr1", 1050, 1050, "+", 1, 0, 0, 1.0,
                ep("chr1", 1050, "+"), ep("chr1", 1050, "+")));
        EXPECTED.put(19, hit(MAPPED, "chr1", 5051, 5051, "-", 1, 0, 0, 1.0,
                ep("chr1", 5051, "-"), ep("chr1", 5051, "-")));
        EXPECTED.put(20, none(UNMAPPED, 0, 0, 0, 0.0, no, no));
        // contained in B6: diagnostics through B6, targetSize = sourceSize = 0, sizeRatio 1.0 (fix round 1 ruling)
        EXPECTED.put(21, none(UNSUPPORTED_CONTIG, 1, 0, 0, 1.0, ep("chrUn_x", 50, "+"), ep("chrUn_x", 50, "+")));
        EXPECTED.put(22, hit(PASSTHROUGH, "chrM", 100, 200, "", 0, 100, 100, 1.0, no, no));
        EXPECTED.put(23, none(UNSUPPORTED_CONTIG, 0, 100, 0, 0.0, no, no));
        EXPECTED.put(24, none(AMBIGUOUS, 2, 9, 0, 0.0, no, no));
        EXPECTED.put(25, none(AMBIGUOUS, 2, 100, 0, 0.0, no, no));
        EXPECTED.put(26, hit(MAPPED, "chr4", 1020, 1040, "+", 1, 20, 20, 1.0,
                ep("chr4", 1020, "+"), ep("chr4", 1040, "+")));
        EXPECTED.put(27, hit(MAPPED, "chr4", 5010, 5020, "+", 2, 10, 10, 1.0,
                ep("chr4", 5010, "+"), ep("chr4", 5020, "+")));
        EXPECTED.put(29, none(SPLIT_CONTIG, 2, 199, 0, 0.0, ep("chrUn_x", 51, "+"), ep("chr1", 9051, "-")));
        EXPECTED.put(30, none(UNSUPPORTED_CONTIG, 2, 100, 200, 2.0,
                ep("chrUn_y", 251, "+"), ep("chrUn_y", 51, "+")));
        EXPECTED.put(31, hit(MAPPED, "chr1", 1051, 1050, "+", 1, 0, 0, 1.0, no, no));
        EXPECTED.put(32, none(UNMAPPED, 0, 0, 0, 0.0, no, no));
        EXPECTED.put(33, none(UNMAPPED, 0, 0, 0, 0.0, no, no));
        EXPECTED.put(34, hit(MAPPED, "chr1", 5051, 5050, "-", 1, 0, 0, 1.0, no, no));
        EXPECTED.put(35, none(AMBIGUOUS, 2, 0, 0, 0.0, no, no));
        EXPECTED.put(36, hit(MAPPED, "chr4", 5025, 5024, "+", 2, 0, 0, 1.0, no, no));
    }

    // ---- LiftMode, ContigPolicy, constants ----

    @Test
    public void liftModeFlagsAndStatuses() {
        Assert.assertEquals("-snp", LiftMode.SNP.flag());
        Assert.assertEquals("-var", LiftMode.VAR.flag());
        Assert.assertEquals("-seg", LiftMode.SEG.flag());
        Assert.assertEquals("-seg1", LiftMode.SEG1.flag());
        List<String> point = List.of("passthrough", "unsupported_contig", "ambiguous", "unmapped", "mapped");
        List<String> span = List.of("passthrough", "unsupported_contig", "ambiguous", "unmapped", "start_unmapped",
                "stop_unmapped", "split_contig", "inverted", "mapped", "mapped_across_gap", "size_changed");
        Assert.assertEquals(point, LiftMode.SNP.statuses());
        Assert.assertEquals(point, LiftMode.VAR.statuses());
        Assert.assertEquals(span, LiftMode.SEG.statuses());
        Assert.assertEquals(span, LiftMode.SEG1.statuses());
        Assert.assertEquals(List.of(PASSTHROUGH, UNSUPPORTED_CONTIG, AMBIGUOUS, UNMAPPED, START_UNMAPPED,
                STOP_UNMAPPED, SPLIT_CONTIG, INVERTED, MAPPED, MAPPED_ACROSS_GAP, SIZE_CHANGED), span);
        Assert.assertEquals(Set.of("mapped", "mapped_across_gap", "size_changed", "passthrough"),
                LiftEngine.HAS_TARGET);
    }

    @Test
    public void contigPolicy() {
        Assert.assertTrue(PASSTHROUGH_CHRM.isPassthrough("chrM"));
        Assert.assertFalse(PASSTHROUGH_CHRM.isPassthrough("chrm"));
        Assert.assertTrue(PASSTHROUGH_CHRM.allowsTarget("anything"));
        Assert.assertTrue(MAPPED_CHR1_CHR2.allowsTarget("chr2"));
        Assert.assertFalse(MAPPED_CHR1_CHR2.allowsTarget("Chr2"));
        Assert.assertFalse(MAPPED_CHR1_CHR2.allowsTarget("chrUn_x"));
        Assert.assertFalse(MAPPED_CHR1_CHR2.isPassthrough("chr1"));
        ContigPolicy nulls = new ContigPolicy(null, null);
        Assert.assertFalse(nulls.isPassthrough("chrM"));
        Assert.assertTrue(nulls.allowsTarget("chrM"));
        Assert.assertNull(nulls.mapped());
        Assert.assertFalse(new ContigPolicy(Set.of(), Set.of()).allowsTarget("chr1"));
    }

    // ---- liftAt / liftEndpoint ----

    @Test
    public void liftAtIsTheOneFormula() {
        ContigIndex chr1 = ENGINE_CHAIN.contig("chr1");
        // B1 + [100,200) -> chr1 1000
        Assert.assertEquals(new Target("chr1", 1050, 1080, "+"), LiftEngine.liftAt(chr1, 0, 150, 180));
        Assert.assertEquals(new Target("chr1", 1050, 1050, "+"), LiftEngine.liftAt(chr1, 0, 150, 150));
        // B4 - [500,600) -> chr1 5000
        Assert.assertEquals(new Target("chr1", 5040, 5090, "-"), LiftEngine.liftAt(chr1, 3, 510, 560));
        Assert.assertEquals(new Target("chr1", 5050, 5050, "-"), LiftEngine.liftAt(chr1, 3, 550, 550));
        Assert.assertEquals(new Target("chr1", 5099, 5100, "-"), LiftEngine.liftAt(chr1, 3, 500, 501));
        // B5 + [600,700) -> chr2 100
        Assert.assertEquals(new Target("chr2", 100, 101, "+"), LiftEngine.liftAt(chr1, 4, 600, 601));
    }

    @Test
    public void liftEndpointIsTheRawLookup() {
        Assert.assertEquals(new EndpointLift(0, ep("chr1", 1001, "+")),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr1", 101));
        Assert.assertEquals(new EndpointLift(3, ep("chr1", 5100, "-")),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr1", 501));
        Assert.assertEquals(new EndpointLift(ContigIndex.UNMAPPED, NO_ENDPOINT),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr1", 100));
        Assert.assertEquals(new EndpointLift(ContigIndex.AMBIGUOUS, NO_ENDPOINT),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr5", 150));
        Assert.assertEquals(new EndpointLift(1, ep("chr4", 5025, "+")),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr4", 175));
        // no ContigPolicy: chrUn_x is returned as is
        Assert.assertEquals(new EndpointLift(5, ep("chrUn_x", 50, "+")),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr1", 850));
        Assert.assertEquals(new EndpointLift(ContigIndex.UNMAPPED, NO_ENDPOINT),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr9", 100));
        Assert.assertEquals(new Endpoint("", 0, ""), NO_ENDPOINT);
    }

    // ---- snp ----

    @Test
    public void snp() {
        Assert.assertEquals(p(UNMAPPED), snp("chr1", 100));
        Assert.assertEquals(p(MAPPED, "chr1", 1001, "+"), snp("chr1", 101));
        Assert.assertEquals(p(MAPPED, "chr1", 1100, "+"), snp("chr1", 200));
        Assert.assertEquals(p(MAPPED, "chr1", 1101, "+"), snp("chr1", 201));
        Assert.assertEquals(p(MAPPED, "chr1", 1350, "+"), snp("chr1", 400));
        Assert.assertEquals(p(UNMAPPED), snp("chr1", 401));
        Assert.assertEquals(p(MAPPED, "chr1", 5100, "-"), snp("chr1", 501));
        Assert.assertEquals(p(MAPPED, "chr1", 5001, "-"), snp("chr1", 600));
        Assert.assertEquals(p(MAPPED, "chr2", 101, "+"), snp("chr1", 601));
        Assert.assertEquals(p(MAPPED, "chr4", 1020, "+"), snp("chr4", 120));
        Assert.assertEquals(p(MAPPED, "chr4", 5025, "+"), snp("chr4", 175));
        Assert.assertEquals(p(AMBIGUOUS), snp("chr5", 150));
        Assert.assertEquals(p(UNMAPPED), snp("chr9", 100));
    }

    @Test
    public void snpContigPolicy() {
        Assert.assertEquals(p(UNSUPPORTED_CONTIG), LiftEngine.liftSnp(ENGINE, "chr1", 850, MAPPED_CHR1_CHR2));
        Assert.assertEquals(p(MAPPED, "chrUn_x", 50, "+"), snp("chr1", 850));
        Assert.assertEquals(p(MAPPED, "chr1", 1001, "+"), LiftEngine.liftSnp(ENGINE, "chr1", 101, MAPPED_CHR1_CHR2));
        // source contig outside -mapped: no chain access
        Assert.assertEquals(p(UNSUPPORTED_CONTIG), LiftEngine.liftSnp(() -> THROWING_CHAIN, "chr9", 100,
                MAPPED_CHR1_CHR2));
        Assert.assertEquals(p(UNSUPPORTED_CONTIG), LiftEngine.liftSnp(NEVER, "chr9", 100, MAPPED_CHR1_CHR2));
        // passthrough: no chain access
        Assert.assertEquals(p(PASSTHROUGH, "chrM", 100, ""), LiftEngine.liftSnp(() -> THROWING_CHAIN, "chrM", 100,
                PASSTHROUGH_CHRM));
        Assert.assertEquals(p(PASSTHROUGH, "chrM", 100, ""), LiftEngine.liftSnp(NEVER, "chrM", 100,
                PASSTHROUGH_CHRM));
        Assert.assertEquals(p(MAPPED, "chrM", 102, "+"), snp("chrM", 100));
        // passthrough beats -mapped
        ContigPolicy both = new ContigPolicy(Set.of("chrM"), Set.of("chr1"));
        Assert.assertEquals(p(PASSTHROUGH, "chrM", 100, ""), LiftEngine.liftSnp(NEVER, "chrM", 100, both));
        // an ambiguous or unmapped lookup is reported before the target check
        Assert.assertEquals(p(AMBIGUOUS), LiftEngine.liftSnp(ENGINE, "chr5", 150, new ContigPolicy(Set.of(),
                Set.of("chr5"))));
        Assert.assertEquals(p(UNMAPPED), LiftEngine.liftSnp(ENGINE, "chr1", 100, MAPPED_CHR1_CHR2));
    }

    // ---- var ----

    @Test
    public void var() {
        Assert.assertEquals(p(MAPPED, "chr1", 1098, "+"), var("chr1", 198, 3));
        Assert.assertEquals(p(UNMAPPED), var("chr1", 199, 3));
        Assert.assertEquals(p(MAPPED, "chr1", 5088, "-"), var("chr1", 511, 3));
        Assert.assertEquals(p(MAPPED, "chr1", 5001, "-"), var("chr1", 600, 1));
        Assert.assertEquals(p(MAPPED, "chr4", 5045, "+"), var("chr4", 195, 10));
        Assert.assertEquals(p(AMBIGUOUS), var("chr5", 150, 5));
    }

    @Test
    public void varEmptyRef() {
        Assert.assertEquals(p(UNMAPPED), var("chr1", 101, 0));
        Assert.assertEquals(p(MAPPED, "chr1", 1002, "+"), var("chr1", 102, 0));
        Assert.assertEquals(p(MAPPED, "chr1", 1100, "+"), var("chr1", 200, 0));
        Assert.assertEquals(p(UNMAPPED), var("chr1", 201, 0));
        Assert.assertEquals(p(MAPPED, "chr1", 5002, "-"), var("chr1", 600, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void varNegativeRefLengthIsAProgrammingError() {
        var("chr1", 150, -1);
    }

    @Test
    public void varContigPolicy() {
        Assert.assertEquals(p(UNSUPPORTED_CONTIG), LiftEngine.liftVar(ENGINE, "chr1", 850, 1, MAPPED_CHR1_CHR2));
        Assert.assertEquals(p(MAPPED, "chrUn_x", 50, "+"), var("chr1", 850, 1));
        Assert.assertEquals(p(UNSUPPORTED_CONTIG), LiftEngine.liftVar(NEVER, "chr9", 100, 1, MAPPED_CHR1_CHR2));
        Assert.assertEquals(p(PASSTHROUGH, "chrM", 100, ""), LiftEngine.liftVar(NEVER, "chrM", 100, 4,
                PASSTHROUGH_CHRM));
    }

    // ---- classifySpan ----

    @Test
    public void spanCasesAreTheWorkedRows() {
        Set<Integer> numbers = new HashSet<>();
        for (SpanCase c : LiftFixtures.SPAN_CASES) {
            Assert.assertTrue("duplicate case " + c.number(), numbers.add(c.number()));
        }
        Assert.assertEquals(EXPECTED.keySet(), new HashSet<>(numbers));
        Assert.assertEquals(35, numbers.size());
    }

    @Test
    public void classifySpanWorkedCases() {
        for (SpanCase c : LiftFixtures.SPAN_CASES) {
            Assert.assertEquals("case " + c.number() + " " + c.chrom() + ":" + c.s1() + ".." + c.e1(),
                    EXPECTED.get(c.number()), classify(c));
        }
    }

    @Test
    public void classifySpanPolicyVariants() {
        // 15 without -mapped
        Assert.assertEquals(hit(MAPPED, "chrUn_x", 11, 50, "+", 1, 39, 39, 1.0,
                ep("chrUn_x", 11, "+"), ep("chrUn_x", 50, "+")), classify("chr1", 811, 850, NO_POLICY));
        // 22 without passthrough
        Assert.assertEquals(hit(MAPPED, "chrM", 102, 202, "+", 1, 100, 100, 1.0,
                ep("chrM", 102, "+"), ep("chrM", 202, "+")), classify("chrM", 100, 200, NO_POLICY));
        // 30 without -mapped: inverted
        Assert.assertEquals(none(INVERTED, 2, 100, 200, 2.0, ep("chrUn_y", 251, "+"), ep("chrUn_y", 51, "+")),
                classify("chr1", 5051, 5151, NO_POLICY));
        // 21 without -mapped
        Assert.assertEquals(hit(MAPPED, "chrUn_x", 50, 50, "+", 1, 0, 0, 1.0,
                ep("chrUn_x", 50, "+"), ep("chrUn_x", 50, "+")), classify("chr1", 850, 850, NO_POLICY));
        // 29 without -mapped: still split_contig
        Assert.assertEquals(SPLIT_CONTIG, classify("chr1", 851, 1050, NO_POLICY).status());
        // passthrough and source unsupported never touch the chain
        Assert.assertEquals(EXPECTED.get(22), LiftEngine.classifySpan(NEVER, "chrM", 100, 200, PASSTHROUGH_CHRM, TOL));
        Assert.assertEquals(EXPECTED.get(23), LiftEngine.classifySpan(NEVER, "chr9", 100, 200, MAPPED_CHR1_CHR2,
                TOL));
        // a junction and a point under passthrough / unsupported source
        Assert.assertEquals(hit(PASSTHROUGH, "chrM", 101, 100, "", 0, 0, 0, 1.0, NO_ENDPOINT, NO_ENDPOINT),
                LiftEngine.classifySpan(NEVER, "chrM", 101, 100, PASSTHROUGH_CHRM, TOL));
        Assert.assertEquals(none(UNSUPPORTED_CONTIG, 0, 0, 0, 0.0, NO_ENDPOINT, NO_ENDPOINT),
                LiftEngine.classifySpan(NEVER, "chr9", 100, 100, MAPPED_CHR1_CHR2, TOL));
        // a junction contained in a block on a target outside -mapped: diagnostics through that block
        Assert.assertEquals(none(UNSUPPORTED_CONTIG, 1, 0, 0, 1.0, NO_ENDPOINT, NO_ENDPOINT),
                classify("chr1", 851, 850, MAPPED_CHR1_CHR2));
    }

    @Test
    public void containedSpanEndpointsComeFromContainingBlock() {
        // only O1 contains chr4 120..170; the point 170 is also in O2, which scores higher
        Assert.assertEquals(new EndpointLift(1, ep("chr4", 5020, "+")),
                LiftEngine.liftEndpoint(ENGINE_CHAIN, "chr4", 170));
        Assert.assertEquals(hit(MAPPED, "chr4", 1020, 1070, "+", 2, 50, 50, 1.0,
                ep("chr4", 1020, "+"), ep("chr4", 1070, "+")), classify("chr4", 120, 170, NO_POLICY));
    }

    @Test
    public void containedUnsupportedSpanReportsThroughContainingBlock() {
        // The O1/O2 geometry of the fixture, but O1 targets chrUn_a, so chr4 itself passes -mapped chr4 as a source
        // contig while O1's target does not. Only O1 contains 120..170; the point 170 is also in O2 (higher score),
        // so a raw endpoint lookup would give stop (chr4,5020,+).
        ChainSource chain = LiftFixtures.mapChain(Map.of("chr4", ContigIndex.of("chr4", OverlapPolicy.BEST, List.of(
                LiftFixtures.blk(100, 200, "chrUn_a", 1000, "+", 10),
                LiftFixtures.blk(150, 250, "chr4", 5000, "+", 20)))));
        ContigPolicy chr4Only = new ContigPolicy(Set.of(), Set.of("chr4"));
        Assert.assertEquals(new EndpointLift(1, ep("chr4", 5020, "+")), LiftEngine.liftEndpoint(chain, "chr4", 170));
        Assert.assertEquals(none(UNSUPPORTED_CONTIG, 2, 50, 50, 1.0,
                ep("chrUn_a", 1020, "+"), ep("chrUn_a", 1070, "+")),
                LiftEngine.classifySpan(() -> chain, "chr4", 120, 170, chr4Only, TOL));
        // without -mapped the same span is mapped through O1 with the same diagnostics
        Assert.assertEquals(hit(MAPPED, "chrUn_a", 1020, 1070, "+", 2, 50, 50, 1.0,
                ep("chrUn_a", 1020, "+"), ep("chrUn_a", 1070, "+")),
                LiftEngine.classifySpan(() -> chain, "chr4", 120, 170, NO_POLICY, TOL));
        // a contained junction (only O1): no endpoints, targetSize = sourceSize = 0, sizeRatio 1.0
        Assert.assertEquals(none(UNSUPPORTED_CONTIG, 1, 0, 0, 1.0, NO_ENDPOINT, NO_ENDPOINT),
                LiftEngine.classifySpan(() -> chain, "chr4", 121, 120, chr4Only, TOL));
        // a source contig outside -mapped is rejected before any lookup
        Assert.assertEquals(none(UNSUPPORTED_CONTIG, 0, 50, 0, 0.0, NO_ENDPOINT, NO_ENDPOINT),
                LiftEngine.classifySpan(ENGINE, "chr4", 120, 170, new ContigPolicy(Set.of(), Set.of("chr1")), TOL));
        // case 15 unchanged
        Assert.assertEquals(EXPECTED.get(15), classify("chr1", 811, 850, MAPPED_CHR1_CHR2));
    }

    @Test
    public void classifySpanToleranceIsStrict() {
        // case 5 has ratio 1.04: inside 0.05, outside 0.04
        Assert.assertEquals(MAPPED_ACROSS_GAP, classify("chr1", 2151, 2251, NO_POLICY).status());
        Assert.assertEquals(SIZE_CHANGED,
                LiftEngine.classifySpan(ENGINE, "chr1", 2151, 2251, NO_POLICY, 0.04).status());
        // case 3 (ratio 149/99) is mapped_across_gap with a wide tolerance
        Assert.assertEquals(MAPPED_ACROSS_GAP,
                LiftEngine.classifySpan(ENGINE, "chr1", 251, 350, NO_POLICY, 0.6).status());
    }

    @Test
    public void classifySpanInvalidInterval() {
        try {
            classify("chr1", 160, 150, NO_POLICY);
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("chr1:160"));
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("LIFTOVER_ANNOTATE: invalid interval"));
        }
        // the interval check comes before passthrough
        try {
            LiftEngine.classifySpan(NEVER, "chrM", 100, 98, PASSTHROUGH_CHRM, TOL);
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("chrM:100"));
        }
    }

    // ---- templateParity ----

    /** Direct transcription of the interval_liftover template status expression. */
    private static String referenceStatus(int s1, int e1) {
        PointResult s = snp("chr1", s1);
        PointResult e = snp("chr1", e1);
        String sc = s.chrom();
        String ec = e.chrom();
        int sp = s.pos();
        int ep = e.pos();
        String ss = s.strand();
        String es = e.strand();
        int n = 0;
        for (ChainBlock b : LiftFixtures.CHR1_BLOCKS) {
            if (b.tStart() < e1 && b.tEnd() >= s1) {
                n++;
            }
        }
        boolean anyMinus = ss.equals("-") || es.equals("-");
        int tS = anyMinus ? Math.min(sp, ep) : sp;
        int tE = anyMinus ? Math.max(sp, ep) : ep;
        double ratio = (double) (tE - tS) / (e1 - s1);
        if (sp == 0 && ep == 0) {
            return UNMAPPED;
        } else if (sp == 0) {
            return START_UNMAPPED;
        } else if (ep == 0) {
            return STOP_UNMAPPED;
        } else if (!sc.equals(ec)) {
            return SPLIT_CONTIG;
        } else if (!ss.equals(es) || (ss.equals("+") && ep <= sp) || (ss.equals("-") && ep >= sp)) {
            return INVERTED;
        } else if (n == 1) {
            return MAPPED;
        } else if (ratio > 0.95 && ratio < 1.05) {
            return MAPPED_ACROSS_GAP;
        } else {
            return SIZE_CHANGED;
        }
    }

    @Test
    public void templateParity() {
        Random rnd = new Random(4117);
        Map<String, Integer> seen = new HashMap<>();
        for (int k = 0; k < 5000; k++) {
            int s1 = 1 + rnd.nextInt(4599);
            int room = 4600 - s1;
            int e1 = rnd.nextBoolean() ? s1 + 1 + rnd.nextInt(room) : s1 + 1 + rnd.nextInt(Math.min(room, 250));
            String expected = referenceStatus(s1, e1);
            Assert.assertEquals("chr1:" + s1 + ".." + e1, expected, classify("chr1", s1, e1, NO_POLICY).status());
            seen.merge(expected, 1, Integer::sum);
        }
        for (String st : List.of(UNMAPPED, START_UNMAPPED, STOP_UNMAPPED, SPLIT_CONTIG, INVERTED, MAPPED,
                MAPPED_ACROSS_GAP, SIZE_CHANGED)) {
            Assert.assertTrue("status never produced: " + st + " " + seen, seen.getOrDefault(st, 0) > 0);
        }
    }

    // ---- revcompAlleles ----

    @Test
    public void revcompAlleles() {
        Assert.assertEquals("CGT", LiftEngine.revcompAlleles("ACG"));
        Assert.assertEquals("T,C", LiftEngine.revcompAlleles("A,G"));
        Assert.assertEquals("C,TAA", LiftEngine.revcompAlleles("G,TTA"));
        Assert.assertEquals("GT,<INS>,*", LiftEngine.revcompAlleles("AC,<INS>,*"));
        Assert.assertEquals("<DEL>", LiftEngine.revcompAlleles("<DEL>"));
        Assert.assertEquals(".", LiftEngine.revcompAlleles("."));
        Assert.assertEquals("", LiftEngine.revcompAlleles(""));
        Assert.assertEquals("cgt", LiftEngine.revcompAlleles("acg"));
        Assert.assertEquals("G]chr1:100]", LiftEngine.revcompAlleles("G]chr1:100]"));
        Assert.assertEquals("[chr2:5[T", LiftEngine.revcompAlleles("[chr2:5[T"));
        Assert.assertEquals("T,,C", LiftEngine.revcompAlleles("A,,G"));
        Assert.assertEquals("T,", LiftEngine.revcompAlleles("A,"));
    }

    // ---- liftAll ----

    private static String all(String chrom, int s0, int e0, boolean point) {
        List<String> r = new ArrayList<>();
        for (ScoredTarget st : LiftEngine.liftAll(ENGINE_CHAIN, chrom, s0, e0, point)) {
            Target t = st.target();
            r.add("(" + t.chrom() + "," + t.start0() + "," + t.end0() + "," + t.strand() + "," + st.score() + ")");
        }
        return String.join(" ", r);
    }

    @Test
    public void liftAll() {
        Assert.assertEquals("(chr4,1074,1075,+,10.0) (chr4,5024,5025,+,20.0)", all("chr4", 174, 175, true));
        Assert.assertEquals("(chr5,1049,1050,+,10.0) (chr5,3049,3050,+,10.0)", all("chr5", 149, 150, true));
        Assert.assertEquals("", all("chr1", 400, 401, true));
        Assert.assertEquals("(chr4,1059,1070,+,10.0) (chr4,5009,5020,+,20.0)", all("chr4", 159, 170, false));
        Assert.assertEquals("(chr1,1050,1050,+,10.0)", all("chr1", 150, 150, false));
        Assert.assertEquals("(chr1,5040,5090,-,10.0)", all("chr1", 510, 560, false));
        Assert.assertEquals("", all("chr9", 10, 20, false));
        Assert.assertTrue(LiftEngine.liftAll(ENGINE_CHAIN, "chr1", 400, 401, true).isEmpty());
    }

    // ---- naiveReference ----

    private static final String S = "chrS";
    private static final String[] TARGETS = {"chr1", "chr2", "chrX"};

    private enum Kind { SNP, VAR, SPAN, JUNCTION }

    private static List<ChainBlock> randomBlocks(Random rnd) {
        List<ChainBlock> r = new ArrayList<>();
        int maxEnd = rnd.nextInt(50);
        for (int k = 0; k < 100; k++) {
            String q = TARGETS[rnd.nextInt(TARGETS.length)];
            int qStart = rnd.nextInt(100_000);
            String strand = rnd.nextBoolean() ? "+" : "-";
            ChainBlock prev = k == 0 ? null : r.get(k - 1);
            ChainBlock b;
            if (prev != null && rnd.nextInt(5) == 0) {
                if (rnd.nextBoolean()) {
                    // exact tie: same source block and score, another target
                    b = LiftFixtures.blk(prev.tStart(), prev.tEnd(), q, qStart, strand, prev.score());
                } else {
                    // overlapping: starts inside the previous block
                    int ts = prev.tStart() + rnd.nextInt(prev.tEnd() - prev.tStart());
                    b = LiftFixtures.blk(ts, ts + 1 + rnd.nextInt(400), q, qStart, strand, rnd.nextInt(4));
                }
            } else {
                int ts = maxEnd + rnd.nextInt(51);
                b = LiftFixtures.blk(ts, ts + 1 + rnd.nextInt(400), q, qStart, strand, rnd.nextInt(4));
            }
            r.add(b);
            maxEnd = Math.max(maxEnd, b.tEnd());
        }
        return r;
    }

    /** 60% within 2 bases of a block edge, else uniform over the chain extent. */
    private static int coordinate(Random rnd, List<ChainBlock> blocks, int maxEnd, int min) {
        int x;
        if (rnd.nextInt(10) < 6) {
            ChainBlock b = blocks.get(rnd.nextInt(blocks.size()));
            x = (rnd.nextBoolean() ? b.tStart() : b.tEnd()) + rnd.nextInt(5) - 2;
        } else {
            x = rnd.nextInt(maxEnd + 50);
        }
        return Math.max(min, x);
    }

    private static String show(PointResult r) {
        return r.status() + " " + r.chrom() + ":" + r.pos() + " " + r.strand();
    }

    /** As a PointResult would show; pos is the target's end0 (snp) or start0 + 1 (var). */
    private static String show(NaiveLift.Result r, boolean snp) {
        NaiveLift.Hit h = r.hit();
        if (h == null) {
            return r.status() + " :0 ";
        }
        return r.status() + " " + h.chrom() + ":" + (snp ? h.end0() : h.start0() + 1) + " " + h.strand();
    }

    private static String showSpan(SpanResult r) {
        String head = MAPPED.equals(r.status())
                ? MAPPED + " " + r.chrom() + ":" + r.start1() + "-" + r.stop1() + " " + r.strand()
                : AMBIGUOUS.equals(r.status()) ? AMBIGUOUS : NaiveLift.OTHER;
        return head + " n=" + r.chainBlocks();
    }

    private static String showSpan(NaiveLift.Result r, int n) {
        NaiveLift.Hit h = r.hit();
        String head = h == null ? r.status()
                : r.status() + " " + h.chrom() + ":" + (h.start0() + 1) + "-" + h.end0() + " " + h.strand();
        return head + " n=" + n;
    }

    private static String showAll(List<ScoredTarget> targets) {
        List<String> r = new ArrayList<>();
        for (ScoredTarget st : targets) {
            Target t = st.target();
            r.add(t.chrom() + ":" + t.start0() + "-" + t.end0() + t.strand() + "@" + st.score());
        }
        return r.toString();
    }

    private static String showHits(List<NaiveLift.Hit> hits) {
        List<String> r = new ArrayList<>();
        for (NaiveLift.Hit h : hits) {
            r.add(h.chrom() + ":" + h.start0() + "-" + h.end0() + h.strand() + "@" + h.score());
        }
        return r.toString();
    }

    private static void check(long seed, List<ChainBlock> blocks, String input, String naive, String engine) {
        if (!naive.equals(engine)) {
            StringBuilder sb = new StringBuilder("engine differs from NaiveLift\nchain seed " + seed + "\ninput "
                    + input + "\nnaive  " + naive + "\nengine " + engine + "\nblocks (tStart tEnd qChrom qStart "
                    + "strand score):\n");
            for (int i = 0; i < blocks.size(); i++) {
                ChainBlock b = blocks.get(i);
                sb.append(i).append(": ").append(b.tStart()).append(' ').append(b.tEnd()).append(' ')
                        .append(b.qChrom()).append(' ').append(b.qStart()).append(' ').append(b.strand())
                        .append(' ').append(b.score()).append('\n');
            }
            Assert.fail(sb.toString());
        }
    }

    @Test
    public void naiveReference() {
        Random master = new Random(4117);
        Map<Kind, Map<String, Integer>> outcomes = new EnumMap<>(Kind.class);
        for (Kind k : Kind.values()) {
            outcomes.put(k, new HashMap<>());
        }
        int overlappingChains = 0;
        for (int c = 0; c < 50; c++) {
            long seed = master.nextLong();
            Random rnd = new Random(seed);
            List<ChainBlock> blocks = randomBlocks(rnd);
            ChainSource chain = LiftFixtures.mapChain(Map.of(S, ContigIndex.of(S, OverlapPolicy.BEST, blocks)));
            Supplier<ChainSource> sup = () -> chain;
            NaiveLift naive = new NaiveLift(blocks);
            int maxEnd = blocks.stream().mapToInt(ChainBlock::tEnd).max().orElse(0);
            boolean overlaps = false;
            for (int i = 1; i < blocks.size(); i++) {
                overlaps |= blocks.get(i).tStart() < blocks.get(i - 1).tEnd();
            }
            overlappingChains += overlaps ? 1 : 0;

            for (int k = 0; k < 2000; k++) {
                // point
                int p = coordinate(rnd, blocks, maxEnd, 1);
                NaiveLift.Result np = naive.point(p);
                check(seed, blocks, "snp " + p, show(np, true), show(LiftEngine.liftSnp(sup, S, p, NO_POLICY)));
                check(seed, blocks, "liftAll point " + p, showHits(naive.pointHits(p)),
                        showAll(LiftEngine.liftAll(chain, S, p - 1, p, true)));
                outcomes.get(Kind.SNP).merge(np.status(), 1, Integer::sum);

                // var
                int pos = coordinate(rnd, blocks, maxEnd, 1);
                int refLen = rnd.nextInt(13);
                NaiveLift.Result nv = naive.var(pos, refLen);
                check(seed, blocks, "var " + pos + " refLen " + refLen, show(nv, false),
                        show(LiftEngine.liftVar(sup, S, pos, refLen, NO_POLICY)));
                outcomes.get(Kind.VAR).merge(nv.status() + (refLen == 0 ? "/empty" : ""), 1, Integer::sum);

                // interval of length 1-300, with its start or its stop near an edge
                int len = 1 + rnd.nextInt(300);
                int s1;
                if (rnd.nextBoolean()) {
                    s1 = coordinate(rnd, blocks, maxEnd, 1);
                } else {
                    s1 = Math.max(1, coordinate(rnd, blocks, maxEnd, 1) - len + 1);
                }
                int e1 = s1 + len - 1;
                NaiveLift.Result ns = naive.span(s1, e1);
                check(seed, blocks, "span " + s1 + ".." + e1, showSpan(ns, naive.count(s1, e1)),
                        showSpan(LiftEngine.classifySpan(sup, S, s1, e1, NO_POLICY, TOL)));
                check(seed, blocks, "liftAll span " + (s1 - 1) + "," + e1, showHits(naive.intervalHits(s1, e1)),
                        showAll(LiftEngine.liftAll(chain, S, s1 - 1, e1, false)));
                outcomes.get(Kind.SPAN).merge(ns.status(), 1, Integer::sum);

                // junction at the 0-based boundary x: s1 = x + 1, e1 = x
                int x = coordinate(rnd, blocks, maxEnd, 0);
                NaiveLift.Result nj = naive.span(x + 1, x);
                check(seed, blocks, "junction " + (x + 1) + ".." + x, showSpan(nj, naive.count(x + 1, x)),
                        showSpan(LiftEngine.classifySpan(sup, S, x + 1, x, NO_POLICY, TOL)));
                check(seed, blocks, "liftAll junction " + x, showHits(naive.junctionHits(x)),
                        showAll(LiftEngine.liftAll(chain, S, x, x, false)));
                outcomes.get(Kind.JUNCTION).merge(nj.status(), 1, Integer::sum);
            }
        }
        // non-vacuity: the generator exercises every outcome
        Assert.assertTrue("overlapping chains " + overlappingChains, overlappingChains >= 45);
        for (String st : List.of(NaiveLift.MAPPED, NaiveLift.AMBIGUOUS, NaiveLift.UNMAPPED)) {
            Assert.assertTrue(outcomes.toString(), outcomes.get(Kind.SNP).getOrDefault(st, 0) > 100);
            Assert.assertTrue(outcomes.toString(), outcomes.get(Kind.VAR).getOrDefault(st, 0) > 100);
            Assert.assertTrue(outcomes.toString(), outcomes.get(Kind.VAR).getOrDefault(st + "/empty", 0) > 20);
        }
        for (String st : List.of(NaiveLift.MAPPED, NaiveLift.AMBIGUOUS, NaiveLift.OTHER)) {
            Assert.assertTrue(outcomes.toString(), outcomes.get(Kind.SPAN).getOrDefault(st, 0) > 100);
            Assert.assertTrue(outcomes.toString(), outcomes.get(Kind.JUNCTION).getOrDefault(st, 0) > 100);
        }
    }
}
