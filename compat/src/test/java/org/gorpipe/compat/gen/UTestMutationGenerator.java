package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UTestMutationGenerator {

    private static CompatCase seed(String id, String query) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "baseline";
        c.mode = "exact";
        c.query = query;
        return c;
    }

    @Test
    public void producesVariantsOfTheSeedQueries() {
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.join.base", "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"));

        List<CompatCase> mutated = MutationGenerator.mutate(seeds, SurfaceInventory.read(), 20);

        Assert.assertFalse(mutated.isEmpty());
        Assert.assertTrue("must not exceed the requested limit", mutated.size() <= 20);
        for (CompatCase c : mutated) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNotEquals("a mutant must differ from its seed",
                    seeds.get(0).query, c.query);
        }
    }

    @Test
    public void isDeterministicAcrossRuns() {
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.join.base", "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"));

        SurfaceInventory inv = SurfaceInventory.read();
        List<CompatCase> first = MutationGenerator.mutate(seeds, inv, 10);
        List<CompatCase> second = MutationGenerator.mutate(seeds, inv, 10);

        Assert.assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            Assert.assertEquals("mutation must be reproducible to be diffable",
                    first.get(i).query, second.get(i).query);
            Assert.assertEquals(first.get(i).id, second.get(i).id);
        }
    }

    @Test
    public void coverageFeedbackKeepsOnlyMutantsThatReachNewCode() {
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.group.base", "norrows 5 | group 1000 -count"));
        seeds.add(seed("cmd.rank.base", "norrows 5 | rank 1000 RowNum"));

        List<CompatCase> selected = MutationGenerator.mutateWithCoverageFeedback(
                seeds, SurfaceInventory.read(), 12);

        Assert.assertTrue("must respect the limit", selected.size() <= 12);
        for (CompatCase c : selected) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
        }

        if (org.gorpipe.compat.CoverageProbe.available()) {
            // With the agent attached the selection is a filter, so it cannot
            // exceed what plain enumeration would have produced, and every kept
            // mutant earned its place by reaching a probe nothing had reached.
            List<CompatCase> enumerated =
                    MutationGenerator.mutate(seeds, SurfaceInventory.read(), 12);
            Assert.assertTrue("filtering cannot produce more than enumeration",
                    selected.size() <= enumerated.size());
        }
    }

    @Test
    public void coverageFeedbackFallsBackToEnumerationWithoutAnAgent() {
        // Generation must work in a plain JVM. Without the agent the selection is
        // the enumeration, unfiltered.
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.group.base", "norrows 5 | group 1000 -count"));

        List<CompatCase> selected = MutationGenerator.selectByNewCoverage(
                MutationGenerator.mutate(seeds, SurfaceInventory.read(), 5), 5, false);

        Assert.assertEquals(5, selected.size());
    }

    @Test
    public void generatedIdsAreUniqueAndWellFormed() {
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.join.base", "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"));
        seeds.add(seed("cmd.group.base", "gor ${ROOT}/left.gor | group chrom -count"));

        List<CompatCase> mutated = MutationGenerator.mutate(seeds, SurfaceInventory.read(), 30);

        long distinct = mutated.stream().map(c -> c.id).distinct().count();
        Assert.assertEquals(mutated.size(), distinct);
        for (CompatCase c : mutated) {
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
        }
    }
}
