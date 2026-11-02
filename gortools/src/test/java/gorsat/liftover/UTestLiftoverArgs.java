package gorsat.liftover;

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;

public class UTestLiftoverArgs {

    private static final String[] NO_ARGS = {};

    private static int ref(String header) {
        return LiftoverArgs.refColumn(NO_ARGS, header, false);
    }

    private static int alt(String header) {
        return LiftoverArgs.altColumn(NO_ARGS, header, false);
    }

    @Test
    public void refPrefersRefOverReferenceWhateverTheOrder() {
        Assert.assertEquals(3, ref("Chrom\tPos\tReference\tRef\tAlt"));
        Assert.assertEquals(2, ref("Chrom\tPos\tRef\tReference\tAlt"));
        Assert.assertEquals(2, ref("Chrom\tPos\tReference"));
    }

    @Test
    public void altOrderIsAltThenCallThenAllele() {
        Assert.assertEquals(4, alt("Chrom\tPos\tAllele\tCall\tAlt"));
        Assert.assertEquals(3, alt("Chrom\tPos\tAllele\tCall"));
        Assert.assertEquals(2, alt("Chrom\tPos\tAllele"));
        Assert.assertEquals(3, alt("Chrom\tPos\tCall\tAlt\tAllele"));
    }

    @Test
    public void matchingIsCaseInsensitive() {
        Assert.assertEquals(2, ref("chrom\tpos\trEf\taLT"));
        Assert.assertEquals(3, alt("chrom\tpos\trEf\taLT"));
        Assert.assertEquals(2, ref("chrom\tpos\treFERENCE"));
        Assert.assertEquals(2, alt("chrom\tpos\tcALL"));
    }

    @Test
    public void explicitOptionOverridesTheHeaderLookup() {
        String header = "Chrom\tPos\tRef\tAlt\tmyref\tmyalt";
        Assert.assertEquals(4, LiftoverArgs.refColumn(new String[]{"-ref", "myref"}, header, false));
        Assert.assertEquals(5, LiftoverArgs.altColumn(new String[]{"-alt", "MYALT"}, header, false));
        // a number is 1-based
        Assert.assertEquals(4, LiftoverArgs.refColumn(new String[]{"-ref", "5"}, header, false));
        Assert.assertEquals(5, LiftoverArgs.altColumn(new String[]{"-alt", "6"}, header, false));
    }

    @Test
    public void explicitOptionNamingNoColumnFails() {
        try {
            LiftoverArgs.refColumn(new String[]{"-ref", "nosuch"}, "Chrom\tPos\tRef", false);
            Assert.fail("expected GorParsingException");
        } catch (GorParsingException e) {
            Assert.assertEquals("-ref", e.getOption());
        }
    }

    @Test
    public void missingColumnGivesMinusOne() {
        Assert.assertEquals(-1, ref("Chrom\tPos\tAlt"));
        Assert.assertEquals(-1, alt("Chrom\tPos\tRef"));
        Assert.assertEquals(-1, ref("Chrom\tPos"));
    }

    @Test
    public void norHeaderLookupReturnsFullRowIndex() {
        String header = "ChromNOR\tPosNOR\tid\tREF\tALT";
        Assert.assertEquals(3, LiftoverArgs.refColumn(NO_ARGS, header, true));
        Assert.assertEquals(4, LiftoverArgs.altColumn(NO_ARGS, header, true));
    }

    @Test
    public void norExplicitOptionCountsUserColumns() {
        String header = "ChromNOR\tPosNOR\tid\tr\ta";
        Assert.assertEquals(3, LiftoverArgs.refColumn(new String[]{"-ref", "r"}, header, true));
        Assert.assertEquals(4, LiftoverArgs.altColumn(new String[]{"-alt", "a"}, header, true));
        // user column numbers start after ChromNOR and PosNOR
        Assert.assertEquals(3, LiftoverArgs.refColumn(new String[]{"-ref", "2"}, header, true));
        Assert.assertEquals(4, LiftoverArgs.altColumn(new String[]{"-alt", "3"}, header, true));
    }
}
