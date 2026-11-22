package gorsat.liftover;

import org.gorpipe.model.gor.iterators.RefSeq;

import java.util.function.Supplier;

/**
 * LIFTOVER -var -haplotype (ENGKNOW-4224): opens the source (-srcseq) and target (-tgtseq) reference sequences that
 * {@link AlleleFix} reads. Each pipe step opens its own readers on first use and closes them when it finishes.
 */
public record AlleleFixConfig(Supplier<RefSeq> source, Supplier<RefSeq> target) {
}
