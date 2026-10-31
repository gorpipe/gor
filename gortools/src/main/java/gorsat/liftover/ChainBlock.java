package gorsat.liftover;

/**
 * One chain block: source (tStart, tEnd) is 0-based half-open on the chain's contig, the query side
 * (qChrom, qStart, qEnd) is on the forward strand. strand is "+" or "-" (anything else is rejected at build).
 */
public record ChainBlock(int tStart, int tEnd, String qChrom, int qStart, int qEnd, String strand, double score) {
}
