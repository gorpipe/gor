package gorsat.liftover;

import java.util.List;

/**
 * The input type of a liftover: a point (-snp), a variant (-var), a GOR half-open segment (-seg) or a 1-based
 * inclusive segment (-seg1). Point types report the point statuses, segment types all eleven interval statuses.
 */
public enum LiftMode {
    SNP("-snp"), VAR("-var"), SEG("-seg"), SEG1("-seg1");

    private static final List<String> POINT_STATUSES = List.of(LiftEngine.PASSTHROUGH,
            LiftEngine.UNSUPPORTED_CONTIG, LiftEngine.AMBIGUOUS, LiftEngine.UNMAPPED, LiftEngine.MAPPED);

    private static final List<String> SPAN_STATUSES = List.of(LiftEngine.PASSTHROUGH,
            LiftEngine.UNSUPPORTED_CONTIG, LiftEngine.AMBIGUOUS, LiftEngine.UNMAPPED, LiftEngine.START_UNMAPPED,
            LiftEngine.STOP_UNMAPPED, LiftEngine.SPLIT_CONTIG, LiftEngine.INVERTED, LiftEngine.MAPPED,
            LiftEngine.MAPPED_ACROSS_GAP, LiftEngine.SIZE_CHANGED);

    private final String flag;

    LiftMode(String flag) {
        this.flag = flag;
    }

    /** The command-line flag selecting this mode. */
    public String flag() {
        return flag;
    }

    /** Every status this mode can report. */
    public List<String> statuses() {
        return this == SNP || this == VAR ? POINT_STATUSES : SPAN_STATUSES;
    }
}
