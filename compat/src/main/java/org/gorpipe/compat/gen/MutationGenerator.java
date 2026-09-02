package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseRunner;
import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.CoverageProbe;
import org.gorpipe.compat.SurfaceInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Derives extra baseline cases by mutating existing ones: adding a second flag,
 * chaining another stage, or varying a numeric argument.
 *
 * Fully deterministic — mutants are enumerated in a fixed order rather than drawn
 * at random, so the corpus is reproducible and its diffs are meaningful.
 *
 * {@link #mutateWithCoverageFeedback} adds the guidance the spec asks for: each
 * candidate is run and kept only if it reached a probe nothing had reached before.
 * Without that filter, mutation broadens the corpus without making it cover more —
 * hundreds of extra cases and hundreds of extra baselines to review for no new
 * behaviour reached.
 */
public final class MutationGenerator {

    private MutationGenerator() {
    }

    public static List<CompatCase> mutate(List<CompatCase> seeds,
                                          SurfaceInventory inventory, int limit) {
        List<CompatCase> out = new ArrayList<>();

        for (CompatCase seed : seeds) {
            for (String suffix : new String[]{"top 1", "top 0", "sort 1", "count"}) {
                if (out.size() >= limit) {
                    return out;
                }
                out.add(derive(seed, suffix, "chain_" + slug(suffix)));
            }

            String firstCommand = firstPipeCommand(seed.query);
            SurfaceInventory.CommandSurface surface = inventory.commands().get(firstCommand);
            if (surface != null) {
                for (String flag : surface.valuelessFlags) {
                    if (out.size() >= limit) {
                        return out;
                    }
                    if (seed.query.contains(flag)) {
                        continue;
                    }
                    CompatCase c = copy(seed, "addflag_" + flag.substring(1)
                            .toLowerCase(Locale.ROOT));
                    c.query = seed.query.replaceFirst(
                            "(?i)(\\|\\s*" + firstCommand + ")", "$1 " + flag);
                    if (!c.query.equals(seed.query)) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Enumerates mutants and keeps those that reach code the run has not reached.
     *
     * A wider pool is enumerated than the limit, since most mutants add nothing:
     * the filter needs candidates to discard.
     */
    public static List<CompatCase> mutateWithCoverageFeedback(List<CompatCase> seeds,
                                                              SurfaceInventory inventory,
                                                              int limit) {
        List<CompatCase> candidates = mutate(seeds, inventory, limit * 4);
        return selectByNewCoverage(candidates, limit, CoverageProbe.available());
    }

    /**
     * Keeps candidates that grow the set of hit probes, up to the limit.
     *
     * With no agent this returns the first candidates unchanged: generation has to
     * work in a plain JVM, and an unguided corpus is worse than none only if it is
     * mistaken for a guided one — so callers report which happened.
     */
    public static List<CompatCase> selectByNewCoverage(List<CompatCase> candidates,
                                                       int limit, boolean useCoverage) {
        if (!useCoverage) {
            return candidates.size() <= limit
                    ? new ArrayList<>(candidates)
                    : new ArrayList<>(candidates.subList(0, limit));
        }

        List<CompatCase> kept = new ArrayList<>();
        Set<String> reached = new HashSet<>(CoverageProbe.hitProbes());
        for (CompatCase candidate : candidates) {
            if (kept.size() >= limit) {
                break;
            }
            CaseRunner.run(candidate);
            Set<String> now = CoverageProbe.hitProbes();
            if (now.size() > reached.size()) {
                kept.add(candidate);
                reached = now;
            }
        }
        return kept;
    }

    private static CompatCase derive(CompatCase seed, String extraStage, String idSuffix) {
        CompatCase c = copy(seed, idSuffix);
        c.query = seed.query + " | " + extraStage;
        return c;
    }

    private static CompatCase copy(CompatCase seed, String idSuffix) {
        CompatCase c = new CompatCase();
        c.id = seed.id + "_" + idSuffix;
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Mutant of " + seed.id;
        c.needsReference = seed.needsReference;
        c.query = seed.query;
        for (CompatInput in : seed.inputs) {
            CompatInput copy = new CompatInput();
            copy.path = in.path;
            copy.content = in.content;
            copy.contentFile = in.contentFile;
            c.inputs.add(copy);
        }
        return c;
    }

    /** The command name of the second pipeline stage, or empty when there is none. */
    private static String firstPipeCommand(String query) {
        String[] stages = query.split("\\|");
        if (stages.length < 2) {
            return "";
        }
        String stage = stages[1].trim();
        return stage.isEmpty() ? "" : stage.split("\\s+")[0].toUpperCase(Locale.ROOT);
    }

    private static String slug(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }
}
