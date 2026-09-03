package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestMacroMatrixGenerator {

    private static MacroMatrixGenerator.GenerationResult result;

    @BeforeClass
    public static void generate() {
        result = MacroMatrixGenerator.generate(SurfaceInventory.read());
    }

    @Test
    public void coversTheMacrosThatCanRunOnTheirOwn() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.toString(), ids.contains("macro.pgor.bare"));
        Assert.assertTrue(ids.toString(), ids.contains("macro.tablefunction.bare"));
    }

    @Test
    public void generatesCasesForMacroFlags() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.toString(), ids.contains("macro.pgor.flag_nowithin"));
    }

    @Test
    public void excludesTheMacrosNeedingAConstructTheHarnessCannotSupply() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.toString(),
                ids.stream().noneMatch(id -> id.startsWith("macro.partgor.")));
        Assert.assertTrue(ids.toString(),
                ids.stream().noneMatch(id -> id.startsWith("macro.parallel.")));
    }

    @Test
    public void theMacroLeadsTheQueryRatherThanSittingInThePipe() {
        CompatCase pgor = result.cases.stream()
                .filter(c -> c.id.equals("macro.pgor.bare"))
                .findFirst().orElseThrow(() -> new AssertionError("no bare PGOR case"));
        Assert.assertTrue(pgor.query, pgor.query.startsWith("PGOR "));
        Assert.assertFalse("a macro expands into a script; it is not a pipe step",
                pgor.query.contains("| PGOR"));
        Assert.assertFalse(pgor.inputs.isEmpty());
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
}
