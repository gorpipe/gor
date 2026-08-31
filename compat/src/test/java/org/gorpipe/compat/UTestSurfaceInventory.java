package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UTestSurfaceInventory {

    private static SurfaceInventory inventory;

    @BeforeClass
    public static void readInventory() {
        inventory = SurfaceInventory.read();
    }

    @Test
    public void findsTheRegisteredCommands() {
        // Measured at 108. Asserted as a floor so adding a command does not fail
        // this test, while losing a large number of them would.
        Assert.assertTrue("expected at least 100 commands, got " + inventory.commands().size(),
                inventory.commands().size() >= 100);
        Assert.assertTrue(inventory.commands().containsKey("JOIN"));
        Assert.assertTrue(inventory.commands().containsKey("CALC"));
        Assert.assertTrue(inventory.commands().containsKey("GROUP"));
    }

    @Test
    public void parsesJoinFlagsExactly() {
        SurfaceInventory.CommandSurface join = inventory.commands().get("JOIN");

        Assert.assertTrue(join.valuelessFlags.contains("-snpsnp"));
        Assert.assertTrue(join.valuelessFlags.contains("-segseg"));
        Assert.assertTrue(join.valuelessFlags.contains("-xcis"));
        Assert.assertTrue(join.valueFlags.contains("-maxseg"));
        Assert.assertTrue(join.valueFlags.contains("-refr"));

        Assert.assertFalse("value-taking flags must not appear as valueless",
                join.valuelessFlags.contains("-maxseg"));

        Assert.assertEquals(1, join.minArgs);
        Assert.assertEquals(1, join.maxArgs);
    }

    @Test
    public void countsFlagsAcrossTheSurface() {
        // Measured: 217 valueless + 242 value-taking = 459.
        Assert.assertTrue("expected at least 400 flags, got " + inventory.totalFlagCount(),
                inventory.totalFlagCount() >= 400);
    }

    @Test
    public void enumeratesFunctions() {
        Assert.assertTrue("expected at least 50 functions, got "
                + inventory.functionNames().size(), inventory.functionNames().size() >= 50);
    }

    @Test
    public void jsonIsStableAcrossReads() {
        String first = SurfaceInventory.read().toJson();
        String second = SurfaceInventory.read().toJson();
        Assert.assertEquals("inventory JSON must be deterministic to be diffable",
                first, second);
        Assert.assertTrue(first.endsWith("\n"));
    }

    @Test
    public void flagsAreWellFormed() {
        for (SurfaceInventory.CommandSurface c : inventory.commands().values()) {
            for (String f : c.allFlags()) {
                Assert.assertFalse(c.name + " has an empty flag", f.isEmpty());
                Assert.assertTrue(c.name + " flag missing leading dash: " + f, f.startsWith("-"));
            }
        }
    }
}
