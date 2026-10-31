package gorsat.liftover;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.Row;

import java.util.Arrays;

/**
 * Accumulates the blocks of one contig, validating each as it is added, and builds the {@link ContigIndex}.
 * Blocks must arrive sorted by tStart. Not thread safe.
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

    public ContigIndexBuilder(String contig, OverlapPolicy policy, String source) {
        this.contig = contig;
        this.policy = policy;
        this.source = source;
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
        n++;
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
        return new ContigIndex(contig, n, Arrays.copyOf(tStart, n), Arrays.copyOf(tEnd, n),
                Arrays.copyOf(qStart, n), Arrays.copyOf(prefMaxEnd, n), Arrays.copyOf(minus, n),
                Arrays.copyOf(score, n), Arrays.copyOf(qChrom, n), overlapping);
    }
}
