package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UTestCaseLint {

    private static CompatCase c(String id, String query) {
        CompatCase x = new CompatCase();
        x.id = id;
        x.tier = "spec";
        x.mode = "exact";
        x.source = "review";
        x.query = query;
        x.expected = "h\n";
        x.cites.add("documentation/src/command/CALC.rst");
        return x;
    }

    @Test
    public void cleanCorpusProducesNoViolations() {
        List<String> v = CaseLint.check(Collections.singletonList(c("cmd.a.one", "norrows 1")));
        Assert.assertTrue(v.toString(), v.isEmpty());
    }

    @Test
    public void nonDeterministicConstructIsAViolation() {
        List<String> v = CaseLint.check(
                Collections.singletonList(c("cmd.a.rand", "norrows 1 | calc X random()")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("random"));
    }

    @Test
    public void machineIdentityIsAViolation() {
        // IP returns the host's address, which changes with the network rather
        // than with the engine.
        List<String> v = CaseLint.check(
                Collections.singletonList(c("fn.ip.a", "norrows 1 | calc X IP()")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("ip("));
    }

    @Test
    public void buildIdentityIsAViolation() {
        // GORVERSION embeds the git SHA, so a baseline for it changes on every
        // commit. Reviewers who see baseline churn every commit stop reading the
        // diffs, which is the one habit that makes the whole tier worthless.
        List<String> v = CaseLint.check(
                Collections.singletonList(c("fn.gorversion.a", "norrows 1 | calc X GORVERSION()")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("gorversion"));
    }

    @Test
    public void absolutePathOutsideRootIsAViolation() {
        List<String> v = CaseLint.check(
                Collections.singletonList(c("cmd.a.abs", "gor /Users/someone/data.gor")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("absolute path"));
    }

    @Test
    public void rootPlaceholderIsNotAnAbsolutePathViolation() {
        List<String> v = CaseLint.check(
                Collections.singletonList(c("cmd.a.ok", "gor ${ROOT}/data.gor")));
        Assert.assertTrue(v.toString(), v.isEmpty());
    }

    @Test
    public void identicalBodiesAreAViolation() {
        List<String> v = CaseLint.check(Arrays.asList(
                c("cmd.a.one", "norrows 1"),
                c("cmd.a.dup", "norrows 1")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("duplicate body"));
    }

    @Test
    public void unreferencedInputIsAViolation() {
        CompatCase x = c("cmd.a.orphan", "norrows 1");
        CompatInput in = new CompatInput();
        in.path = "unused.gor";
        in.content = "h\n";
        x.inputs.add(in);

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("unused.gor"));
    }

    @Test
    public void specCaseWithoutCitationIsAViolation() {
        CompatCase x = c("cmd.a.nocite", "norrows 1");
        x.cites.clear();

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("citation"));
    }

    @Test
    public void baselineCaseWithInlineExpectedIsAViolation() {
        CompatCase x = c("cmd.a.baseline", "norrows 1");
        x.tier = "baseline";
        x.source = null;
        x.cites.clear();
        x.expected = "h\n";

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("inline expected"));
    }

    @Test
    public void exactSpecCaseWithoutExpectedIsAViolation() {
        CompatCase x = c("cmd.a.noexpected", "norrows 1");
        x.expected = null;

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("no expected output"));
    }
}
