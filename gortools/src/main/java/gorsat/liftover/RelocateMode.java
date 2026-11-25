package gorsat.liftover;

import java.util.List;

/**
 * The input type of LIFTOVER: a point (-snp), a variant (-var), a GOR half-open segment (-seg) or a 1-based inclusive
 * read (-bam).
 */
public enum RelocateMode {
    SNP, VAR, SEG, BAM;

    /** True for the types with an end column (-seg, -bam). */
    public boolean isSpan() {
        return this == SEG || this == BAM;
    }

    /** Every status this mode can report under -mapped, -passthrough or -failon: the point or the span statuses. */
    public List<String> statuses() {
        return isSpan() ? LiftMode.SEG.statuses() : LiftMode.SNP.statuses();
    }
}
