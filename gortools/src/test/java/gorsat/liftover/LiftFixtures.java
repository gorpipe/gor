package gorsat.liftover;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared chain fixtures for the liftover tests. Java because test Java compiles before test Scala.
 */
public final class LiftFixtures {

    private LiftFixtures() {
    }

    /** Chain source over explicit per-contig indexes; an absent contig gives {@link ContigIndex#empty}. */
    public static ChainSource mapChain(Map<String, ContigIndex> m) {
        return contig -> m.getOrDefault(contig, ContigIndex.empty(contig));
    }

    /** Chain source that fails on any access, for tests that must not touch the chain. */
    public static final ChainSource THROWING_CHAIN = contig -> {
        throw new IllegalStateException("chain accessed");
    };

    /** Block whose qEnd is qStart + (tEnd - tStart). */
    public static ChainBlock blk(int tStart, int tEnd, String qChrom, int qStart, String strand, double score) {
        return new ChainBlock(tStart, tEnd, qChrom, qStart, qStart + (tEnd - tStart), strand, score);
    }

    private static ChainBlock b(int tStart, int tEnd, String qChrom, int qStart, String strand) {
        return blk(tStart, tEnd, qChrom, qStart, strand, 10);
    }

    /** The engine fixture, chr1 part (26 blocks over four contigs in total). */
    public static final List<ChainBlock> CHR1_BLOCKS = List.of(
            b(100, 200, "chr1", 1000, "+"),     // B1  idx 0
            b(200, 300, "chr1", 1100, "+"),     // B2
            b(300, 400, "chr1", 1250, "+"),     // B3
            b(500, 600, "chr1", 5000, "-"),     // B4
            b(600, 700, "chr2", 100, "+"),      // B5
            b(800, 900, "chrUn_x", 0, "+"),     // B6
            b(1000, 1100, "chr1", 9000, "-"),   // B7
            b(1100, 1200, "chr1", 8000, "+"),   // B8
            b(1300, 1400, "chr1", 20000, "+"),  // B9
            b(1400, 1500, "chr1", 19000, "+"),  // B10
            b(2000, 2100, "chr1", 30000, "+"),  // B11
            b(2100, 2200, "chr1", 30105, "+"),  // B12
            b(2200, 2300, "chr1", 30209, "+"),  // B13
            b(3000, 3100, "chr1", 40000, "+"),  // B14
            b(3105, 3205, "chr1", 40100, "+"),  // B15
            b(3300, 3400, "chr1", 41000, "+"),  // B16
            b(3404, 3504, "chr1", 41100, "+"),  // B17
            b(4000, 4100, "chr1", 50000, "-"),  // B18
            b(4100, 4200, "chr1", 49900, "-"),  // B19
            b(5000, 5100, "chrUn_y", 200, "+"), // B20
            b(5100, 5200, "chrUn_y", 0, "+"));  // B21

    public static final List<ChainBlock> CHR4_BLOCKS = List.of(
            blk(100, 200, "chr4", 1000, "+", 10),   // O1
            blk(150, 250, "chr4", 5000, "+", 20));  // O2

    public static final List<ChainBlock> CHR5_BLOCKS = List.of(
            b(100, 200, "chr5", 1000, "+"),         // T1
            b(100, 200, "chr5", 3000, "+"));        // T2, exact tie

    public static final List<ChainBlock> CHRM_BLOCKS = List.of(
            b(0, 16571, "chrM", 2, "+"));

    /** The 26-block engine fixture: chr1 under ERROR, chr4, chr5 and chrM under BEST. */
    public static final ChainSource ENGINE_CHAIN = mapChain(Map.of(
            "chr1", ContigIndex.of("chr1", OverlapPolicy.ERROR, CHR1_BLOCKS),
            "chr4", ContigIndex.of("chr4", OverlapPolicy.BEST, CHR4_BLOCKS),
            "chr5", ContigIndex.of("chr5", OverlapPolicy.BEST, CHR5_BLOCKS),
            "chrM", ContigIndex.of("chrM", OverlapPolicy.BEST, CHRM_BLOCKS)));

    // ---- span cases (Task 4 rows 1-27 and 29-36; Task 5 reuses them) ----

    /** No passthrough contigs, no -mapped restriction. */
    public static final ContigPolicy NO_POLICY = new ContigPolicy(Set.of(), null);
    /** -mapped chr1,chr2. */
    public static final ContigPolicy MAPPED_CHR1_CHR2 = new ContigPolicy(Set.of(), Set.of("chr1", "chr2"));
    /** -passthrough chrM. */
    public static final ContigPolicy PASSTHROUGH_CHRM = new ContigPolicy(Set.of("chrM"), null);

