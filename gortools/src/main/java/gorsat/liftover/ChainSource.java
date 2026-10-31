package gorsat.liftover;

/**
 * Source of per-contig chain indexes. Implementations are thread safe and never return null: a contig without
 * blocks gives {@link ContigIndex#empty(String)}.
 */
public interface ChainSource {
    ContigIndex contig(String chrom);
}
