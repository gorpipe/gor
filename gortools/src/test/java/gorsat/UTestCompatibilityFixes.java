package gorsat;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Regression tests for the defects the new GOR compatibility suite turned up
 * (ENGKNOW-3780, and INVSTUDENT under ENGKNOW-3776).
 *
 * That suite pins these behaviours too, but it lives in its own module and lands
 * separately, so these keep the fixes covered from inside the engine's own tests.
 */
public class UTestCompatibilityFixes {

    // --- GAVA ----------------------------------------------------------------

    @Test
    public void gavaAcceptsTheDocumentedUsePhaseFlag() {
        // GAVA.rst documents -usePhase and Gava.scala reads it, but it was absent
        // from the declaration, so every invocation using it was rejected outright.
        // Whatever else this query does, it must not fail on the flag being unknown.
        try {
            TestUtils.runGorPipeLines("gorrow chr1,1 | calc Ref 'A' | calc Alt 'G' "
                    + "| GAVA 10 -usePhase -caselist PN001 -ctrllist PN002");
        } catch (Exception e) {
            Assert.assertFalse("-usePhase must be a recognised GAVA option: " + e.getMessage(),
                    String.valueOf(e.getMessage()).contains("not a part of this command"));
        }
    }

    @Test
    public void gavaNamesTheMissingColumnRatherThanThrowingAnIndexError() {
        // GAVA falls back to fixed column positions for any column the header does
        // not name. This input is narrower than those positions, and used to fail
        // with a bare ArrayIndexOutOfBoundsException naming no column.
        try {
            TestUtils.runGorPipeLines("gorrow chr1,1 | calc Ref 'A' | calc Alt 'G' "
                    + "| GAVA 10 -caselist PN001 -ctrllist PN002");
            Assert.fail("expected a narrow input to be rejected");
        } catch (GorDataException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("GAVA"));
            Assert.assertTrue("the message should name a column: " + expected.getMessage(),
                    expected.getMessage().contains("column"));
        }
    }

    // --- LEFTWHERE -----------------------------------------------------------

    @Test
    public void leftWhereOnTheLastColumnPassesRowsThroughUnchanged() {
        // Naming the last column leaves no right-source columns to blank, which
        // used to fail with "tail of empty list". With nothing to blank, the row
        // is unchanged.
        String[] lines = TestUtils.runGorPipeLines(
                "gorrow chr1,1 | calc Ref 'A' | calc Alt 'G' | LEFTWHERE Alt Ref = 'A'");

        Assert.assertEquals("chrom\tpos\tRef\tAlt\n", lines[0]);
        Assert.assertEquals("chr1\t1\tA\tG\n", lines[1]);
    }

    @Test
    public void leftWhereFailingConditionOnTheLastColumnEmitsNoTrailingTab() {
        // The false condition is the path that appended a separator for zero blank
        // fields, producing a row one column wider than its own header.
        String[] lines = TestUtils.runGorPipeLines(
                "gorrow chr1,1 | calc Ref 'A' | calc Alt 'G' | LEFTWHERE Alt Ref = 'Z'");

        int headerColumns = lines[0].trim().split("\t", -1).length;
        for (int i = 1; i < lines.length; i++) {
            Assert.assertEquals("row " + i + " is a different width than the header: "
                            + lines[i].replace("\t", "<TAB>"),
                    headerColumns, lines[i].trim().split("\t", -1).length);
        }
    }

    // --- INVSTUDENT ----------------------------------------------------------

    // Bounded deliberately: without the fix this query does not return, and an
    // unbounded test would hang the build instead of failing it. Confirmed by
    // reverting the fix, which hung the test run rather than reporting a failure.
    @Test(timeout = 30_000)
    public void invStudentRejectsAProbabilityThatCannotConverge() {
        // colt's root finder never returns for a value outside [0,1], so the query
        // hung indefinitely rather than failing.
        try {
            TestUtils.runGorPipeLines("gorrow chr1,1 | calc X INVSTUDENT(1.5,10)");
            Assert.fail("expected an out-of-range probability to be rejected");
        } catch (GorDataException expected) {
            Assert.assertTrue(expected.getMessage(),
                    expected.getMessage().contains("INVSTUDENT"));
        }
    }

    @Test
    public void invStudentKeepsTheDefinedOutcomeAtTheBoundary() {
        // alpha = 1 returns 0.0 and always did. The range check must not take the
        // boundaries with it.
        String[] lines = TestUtils.runGorPipeLines("gorrow chr1,1 | calc X INVSTUDENT(1.0,10)");
        Assert.assertEquals("chr1\t1\t0.0\n", lines[1]);
    }

    @Test
    public void invStudentStillEvaluatesAValidProbability() {
        // The two-tailed t quantile for df=10 at alpha=0.5. A published t-table
        // gives 0.700 for one tail at 0.25, so the value is verified, not copied.
        String[] lines = TestUtils.runGorPipeLines("gorrow chr1,1 | calc X INVSTUDENT(0.5,10)");
        Assert.assertEquals("chr1\t1\t0.6998121397488263\n", lines[1]);
    }

    // --- GTLD ----------------------------------------------------------------

    @Test
    public void gtldRejectionNamesTheFlagsItRegisters() {
        // The message used to name -sumLD and -calcLD, which are not flags of this
        // command or any other, so following it failed again.
        try {
            TestUtils.runGorPipeLines(
                    "gorrow chr1,1 | calc bucket 'b1' | calc values '0,1' | GTLD");
            Assert.fail("expected GTLD to reject an invocation with neither -sum nor -calc");
        } catch (GorParsingException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("-sum"));
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("-calc"));
            Assert.assertFalse("the message must not name flags that do not exist: "
                    + expected.getMessage(), expected.getMessage().contains("LD,"));
        }
    }
}