    /** One worked classifySpan input: 1-based inclusive s1..e1 (e1 == s1 - 1 is a junction). */
    public record SpanCase(int number, String chrom, int s1, int e1, ContigPolicy policy, ChainSource chain) {
    }

    private static SpanCase sc(int number, String chrom, int s1, int e1) {
        return new SpanCase(number, chrom, s1, e1, NO_POLICY, ENGINE_CHAIN);
    }

    private static SpanCase sc(int number, String chrom, int s1, int e1, ContigPolicy policy, ChainSource chain) {
        return new SpanCase(number, chrom, s1, e1, policy, chain);
    }

    /** The worked span cases over {@link #ENGINE_CHAIN}; case 28 (an invalid interval) is not here. */
    public static final List<SpanCase> SPAN_CASES = List.of(
            sc(1, "chr1", 151, 180),
            sc(2, "chr1", 151, 250),
            sc(3, "chr1", 251, 350),
            sc(4, "chr1", 2051, 2151),
            sc(5, "chr1", 2151, 2251),
            sc(6, "chr1", 3051, 3151),
            sc(7, "chr1", 3351, 3451),
            sc(8, "chr1", 511, 560),
            sc(9, "chr1", 4051, 4151),
            sc(10, "chr1", 401, 500),
            sc(11, "chr1", 750, 950),
            sc(12, "chr1", 380, 450),
            sc(13, "chr1", 450, 520),
            sc(14, "chr1", 551, 650),
            sc(15, "chr1", 811, 850, MAPPED_CHR1_CHR2, ENGINE_CHAIN),
            sc(16, "chr1", 1051, 1151),
            sc(17, "chr1", 1351, 1451),
            sc(18, "chr1", 150, 150),
            sc(19, "chr1", 550, 550),
            sc(20, "chr1", 450, 450),
            sc(21, "chr1", 850, 850, MAPPED_CHR1_CHR2, ENGINE_CHAIN),
            sc(22, "chrM", 100, 200, PASSTHROUGH_CHRM, THROWING_CHAIN),
            sc(23, "chr9", 100, 200, MAPPED_CHR1_CHR2, THROWING_CHAIN),
            sc(24, "chr5", 151, 160),
            sc(25, "chr5", 150, 250),
            sc(26, "chr4", 120, 140),
            sc(27, "chr4", 160, 170),
            sc(29, "chr1", 851, 1050, MAPPED_CHR1_CHR2, ENGINE_CHAIN),
            sc(30, "chr1", 5051, 5151, MAPPED_CHR1_CHR2, ENGINE_CHAIN),
            sc(31, "chr1", 151, 150),
            sc(32, "chr1", 101, 100),
            sc(33, "chr1", 201, 200),
            sc(34, "chr1", 551, 550),
            sc(35, "chr5", 151, 150),
            sc(36, "chr4", 175, 174));

    /**
     * Independent reference model of the lift rules over a plain block list of one source contig, written from the
     * 1-based definitions only. It never calls {@link ContigIndex}, {@code LiftEngine.liftAt} or any engine code.
     * <ul>
     * <li>a point p maps through b iff b.tStart+1 &lt;= p &lt;= b.tEnd, to qStart+(p-tStart) (+) or
     * qStart+(tEnd-p)+1 (-);</li>
     * <li>an interval s1..e1 maps iff tStart+1 &lt;= s1 and e1 &lt;= tEnd, to qStart+(s1-tStart) ..
     * qStart+(e1-tStart) (+) or qStart+(tEnd-e1)+1 .. qStart+(tEnd-s1)+1 (-);</li>
     * <li>the junction right after 1-based base x (the 0-based boundary x) maps iff tStart &lt; x &lt; tEnd, to
     * qStart+(x-tStart) (+) or qStart+(tEnd-x) (-).</li>
     * </ul>
     * Best is the maximum score; an exact tie for the maximum is ambiguous. Results are reported as 0-based half-open
     * targets: a point p' is [p'-1, p'), an interval a..b is [a-1, b), a junction j is [j, j).
     */
    public static final class NaiveLift {

        public static final String MAPPED = "mapped";
        public static final String AMBIGUOUS = "ambiguous";
        public static final String UNMAPPED = "unmapped";
        /** Span projection status for anything that is neither mapped nor ambiguous. */
        public static final String OTHER = "other";

        /** One containing block's target, 0-based half-open, with the block's score. */
        public record Hit(String chrom, int start0, int end0, String strand, double score) {
        }

        /** status is MAPPED (hit set), AMBIGUOUS, UNMAPPED or OTHER (hit null). */
        public record Result(String status, Hit hit) {
        }

