package gorsat.liftover;

import gorsat.Commands.Analysis;
import gorsat.Commands.RowHeader;
import gorsat.parser.ParseUtilities;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The pipe step of LIFTOVER: moves every row to its lifted location on the target assembly, on the same engine as
 * LIFTOVER_ANNOTATE. LIFTOVER follows it with {@code sort genome}.
 * <p>
 * Each output row is the target location, the input columns after the location, the source location, the strand and
 * the status (plus the block score under -all). The status is the projection of the engine's status (N8): mapped,
 * ambiguous, or unmapped for everything else. Rows that do not map are placed at (source chrom, 0[, 1]) with strand
 * ''. Without -all every input row gives one output row. With -all (N14) every block containing the input gives a
 * mapped row with its score, and an input no block contains gives one unmapped row with score 0.0.
 * <p>
 * On the - strand, -var reverse-complements REF and ALT per allele (N16), and -bam reverse-complements SEQ, reverses
 * QUAL and reverses CIGAR. -bam lifts MPOS on every row (N15); an MPOS that is not an integer &gt; 0 passes through
 * unchanged (R18).
 */
public class LiftoverRelocateAnalysis extends Analysis {

    private static final String PREFIX = "LIFTOVER: ";
    private static final String STATS_SENDER = "Liftover";
    private static final ContigPolicy NO_POLICY = new ContigPolicy(Set.of(), null);
    /** Only used by the interval taxonomy, which the status projection collapses to unmapped. */
    private static final double TOLERANCE = 0.05;

    private final RelocateMode mode;
    private final boolean all;
    private final RelocateColumns cols;
    private final RowHeader outgoingHeader;
    private final Supplier<ChainSource> chainFactory;
    private ChainSource chain;
    private final Supplier<ChainSource> lazyChain = this::resolveChain;

    public LiftoverRelocateAnalysis(RelocateMode mode, boolean all, RelocateColumns cols, RowHeader outHeader,
                                    Supplier<ChainSource> chainFactory) {
        this.mode = mode;
        this.all = all;
        this.cols = cols;
        this.outgoingHeader = outHeader;
        this.chainFactory = chainFactory;
        statsSenderName_$eq(STATS_SENDER);
    }

    private ChainSource resolveChain() {
        if (chain == null) {
            chain = chainFactory.get();
        }
        return chain;
    }

    /**
     * A working copy of a step that stands alone, with the same configuration and its own lazily resolved chain.
     * The inherited clone would return a plain pass-through step that silently drops the liftover. A step connected
     * to other steps is refused, as in {@link LiftoverAnnotateAnalysis#clone()}.
     */
    @Override
    public Analysis clone() {
        if (pipeTo() != null || pipeFrom() != null) {
            throw new UnsupportedOperationException("LIFTOVER cannot be cloned inside a connected pipe");
        }
        LiftoverRelocateAnalysis copy = new LiftoverRelocateAnalysis(mode, all, cols, outgoingHeader, chainFactory);
        cloned_$eq(copy);
        init(copy);
        return copy;
    }

    @Override
    public boolean isTypeInformationMaintained() {
        return true;
    }

    @Override
    public void setRowHeader(RowHeader header) {
        rowHeader_$eq(header);
        if (pipeTo() != null) {
            pipeTo().setRowHeader(outgoingHeader.propagateTypes(header));
        }
    }

    @Override
    public void process(Row r) {
        switch (mode) {
            case SNP:
                snp(r);
                break;
            case VAR:
                var(r);
                break;
            default:
                span(r);
                break;
        }
    }

    /**
     * The status projection (N8) of a status that is not mapped (the callers place mapped rows themselves):
     * ambiguous stays, everything else is unmapped.
     */
    private static String project(String status) {
        return LiftEngine.AMBIGUOUS.equals(status) ? LiftEngine.AMBIGUOUS : LiftEngine.UNMAPPED;
    }

