package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestNorContextGenerator {

    private static NorContextGenerator.GenerationResult result;
    private static SurfaceInventory inventory;

    @BeforeClass
    public static void generate() {
        inventory = SurfaceInventory.read();
        result = NorContextGenerator.generate(inventory);
    }

    @Test
    public void coversCommandsValidInBothContexts() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.toString(), ids.contains("nor.calc.bare"));
        Assert.assertTrue(ids.toString(), ids.contains("nor.group.bare"));
        Assert.assertTrue("expected a case for most of the 62 commands valid in both, got "
                + result.cases.size(), result.cases.size() >= 40);
    }

    @Test
    public void skipsCommandsTheRegistryMarksGorOnly() {
        // JOIN is refused in a NOR query: "trying to execute JOIN in a nor query".
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertFalse(ids.toString(), ids.contains("nor.join.bare"));
        Assert.assertTrue(result.gorOnly.contains("JOIN"));
    }

    @Test
    public void everyCaseRunsInANorQuery() {
        for (CompatCase c : result.cases) {
            Assert.assertTrue("a NOR case must lead with nor: " + c.query,
                    c.query.startsWith("nor "));
            Assert.assertEquals("baseline", c.tier);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
        }
    }

    @Test
    public void usesTheNorPositionalWhereOneIsCurated() {
        // GROUP takes a bin size in GOR and none in NOR.
        CompatCase group = result.cases.stream()
                .filter(c -> c.id.equals("nor.group.bare"))
                .findFirst().orElseThrow(() -> new AssertionError("no NOR GROUP case"));
        Assert.assertFalse("the GOR bin size must not leak into the NOR query: " + group.query,
                group.query.contains("GROUP 1000"));
    }
}
