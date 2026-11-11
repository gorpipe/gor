package gorsat.liftover;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.Row;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Accumulates the blocks of one contig, validating each as it is added, and builds the {@link ContigIndex}.
 * Blocks must arrive sorted by tStart. Not thread safe.
 *
 * It also links each block to the block before it when both belong to one chain. A chain file carries no chain id, so
 * blocks with the same qChrom, strand and score are taken to be one chain (the score is the chain score). Two that
 * abut on both sides are linked directly. Two separated by a double-sided gap of equal length that no other block
 * reaches into (a run of substituted bases in a UCSC chain) get a synthetic gap block that spans the gap (ENGKNOW-4208), as
 * if the chain file had been merged across it.
 */
public final class ContigIndexBuilder {

    private final String contig;
    private final OverlapPolicy policy;
    private final String source;

    private int n;
    private int[] tStart = new int[16];
    private int[] tEnd = new int[16];
    private int[] qStart = new int[16];
    private int[] prefMaxEnd = new int[16];
    private byte[] minus = new byte[16];
    private double[] score = new double[16];
    private String[] qChrom = new String[16];
    private boolean overlapping;

    /** False for the builder of the gap blocks themselves, which are not linked into chains. */
    private final boolean linkChains;
    /** False with -nogapfill: equal gaps are not filled, abutting blocks are still linked. */
    private final boolean fillGaps;
    /** Pairs (i, j) of real blocks of one chain that abut on both sides. */
    private final List<int[]> abutting = new ArrayList<>();
    /** The gap blocks, in tStart order. */
    private final List<PendingGap> gaps = new ArrayList<>();

    /** An equal double-sided gap between real blocks prev and next of one chain. */
    private record PendingGap(ChainBlock block, int prev, int next) {
    }

    public ContigIndexBuilder(String contig, OverlapPolicy policy, String source) {
        this(contig, policy, source, true);
    }

    /** With fillGaps false (-nogapfill) no gap block is made, so the index lifts as it did before ENGKNOW-4208. */
    public ContigIndexBuilder(String contig, OverlapPolicy policy, String source, boolean fillGaps) {
        this(contig, policy, source, true, fillGaps);
    }

    private ContigIndexBuilder(String contig, OverlapPolicy policy, String source, boolean linkChains,
                               boolean fillGaps) {
        this.contig = contig;
        this.policy = policy;
        this.source = source;
        this.linkChains = linkChains;
        this.fillGaps = fillGaps;
    }

    public void add(ChainBlock b) {
        add(b.tStart(), b.tEnd(), b.qChrom(), b.qStart(), b.qEnd(), b.strand(), b.score());
    }

    /** Adds the chain row {@code r} (tStart is the row position) using the column positions in {@code cols}. */
    void addRow(Row r, ChainColumns cols) {
        requireCell(r, 1, "tStart");
        requireCell(r, cols.tEnd(), "tEnd");
        requireCell(r, cols.qChrom(), "qChrom");
        requireCell(r, cols.qStart(), "qStart");
        requireCell(r, cols.qEnd(), "qEnd");
        try {
            add(r.pos, r.colAsInt(cols.tEnd()), r.colAsString(cols.qChrom()).toString(), r.colAsInt(cols.qStart()),
                    r.colAsInt(cols.qEnd()), r.colAsString(cols.qStrand()).toString(), r.colAsDouble(cols.qScore()));
        } catch (NumberFormatException e) {
            throw new GorDataException(prefix() + "invalid number in row " + r.chr + ":" + r.pos + ": "
                    + e.getMessage(), e);
        }
    }

    /**
     * RowObj.colInt reads an empty cell or a lone '-' as 0, which would silently turn a missing coordinate into a valid
     * one.
     */
    private void requireCell(Row r, int col, String name) {
        String cell = r.colAsString(col).toString();
        if (cell.isBlank()) {
            throw new GorDataException(prefix() + "empty " + name + " in row " + r.chr + ":" + r.pos);
        }
        if (cell.strip().equals("-")) {
            throw new GorDataException(prefix() + "invalid " + name + " '" + cell + "' in row " + r.chr + ":" + r.pos);
        }
    }

    public int size() {
        return n;
    }

    private String prefix() {
        return "Liftover chain " + source + ": ";
    }

    private GorDataException error(String what) {
        return new GorDataException(prefix() + what + " (block " + n + " on " + contig + ")");
    }

