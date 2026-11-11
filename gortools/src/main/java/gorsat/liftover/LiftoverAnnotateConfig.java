package gorsat.liftover;

import java.util.Set;

/**
 * The parsed arguments of a LIFTOVER_ANNOTATE step. The column fields are 0-based indices into the full input row;
 * -1 means the column is not used by the mode (endCol outside -seg/-seg1, refCol/altCol outside -var). failOn holds
 * the statuses that abort the query. aligned (-var -aligned, ENGKNOW-4207) appends the aligned extent.
 */
public record LiftoverAnnotateConfig(LiftMode mode, int chromCol, int posCol, int endCol, int refCol, int altCol,
                                     ContigPolicy policy, double tolerance, boolean diag, Set<String> failOn,
                                     boolean aligned) {

    public LiftoverAnnotateConfig {
        failOn = failOn == null ? Set.of() : Set.copyOf(failOn);
        if (aligned && mode != LiftMode.VAR) {
            throw new IllegalArgumentException("aligned is only valid with VAR");
        }
    }

    /** A step without -aligned. */
    public LiftoverAnnotateConfig(LiftMode mode, int chromCol, int posCol, int endCol, int refCol, int altCol,
                                  ContigPolicy policy, double tolerance, boolean diag, Set<String> failOn) {
        this(mode, chromCol, posCol, endCol, refCol, altCol, policy, tolerance, diag, failOn, false);
    }
}
