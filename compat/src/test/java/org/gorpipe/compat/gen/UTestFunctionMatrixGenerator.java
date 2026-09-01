package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestFunctionMatrixGenerator {

    private static FunctionMatrixGenerator.GenerationResult result;

    @BeforeClass
    public static void generate() {
        result = FunctionMatrixGenerator.generate(SurfaceInventory.read());
    }

    @Test
    public void generatesACaseForMostRegisteredFunctions() {
        // 208 functions are registered. Every one whose argument types have a
        // sample value should get a case.
        Assert.assertTrue("expected at least 150 function cases, got " + result.cases.size(),
                result.cases.size() >= 150);
    }

    @Test
    public void buildsACallWithArgumentsMatchingTheSignature() {
        CompatCase upper = result.cases.stream()
                .filter(c -> c.id.equals("fn.upper.call"))
                .findFirst().orElseThrow(() -> new AssertionError("no case for UPPER"));

        // UPPER takes one string: sv2sv.
        Assert.assertEquals("norrows 1 | calc X UPPER('a')", upper.query);
    }

    @Test
    public void usesNoArgumentsForAZeroArityFunction() {
        // A signature of "e2..." takes nothing at all.
        Assert.assertTrue(result.cases.stream()
                .anyMatch(c -> c.query.matches(".*\\b[A-Z0-9_]+\\(\\)$")));
    }

    @Test
    public void everyCaseIsBaselineTierAndWellFormed() {
        for (CompatCase c : result.cases) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNull(c.expected);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
            Assert.assertTrue("query should call the function: " + c.query,
                    c.query.contains("("));
        }
    }

    @Test
    public void idsAreUnique() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertEquals(ids.size(), ids.stream().distinct().count());
    }

    @Test
    public void generatedCasesPassCorpusLint() {
        // MAXMEM, OPENFILES and CURRENTDATE report JVM or machine state, so their
        // output differs between runs even though it is stable within one JVM —
        // which is why the runtime reproducibility probe alone did not catch them.
        List<String> violations = org.gorpipe.compat.CaseLint.check(result.cases);
        Assert.assertTrue("generated function cases violate lint:\n  "
                + String.join("\n  ", violations), violations.isEmpty());
    }

    @Test
    public void reportsEnvironmentDependentFunctionsAsScreenedOut() {
        Assert.assertTrue(result.nonDeterministic.toString(),
                result.nonDeterministic.stream().anyMatch(e -> e.startsWith("MAXMEM ")));
        Assert.assertTrue(result.nonDeterministic.toString(),
                result.nonDeterministic.stream().anyMatch(e -> e.startsWith("RANDOM ")));
        Assert.assertTrue(result.cases.stream().noneMatch(c -> c.id.equals("fn.maxmem.call")));
    }

    @Test
    public void reportsFunctionsItCannotCall() {
        // A signature whose argument types have no sample value is reported rather
        // than turned into a broken call.
        Assert.assertNotNull(result.unsupported);
        for (String entry : result.unsupported) {
            Assert.assertTrue(entry, entry.contains(" "));
        }
    }
}