    private void add(int tStart, int tEnd, String qChrom, int qStart, int qEnd, String strand, double score) {
        String at = contig + ":" + tStart + "-" + tEnd;
        if (tEnd <= tStart) {
            throw error("tEnd <= tStart at " + at);
        }
        if (tStart < 0) {
            throw error("tStart < 0 at " + at);
        }
        if (qStart < 0) {
            throw error("qStart < 0 at " + at);
        }
        if (qChrom == null || qChrom.isBlank()) {
            throw error("empty qChrom at " + at);
        }
        if (qEnd - qStart != tEnd - tStart) {
            throw error("gapped block at " + at + ", the chain must have equal source and target lengths");
        }
        if (!"+".equals(strand) && !"-".equals(strand)) {
            throw error("invalid strand '" + strand + "' at " + at);
        }
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            throw error("invalid score '" + score + "' at " + at + ", the score must be a finite number");
        }
        if (n > 0 && tStart < this.tStart[n - 1]) {
            throw error("not sorted by tStart at " + at);
        }
        boolean overlaps = n > 0 && tStart < prefMaxEnd[n - 1];
        if (overlaps) {
            if (policy == OverlapPolicy.ERROR) {
                throw error("overlapping blocks at " + at + ", use -overlap best to allow overlaps");
            }
            overlapping = true;
        }
        if (n == this.tStart.length) {
            grow();
        }
        this.tStart[n] = tStart;
        this.tEnd[n] = tEnd;
        this.qStart[n] = qStart;
        this.prefMaxEnd[n] = n == 0 ? tEnd : Math.max(prefMaxEnd[n - 1], tEnd);
        this.minus[n] = (byte) ("-".equals(strand) ? 1 : 0);
        this.score[n] = score;
        this.qChrom[n] = qChrom.intern();
        if (linkChains && n > 0) {
            link(n - 1, n);
        }
        n++;
    }

    /**
     * Links block j to block i before it when both are one chain and they abut, or are separated by an equal gap that
     * no earlier block reaches into.
     */
    private void link(int i, int j) {
        if (minus[i] != minus[j] || score[i] != score[j] || !qChrom[i].equals(qChrom[j])) {
            return;
        }
        int gap = tStart[j] - tEnd[i];
        if (gap < 0 || prefMaxEnd[i] > tEnd[i]) {
            // overlapping blocks (-overlap best) are not consecutive blocks of a chain
            return;
        }
        int qEndI = qStart[i] + tEnd[i] - tStart[i];
        int qEndJ = qStart[j] + tEnd[j] - tStart[j];
        boolean minusStrand = minus[i] != 0;
        int qGap = minusStrand ? qStart[i] - qEndJ : qStart[j] - qEndI;
        if (qGap != gap) {
            return;
        }
        if (gap == 0) {
            abutting.add(new int[]{i, j});
        } else if (fillGaps) {
            // + lifts the gap on from the end of i; - lifts it on from the end of j, which comes first on the query
            int q = minusStrand ? qEndJ : qEndI;
            gaps.add(new PendingGap(new ChainBlock(tEnd[i], tStart[j], qChrom[i], q, q + gap,
                    minusStrand ? "-" : "+", score[i]), i, j));
        }
    }

    private void grow() {
        int cap = n * 2;
        tStart = Arrays.copyOf(tStart, cap);
        tEnd = Arrays.copyOf(tEnd, cap);
        qStart = Arrays.copyOf(qStart, cap);
        prefMaxEnd = Arrays.copyOf(prefMaxEnd, cap);
        minus = Arrays.copyOf(minus, cap);
        score = Arrays.copyOf(score, cap);
        qChrom = Arrays.copyOf(qChrom, cap);
    }

    public ContigIndex build() {
        if (n == 0) {
            return ContigIndex.empty(contig);
        }
        ContigIndex gapIndex = null;
        int[] next = null;
        int[] prev = null;
        if (linkChains && (!gaps.isEmpty() || !abutting.isEmpty())) {
            // a gap lies between two consecutive blocks, so the gaps do not overlap and arrive in tStart order
            ContigIndexBuilder gb = new ContigIndexBuilder(contig, OverlapPolicy.ERROR, source, false, false);
            for (PendingGap g : gaps) {
                gb.add(g.block());
            }
            gapIndex = gaps.isEmpty() ? null : gb.build();
            next = new int[n + gaps.size()];
            prev = new int[n + gaps.size()];
            Arrays.fill(next, -1);
            Arrays.fill(prev, -1);
            for (int[] ab : abutting) {
                next[ab[0]] = ab[1];
                prev[ab[1]] = ab[0];
            }
            for (int k = 0; k < gaps.size(); k++) {
                PendingGap g = gaps.get(k);
                int gi = n + k;
                next[g.prev()] = gi;
                prev[gi] = g.prev();
                next[gi] = g.next();
                prev[g.next()] = gi;
            }
        }
        return new ContigIndex(contig, n, Arrays.copyOf(tStart, n), Arrays.copyOf(tEnd, n),
                Arrays.copyOf(qStart, n), Arrays.copyOf(prefMaxEnd, n), Arrays.copyOf(minus, n),
                Arrays.copyOf(score, n), Arrays.copyOf(qChrom, n), overlapping, gapIndex, next, prev);
    }
}
