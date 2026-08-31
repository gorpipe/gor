package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UTestGrammarFuzzer {

    @Test
    public void readsACommittedSeed() {
        // A committed fixed seed is what keeps fuzz baselines from churning.
        Assert.assertTrue("fuzz seed must be positive", GrammarFuzzer.fuzzSeed() != 0L);
    }

    @Test
    public void isReproducibleForTheSameSeed() {
        SurfaceInventory inv = SurfaceInventory.read();
        List<CompatCase> first = GrammarFuzzer.generate(inv, 25);
        List<CompatCase> second = GrammarFuzzer.generate(inv, 25);

        Assert.assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            Assert.assertEquals("fuzzing must be reproducible from the committed seed",
                    first.get(i).query, second.get(i).query);
        }
    }

    @Test
    public void respectsTheRequestedCap() {
        List<CompatCase> cases = GrammarFuzzer.generate(SurfaceInventory.read(), 15);
        Assert.assertTrue("expected at most 15 cases, got " + cases.size(),
                cases.size() <= 15);
        Assert.assertFalse(cases.isEmpty());
    }

    @Test
    public void generatedCasesAreWellFormedBaselineCases() {
        for (CompatCase c : GrammarFuzzer.generate(SurfaceInventory.read(), 20)) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNull(c.expected);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
            Assert.assertFalse(c.query.isBlank());
        }
    }
}