        private final List<ChainBlock> blocks;

        public NaiveLift(List<ChainBlock> blocks) {
            this.blocks = List.copyOf(blocks);
        }

        private static boolean plus(ChainBlock b) {
            return "+".equals(b.strand());
        }

        /** Every block containing the 1-based point p, in list order. */
        public List<Hit> pointHits(int p) {
            List<Hit> r = new ArrayList<>();
            for (ChainBlock b : blocks) {
                if (b.tStart() + 1 <= p && p <= b.tEnd()) {
                    int q = plus(b) ? b.qStart() + (p - b.tStart()) : b.qStart() + (b.tEnd() - p) + 1;
                    r.add(new Hit(b.qChrom(), q - 1, q, b.strand(), b.score()));
                }
            }
            return r;
        }

        /** Every block containing the 1-based inclusive interval s1..e1 (s1 &lt;= e1), in list order. */
        public List<Hit> intervalHits(int s1, int e1) {
            List<Hit> r = new ArrayList<>();
            for (ChainBlock b : blocks) {
                if (b.tStart() + 1 <= s1 && e1 <= b.tEnd()) {
                    int a;
                    int z;
                    if (plus(b)) {
                        a = b.qStart() + (s1 - b.tStart());
                        z = b.qStart() + (e1 - b.tStart());
                    } else {
                        a = b.qStart() + (b.tEnd() - e1) + 1;
                        z = b.qStart() + (b.tEnd() - s1) + 1;
                    }
                    r.add(new Hit(b.qChrom(), a - 1, z, b.strand(), b.score()));
                }
            }
            return r;
        }

        /** Every block strictly containing the junction at the 0-based boundary x, in list order. */
        public List<Hit> junctionHits(int x) {
            List<Hit> r = new ArrayList<>();
            for (ChainBlock b : blocks) {
                if (b.tStart() < x && x < b.tEnd()) {
                    int j = plus(b) ? b.qStart() + (x - b.tStart()) : b.qStart() + (b.tEnd() - x);
                    r.add(new Hit(b.qChrom(), j, j, b.strand(), b.score()));
                }
            }
            return r;
        }

        /** Max score wins, an exact tie for the max is ambiguous, no hit is unmapped. */
        public static Result best(List<Hit> hits) {
            Hit best = null;
            boolean tie = false;
            for (Hit h : hits) {
                if (best == null || h.score() > best.score()) {
                    best = h;
                    tie = false;
                } else if (h.score() == best.score()) {
                    tie = true;
                }
            }
            if (best == null) {
                return new Result(UNMAPPED, null);
            }
            return tie ? new Result(AMBIGUOUS, null) : new Result(MAPPED, best);
        }

        /** -snp at the 1-based position p. */
        public Result point(int p) {
            return best(pointHits(p));
        }

        /** -var at the 1-based pos with a REF of length refLen (0 is an empty REF: the junction after pos-1). */
        public Result var(int pos, int refLen) {
            return refLen == 0 ? best(junctionHits(pos - 1)) : best(intervalHits(pos, pos + refLen - 1));
        }

        /**
         * The mapped/ambiguous/other projection of the interval status for the 1-based s1..e1 (e1 == s1 - 1 is the
         * junction at boundary e1). Containment decides first; when no block contains a non-point interval, an
         * ambiguous endpoint makes it ambiguous.
         */
        public Result span(int s1, int e1) {
            if (e1 == s1 - 1) {
                Result j = best(junctionHits(e1));
                return UNMAPPED.equals(j.status()) ? new Result(OTHER, null) : j;
            }
            Result c = best(intervalHits(s1, e1));
            if (!UNMAPPED.equals(c.status())) {
                return c;
            }
            if (s1 != e1 && (AMBIGUOUS.equals(point(s1).status()) || AMBIGUOUS.equals(point(e1).status()))) {
                return new Result(AMBIGUOUS, null);
            }
            return new Result(OTHER, null);
        }

        /**
         * chain_blocks for s1..e1: blocks overlapping the 1-based interval (tStart+1 &lt;= e1 and tEnd &gt;= s1), or
         * for a junction the blocks strictly containing it.
         */
        public int count(int s1, int e1) {
            int n = 0;
            for (ChainBlock b : blocks) {
                boolean hit = e1 == s1 - 1
                        ? b.tStart() < e1 && e1 < b.tEnd()
                        : b.tStart() + 1 <= e1 && b.tEnd() >= s1;
                if (hit) {
                    n++;
                }
            }
            return n;
        }
    }

    public static List<ChainBlock> concat(List<ChainBlock> a, List<ChainBlock> b) {
        List<ChainBlock> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }
}
