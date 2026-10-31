package gorsat.liftover;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static List<ChainBlock> concat(List<ChainBlock> a, List<ChainBlock> b) {
        List<ChainBlock> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }
}
