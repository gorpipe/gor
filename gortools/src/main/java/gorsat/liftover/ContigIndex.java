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
 *
 * Gap blocks (ENGKNOW-4208) span an equal double-sided gap between two consecutive blocks of one chain, with no other
 * block in it, and lift it by offset, as if the chain had been merged across it. They have indexes {@link #size()} to
 * size() + {@link #gapCount()} - 1 and every accessor takes them. {@link #nextInChain} and {@link #prevInChain} walk
 * a chain across abutting blocks and gap blocks; along such a run the offset does not change, so any block of it lifts
 * every position of the run.
 *
 * A lookup that no real block answers falls back to a gap block that contains the input, and a span that no single
 * block contains falls back to a run through at least one gap block that does, reported as the run's first block. A
 * run of abutting blocks alone is not used, so chains without filled gaps behave as before.
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
    /** The gap blocks, an index of their own, or null when there are none. */
    private final ContigIndex gaps;
    /** Chain links over real and gap indexes, -1 at a break; null when there are no blocks. */
    private final int[] next;
    private final int[] prev;

    ContigIndex(String contig, int size, int[] tStart, int[] tEnd, int[] qStart, int[] prefMaxEnd, byte[] minus,
                double[] score, String[] qChrom, boolean overlapping) {
        this(contig, size, tStart, tEnd, qStart, prefMaxEnd, minus, score, qChrom, overlapping, null, null, null);
    }

    ContigIndex(String contig, int size, int[] tStart, int[] tEnd, int[] qStart, int[] prefMaxEnd, byte[] minus,
                double[] score, String[] qChrom, boolean overlapping, ContigIndex gaps, int[] next, int[] prev) {
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
        this.gaps = gaps;
        this.next = next;
        this.prev = prev;
    }

    public static ContigIndex empty(String contig) {
        return new ContigIndex(contig, 0, NONE, NONE, NONE, NONE, new byte[0], new double[0], new String[0], false);
    }

    public static ContigIndex of(String contig, OverlapPolicy policy, List<ChainBlock> blocks) {
        return of(contig, policy, blocks, "<memory>");
    }

    public static ContigIndex of(String contig, OverlapPolicy policy, List<ChainBlock> blocks, String source) {
        return of(contig, policy, blocks, source, ContigIndexBuilder.maxGapFillProperty());
    }

    /** As above, filling equal gaps of at most maxGapFill bases (0, as with -nogapfill, fills none). */
    public static ContigIndex of(String contig, OverlapPolicy policy, List<ChainBlock> blocks, String source,
                                 int maxGapFill) {
        ContigIndexBuilder builder = new ContigIndexBuilder(contig, policy, source, maxGapFill);
        for (ChainBlock b : blocks) {
            builder.add(b);
        }
        return builder.build();
    }

    public String contig() {
        return contig;
    }

    /** The number of real blocks. */
    public int size() {
        return size;
    }

    /** The number of gap blocks, whose indexes follow the real blocks. */
    public int gapCount() {
        return gaps == null ? 0 : gaps.size;
    }

    /** True when i is a gap block. */
    public boolean isGap(int i) {
        return i >= size;
    }

    public int tStart(int i) {
        return i < size ? tStart[i] : gaps.tStart[i - size];
    }

    public int tEnd(int i) {
        return i < size ? tEnd[i] : gaps.tEnd[i - size];
    }

    public String qChrom(int i) {
        return i < size ? qChrom[i] : gaps.qChrom[i - size];
    }

    public int qStart(int i) {
        return i < size ? qStart[i] : gaps.qStart[i - size];
    }

    public boolean minus(int i) {
        return (i < size ? minus[i] : gaps.minus[i - size]) != 0;
    }

    public double score(int i) {
        return i < size ? score[i] : gaps.score[i - size];
    }

    /**
     * The block that continues the chain of block i (real or gap) without a break: the next block of the chain when
     * it abuts i on both sides, the gap block when an equal gap follows, or -1.
     */
    public int nextInChain(int i) {
        return next == null || i < 0 || i >= next.length ? -1 : next[i];
    }

    /** The block that i continues without a break, the mirror of {@link #nextInChain}, or -1. */
    public int prevInChain(int i) {
        return prev == null || i < 0 || i >= prev.length ? -1 : prev[i];
    }

    /** Best block containing the 1-based position p, that is tStart &lt; p &lt;= tEnd. */
    public int findPoint(int p) {
        return orGap(contain(p - 1, p), p - 1, p);
    }

    /**
     * Best block containing the 0-based half-open span [s0, e0) entirely. A zero-length span (a junction) is
     * contained when tStart &lt; s0 &lt; tEnd, and e0 &lt; s0 is never contained.
     */
    public int findSpan(int s0, int e0) {
        if (e0 < s0) {
            return UNMAPPED;
        }
        int i = e0 == s0 ? orGap(contain(s0 - 1, s0 + 1), s0 - 1, s0 + 1) : orGap(contain(s0, e0), s0, e0);
        return i == UNMAPPED ? run(s0, e0) : i;
    }

    /** The block (real or gap) that lifts the 0-based base b, or UNMAPPED/AMBIGUOUS. */
    private int base(int b) {
        return orGap(contain(b, b + 1), b, b + 1);
    }

    /**
     * The first block of a run that contains the span [s0, e0) and crosses at least one gap block, or UNMAPPED. A
     * junction is contained when the bases on both sides of it are.
     */
    private int run(int s0, int e0) {
        if (gaps == null) {
            return UNMAPPED;
        }
        int first = e0 == s0 ? base(s0 - 1) : base(s0);
        int last = e0 == s0 ? base(s0) : base(e0 - 1);
        if (first < 0 || last < 0) {
            return UNMAPPED;
        }
        int lastStart = tStart(last);
        boolean gap = isGap(first);
        for (int i = first; i != last; ) {
            i = next[i];
            if (i < 0 || tStart(i) > lastStart) {
                return UNMAPPED;
            }
            gap |= isGap(i);
        }
        return gap ? first : UNMAPPED;
    }

    /** A real lookup result, or the best gap block containing [a, b] when no real block does. */
    private int orGap(int real, int a, int b) {
        if (real != UNMAPPED || gaps == null) {
            return real;
        }
        int g = gaps.contain(a, b);
        return g < 0 ? g : size + g;
    }

    /** All blocks containing the 1-based position p, in ascending block index. */
    public int[] allPoint(int p) {
        return orGaps(collect(p - 1, p), p - 1, p);
    }

    /** All blocks containing the span, same rule as {@link #findSpan}, in ascending block index. */
    public int[] allSpan(int s0, int e0) {
        if (e0 < s0) {
            return NONE;
        }
        int[] r = e0 == s0 ? orGaps(collect(s0 - 1, s0 + 1), s0 - 1, s0 + 1) : orGaps(collect(s0, e0), s0, e0);
        if (r.length > 0) {
            return r;
        }
        int i = run(s0, e0);
        return i < 0 ? NONE : new int[]{i};
    }

    private int[] orGaps(int[] real, int a, int b) {
        if (real.length > 0 || gaps == null) {
            return real;
        }
        int[] g = gaps.collect(a, b);
        for (int k = 0; k < g.length; k++) {
            g[k] += size;
        }
        return g;
    }

    /**
     * Number of blocks, gap blocks included, overlapping the 1-based inclusive span [s1, e1]: tStart &lt; e1 and
     * tEnd &gt;= s1.
     */
    public int countBlocks(int s1, int e1) {
        return countReal(s1, e1) + (gaps == null ? 0 : gaps.countReal(s1, e1));
    }

    private int countReal(int s1, int e1) {
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
