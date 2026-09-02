package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.Set;

public class UTestCoverageProbe {

    private static CompatCase baselineCase(String id, String query) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "baseline";
        c.mode = "exact";
        c.query = query;
        return c;
    }

    @Test
    public void isAvailableWhenTheAgentIsAttached() {
        // The test task is instrumented by the jacoco plugin, so the agent is
        // present here. Outside such a JVM the probe must report unavailable
        // rather than fail, which is what lets generate work without coverage.
        Assert.assertTrue("expected the jacoco agent under :compat:test",
                CoverageProbe.available());
    }

    @Test
    public void reportsProbesThatAreAlreadyHit() {
        Assume.assumeTrue(CoverageProbe.available());
        Assert.assertFalse("expected some probes to be hit by now",
                CoverageProbe.hitProbes().isEmpty());
    }

    @Test
    public void runningANewQueryReachesProbesNotHitBefore() {
        Assume.assumeTrue(CoverageProbe.available());

        Set<String> before = CoverageProbe.hitProbes();
        CaseRunner.run(baselineCase("cmd.rank.probe",
                "norrows 5 | rank 1000 RowNum -o | top 2"));
        Set<String> after = CoverageProbe.hitProbes();

        Assert.assertTrue("running a query should reach probes not hit before",
                after.size() > before.size());
        Assert.assertTrue("probe sets must grow monotonically", after.containsAll(before));
    }
}
