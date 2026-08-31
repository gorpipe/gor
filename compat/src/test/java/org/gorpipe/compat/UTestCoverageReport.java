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
    public void renderIncludesEveryHeadlineNumber() {
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));
        cases.add(c("cmd.join.a", "baseline", "gor a.gor | join -snpsnp b.gor"));

        String out = CoverageReport.of(cases, SurfaceInventory.read()).render();
        Assert.assertTrue(out, out.contains("SPEC"));
        Assert.assertTrue(out, out.contains("BASELINE"));
        Assert.assertTrue(out, out.contains("SURFACE"));
        Assert.assertTrue(out, out.contains("commands"));
        Assert.assertTrue(out, out.contains("EXCLUDED"));
    }
}
