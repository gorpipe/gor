package gorsat.liftover;

import java.util.Set;

/**
 * Contig lists of a liftover, matched exactly and case-sensitively. Rows on a passthrough contig are copied
 * unchanged without touching the chain. When mapped is non-null (-mapped), only those contigs are accepted, both as
 * source contigs and as target contigs; null means no restriction. A null passthrough is an empty set.
 */
public record ContigPolicy(Set<String> passthrough, Set<String> mapped) {

    public ContigPolicy {
        passthrough = passthrough == null ? Set.of() : Set.copyOf(passthrough);
        mapped = mapped == null ? null : Set.copyOf(mapped);
    }

    public boolean isPassthrough(String c) {
        return passthrough.contains(c);
    }

    /** True when c is acceptable under -mapped (always, without -mapped). */
    public boolean allowsTarget(String c) {
        return mapped == null || mapped.contains(c);
    }
}
