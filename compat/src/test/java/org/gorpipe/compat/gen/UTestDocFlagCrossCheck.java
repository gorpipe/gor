package org.gorpipe.compat.gen;

import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UTestDocFlagCrossCheck {

    private static DocFlagCrossCheck.CrossCheckResult result;

    @BeforeClass
    public static void crossCheck() {
        result = DocFlagCrossCheck.run(SurfaceInventory.read());
    }

    @Test
    public void parsesTheCommandPages() {
        // 117 command pages exist.
        Assert.assertTrue("expected at least 50 command pages parsed, got "
                + result.pagesParsed, result.pagesParsed >= 50);
    }

    @Test
    public void reportsFindingsInABothWaysComparison() {
        // Both directions must be computed. Either list may legitimately be empty
        // on a well-documented codebase, but the totals must be reported.
        Assert.assertNotNull(result.undocumented);
        Assert.assertNotNull(result.phantom);
    }

    @Test
    public void everyFindingNamesACommandAndAFlag() {
        for (String entry : result.undocumented) {
            Assert.assertTrue(entry, entry.contains(" -"));
        }
        for (String entry : result.phantom) {
            Assert.assertTrue(entry, entry.contains(" -"));
        }
    }
}
