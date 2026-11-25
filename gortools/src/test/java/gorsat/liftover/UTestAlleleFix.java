package gorsat.liftover;

import gorsat.Analysis.AnalysisSink;
import gorsat.Commands.Analysis;
import gorsat.Commands.RowHeader;
import gorsat.UTestGorLiftoverHaplotype;
import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.RefSeq;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ENGKNOW-4224: {@link AlleleFix} on chain indexes built in the test, for the cases the query-level tests in
 * {@link UTestGorLiftoverHaplotype} cannot reach: the strand of the result, the -mapped policy, the contig start, the
 * realignment size cap, chain order, and a cloned LIFTOVER step.
 */
public class UTestAlleleFix {

    private static final String SOURCE = UTestGorLiftoverHaplotype.SOURCE;
    private static final String TARGET = UTestGorLiftoverHaplotype.TARGET;
    private static final ContigPolicy ANY = new ContigPolicy(Set.of(), null);

    private static ContigIndex index(ChainBlock... blocks) {
        ContigIndexBuilder b = new ContigIndexBuilder("chr1", OverlapPolicy.BEST, "test");
        for (ChainBlock block : blocks) {
            b.add(block);
        }
        return b.build();
    }

    /** SOURCE -> TARGET, which has TAA after source base 101. */
    private static ContigIndex forward() {
        return index(new ChainBlock(0, 101, "chr1", 0, 101, "+", 1000),
                new ChainBlock(101, 201, "chr1", 104, 204, "+", 1000));
    }

    private static AlleleFix.Bases bases(String seq) {
        return (chrom, a, b) -> {
            StringBuilder sb = new StringBuilder();
            for (int p = a; p <= b; p++) {
                sb.append(p >= 1 && p <= seq.length() ? seq.charAt(p - 1) : 'N');
            }
            return sb.toString();
        };
    }

