package org.gorpipe.compat.gen;

import org.junit.Assert;
import org.junit.Test;

public class UTestFlagValues {

    @Test
    public void loadsCuratedValues() {
        FlagValues values = FlagValues.load();
        Assert.assertTrue("expected a curated value for JOIN -maxseg",
                values.has("JOIN", "-maxseg"));
        Assert.assertEquals("1000", values.valueFor("JOIN", "-maxseg"));
    }

    @Test
    public void reportsUnmappedFlagsAsAbsentRatherThanThrowing() {
        FlagValues values = FlagValues.load();
        Assert.assertFalse(values.has("JOIN", "-nosuchflagever"));
        Assert.assertNull(values.valueFor("JOIN", "-nosuchflagever"));
    }

    @Test
    public void fallsBackToAWildcardEntryWhenCommandIsUnlisted() {
        // A flag name that means the same thing across commands can be mapped once
        // under the '*' key rather than repeated for all 108 commands.
        FlagValues values = FlagValues.load();
        Assert.assertTrue("expected a wildcard entry for -s", values.has("SOMEUNLISTEDCMD", "-s"));
    }
}
