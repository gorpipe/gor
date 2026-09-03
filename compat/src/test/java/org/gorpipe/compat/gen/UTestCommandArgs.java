package org.gorpipe.compat.gen;

import org.junit.Assert;
import org.junit.Test;

public class UTestCommandArgs {

    @Test
    public void suppliesTheDefaultPositionalForAnUnlistedCommand() {
        CommandArgs args = CommandArgs.load();
        Assert.assertEquals("${ROOT}/right.gor", args.positional("SOMEUNLISTEDCMD"));
        Assert.assertEquals("", args.requiredFlags("SOMEUNLISTEDCMD"));
    }

    @Test
    public void joinDeclaresARequiredJoinType() {
        // JOIN rejects every invocation that does not name a join type, so a case
        // testing an unrelated JOIN flag needs one supplied alongside it.
        CommandArgs args = CommandArgs.load();
        Assert.assertEquals("-snpsnp", args.requiredFlags("JOIN"));
    }

    @Test
    public void groupTakesABinSizeRatherThanAFile() {
        CommandArgs args = CommandArgs.load();
        Assert.assertEquals("1000", args.positional("GROUP"));
    }

    @Test
    public void marksTheCommandsThatNeedAReferenceBuild() {
        CommandArgs args = CommandArgs.load();
        Assert.assertTrue(args.needsReference("VERIFYVARIANT"));
        Assert.assertFalse(args.needsReference("CALC"));
    }

    @Test
    public void suppliesTheSourceFileACommandNeedsTheShapeOf() {
        // BASES reads a CIGAR column, which the primary fixture does not carry, so
        // every generated BASES case failed on the input shape rather than on the
        // flag under test.
        CommandArgs args = CommandArgs.load();
        Assert.assertEquals("${ROOT}/left.gor", args.source("SOMEUNLISTEDCMD"));
        Assert.assertEquals("${ROOT}/reads.gor", args.source("BASES"));
        Assert.assertEquals("${ROOT}/pvalues.gor", args.source("ADJUST"));
    }

    @Test
    public void anExplicitPositionalIsDistinguishedFromTheDefault() {
        // GROUP declares minArgs 0 yet rejects an invocation with no bin size, so
        // the generator needs to know the entry was curated rather than defaulted.
        CommandArgs args = CommandArgs.load();
        Assert.assertTrue(args.hasExplicitPositional("GROUP"));
        Assert.assertFalse(args.hasExplicitPositional("SOMEUNLISTEDCMD"));
    }
}
