package org.gorpipe.model.genome.files.gor.bgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestHardCallDataBlockFactory {

    @Test
    public void test_parse() {
        final HardCallDataBlockFactory dbf = new HardCallDataBlockFactory();
        final HardCallDataBlock db1 = dbf.parse("chr1", 1, "A", "C", "rs1", "var1", "0123");
        final int[] db1gt1 = db1.gt1;
        final int[] db1gt2 = db1.gt2;
        final boolean[] db1flags = db1.existing;
        Assert.assertArrayEquals(new int[] {0, 0, 1, -1}, db1gt1);
        Assert.assertArrayEquals(new int[] {0, 1, 1, -1}, db1gt2);
        Assert.assertArrayEquals(new boolean[] {true, true, true, false}, db1flags);

        final HardCallDataBlock db2 = dbf.parse("chr1", 2, "A", "C", "rs2", "var2", "3210");
        final int[] db2gt1 = db2.gt1;
        final int[] db2gt2 = db2.gt2;
        final boolean[] db2flags = db2.existing;
        Assert.assertArrayEquals(new int[] {-1, 1, 0, 0}, db2gt1);
        Assert.assertArrayEquals(new int[] {-1, 1, 1, 0}, db2gt2);
        Assert.assertArrayEquals(new boolean[] {false, true, true, true}, db2flags);

        Assert.assertSame(db1, db2);
        Assert.assertSame(db1gt1, db2gt1);
        Assert.assertSame(db1gt2, db2gt2);
        Assert.assertSame(db1flags, db2flags);
    }
}
