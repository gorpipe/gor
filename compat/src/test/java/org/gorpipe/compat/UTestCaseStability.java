package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

public class UTestCaseStability {

    private static CompatCase baselineCase(String id, String query) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "baseline";
        c.mode = "exact";
        c.query = query;
        return c;
    }

    @Test
    public void aPlainQueryIsReproducible() {
        Assert.assertTrue(CaseStability.isReproducible(baselineCase("cmd.norrows.x", "norrows 2")));
    }

    @Test
    public void repeatsARunToCatchOrderThatVariesBetweenRuns() {
        // Not every unstable case depends on the path. KING -sym emits its pair
        // rows in a nondeterministic order with identical values, so comparing two
        // runs agrees about half the time; a repeated run raises the odds of
        // catching it. This asserts the screen does more than one comparison,
        // through a query that is stable, so it must still be judged reproducible.
        Assert.assertTrue(CaseStability.isReproducible(
                baselineCase("cmd.norrows.stable", "norrows 3 | calc X RowNum*2")));
    }

    @Test
    public void aQueryWhoseOutputDependsOnThePathIsNotReproducible() {
        // PIPESTEPS reports "begin 0, end -1, length 89" — the length of the
        // project root path. That varies per run and per machine, so no committed
        // baseline could ever match it. Canonicalising ${ROOT} cannot help: the
        // leak is a length, not the path itself.
        CompatCase c = baselineCase("cmd.pipesteps.bare",
                "gor ${ROOT}/left.gor | PIPESTEPS ${ROOT}/right.gor | top 5");
        CompatInput in = new CompatInput();
        in.path = "left.gor";
        in.content = "Chrom\tPos\tVal\nchr1\t1\t10\n";
        c.inputs.add(in);
        CompatInput right = new CompatInput();
        right.path = "right.gor";
        right.content = "Chrom\tPos\tGene\nchr1\t1\tBRCA1\n";
        c.inputs.add(right);

        Assert.assertFalse(CaseStability.isReproducible(c));
    }
}
