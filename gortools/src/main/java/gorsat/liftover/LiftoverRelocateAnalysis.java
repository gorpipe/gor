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
 * <p>
 * With -mapped, -passthrough or -failon (fullStatus, ENGKNOW-4191) the status is the engine's own, as in
 * LIFTOVER_ANNOTATE, and the contig policy applies: a passthrough row keeps its location, strand '', without the
 * chain; a -seg row that is mapped_across_gap or size_changed is placed at its target (a -bam read is not: its SEQ,
 * QUAL and CIGAR would not fit the lifted span); every other row that does not map is placed at
 * (source chrom, 0[, 1]) as before. A row whose status is in failOn fails the query.
 * <p>
 * With -var -aligned (ENGKNOW-4207) each row also carries its aligned extent ({@link LiftEngine#alignedExtent}): the
 * target bases left and right of the lifted position in the same gap-free alignment, after the status. A row that
 * does not lift through the chain (unmapped, ambiguous, unsupported_contig, passthrough) has 0 and 0.
 */
public class LiftoverRelocateAnalysis extends Analysis {

    private static final String PREFIX = "LIFTOVER: ";
    private static final String STATS_SENDER = "Liftover";
    private static final ContigPolicy NO_POLICY = new ContigPolicy(Set.of(), null);

    private final RelocateMode mode;
    private final boolean all;
    private final RelocateColumns cols;
    private final RowHeader outgoingHeader;
    private final Supplier<ChainSource> chainFactory;
    private final boolean fullStatus;
    private final ContigPolicy policy;
    private final Set<String> failOn;
    /** The mapped_across_gap / size_changed boundary; without fullStatus both are reported as unmapped. */
    private final double tolerance;
    /** -var -aligned: emit the aligned extent of every row. */
    private final boolean aligned;
    private ChainSource chain;
    private final Supplier<ChainSource> lazyChain = this::resolveChain;

    /** The step without -mapped, -passthrough or -failon: the projected statuses, no contig policy. */
    public LiftoverRelocateAnalysis(RelocateMode mode, boolean all, RelocateColumns cols, RowHeader outHeader,
                                    Supplier<ChainSource> chainFactory) {
        this(mode, all, cols, outHeader, chainFactory, false, NO_POLICY, Set.of(), LiftoverArgs.DEFAULT_TOLERANCE,
                false);
    }

    /**
     * fullStatus reports the engine's statuses and places passthrough rows, and -seg mapped_across_gap and
     * size_changed rows (set by -mapped, -passthrough, -failon or -tolerance, which give policy, failOn and
     * tolerance). fullStatus cannot be combined with all.
     */
    public LiftoverRelocateAnalysis(RelocateMode mode, boolean all, RelocateColumns cols, RowHeader outHeader,
                                    Supplier<ChainSource> chainFactory, boolean fullStatus, ContigPolicy policy,
                                    Set<String> failOn, double tolerance) {
        this(mode, all, cols, outHeader, chainFactory, fullStatus, policy, failOn, tolerance, false);
    }

    /** As the step above, and with aligned (-var -aligned, ENGKNOW-4207) each row carries its aligned extent. */
    public LiftoverRelocateAnalysis(RelocateMode mode, boolean all, RelocateColumns cols, RowHeader outHeader,
                                    Supplier<ChainSource> chainFactory, boolean fullStatus, ContigPolicy policy,
                                    Set<String> failOn, double tolerance, boolean aligned) {
        if (fullStatus && all) {
            throw new IllegalArgumentException("fullStatus cannot be combined with all");
        }
        if (aligned && mode != RelocateMode.VAR) {
            throw new IllegalArgumentException("aligned is only valid with VAR");
        }
        this.mode = mode;
        this.all = all;
        this.cols = cols;
        this.outgoingHeader = outHeader;
        this.chainFactory = chainFactory;
        this.fullStatus = fullStatus;
        this.policy = policy == null ? NO_POLICY : policy;
        this.failOn = failOn == null ? Set.of() : Set.copyOf(failOn);
        this.tolerance = tolerance;
        this.aligned = aligned;
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
        LiftoverRelocateAnalysis copy = new LiftoverRelocateAnalysis(mode, all, cols, outgoingHeader, chainFactory,
                fullStatus, policy, failOn, tolerance, aligned);
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
     * The reported status of a row that is not mapped (the callers place mapped rows themselves). Under fullStatus
     * the engine's status; otherwise its projection (N8): ambiguous stays, everything else is unmapped.
     */
    private String reportedStatus(String status) {
        if (fullStatus) {
            return status;
        }
        return LiftEngine.AMBIGUOUS.equals(status) ? LiftEngine.AMBIGUOUS : LiftEngine.UNMAPPED;
    }

    private void snp(Row r) {
        String chrom = r.chr;
        int pos = startColumn(r);
        if (all) {
            List<LiftEngine.ScoredTarget> targets = LiftEngine.liftAll(resolveChain(), chrom, pos - 1, pos, true);
            for (LiftEngine.ScoredTarget st : targets) {
                LiftEngine.Target t = st.target();
                emit(r, t.chrom(), t.end0(), 0, t.strand(), LiftEngine.MAPPED, st.score(), null,
                        LiftEngine.Extent.NONE);
            }
            if (targets.isEmpty()) {
                emitUnplaced(r, LiftEngine.UNMAPPED);
            }
            return;
        }
        LiftEngine.PointResult p = LiftEngine.liftSnp(lazyChain, chrom, pos, policy);
        if (LiftEngine.MAPPED.equals(p.status()) || LiftEngine.PASSTHROUGH.equals(p.status())) {
            emit(r, p.chrom(), p.pos(), 0, p.strand(), p.status(), 0.0, null, LiftEngine.Extent.NONE);
        } else {
            emitUnplaced(r, reportedStatus(p.status()));
        }
    }

    private void var(Row r) {
        String chrom = r.chr;
        int pos = startColumn(r);
        int refLen = r.colAsString(cols.ref()).length();
        if (all) {
            int s0 = pos - 1;
            List<LiftEngine.ScoredTarget> targets = LiftEngine.liftAll(resolveChain(), chrom, s0, s0 + refLen, false);
            ContigIndex ci = aligned && !targets.isEmpty() ? resolveChain().contig(chrom) : null;
            for (LiftEngine.ScoredTarget st : targets) {
                LiftEngine.Target t = st.target();
                LiftEngine.Extent extent = ci == null ? LiftEngine.Extent.NONE
                        : LiftEngine.alignedExtent(ci, st.block(), pos, refLen);
                emit(r, t.chrom(), t.start0() + 1, 0, t.strand(), LiftEngine.MAPPED, st.score(), null, extent);
            }
            if (targets.isEmpty()) {
                emitUnplaced(r, LiftEngine.UNMAPPED);
            }
            return;
        }
        LiftEngine.VarLift v = LiftEngine.liftVarAt(lazyChain, chrom, pos, refLen, policy);
        LiftEngine.PointResult p = v.point();
        if (LiftEngine.MAPPED.equals(p.status()) || LiftEngine.PASSTHROUGH.equals(p.status())) {
            LiftEngine.Extent extent = aligned ? LiftEngine.alignedExtent(v, pos, refLen) : LiftEngine.Extent.NONE;
            emit(r, p.chrom(), p.pos(), 0, p.strand(), p.status(), 0.0, null, extent);
        } else {
            emitUnplaced(r, reportedStatus(p.status()));
        }
    }

    /** -seg is the GOR half-open [start, end), -bam the 1-based inclusive [Pos, End] (N12, N15). */
    private void span(Row r) {
        String chrom = r.chr;
        boolean bam = mode == RelocateMode.BAM;
        int start = startColumn(r);
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
                        st.score(), mpos, LiftEngine.Extent.NONE);
            }
            if (targets.isEmpty()) {
                emitUnplaced(r, LiftEngine.UNMAPPED);
            }
            return;
        }
        LiftEngine.SpanResult s = LiftEngine.classifySpan(lazyChain, chrom, s0 + 1, end, policy, tolerance);
        String status = s.status();
        if (LiftEngine.PASSTHROUGH.equals(status)) {
            // only with -passthrough: the row keeps its location and MPOS, and the chain is not used
            emit(r, chrom, start, end, "", status, 0.0, bam ? r.colAsString(cols.mpos()).toString() : null,
                    LiftEngine.Extent.NONE);
            return;
        }
        // a -bam read across a gap is not moved: its SEQ, QUAL and CIGAR would not fit the lifted span
        boolean placed = LiftEngine.MAPPED.equals(status)
                || (fullStatus && !bam && LiftEngine.HAS_TARGET.contains(status));
        if (!placed) {
            emitUnplaced(r, reportedStatus(status));
            return;
        }
        String mpos = null;
        if (bam) {
            // the block classifySpan mapped the read through, for MPOS (N15)
            ContigIndex ci = resolveChain().contig(chrom);
            mpos = mate(r, chrom, ci, ci.findSpan(s0, end), s.chrom());
        }
        emit(r, s.chrom(), bam ? s.start1() : s.start1() - 1, s.stop1(), s.strand(), status, 0.0, mpos,
                LiftEngine.Extent.NONE);
    }

    /**
     * The position cell (column #2) as an int. An empty or blank cell, or a lone '-', is an error: RowObj.colInt reads
     * it as 0, which would lift a missing coordinate as if it were a real one.
     */
    private int startColumn(Row r) {
        return coordinate(r, 1, "start");
    }

    private int endColumn(Row r) {
        return coordinate(r, 2, "stop");
    }

    private int coordinate(Row r, int col, String what) {
        String cell = r.colAsString(col).toString();
        if (cell.isBlank()) {
            throw new GorDataException(PREFIX + "column #" + (col + 1) + " is empty at " + location(r), col + 1,
                    getHeader(), r.toString());
        }
        try {
            if (cell.strip().equals("-")) {
                throw new NumberFormatException(cell);
            }
            return r.colAsInt(col);
        } catch (NumberFormatException e) {
            throw new GorDataException(PREFIX + "Illegal " + what + " position in column #" + (col + 1) + ": '"
                    + cell + "' at " + location(r), col + 1, getHeader(), r.toString(), e);
        }
    }

    /** chrom:pos of a row as given in the input, for error messages. */
    private static String location(Row r) {
        return r.chr + ":" + r.colAsString(1);
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
        emit(r, r.chr, 0, 1, "", status, 0.0, mode == RelocateMode.BAM ? unplacedMate(r) : null,
                LiftEngine.Extent.NONE);
    }

    /**
     * Emits tChrom, a[, b], the input columns after the location (rewritten on the - strand), the source location,
     * strand and status, the aligned extent under -aligned and the score under -all. mpos replaces the -bam MPOS
     * column.
     */
    private void emit(Row r, String tChrom, int a, int b, String strand, String status, double score, String mpos,
                      LiftEngine.Extent extent) {
        statsInc(status);
        if (failOn.contains(status)) {
            throw new GorDataException(PREFIX + "status '" + status + "' at " + location(r));
        }
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
        if (aligned) {
            sb.append('\t').append(extent.left()).append('\t').append(extent.right());
        }
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
