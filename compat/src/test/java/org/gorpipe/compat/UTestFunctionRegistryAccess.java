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
