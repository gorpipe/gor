package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

/**
 * The compatibility suite exists to be independent of the unit test
 * infrastructure. That independence is asserted here rather than left to
 * convention, so that reintroducing the coupling fails a test.
 */
public class UTestModuleBoundary {

    @Test
    public void gortoolsIsOnTheClasspath() throws Exception {
        Assert.assertNotNull(Class.forName("gorsat.process.PipeInstance"));
        Assert.assertNotNull(Class.forName("gorsat.process.CLISessionFactory"));
    }

    @Test
    public void testInfrastructureIsNotOnTheClasspath() {
        try {
            Class.forName("gorsat.TestUtils");
            Assert.fail("gorsat.TestUtils must not be reachable from :compat — "
                    + "the :test dependency has leaked back in");
        } catch (ClassNotFoundException expected) {
            // this is the contract
        }
    }
}