    private static String revcomp(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append("ACGT".charAt(3 - "ACGT".indexOf(s.charAt(i))));
        }
        return sb.toString();
    }

    @Test
    public void rewriteCarriesTheStrandOfItsChain() {
        AlleleFix.Result plus = AlleleFix.rewrite(forward(), "chr1", 100, "A", "AT", bases(SOURCE), bases(TARGET), ANY);
        Assert.assertEquals(new AlleleFix.Result(AlleleFix.REWRITTEN, "chr1", 101, "+", "TTAA", "TT"), plus);

        ContigIndex minus = index(new ChainBlock(0, 101, "chr1", 103, 204, "-", 1000),
                new ChainBlock(101, 201, "chr1", 0, 100, "-", 1000));
        AlleleFix.Result r = AlleleFix.rewrite(minus, "chr1", 100, "A", "AT", bases(SOURCE), bases(revcomp(TARGET)),
                ANY);
        Assert.assertEquals(new AlleleFix.Result(AlleleFix.REWRITTEN, "chr1", 99, "-", "CATT", "CA"), r);
    }

    @Test
    public void aTargetContigThePolicyRefusesIsUnresolved() {
        ContigPolicy onlyChr2 = new ContigPolicy(Set.of(), Set.of("chr2"));
        Assert.assertEquals(AlleleFix.UNRESOLVED, AlleleFix.rewrite(forward(), "chr1", 100, "A", "AT", bases(SOURCE),
                bases(TARGET), onlyChr2).fix());
        Assert.assertEquals(AlleleFix.REWRITTEN, AlleleFix.rewrite(forward(), "chr1", 100, "A", "AT", bases(SOURCE),
                bases(TARGET), new ContigPolicy(Set.of(), Set.of("chr1"))).fix());
    }

    @Test
    public void extensionStopsAtTheContigStart() {
        // SOURCE starts AATAAT: AA>A at 1 extends left past base 1 in BCFtools; here it stops and lifts as before
        Assert.assertEquals("AATAAT", SOURCE.substring(0, 6));
        Assert.assertEquals(AlleleFix.Result.of(AlleleFix.NONE), AlleleFix.rewrite(forward(), "chr1", 1, "AA", "A",
                bases(SOURCE), bases(TARGET), ANY));
        Assert.assertEquals(AlleleFix.Result.of(AlleleFix.NONE), AlleleFix.rewrite(forward(), "chr1", 1, "A", "AA",
                bases(SOURCE), bases(TARGET), ANY));
    }

    /**
     * A deletion at the start of a poly-A run that ends in a source-only gap (TTT): the extended REF ends in the gap,
     * so it is padded to the next block and realigned over the whole run.
     */
    private static AlleleFix.Result runIntoGap(int run) {
        String tail = "CGCATGCATGCGCATGCATG".repeat(5);
        String src = "GC" + "A".repeat(run) + "TTT" + tail;
        String tgt = "GC" + "A".repeat(run) + tail;
        ContigIndex ci = index(new ChainBlock(0, 2 + run, "chr1", 0, 2 + run, "+", 1000),
                new ChainBlock(2 + run + 3, src.length(), "chr1", 2 + run, tgt.length(), "+", 1000));
        return AlleleFix.rewrite(ci, "chr1", 2, "CA", "C", bases(src), bases(tgt), ANY);
    }

    @Test
    public void aRealignmentOverTheSizeCapIsUnresolved() {
        // BCFtools leaves both unchanged (CA>C at 2): the realigned target span carries the source REF. Over the cap
        // (about 2000 x 2000 bases) the realignment is not made and the row is unresolved instead.
        Assert.assertEquals(AlleleFix.Result.of(AlleleFix.NONE), runIntoGap(500));
        Assert.assertEquals(AlleleFix.Result.of(AlleleFix.UNRESOLVED), runIntoGap(2200));
    }

    @Test
    public void padsToTheNextBlockOfTheSameChainInBothBuilds() {
        // TARGET -> SOURCE: TAA (target 102..104) is a source-only gap. A decoy block with the same chain fields sits
        // in it but maps back before the chain in the target, so it is another chain and is skipped.
        ContigIndex rev = index(new ChainBlock(0, 101, "chr1", 0, 101, "+", 1000),
                new ChainBlock(103, 104, "chr1", 50, 51, "+", 1000),
                new ChainBlock(104, 204, "chr1", 101, 201, "+", 1000));
        AlleleFix.Result r = AlleleFix.rewrite(rev, "chr1", 100, "A", "AT", bases(TARGET), bases(SOURCE), ANY);
        // BCFtools on the chain without the decoy: chr1 100 A AT,ATT
        Assert.assertEquals(new AlleleFix.Result(AlleleFix.REWRITTEN, "chr1", 100, "+", "A", "ATT"), r);
    }

    @Test
    public void allelesAreClassifiedBeforeTheLengthCheck() {
        // a symbolic ALT or several ALTs as long as REF are not SNVs: unresolved, not none
        Assert.assertEquals(AlleleFix.UNRESOLVED, fix("ACG", "A,C"));
        Assert.assertEquals(AlleleFix.UNRESOLVED, fix("ACGTA", "<DEL>"));
        Assert.assertEquals(AlleleFix.UNRESOLVED, fix("A", "AT,ATT"));
        // SNVs and MNVs, with one ALT or several, keep the plain lift
        Assert.assertEquals(AlleleFix.NONE, fix("A", "C,G"));
        Assert.assertEquals(AlleleFix.NONE, fix("AT", "GC"));
    }

    private static String fix(String ref, String alt) {
        return AlleleFix.rewrite(forward(), "chr1", 100, ref, alt, bases(SOURCE), bases(TARGET), ANY).fix();
    }

    @Test
    public void aOneBaseEqualGapLiftsWithAndWithoutGapFill() {
        // a 1-base equal gap at source base 103, the 3' anchor of A>AT at 100 (extended to ATTG, 100..103)
        ChainBlock a = new ChainBlock(0, 101, "chr1", 0, 101, "+", 1000);
        ChainBlock b1 = new ChainBlock(101, 102, "chr1", 104, 105, "+", 1000);
        ChainBlock b2 = new ChainBlock(103, 201, "chr1", 106, 204, "+", 1000);
        ContigIndexBuilder filled = new ContigIndexBuilder("chr1", OverlapPolicy.BEST, "test");
        ContigIndexBuilder unfilled = new ContigIndexBuilder("chr1", OverlapPolicy.BEST, "test", 0);
        for (ChainBlock block : new ChainBlock[]{a, b1, b2}) {
            filled.add(block);
            unfilled.add(block);
        }
        AlleleFix.Result expected = new AlleleFix.Result(AlleleFix.REWRITTEN, "chr1", 101, "+", "TTAA", "TT");
        Assert.assertEquals(expected, AlleleFix.rewrite(filled.build(), "chr1", 100, "A", "AT", bases(SOURCE),
                bases(TARGET), ANY));
        Assert.assertEquals(expected, AlleleFix.rewrite(unfilled.build(), "chr1", 100, "A", "AT", bases(SOURCE),
                bases(TARGET), ANY));
    }

    @Test
    public void aPaddedAnchorTooFarAwayIsPlacedTheSameWayOnBothStrands() {
        // A 500-base poly-A run ending in a source-only gap: the padded 3' anchor lifts more than 250 bases from the
        // 5' one, so it is placed by arithmetic. BCFtools gives chr1 2 CA C on +, but on - its formula leaves out the
        // allele length and it writes chr1 100 G G+T(500) (SWAP=-1), a 500-base insertion the target already has.
        // The mirror of the + formula gives the same record as the 50-base run on - (chr1 100 GT G).
        String tail = "CGCATGCATGCGCATGCATG".repeat(5);
        String src = "GC" + "A".repeat(500) + "TTT" + tail;
        String tgt = "GC" + "A".repeat(500) + tail;
        ContigIndex plus = index(new ChainBlock(0, 502, "chr1", 0, 502, "+", 1000),
                new ChainBlock(505, 605, "chr1", 502, 602, "+", 1000));
        ContigIndex minus = index(new ChainBlock(0, 502, "chr1", 100, 602, "-", 1000),
                new ChainBlock(505, 605, "chr1", 0, 100, "-", 1000));
        Assert.assertEquals(AlleleFix.Result.of(AlleleFix.NONE), AlleleFix.rewrite(plus, "chr1", 2, "CA", "C",
                bases(src), bases(tgt), ANY));
        Assert.assertEquals(new AlleleFix.Result(AlleleFix.REWRITTEN, "chr1", 100, "-", "GT", "G"),
                AlleleFix.rewrite(minus, "chr1", 2, "CA", "C", bases(src), bases(revcomp(tgt)), ANY));
    }

    @Test
    public void aSpanAcrossAGapThatKeepsTheSourceRefTakesTheBcftoolsPosition() {
        // poly-A run ending in a source-only gap (TTT): the target over the span is the source REF (BCFtools no SWAP),
        // and BCFtools writes the record left aligned over the span
        String tail = "CGCATGCATGCGCATGCATG".repeat(5);
        String src = "GC" + "A".repeat(50) + "TTT" + tail;
        String tgt = "GC" + "A".repeat(50) + tail;
        ContigIndex plus = index(new ChainBlock(0, 52, "chr1", 0, 52, "+", 1000),
                new ChainBlock(55, 155, "chr1", 52, 152, "+", 1000));
        ContigIndex minus = index(new ChainBlock(0, 52, "chr1", 100, 152, "-", 1000),
                new ChainBlock(55, 155, "chr1", 0, 100, "-", 1000));
        // BCFtools on +: chr1 2 CA C for both; the plain lift of the first is the same, of the second it is not
        Assert.assertEquals(AlleleFix.Result.of(AlleleFix.NONE), AlleleFix.rewrite(plus, "chr1", 2, "CA", "C",
                bases(src), bases(tgt), ANY));
        Assert.assertEquals(new AlleleFix.Result(AlleleFix.REWRITTEN, "chr1", 2, "+", "CA", "C"),
                AlleleFix.rewrite(plus, "chr1", 10, "AA", "A", bases(src), bases(tgt), ANY));
        // BCFtools on -: chr1 100 GT G (FLIP) for both, left aligned in the poly-T run
        for (int pos : new int[]{2, 10}) {
            String ref = pos == 2 ? "CA" : "AA";
            String alt = pos == 2 ? "C" : "A";
            Assert.assertEquals(new AlleleFix.Result(AlleleFix.REWRITTEN, "chr1", 100, "-", "GT", "G"),
                    AlleleFix.rewrite(minus, "chr1", pos, ref, alt, bases(src), bases(revcomp(tgt)), ANY));
        }
    }

    /** A reference over a sequence that counts how often it is closed. */
    private static RefSeq refSeq(String seq, AtomicInteger closed) {
        AlleleFix.Bases b = bases(seq);
        return new RefSeq() {
            @Override
            public char getBase(String chr, int pos) {
                return b.get(chr, pos, pos).charAt(0);
            }

            @Override
            public String getBases(String chr, int pos1, int pos2) {
                return b.get(chr, pos1, pos2);
            }

            @Override
            public void close() {
                closed.incrementAndGet();
            }
        };
    }

    private static List<String> lift(Analysis step, String... rows) {
        AnalysisSink sink = new AnalysisSink();
        step.$bar(sink);
        for (String r : rows) {
            step.process(RowObj.apply(r));
        }
        step.securedFinish(null);
        List<String> out = new ArrayList<>();
        scala.collection.Iterator<Row> it = sink.rows().iterator();
        while (it.hasNext()) {
            out.add(it.next().toString());
        }
        return out;
    }

    @Test
    public void aClonedStepRewritesAndClosesItsOwnReferences() {
        AtomicInteger closed = new AtomicInteger();
        AlleleFixConfig fix = new AlleleFixConfig(() -> refSeq(SOURCE, closed), () -> refSeq(TARGET, closed));
        RowHeader in = RowHeader.apply("Chrom\tPos\tRef\tAlt", new String[]{"S", "I", "S", "S"});
        RowHeader out = RowHeader.apply("Chrom\tPos\tRef\tAlt\thg_Chrom\thg_Pos\thg_qStrand\thg_liftoverStatus"
                + "\thg_alleleFix", new String[]{"S", "I", "S", "S", "S", "I", "S", "S", "S"});
        ChainSource chain = LiftFixtures.mapChain(Map.of("chr1", forward()));
        LiftoverRelocateAnalysis original = new LiftoverRelocateAnalysis(RelocateMode.VAR, false,
                new RelocateColumns(2, 3, -1, -1, -1, -1, -1), out, () -> chain, false, ANY, Set.of(),
                LiftoverArgs.DEFAULT_TOLERANCE, false, fix);
        original.setRowHeader(in);
        Analysis copy = original.clone();
        Assert.assertTrue(copy instanceof LiftoverRelocateAnalysis);
        Assert.assertNotSame(original, copy);
        List<String> expected = List.of("chr1\t101\tTTAA\tTT\tchr1\t100\t+\tmapped\trewritten");
        Assert.assertEquals(expected, lift(original, "chr1\t100\tA\tAT"));
        Assert.assertEquals(2, closed.get());
        Assert.assertEquals(expected, lift(copy, "chr1\t100\tA\tAT"));
        Assert.assertEquals(4, closed.get());
    }
}
