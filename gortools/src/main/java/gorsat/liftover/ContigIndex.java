package gorsat.liftover;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Immutable index of the chain blocks of one source contig, sorted by tStart (0-based half-open). Lookups are
 * binary searches on tStart plus a bounded backwards scan, bounded by a prefix maximum of tEnd, so overlapping
 * blocks (overlap policy best) are supported.
 *
 * Block indexes are positions in tStart order. Lookups return a block index, {@link #UNMAPPED} when no block
 * contains the position or span, or {@link #AMBIGUOUS} when several contain it with exactly the same best score.
 */
public final class ContigIndex {

    public static final int UNMAPPED = -1;
    public static final int AMBIGUOUS = -2;

    private static final int[] NONE = new int[0];

    private final String contig;
    private final int size;
    private final int[] tStart;
    private final int[] tEnd;
    private final int[] qStart;
    private final int[] prefMaxEnd;
    private final byte[] minus;
    private final double[] score;
    private final String[] qChrom;
    private final boolean overlapping;

    ContigIndex(String contig, int size, int[] tStart, int[] tEnd, int[] qStart, int[] prefMaxEnd, byte[] minus,
                double[] score, String[] qChrom, boolean overlapping) {
        this.contig = contig;
        this.size = size;
        this.tStart = tStart;
        this.tEnd = tEnd;
        this.qStart = qStart;
        this.prefMaxEnd = prefMaxEnd;
        this.minus = minus;
        this.score = score;
        this.qChrom = qChrom;
        this.overlapping = overlapping;
    }

    public static ContigIndex empty(String contig) {
        return new ContigIndex(contig, 0, NONE, NONE, NONE, NONE, new byte[0], new double[0], new String[0], false);
    }

    public static ContigIndex of(String contig, OverlapPolicy policy, List<ChainBlock> blocks) {
        return of(contig, policy, blocks, "<memory>");
    }

    public static ContigIndex of(String contig, OverlapPolicy policy, List<ChainBlock> blocks, String source) {
        ContigIndexBuilder builder = new ContigIndexBuilder(contig, policy, source);
        for (ChainBlock b : blocks) {
            builder.add(b);
        }
        return builder.build();
    }

    public String contig() {
        return contig;
    }

    public int size() {
        return size;
    }

    public int tStart(int i) {
        return tStart[i];
    }

    public int tEnd(int i) {
        return tEnd[i];
    }

    public String qChrom(int i) {
        return qChrom[i];
    }

    public int qStart(int i) {
        return qStart[i];
    }

    public boolean minus(int i) {
        return minus[i] != 0;
    }

    public double score(int i) {
        return score[i];
    }

    /** Best block containing the 1-based position p, that is tStart &lt; p &lt;= tEnd. */
    public int findPoint(int p) {
        return contain(p - 1, p);
    }

    /**
     * Best block containing the 0-based half-open span [s0, e0) entirely. A zero-length span (a junction) is
     * contained when tStart &lt; s0 &lt; tEnd, and e0 &lt; s0 is never contained.
     */
    public int findSpan(int s0, int e0) {
        if (e0 < s0) {
            return UNMAPPED;
        }
        return e0 == s0 ? contain(s0 - 1, s0 + 1) : contain(s0, e0);
    }

    /** All blocks containing the 1-based position p, in ascending block index. */
    public int[] allPoint(int p) {
        return collect(p - 1, p);
    }

    /** All blocks containing the span, same rule as {@link #findSpan}, in ascending block index. */
    public int[] allSpan(int s0, int e0) {
        if (e0 < s0) {
            return NONE;
        }
        return e0 == s0 ? collect(s0 - 1, s0 + 1) : collect(s0, e0);
    }

    /** Number of blocks overlapping the 1-based inclusive span [s1, e1]: tStart &lt; e1 and tEnd &gt;= s1. */
    public int countBlocks(int s1, int e1) {
        int hi = lowerBound(tStart, e1);
        if (!overlapping) {
            // tEnd is sorted too when the blocks do not overlap
            return Math.max(0, hi - lowerBound(tEnd, s1));
        }
        int count = 0;
        for (int i = hi - 1; i >= 0 && prefMaxEnd[i] >= s1; i--) {
            if (tEnd[i] >= s1) {
                count++;
            }
        }
        return count;
    }

    /**
     * The one candidate scan: calls f, in descending block index, for every block with tStart &lt;= a and
     * tEnd &gt;= b. prefMaxEnd is non-decreasing, so the scan stops at the first block whose prefix maximum
     * is already below b.
     */
    private void foreachContaining(int a, int b, IntConsumer f) {
        int i = lowerBound(tStart, a + 1) - 1;
        while (i >= 0 && prefMaxEnd[i] >= b) {
            if (tEnd[i] >= b) {
                f.accept(i);
            }
            i--;
        }
    }

    private int contain(int a, int b) {
        Pick pick = new Pick(score);
        foreachContaining(a, b, pick);
        return pick.tie ? AMBIGUOUS : pick.best;
    }

    private int[] collect(int a, int b) {
        Collect c = new Collect();
        foreachContaining(a, b, c);
        return c.ascending();
    }

    /** Number of elements of the sorted array that are &lt; x. */
    private int lowerBound(int[] sorted, int x) {
        int lo = 0;
        int hi = size;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid] < x) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static final class Pick implements IntConsumer {
        private final double[] score;
        int best = UNMAPPED;
        double bestScore = Double.NEGATIVE_INFINITY;
        boolean tie;

        Pick(double[] score) {
            this.score = score;
        }

        @Override
        public void accept(int i) {
            double s = score[i];
            if (s > bestScore) {
                best = i;
                bestScore = s;
                tie = false;
            } else if (s == bestScore) {
                tie = true;
            }
        }
    }

    private static final class Collect implements IntConsumer {
        private int[] found = new int[4];
        private int n;

        @Override
        public void accept(int i) {
            if (n == found.length) {
                found = Arrays.copyOf(found, n * 2);
            }
            found[n++] = i;
        }

        int[] ascending() {
            if (n == 0) {
                return NONE;
            }
            int[] r = new int[n];
            for (int k = 0; k < n; k++) {
                r[k] = found[n - 1 - k];
            }
            return r;
        }
    }
}
