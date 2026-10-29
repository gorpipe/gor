package org.gorpipe.compat.gen;

import org.junit.Assert;
import org.junit.Test;

public class UTestExclusions {

    @Test
    public void excludesTheNonHermeticCommands() {
        Exclusions exclusions = Exclusions.load();
        Assert.assertTrue(exclusions.excludesCommand("CMD"));
        Assert.assertTrue(exclusions.excludesCommand("SQL"));
        Assert.assertTrue(exclusions.excludesCommand("WAIT"));
    }

    @Test
    public void doesNotExcludeAnOrdinaryCommand() {
        Exclusions exclusions = Exclusions.load();
        Assert.assertFalse(exclusions.excludesCommand("CALC"));
        Assert.assertFalse(exclusions.excludesCommand("JOIN"));
    }

    @Test
    public void excludesNonHermeticFunctions() {
        Exclusions exclusions = Exclusions.load();
        Assert.assertTrue(exclusions.excludesFunction("SYSTEM"));
        Assert.assertTrue(exclusions.excludesFunction("EVAL"));
        Assert.assertFalse(exclusions.excludesFunction("UPPER"));
    }

    @Test
    public void excludesOneFlagWithoutExcludingItsCommand() {
        // KING -sym orders its pair rows freely, so no baseline can hold; the rest
        // of KING is fine. Named against the flag rather than the generated case id
        // so that the coverage report counts it as excluded too — a case-level
        // exclusion suppresses the case but leaves the flag looking merely unreached.
        Exclusions exclusions = Exclusions.load();
        Assert.assertTrue(exclusions.excludesFlag("KING", "-sym"));
        Assert.assertFalse("excluding one flag must not exclude the whole command",
                exclusions.excludesCommand("KING"));
        Assert.assertFalse(exclusions.excludesFlag("KING", "-bare"));
    }

    @Test
    public void excludesAFlagOfACommandThatIsOtherwiseGenerated() {
        // SIGNATURE -timeres rounds System.currentTimeMillis, so its output moves
        // between runs; -file and -filecontent hash a file and are covered.
        Exclusions exclusions = Exclusions.load();
        Assert.assertTrue(exclusions.excludesFlag("SIGNATURE", "-timeres"));
        Assert.assertFalse(exclusions.excludesFlag("SIGNATURE", "-file"));
        Assert.assertFalse(exclusions.excludesCommand("SIGNATURE"));
    }

    @Test
    public void everyEntryCarriesAReason() {
        Exclusions exclusions = Exclusions.load();
        Assert.assertFalse(exclusions.entries().isEmpty());
        for (Exclusions.Entry e : exclusions.entries()) {
            Assert.assertNotNull("entry without an element", e.element);
            Assert.assertNotNull("entry " + e.element + " has no reason", e.reason);
            Assert.assertFalse("entry " + e.element + " has an empty reason",
                    e.reason.trim().isEmpty());
        }
    }
}
