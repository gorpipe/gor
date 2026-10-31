package gorsat.liftover;

import gorsat.Commands.Analysis;
import gorsat.Commands.RowHeader;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.GorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The 1:1 streaming pipe step of LIFTOVER_ANNOTATE. Every input row produces exactly one output row: the input
 * columns followed by the liftover columns of the configured mode. No row is dropped, merged or reordered, and no
 * state is kept between rows, so the step is safe over unsorted input.
 *
 * The chain is resolved lazily, on the first row that needs it: passthrough and unsupported-source rows never
 * force it.
 */
public class LiftoverAnnotateAnalysis extends Analysis {

    private static final String PREFIX = "LIFTOVER_ANNOTATE: ";
    private static final String STATS_SENDER = "LiftoverAnnotate";

    /** One appended column: its name (with the prefix) and its GOR type (S, I or D). */
    public record OutputColumn(String name, String type) {
    }

    private final LiftoverAnnotateConfig config;
    private final RowHeader outgoingHeader;
    private final Supplier<ChainSource> chainFactory;
    private ChainSource chain;
    private final Supplier<ChainSource> lazyChain = this::resolveChain;

    public LiftoverAnnotateAnalysis(LiftoverAnnotateConfig config, RowHeader outgoingHeader,
                                    Supplier<ChainSource> chainFactory, GorContext context) {
        this.config = config;
        this.outgoingHeader = outgoingHeader;
        this.chainFactory = chainFactory;
        statsSenderName_$eq(STATS_SENDER);
        if (context != null) {
            setContext(context);
        }
    }

    private ChainSource resolveChain() {
        if (chain == null) {
            chain = chainFactory.get();
        }
        return chain;
    }

    /** The appended columns of a mode, in output order, names prefixed with prefix. */
    public static List<OutputColumn> outputColumns(LiftMode mode, String prefix, boolean diag) {
        List<OutputColumn> cols = new ArrayList<>();
        cols.add(new OutputColumn(prefix + "status", "S"));
        switch (mode) {
            case SNP:
                cols.add(new OutputColumn(prefix + "chrom", "S"));
                cols.add(new OutputColumn(prefix + "pos", "I"));
                break;
            case VAR:
                cols.add(new OutputColumn(prefix + "chrom", "S"));
                cols.add(new OutputColumn(prefix + "pos", "I"));
                cols.add(new OutputColumn(prefix + "ref", "S"));
                cols.add(new OutputColumn(prefix + "alt", "S"));
                break;
            default:
                cols.add(new OutputColumn(prefix + "chrom", "S"));
                cols.add(new OutputColumn(prefix + "start", "I"));
                cols.add(new OutputColumn(prefix + "stop", "I"));
                break;
        }
        cols.add(new OutputColumn(prefix + "strand", "S"));
        if (mode == LiftMode.SEG || mode == LiftMode.SEG1) {
            cols.add(new OutputColumn(prefix + "chain_blocks", "I"));
            if (diag) {
                cols.add(new OutputColumn(prefix + "source_size", "I"));
                cols.add(new OutputColumn(prefix + "target_size", "I"));
                cols.add(new OutputColumn(prefix + "size_ratio", "D"));
                cols.add(new OutputColumn(prefix + "start_chrom", "S"));
                cols.add(new OutputColumn(prefix + "start_pos", "I"));
                cols.add(new OutputColumn(prefix + "start_strand", "S"));
                cols.add(new OutputColumn(prefix + "stop_chrom", "S"));
                cols.add(new OutputColumn(prefix + "stop_pos", "I"));
                cols.add(new OutputColumn(prefix + "stop_strand", "S"));
            }
        }
        return cols;
    }

