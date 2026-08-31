package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestCaseLoader {

    private static CompatCase byId(String id) {
        return CaseLoader.loadAll().stream()
                .filter(c -> c.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("case not loaded: " + id));
    }

    @Test
    public void loadsSeedSpecCases() {
        List<String> ids = CaseLoader.loadAll().stream()
                .map(c -> c.id).collect(Collectors.toList());

        Assert.assertTrue(ids.toString(), ids.contains("cmd.calc.add_two_integers"));
        Assert.assertTrue(ids.toString(), ids.contains("cmd.calc.string_literal"));
        Assert.assertTrue(ids.toString(), ids.contains("cmd.calc.missing_expression_errors"));
    }

    @Test
    public void derivesCategoryAndFeatureFromId() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertEquals("cmd", c.category());
        Assert.assertEquals("calc", c.feature());
    }

    @Test
    public void classifiesTier() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertTrue(c.isSpec());
        Assert.assertFalse(c.isBaseline());
    }

    @Test
    public void preservesTabsInExpectedBlocks() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertTrue("expected block lost its tabs", c.expected.contains("\t"));
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t2\n", c.expected);
    }

    @Test
    public void recordsSourceFileForErrorMessages() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertNotNull(c.sourceFile);
        Assert.assertTrue(c.sourceFile, c.sourceFile.endsWith("calc.yml"));
    }
}
