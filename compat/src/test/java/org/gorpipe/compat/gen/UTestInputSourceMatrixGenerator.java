package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestInputSourceMatrixGenerator {

    private static InputSourceMatrixGenerator.GenerationResult result;

    @BeforeClass
    public static void generate() {
        result = InputSourceMatrixGenerator.generate(SurfaceInventory.read());
    }

    @Test
    public void coversTheHermeticInputSources() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.toString(), ids.contains("is.gor.bare"));
        Assert.assertTrue(ids.toString(), ids.contains("is.nor.bare"));
        Assert.assertTrue(ids.toString(), ids.contains("is.gorrow.bare"));
    }

    @Test
    public void generatesCasesForInputSourceFlagsToo() {
        // GOR alone declares 23 flags, none of which the pipe-command matrix reaches.
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.toString(), ids.contains("is.gor.flag_nowithin"));
        Assert.assertTrue("expected at least 40 input source cases, got " + result.cases.size(),
                result.cases.size() >= 40);
    }

    @Test
    public void excludesTheSourcesThatCannotRunHermetically() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.toString(), ids.stream().noneMatch(id -> id.startsWith("is.cmd.")));
        Assert.assertTrue(ids.toString(), ids.stream().noneMatch(id -> id.startsWith("is.sql.")));
        Assert.assertTrue(ids.toString(),
                ids.stream().noneMatch(id -> id.startsWith("is.gorsql.")));
    }

    @Test
    public void usesTheCuratedArgumentForEachSource() {
        CompatCase gorrow = result.cases.stream()
                .filter(c -> c.id.equals("is.gorrow.bare"))
                .findFirst().orElseThrow(() -> new AssertionError("no bare GORROW case"));
        // GORROW takes a position, not a file.
        Assert.assertEquals("GORROW chr1,1,1 | top 5", gorrow.query);

        CompatCase gor = result.cases.stream()
                .filter(c -> c.id.equals("is.gor.bare"))
                .findFirst().orElseThrow(() -> new AssertionError("no bare GOR case"));
        Assert.assertTrue(gor.query, gor.query.contains("${ROOT}/left.gor"));
        Assert.assertFalse("a case must declare the fixtures its query uses",
                gor.inputs.isEmpty());
    }

    @Test
    public void everyCaseIsBaselineTierAndWellFormed() {
        for (CompatCase c : result.cases) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNull(c.expected);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
        }
    }

    @Test
    public void idsAreUnique() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertEquals(ids.size(), ids.stream().distinct().count());
    }
}
