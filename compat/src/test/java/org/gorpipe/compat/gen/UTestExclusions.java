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
