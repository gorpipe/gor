package gorsat.liftover;

import gorsat.parser.ParseUtilities;
import org.gorpipe.exceptions.GorDataException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The pure liftover engine: one coordinate formula ({@link #liftAt}) and the full status taxonomy, shared by
 * LIFTOVER_ANNOTATE and LIFTOVER. Stateless and thread safe.
 *
 * Every input is normalised to a 0-based half-open source interval [s0, e0) (N12): a point p is [p-1, p), a
 * variant [pos-1, pos-1+len(REF)), a 1-based inclusive segment s1..e1 is [s1-1, e1). e0 == s0 is a junction, which
 * maps only through a block that strictly contains it. A span is mapped when one best block contains it entirely
 * (N8); only otherwise are its two endpoints lifted on their own to classify it.
 */
public final class LiftEngine {

    public static final String PASSTHROUGH = "passthrough";
    public static final String UNSUPPORTED_CONTIG = "unsupported_contig";
    public static final String AMBIGUOUS = "ambiguous";
    public static final String UNMAPPED = "unmapped";
    public static final String START_UNMAPPED = "start_unmapped";
    public static final String STOP_UNMAPPED = "stop_unmapped";
    public static final String SPLIT_CONTIG = "split_contig";
    public static final String INVERTED = "inverted";
    public static final String MAPPED = "mapped";
    public static final String MAPPED_ACROSS_GAP = "mapped_across_gap";
    public static final String SIZE_CHANGED = "size_changed";

    /** The statuses whose rows carry a target location. */
    public static final Set<String> HAS_TARGET = Set.of(MAPPED, MAPPED_ACROSS_GAP, SIZE_CHANGED, PASSTHROUGH);

    /** A lifted 1-based point on a target contig. */
    public record Endpoint(String chrom, int pos, String strand) {
    }

    /** An endpoint that does not lift. */
    public static final Endpoint NO_ENDPOINT = new Endpoint("", 0, "");

    /** A raw point lookup: the block index, or {@link ContigIndex#UNMAPPED}/{@link ContigIndex#AMBIGUOUS}. */
    public record EndpointLift(int block, Endpoint endpoint) {
    }

    /** A -snp/-var result; without a target it is (status, "", 0, ""). */
    public record PointResult(String status, String chrom, int pos, String strand) {
    }

    /**
     * A span result. start1/stop1 are 1-based inclusive (a junction j is j+1..j); without a target the location is
     * "", 0, 0, "". start and stop are the diagnostic endpoint lifts.
     */
    public record SpanResult(String status, String chrom, int start1, int stop1, String strand, int chainBlocks,
                             int sourceSize, int targetSize, double sizeRatio, Endpoint start, Endpoint stop) {
    }

    /** A 0-based half-open target: a point p' is [p'-1, p'), a junction j is [j, j). */
    public record Target(String chrom, int start0, int end0, String strand) {
    }

    /** A target with the score of the block it came through. */
    public record ScoredTarget(Target target, double score) {
    }

    private static final EndpointLift NO_LIFT = new EndpointLift(ContigIndex.UNMAPPED, NO_ENDPOINT);

    private LiftEngine() {
    }

    /**
     * The only coordinate formula: lifts the 0-based half-open source span [s0, e0) through block i of ci.
     * + gives [qStart+s0-tStart, qStart+e0-tStart), - gives [qStart+tEnd-e0, qStart+tEnd-s0).
     */
    static Target liftAt(ContigIndex ci, int i, int s0, int e0) {
        int q = ci.qStart(i);
        if (ci.minus(i)) {
            int te = ci.tEnd(i);
            return new Target(ci.qChrom(i), q + te - e0, q + te - s0, "-");
        }
        int ts = ci.tStart(i);
        return new Target(ci.qChrom(i), q + s0 - ts, q + e0 - ts, "+");
    }

    /** The raw best-block lookup of the 1-based point p, with no ContigPolicy. */
    static EndpointLift liftEndpoint(ChainSource chain, String chrom, int p) {
        return endpoint(chain.contig(chrom), p);
    }

    private static EndpointLift endpoint(ContigIndex ci, int p) {
        int i = ci.findPoint(p);
        if (i < 0) {
            return i == ContigIndex.AMBIGUOUS ? new EndpointLift(i, NO_ENDPOINT) : NO_LIFT;
        }
        return new EndpointLift(i, pointThrough(ci, i, p));
    }

    /** The 1-based point p lifted through block i: pos is end0 of the lifted [p-1, p). */
    private static Endpoint pointThrough(ContigIndex ci, int i, int p) {
        Target t = liftAt(ci, i, p - 1, p);
        return new Endpoint(t.chrom(), t.end0(), t.strand());
    }

    private static PointResult noPoint(String status) {
        return new PointResult(status, "", 0, "");
    }

    /** -snp: lifts the 1-based pos. */
    public static PointResult liftSnp(Supplier<ChainSource> chain, String chrom, int pos, ContigPolicy policy) {
        if (policy.isPassthrough(chrom)) {
            return new PointResult(PASSTHROUGH, chrom, pos, "");
        }
        if (!policy.allowsTarget(chrom)) {
            return noPoint(UNSUPPORTED_CONTIG);
        }
        EndpointLift lift = endpoint(chain.get().contig(chrom), pos);
        if (lift.block() == ContigIndex.AMBIGUOUS) {
            return noPoint(AMBIGUOUS);
        }
        if (lift.block() < 0) {
            return noPoint(UNMAPPED);
        }
        Endpoint e = lift.endpoint();
        if (!policy.allowsTarget(e.chrom())) {
            return noPoint(UNSUPPORTED_CONTIG);
        }
        return new PointResult(MAPPED, e.chrom(), e.pos(), e.strand());
    }

    /**
     * -var: lifts [pos-1, pos-1+refLen) by containment and reports the 1-based start of the target. An empty REF
     * (refLen 0) is the junction at pos-1.
     */
    public static PointResult liftVar(Supplier<ChainSource> chain, String chrom, int pos, int refLen,
                                      ContigPolicy policy) {
        if (refLen < 0) {
            throw new IllegalArgumentException("negative REF length " + refLen + " at " + chrom + ":" + pos);
        }
        if (policy.isPassthrough(chrom)) {
            return new PointResult(PASSTHROUGH, chrom, pos, "");
        }
        if (!policy.allowsTarget(chrom)) {
            return noPoint(UNSUPPORTED_CONTIG);
        }
        ContigIndex ci = chain.get().contig(chrom);
        int s0 = pos - 1;
        int e0 = s0 + refLen;
        int i = ci.findSpan(s0, e0);
        if (i == ContigIndex.AMBIGUOUS) {
            return noPoint(AMBIGUOUS);
        }
        if (i < 0) {
            return noPoint(UNMAPPED);
        }
        Target t = liftAt(ci, i, s0, e0);
        if (!policy.allowsTarget(t.chrom())) {
            return noPoint(UNSUPPORTED_CONTIG);
        }
        return new PointResult(MAPPED, t.chrom(), t.start0() + 1, t.strand());
    }

    /**
     * Classifies the 1-based inclusive span s1..e1 on chrom; e1 == s1 - 1 is a junction (N12), e1 &lt; s1 - 1
     * throws. A span that one best block contains is mapped (N8); otherwise the endpoint taxonomy decides.
     */
    public static SpanResult classifySpan(Supplier<ChainSource> chain, String chrom, int s1, int e1,
                                          ContigPolicy policy, double tolerance) {
        int s0 = s1 - 1;
        int e0 = e1;
        if (e0 < s0) {
            throw new GorDataException("LIFTOVER_ANNOTATE: invalid interval at " + chrom + ":" + s1 + "-" + e1);
        }
        int src = Math.max(e1 - s1, 0);
        if (policy.isPassthrough(chrom)) {
            return new SpanResult(PASSTHROUGH, chrom, s1, e1, "", 0, src, src, 1.0, NO_ENDPOINT, NO_ENDPOINT);
        }
        if (!policy.allowsTarget(chrom)) {
            return new SpanResult(UNSUPPORTED_CONTIG, "", 0, 0, "", 0, src, 0, 0.0, NO_ENDPOINT, NO_ENDPOINT);
        }
        ContigIndex ci = chain.get().contig(chrom);
        // for a junction (s1 = e1 + 1): the blocks that strictly contain it
        int n = ci.countBlocks(s1, e1);
        boolean junction = e0 == s0;
        EndpointLift sl = junction ? NO_LIFT : endpoint(ci, s1);
        EndpointLift el = junction ? NO_LIFT : (s1 == e1 ? sl : endpoint(ci, e1));
        Endpoint s = sl.endpoint();
        Endpoint e = el.endpoint();
        boolean sameContig = sl.block() >= 0 && el.block() >= 0 && s.chrom().equals(e.chrom());

        // N4: target size and ratio of the two endpoint lifts, when both lift to one contig; a point span has 0/0.0
        int tgt = 0;
        double ratio = 0.0;
        if (sameContig && s1 != e1) {
            tgt = Math.abs(e.pos() - s.pos());
            ratio = (double) tgt / (e1 - s1);
        }

        int i = ci.findSpan(s0, e0);
        if (i == ContigIndex.AMBIGUOUS) {
            return noTarget(AMBIGUOUS, n, src, tgt, ratio, s, e);
        }
        if (i >= 0) {
            Target t = liftAt(ci, i, s0, e0);
            if (!policy.allowsTarget(t.chrom())) {
                return noTarget(UNSUPPORTED_CONTIG, n, src, tgt, ratio, s, e);
            }
            Endpoint a = junction ? NO_ENDPOINT : pointThrough(ci, i, s1);
            Endpoint b = junction ? NO_ENDPOINT : pointThrough(ci, i, e1);
            return new SpanResult(MAPPED, t.chrom(), t.start0() + 1, t.end0(), t.strand(), n, src, src, 1.0, a, b);
        }
        if (junction || s1 == e1) {
            return noTarget(UNMAPPED, n, src, tgt, ratio, s, e);
        }
        if (sl.block() == ContigIndex.AMBIGUOUS || el.block() == ContigIndex.AMBIGUOUS) {
            return noTarget(AMBIGUOUS, n, src, tgt, ratio, s, e);
        }
        boolean su = sl.block() < 0;
        boolean eu = el.block() < 0;
        if (su && eu) {
            return noTarget(UNMAPPED, n, src, tgt, ratio, s, e);
        }
        if (su) {
            return noTarget(START_UNMAPPED, n, src, tgt, ratio, s, e);
        }
        if (eu) {
            return noTarget(STOP_UNMAPPED, n, src, tgt, ratio, s, e);
        }
        if (!sameContig) {
            return noTarget(SPLIT_CONTIG, n, src, tgt, ratio, s, e);
        }
        if (!policy.allowsTarget(s.chrom())) {
            return noTarget(UNSUPPORTED_CONTIG, n, src, tgt, ratio, s, e);
        }
        int sp = s.pos();
        int ep = e.pos();
        boolean plus = "+".equals(s.strand());
        if (!s.strand().equals(e.strand()) || (plus && ep <= sp) || (!plus && ep >= sp)) {
            return noTarget(INVERTED, n, src, tgt, ratio, s, e);
        }
        String status = (1.0 - tolerance) < ratio && ratio < (1.0 + tolerance) ? MAPPED_ACROSS_GAP : SIZE_CHANGED;
        return new SpanResult(status, s.chrom(), Math.min(sp, ep), Math.max(sp, ep), s.strand(), n, src, tgt, ratio,
                s, e);
    }

    private static SpanResult noTarget(String status, int n, int src, int tgt, double ratio, Endpoint s,
                                       Endpoint e) {
        return new SpanResult(status, "", 0, 0, "", n, src, tgt, ratio, s, e);
    }

    /**
     * Every block containing the input, in ascending block index, with its score; empty when none (N14). A point
     * uses the 1-based point e0 (tStart &lt; e0 &lt;= tEnd), otherwise [s0, e0) is an interval or a junction (N12).
     */
    public static List<ScoredTarget> liftAll(ChainSource chain, String chrom, int s0, int e0, boolean point) {
        ContigIndex ci = chain.contig(chrom);
        int[] blocks = point ? ci.allPoint(e0) : ci.allSpan(s0, e0);
        List<ScoredTarget> r = new ArrayList<>(blocks.length);
        for (int i : blocks) {
            Target t = point ? liftAt(ci, i, e0 - 1, e0) : liftAt(ci, i, s0, e0);
            r.add(new ScoredTarget(t, ci.score(i)));
        }
        return r;
    }

    /**
     * Reverse-complements each comma-separated allele (N16). Symbolic alleles (starting with '&lt;'), '*', '.'
     * and breakends (containing '[' or ']') are kept as they are.
     */
    public static String revcompAlleles(String s) {
        String[] alleles = s.split(",", -1);
        StringBuilder sb = new StringBuilder(s.length());
        for (int k = 0; k < alleles.length; k++) {
            if (k > 0) {
                sb.append(',');
            }
            String a = alleles[k];
            boolean keep = a.startsWith("<") || a.equals("*") || a.equals(".") || a.indexOf('[') >= 0
                    || a.indexOf(']') >= 0;
            sb.append(keep ? a : ParseUtilities.reverseComplement(a));
        }
        return sb.toString();
    }
}
