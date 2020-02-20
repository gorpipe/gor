package org.gorpipe.model.genome.files.gor.bgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestVariantGrouper {

    @Test
    public void test_basic() {
        final VariantGrouper vg = new VariantGrouper();

        vg.initialize("chr1", 1, "A", null, null);
        vg.add("C", "0123210");
        final HardCallDataBlock db1 = vg.merge();
        final int[] db1gt1 = db1.gt1;
        final int[] db1gt2 = db1.gt2;
        final boolean[] flags1 = db1.existing;
        Assert.assertArrayEquals(new int[] {0, 0, 1, -1, 1, 0, 0}, db1gt1);
        Assert.assertArrayEquals(new int[] {0, 1, 1, -1, 1, 1, 0}, db1gt2);
        Assert.assertArrayEquals(new boolean[] {true, true, true, false, true, true, true}, flags1);
        Assert.assertArrayEquals(new String[] {"A", "C"}, db1.alleles);

        vg.initialize("chr1", 2, "A", null, null);
        vg.add("C", "0011203");
        vg.add("G", "0101023");
        final HardCallDataBlock db2 = vg.merge();
        final int[] db2gt1 = db2.gt1;
        final int[] db2gt2 = db2.gt2;
        final boolean[] flags2 = db2.existing;
        Assert.assertArrayEquals(new int[] {0, 0, 0, 1, 1, 2, -1}, db2gt1);
        Assert.assertArrayEquals(new int[] {0, 2, 1, 2, 1, 2, -1}, db2gt2);
        Assert.assertArrayEquals(new boolean[] {true, true, true, true, true, true, false}, flags2);
        Assert.assertArrayEquals(new String[] {"A", "C", "G"}, db2.alleles);

        Assert.assertSame(db1, db2);
        Assert.assertSame(db1gt1, db2gt1);
        Assert.assertSame(db1gt2, db2gt2);
        Assert.assertSame(flags1, flags2);
    }
}
