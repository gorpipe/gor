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
    public void doesNotCountFlagsMentionedInProse() {
        // The CSVSEL page discusses the GOR command's -f and -ff filtering options
        // in its body text. Counting those as CSVSEL's own documented flags made
        // them look like phantom flags of CSVSEL, which they are not.
        Assert.assertFalse(result.phantom.toString(), result.phantom.contains("CSVSEL -f"));
        Assert.assertFalse(result.phantom.toString(), result.phantom.contains("CSVSEL -ff"));
        Assert.assertFalse(result.phantom.toString(), result.phantom.contains("CSVSEL -nf"));
        // Nor does a deprecation note elsewhere on the CIGARSEGS page.
        Assert.assertFalse(result.phantom.toString(), result.phantom.contains("CIGARSEGS -ref"));
    }

    @Test
    public void resolvesAPageAgainstEveryRegistryThatOwnsTheName() {
        // CMD is both a pipe command and an input source, and the documented -n
        // belongs to the input source. Checking the pipe command alone reported it
        // as a phantom flag of a command that does document it.
        Assert.assertFalse(result.phantom.toString(), result.phantom.contains("CMD -n"));
        Assert.assertFalse(result.undocumented.toString(),
                result.undocumented.contains("CMD -n"));
    }

    @Test
    public void countsAnOptionsTableEntryAsDocumented() {
        // GROUP's table documents -count and the registry declares it, so it must
        // appear in neither list.
        Assert.assertFalse(result.undocumented.toString(),
                result.undocumented.contains("GROUP -count"));
        Assert.assertFalse(result.phantom.toString(), result.phantom.contains("GROUP -count"));
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
