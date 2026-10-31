package gorsat.liftover;

import java.util.Set;

/**
 * The parsed arguments of a LIFTOVER_ANNOTATE step. The column fields are 0-based indices into the full input row;
 * -1 means the column is not used by the mode (endCol outside -seg/-seg1, refCol/altCol outside -var). failOn holds
 * the statuses that abort the query.
 */
public record LiftoverAnnotateConfig(LiftMode mode, int chromCol, int posCol, int endCol, int refCol, int altCol,
                                     ContigPolicy policy, double tolerance, boolean diag, Set<String> failOn) {

    public LiftoverAnnotateConfig {
        failOn = failOn == null ? Set.of() : Set.copyOf(failOn);
    }
}
