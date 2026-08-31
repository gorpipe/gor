package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

public class UTestCaseRunner {

    private static CompatCase specCase(String id, String mode, String query, String expected) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "spec";
        c.mode = mode;
        c.source = "review";
        c.query = query;
        c.expected = expected;
        c.cites.add("documentation/src/command/CALC.rst");
        return c;
    }

    @Test
    public void exactMatchPasses() {
        CaseRunner.assertSpec(specCase("cmd.calc.t1", "exact", "norrows 1 | calc X 1+1",
                "ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t2\n"));
    }

    @Test
    public void exactMismatchFailsAndNamesTheCase() {
        CompatCase c = specCase("cmd.calc.t2", "exact", "norrows 1 | calc X 1+1",
                "ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t999\n");
        try {
            CaseRunner.assertSpec(c);
            Assert.fail("expected an AssertionError for a wrong golden");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("cmd.calc.t2"));
            Assert.assertTrue("failure must report the retained fixture directory",
                    expected.getMessage().contains("fixtures retained at"));
        }
    }

    @Test
    public void errorModePassesWhenQueryFails() {
        CaseRunner.assertSpec(specCase("cmd.calc.t3", "error", "norrows 1 | calc X", null));
    }

    @Test
    public void errorModeFailsWhenQuerySucceeds() {
        CompatCase c = specCase("cmd.calc.t4", "error", "norrows 1", null);
        try {
            CaseRunner.assertSpec(c);
            Assert.fail("expected an AssertionError — the query did not fail");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("cmd.calc.t4"));
        }
    }

    @Test
    public void errorContainsIsCheckedWhenPresent() {
        CompatCase c = specCase("syntax.unknown.t1", "error", "norrows 1 | nosuchcommand", null);
        c.errorContains = "NOSUCHCOMMAND";
        CaseRunner.assertSpec(c);

        CompatCase wrong = specCase("syntax.unknown.t2", "error", "norrows 1 | nosuchcommand", null);
        wrong.errorContains = "this text is not in the message";
        try {
            CaseRunner.assertSpec(wrong);
            Assert.fail("expected an AssertionError for a non-matching errorContains");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(),
                    expected.getMessage().contains("this text is not in the message"));
        }
    }

    @Test
    public void inputFixturesAreMaterialisedUnderRoot() {
        CompatCase c = specCase("cmd.gor.t1", "exact", "gor ${ROOT}/basic.gor",
                "Chrom\tPos\tVal\nchr1\t1\t10\n");
        CompatInput in = new CompatInput();
        in.path = "basic.gor";
        in.content = "Chrom\tPos\tVal\nchr1\t1\t10\n";
        c.inputs.add(in);

        CaseRunner.assertSpec(c);
    }

    @Test
    public void runReturnsResultWithoutAsserting() {
        CompatResult r = CaseRunner.run(specCase("cmd.calc.t5", "exact", "norrows 2", null));
        Assert.assertFalse(r.failed());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum", r.header);
        Assert.assertEquals(2, r.rows.size());
    }
}