    /**
     * The header of the output rows: the input columns with index-string type placeholders (resolved against the
     * incoming header by {@link RowHeader#propagateTypes}), followed by {@link #outputColumns}.
     */
    public static RowHeader outgoingHeader(String inputHeader, LiftMode mode, String prefix, boolean diag) {
        String[] in = inputHeader.split("\t");
        List<OutputColumn> out = outputColumns(mode, prefix, diag);
        String[] names = new String[in.length + out.size()];
        String[] types = new String[names.length];
        for (int i = 0; i < in.length; i++) {
            names[i] = in[i];
            types[i] = Integer.toString(i);
        }
        for (int k = 0; k < out.size(); k++) {
            names[in.length + k] = out.get(k).name();
            types[in.length + k] = out.get(k).type();
        }
        return new RowHeader(names, types);
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
        String chrom = r.colAsString(config.chromCol()).toString();
        StringBuilder sb = new StringBuilder(64);
        String status;
        switch (config.mode()) {
            case SNP:
                status = snp(r, chrom, sb);
                break;
            case VAR:
                status = var(r, chrom, sb);
                break;
            default:
                status = span(r, chrom, sb);
                break;
        }
        if (config.failOn().contains(status)) {
            throw new GorDataException(PREFIX + "status '" + status + "' at " + chrom + ":"
                    + r.colAsString(config.posCol()));
        }
        statsInc(status);
        super.process(r.rowWithAddedColumns(sb));
    }

    private int intCol(Row r, int col) {
        try {
            return r.colAsInt(col);
        } catch (NumberFormatException e) {
            throw new GorDataException(PREFIX + "column #" + (col + 1) + " is not an integer: '"
                    + r.colAsString(col) + "'", col + 1, getHeader(), r.toString());
        }
    }

    private String snp(Row r, String chrom, StringBuilder sb) {
        int pos = intCol(r, config.posCol());
        LiftEngine.PointResult p = LiftEngine.liftSnp(lazyChain, chrom, pos, config.policy());
        sb.append(p.status()).append('\t').append(p.chrom()).append('\t').append(p.pos()).append('\t')
                .append(p.strand());
        return p.status();
    }

    private String var(Row r, String chrom, StringBuilder sb) {
        int pos = intCol(r, config.posCol());
        String ref = r.colAsString(config.refCol()).toString();
        String alt = r.colAsString(config.altCol()).toString();
        LiftEngine.PointResult p = LiftEngine.liftVar(lazyChain, chrom, pos, ref.length(), config.policy());
        String outRef;
        String outAlt;
        if (p.status().equals(LiftEngine.MAPPED) && p.strand().equals("-")) {
            outRef = LiftEngine.revcompAlleles(ref);
            outAlt = LiftEngine.revcompAlleles(alt);
        } else if (LiftEngine.HAS_TARGET.contains(p.status())) {
            outRef = ref;
            outAlt = alt;
        } else {
            outRef = "";
            outAlt = "";
        }
        sb.append(p.status()).append('\t').append(p.chrom()).append('\t').append(p.pos()).append('\t')
                .append(outRef).append('\t').append(outAlt).append('\t').append(p.strand());
        return p.status();
    }

    private String span(Row r, String chrom, StringBuilder sb) {
        boolean seg1 = config.mode() == LiftMode.SEG1;
        int start = intCol(r, config.posCol());
        int end = intCol(r, config.endCol());
        // -seg is [start,end), -seg1 is [start,end]; s1/e1 are 1-based inclusive, so e1 == s1 - 1 is a junction
        int s1 = seg1 ? start : start + 1;
        int e1 = end;
        if (e1 < s1 - 1) {
            throw new GorDataException(PREFIX + "invalid interval [" + start + "," + end + "] at " + chrom + ":"
                    + start, -1, getHeader(), r.toString());
        }
        LiftEngine.SpanResult s = LiftEngine.classifySpan(lazyChain, chrom, s1, e1, config.policy(),
                config.tolerance());
        boolean hasTarget = LiftEngine.HAS_TARGET.contains(s.status());
        int outStart = hasTarget ? (seg1 ? s.start1() : s.start1() - 1) : 0;
        int outStop = hasTarget ? s.stop1() : 0;
        sb.append(s.status()).append('\t').append(s.chrom()).append('\t').append(outStart).append('\t')
                .append(outStop).append('\t').append(s.strand()).append('\t').append(s.chainBlocks());
        if (config.diag()) {
            sb.append('\t').append(s.sourceSize()).append('\t').append(s.targetSize()).append('\t')
                    .append(Double.toString(s.sizeRatio()));
            appendEndpoint(sb, s.start());
            appendEndpoint(sb, s.stop());
        }
        return s.status();
    }

    private static void appendEndpoint(StringBuilder sb, LiftEngine.Endpoint e) {
        sb.append('\t').append(e.chrom()).append('\t').append(e.pos()).append('\t').append(e.strand());
    }

    @Override
    public void finish() {
        // 1:1 and stateless: nothing to flush
    }
}
