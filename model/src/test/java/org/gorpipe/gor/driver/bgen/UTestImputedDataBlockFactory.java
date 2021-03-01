package org.gorpipe.gor.driver.bgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestImputedDataBlockFactory {

    @Test
    public void test_parse() {
        final ImputedDataBlockFactory dbf = new ImputedDataBlockFactory();
        final ImputedDataBlock db1 = dbf.parse("chr1", 1, "A", "C", "rs1", "var1", "~~!~~!  ");
        final float[][] db1probs = db1.prob;
        final boolean[] db1flags = db1.existing;
        Assert.assertArrayEquals(new float[][] {{1f, 0f, 0f}, {0f, 1f, 0f}, {0f, 0f, 1f}, {0f, 0f, 0f}}, db1probs);
        Assert.assertArrayEquals(new boolean[] {true, true, true, false}, db1flags);

        final ImputedDataBlock db2 = dbf.parse("chr1", 2, "A", "C", "rs2", "var2", "  ~!!~~~");
        final float[][] db2probs = db2.prob;
        final boolean[] db2flags = db2.existing;
        Assert.assertArrayEquals(new float[][] {{0f, 0f, 0f}, {0f, 0f, 1f}, {0f, 1f, 0f}, {1f, 0f, 0f}}, db2probs);
        Assert.assertArrayEquals(new boolean[] {false, true, true, true}, db2flags);

        Assert.assertSame(db1, db2);
        Assert.assertSame(db1probs, db2probs);
        Assert.assertSame(db1flags, db2flags);
    }
}
