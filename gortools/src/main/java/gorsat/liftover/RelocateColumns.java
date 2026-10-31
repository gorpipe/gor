package gorsat.liftover;

/**
 * The input columns LIFTOVER rewrites, as 0-based indexes into the input row; -1 means unused. ref and alt are used
 * by -var, seq, qual, cigar and mpos by -bam, and mrnm (optional) by -bam to find mates on other contigs.
 */
public record RelocateColumns(int ref, int alt, int seq, int qual, int cigar, int mpos, int mrnm) {

    /** No rewritten columns (-snp, -seg). */
    public static final RelocateColumns NONE = new RelocateColumns(-1, -1, -1, -1, -1, -1, -1);
}
