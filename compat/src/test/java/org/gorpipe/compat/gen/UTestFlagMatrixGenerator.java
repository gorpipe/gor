package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestFlagMatrixGenerator {

    private static FlagMatrixGenerator.GenerationResult result;

    @BeforeClass
    public static void generate() {
        result = FlagMatrixGenerator.generate(SurfaceInventory.read(), FlagValues.load());
    }

    @Test
    public void generatesCasesForValuelessFlags() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.stream().anyMatch(id -> id.startsWith("cmd.join.flag_snpsnp")));
    }

    @Test
    public void everyGeneratedCaseIsBaselineTierWithNoInlineExpected() {
        for (CompatCase c : result.cases) {
            Assert.assertEquals("generated cases must be baseline tier", "baseline", c.tier);
            Assert.assertNull("generated cases must carry no inline expected", c.expected);
            Assert.assertTrue("generated cases must have a query", c.query.length() > 0);
        }
    }

    @Test
    public void generatedIdsAreUniqueAndWellFormed() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertEquals("generated ids must be unique",
                ids.size(), ids.stream().distinct().count());
        for (String id : ids) {
            Assert.assertTrue("id violates the required pattern: " + id,
                    id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
        }
    }

    @Test
    public void unmappedValueFlagsAreReportedNotSkippedSilently() {
        // 242 value-taking flags exist and the seed mapping covers only common
        // ones, so this list must be non-empty and every entry must name a flag.
        Assert.assertFalse("expected unmapped value flags to be reported",
                result.unmappedValueFlags.isEmpty());
        for (String entry : result.unmappedValueFlags) {
            Assert.assertTrue(entry, entry.contains("-"));
        }
    }

    @Test
    public void generatesNoCaseForAnUnmappedValueFlag() {
        // Matched on the whole id, not the flag suffix: the same flag name can be
        // valueless on one command and unmapped-value-taking on another, so a
        // suffix match would flag a legitimately generated case from elsewhere.
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        for (String entry : result.unmappedValueFlags) {
            String command = entry.substring(0, entry.indexOf(' '));
            String flag = entry.substring(entry.lastIndexOf(' ') + 1);
            String expectedId = "cmd." + command.toLowerCase()
                    + ".flag_" + flag.substring(1).toLowerCase();
            Assert.assertFalse("a case was generated for the unmapped flag " + entry,
                    ids.contains(expectedId));
        }
    }

    @Test
    public void producesCasesForALargeShareOfTheSurface() {
        // 108 commands with 217 valueless flags; a healthy run generates hundreds
        // of cases. A low number means flag parsing regressed.
        Assert.assertTrue("expected at least 200 generated cases, got " + result.cases.size(),
                result.cases.size() >= 200);
    }
}
