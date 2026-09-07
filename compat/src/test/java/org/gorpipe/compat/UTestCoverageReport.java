package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UTestCoverageReport {

    private static CompatCase c(String id, String tier, String query) {
        CompatCase x = new CompatCase();
        x.id = id;
        x.tier = tier;
        x.mode = "exact";
        x.query = query;
        return x;
    }

    @Test
    public void attributesCommandsMentionedInQueries() {
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));

        CoverageReport report = CoverageReport.of(cases, SurfaceInventory.read());
        Assert.assertTrue("CALC should be attributed", report.commandsCovered() >= 1);
        Assert.assertFalse("JOIN should not be attributed",
                report.uncoveredCommands().isEmpty());
    }

    @Test
    public void attributesFlagsAgainstTheOwningCommand() {
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.join.a", "baseline", "gor a.gor | join -snpsnp b.gor"));

        CoverageReport report = CoverageReport.of(cases, SurfaceInventory.read());
        Assert.assertTrue("join -snpsnp should be attributed", report.flagsCovered() >= 1);
    }

    @Test
    public void bothTiersCountTowardCoverage() {
        // A baseline case does exercise the code; it just does not vouch for
        // correctness. Coverage is a diagnostic, so it counts both.
        List<CompatCase> specOnly = new ArrayList<>();
        specOnly.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));

        List<CompatCase> withBaseline = new ArrayList<>(specOnly);
        withBaseline.add(c("cmd.join.a", "baseline", "gor a.gor | join -snpsnp b.gor"));

        SurfaceInventory inv = SurfaceInventory.read();
        Assert.assertTrue(CoverageReport.of(withBaseline, inv).commandsCovered()
                > CoverageReport.of(specOnly, inv).commandsCovered());
    }

    @Test
    public void attributesTheLeadingInputSource() {
        // NORROWS is an input source, not a pipe command; without attributing it
        // the report claimed no coverage for a construct half the corpus uses.
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));

        CoverageReport report = CoverageReport.of(cases, SurfaceInventory.read());
        Assert.assertTrue("NORROWS should be attributed as an input source",
                report.inputSourcesCovered() >= 1);
    }

    @Test
    public void countsNorContextCoverageSeparately() {
        // A command covered in GOR is not thereby covered in NOR: the contexts
        // differ in arguments and in which commands are allowed at all.
        List<CompatCase> gorOnly = new ArrayList<>();
        gorOnly.add(c("cmd.calc.a", "baseline", "gor x.gor | calc X 1"));
        Assert.assertEquals(0, CoverageReport.of(gorOnly, SurfaceInventory.read())
                .norCommandsCovered());

        List<CompatCase> withNor = new ArrayList<>(gorOnly);
        withNor.add(c("nor.calc.bare", "baseline", "nor x.gor | calc X 1"));
        Assert.assertEquals(1, CoverageReport.of(withNor, SurfaceInventory.read())
                .norCommandsCovered());

        String out = CoverageReport.of(withNor, SurfaceInventory.read()).render();
        Assert.assertTrue(out, out.contains("nor ctx"));
    }

    @Test
    public void separatesGapsThatAreExcludedFromGapsWorthChasing() {
        // Most remaining gaps sit on deliberately excluded surface. Reporting one
        // number for both makes the report read as dozens of leads when only a
        // handful are real.
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));

        CoverageReport report = CoverageReport.of(cases, SurfaceInventory.read());
        Assert.assertTrue("some uncovered commands are excluded ones",
                report.excludedCommandGaps() > 0);
        Assert.assertTrue("the two must not double count",
                report.excludedCommandGaps() <= report.uncoveredCommands().size());

        String out = report.render();
        Assert.assertTrue(out, out.contains("excluded"));
        Assert.assertTrue(out, out.contains("reachable"));
    }

    @Test
    public void renderIncludesEveryHeadlineNumber() {
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));
        cases.add(c("cmd.join.a", "baseline", "gor a.gor | join -snpsnp b.gor"));

        String out = CoverageReport.of(cases, SurfaceInventory.read()).render();
        Assert.assertTrue(out, out.contains("SPEC"));
        Assert.assertTrue(out, out.contains("inputsrc"));
        Assert.assertTrue(out, out.contains("macros"));
        Assert.assertTrue(out, out.contains("BASELINE"));
        Assert.assertTrue(out, out.contains("SURFACE"));
        Assert.assertTrue(out, out.contains("commands"));
        Assert.assertTrue(out, out.contains("EXCLUDED"));
    }
}