    private void snp(Row r) {
        String chrom = r.chr;
        int pos = r.pos;
        if (all) {
            List<LiftEngine.ScoredTarget> targets = LiftEngine.liftAll(resolveChain(), chrom, pos - 1, pos, true);
            for (LiftEngine.ScoredTarget st : targets) {
                LiftEngine.Target t = st.target();
                emit(r, t.chrom(), t.end0(), 0, t.strand(), LiftEngine.MAPPED, st.score(), null);
            }
            if (targets.isEmpty()) {
                emitUnplaced(r, LiftEngine.UNMAPPED);
            }
            return;
        }
        LiftEngine.PointResult p = LiftEngine.liftSnp(lazyChain, chrom, pos, NO_POLICY);
        if (LiftEngine.MAPPED.equals(p.status())) {
            emit(r, p.chrom(), p.pos(), 0, p.strand(), LiftEngine.MAPPED, 0.0, null);
        } else {
            emitUnplaced(r, project(p.status()));
        }
    }

    private void var(Row r) {
        String chrom = r.chr;
        int pos = r.pos;
        int refLen = r.colAsString(cols.ref()).length();
        if (all) {
            int s0 = pos - 1;
            List<LiftEngine.ScoredTarget> targets = LiftEngine.liftAll(resolveChain(), chrom, s0, s0 + refLen, false);
            for (LiftEngine.ScoredTarget st : targets) {
                LiftEngine.Target t = st.target();
                emit(r, t.chrom(), t.start0() + 1, 0, t.strand(), LiftEngine.MAPPED, st.score(), null);
            }
            if (targets.isEmpty()) {
                emitUnplaced(r, LiftEngine.UNMAPPED);
            }
            return;
        }
        LiftEngine.PointResult p = LiftEngine.liftVar(lazyChain, chrom, pos, refLen, NO_POLICY);
        if (LiftEngine.MAPPED.equals(p.status())) {
            emit(r, p.chrom(), p.pos(), 0, p.strand(), LiftEngine.MAPPED, 0.0, null);
        } else {
            emitUnplaced(r, project(p.status()));
        }
    }

    /** -seg is the GOR half-open [start, end), -bam the 1-based inclusive [Pos, End] (N12, N15). */
    private void span(Row r) {
        String chrom = r.chr;
        boolean bam = mode == RelocateMode.BAM;
        int start = r.pos;
        int end = endColumn(r);
        int s0 = bam ? start - 1 : start;
        if (end < s0) {
            throw new GorDataException(PREFIX + "invalid interval [" + start + "," + end + "] at " + chrom + ":"
                    + start, -1, getHeader(), r.toString());
        }
        if (all) {
            List<LiftEngine.ScoredTarget> targets = LiftEngine.liftAll(resolveChain(), chrom, s0, end, false);
            ContigIndex ci = bam && !targets.isEmpty() ? resolveChain().contig(chrom) : null;
            for (LiftEngine.ScoredTarget st : targets) {
                LiftEngine.Target t = st.target();
                String mpos = bam ? mate(r, chrom, ci, st.block(), t.chrom()) : null;
                emit(r, t.chrom(), bam ? t.start0() + 1 : t.start0(), t.end0(), t.strand(), LiftEngine.MAPPED,
                        st.score(), mpos);
            }
            if (targets.isEmpty()) {
                emitUnplaced(r, LiftEngine.UNMAPPED);
            }
            return;
        }
        LiftEngine.SpanResult s = LiftEngine.classifySpan(lazyChain, chrom, s0 + 1, end, NO_POLICY, TOLERANCE);
        if (!LiftEngine.MAPPED.equals(s.status())) {
            emitUnplaced(r, project(s.status()));
            return;
        }
        String mpos = null;
        if (bam) {
            // the block classifySpan mapped the read through, for MPOS (N15)
            ContigIndex ci = resolveChain().contig(chrom);
            mpos = mate(r, chrom, ci, ci.findSpan(s0, end), s.chrom());
        }
        emit(r, s.chrom(), bam ? s.start1() : s.start1() - 1, s.stop1(), s.strand(), LiftEngine.MAPPED, 0.0, mpos);
    }

    private int endColumn(Row r) {
        try {
            return r.colAsInt(2);
        } catch (NumberFormatException e) {
            throw new GorDataException(PREFIX + "Illegal stop position in column #3: '" + r.colAsString(2) + "'", 3,
                    getHeader(), r.toString(), e);
        }
    }

