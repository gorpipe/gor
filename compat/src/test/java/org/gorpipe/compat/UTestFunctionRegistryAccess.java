package org.gorpipe.compat;

import gorsat.parser.CalcFunctions;
import gorsat.parser.FunctionRegistry;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * FunctionRegistry is a class, not an object: the CALC/WHERE function surface
 * lives in the self-registering CalcFunctions.registry singleton, so no engine
 * initialisation is needed to enumerate it.
 */
public class UTestFunctionRegistryAccess {

    private static FunctionRegistry registry() {
        return CalcFunctions.registry();
    }

    @Test
    public void enumeratesRegisteredFunctionNames() {
        Set<String> names = registry().functionNames();

        Assert.assertFalse("registry reported no functions", names.isEmpty());
        Assert.assertTrue("expected more than 50 registered functions, got " + names.size(),
                names.size() > 50);
    }

    @Test
    public void everyReportedNameIsRecognisedByHasFunction() {
        for (String name : registry().functionNames()) {
            Assert.assertTrue("hasFunction disagrees with functionNames for " + name,
                    registry().hasFunction(name));
        }
    }

    @Test
    public void reportsTheSignaturesOfEachFunction() {
        // A name alone cannot be called: the generator needs the argument types.
        // Signatures encode them as "sv:iv2sv" — String and Int arguments,
        // returning String.
        java.util.Map<String, java.util.List<String>> signatures = registry().functionSignatures();

        Assert.assertEquals("every named function must report signatures",
                registry().functionNames().size(), signatures.size());
        Assert.assertTrue(signatures.containsKey("UPPER"));
        for (java.util.Map.Entry<String, java.util.List<String>> e : signatures.entrySet()) {
            Assert.assertFalse(e.getKey() + " reports no signature", e.getValue().isEmpty());
            for (String sig : e.getValue()) {
                Assert.assertTrue(e.getKey() + " has a signature with no return type: " + sig,
                        sig.contains("2"));
            }
        }
    }

    @Test
    public void reportedSignatureMapIsNotMutable() {
        java.util.Map<String, java.util.List<String>> signatures = registry().functionSignatures();
        try {
            signatures.put("SHOULD_NOT_BE_POSSIBLE", java.util.Collections.emptyList());
            Assert.fail("functionSignatures must not expose a mutable view");
        } catch (UnsupportedOperationException expected) {
            // this is the contract
        }
    }

    @Test
    public void reportedSetIsNotMutable() {
        Set<String> names = registry().functionNames();
        try {
            names.add("SHOULD_NOT_BE_POSSIBLE");
            Assert.fail("functionNames must not expose a mutable view of the registry");
        } catch (UnsupportedOperationException expected) {
            // this is the contract
        }
    }
}