    /** MPOS as an integer &gt; 0, or -1 when it is not one: such a value is no mate position (R18). */
    private int matePosition(Row r) {
        try {
            int m = Integer.parseInt(r.colAsString(cols.mpos()).toString());
            return m > 0 ? m : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * N15 MPOS of a mapped read. An MPOS that is not an integer &gt; 0 passes through unchanged (R18). Otherwise it is
     * 0 when an MRNM column puts the mate elsewhere ('*', or a contig other than the read's), else MPOS lifted through
     * the read's block, or the best block, kept only on the read's target chrom (else 0).
     */
    private String mate(Row r, String chrom, ContigIndex ci, int block, String targetChrom) {
        int m = matePosition(r);
        if (m < 0) {
            return r.colAsString(cols.mpos()).toString();
        }
        if (cols.mrnm() >= 0 && !sameContig(r.colAsString(cols.mrnm()).toString(), chrom)) {
            return "0";
        }
        return Integer.toString(LiftEngine.liftMate(ci, block, m, targetChrom));
    }

    /** MPOS of a read that does not map: 0 for a mate position, any other value unchanged (N15, R18). */
    private String unplacedMate(Row r) {
        return matePosition(r) > 0 ? "0" : r.colAsString(cols.mpos()).toString();
    }

    /**
     * MRNM names the read's contig: '=', or the same name ignoring case and a leading chr, with M and MT equal. '*'
     * (no mate contig) never matches.
     */
    private static boolean sameContig(String mrnm, String chrom) {
        return mrnm.equals("=") || contigKey(mrnm).equals(contigKey(chrom));
    }

    private static String contigKey(String c) {
        String k = c.regionMatches(true, 0, "chr", 0, 3) ? c.substring(3) : c;
        k = k.toUpperCase(Locale.ROOT);
        return k.equals("M") ? "MT" : k;
    }

    /** A row that does not map: placed at (source chrom, 0[, 1]), strand '', bam MPOS 0 (R18), -all score 0.0. */
    private void emitUnplaced(Row r, String status) {
        emit(r, r.chr, 0, 1, "", status, 0.0, mode == RelocateMode.BAM ? unplacedMate(r) : null);
    }

    /**
     * Emits tChrom, a[, b], the input columns after the location (rewritten on the - strand), the source location,
     * strand and status, and the score under -all. mpos replaces the -bam MPOS column.
     */
    private void emit(Row r, String tChrom, int a, int b, String strand, String status, double score, String mpos) {
        statsInc(status);
        boolean span = mode.isSpan();
        boolean minus = "-".equals(strand);
        int n = r.numCols();
        StringBuilder sb = new StringBuilder(r.length() + 64);
        sb.append(tChrom).append('\t').append(a);
        if (span) {
            sb.append('\t').append(b);
        }
        for (int k = span ? 3 : 2; k < n; k++) {
            sb.append('\t').append(inputColumn(r, k, minus, mpos));
        }
        sb.append('\t').append(r.chr).append('\t').append(r.colAsString(1));
        if (span) {
            sb.append('\t').append(r.colAsString(2));
        }
        sb.append('\t').append(strand).append('\t').append(status);
        if (all) {
            sb.append('\t').append(Double.toString(score));
        }
        super.process(RowObj.apply(sb));
    }

    private CharSequence inputColumn(Row r, int k, boolean minus, String mpos) {
        if (mode == RelocateMode.VAR) {
            if (minus && (k == cols.ref() || k == cols.alt())) {
                return LiftEngine.revcompAlleles(r.colAsString(k).toString());
            }
        } else if (mode == RelocateMode.BAM) {
            if (k == cols.mpos()) {
                return mpos;
            }
            if (minus) {
                if (k == cols.seq()) {
                    return ParseUtilities.reverseComplement(r.colAsString(k).toString());
                }
                if (k == cols.qual()) {
                    return new StringBuilder(r.colAsString(k)).reverse();
                }
                if (k == cols.cigar()) {
                    return ParseUtilities.revCigar(r.colAsString(k).toString());
                }
            }
        }
        return r.colAsString(k);
    }

    @Override
    public void finish() {
        // stateless: nothing to flush
    }
}
