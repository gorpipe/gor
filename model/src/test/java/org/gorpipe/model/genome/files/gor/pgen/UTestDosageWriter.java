package org.gorpipe.model.genome.files.gor.pgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestDosageWriter {

    @Test
    public void test_emptyArguments() {
        final DosageWriter dw = new DosageWriter(new float[0]);
        Assert.assertEquals(0, dw.write(new byte[0], 0, 0));
        Assert.assertTrue(dw.done());
    }

    @Test
    public void test_writeTooSmallBuffer() {
        final float[] dosages = {0, 1, 2};
        final byte[] buffer = new byte[2];
        final DosageWriter dw = new DosageWriter(dosages);

        Assert.assertEquals(2, dw.write(buffer, 0, buffer.length));
        Assert.assertFalse(dw.done());
        Assert.assertEquals(0, buffer[0] & 0xff);
        Assert.assertEquals(0, buffer[1] & 0xff);

        Assert.assertEquals(2, dw.write(buffer, 0, buffer.length));
        Assert.assertFalse(dw.done());
        Assert.assertEquals(0, buffer[0] & 0xff);
        Assert.assertEquals(0x40, buffer[1] & 0xff);

        Assert.assertEquals(-2, dw.write(buffer, 0, buffer.length));
        Assert.assertTrue(dw.done());
        Assert.assertEquals(0, buffer[0] & 0xff);
        Assert.assertEquals(0x80, buffer[1] & 0xff);
    }

    @Test
    public void test_bigBuffer() {
        final float[] dosages = {0, 1, 2};
        final byte[] buffer = new byte[6];
        final DosageWriter dw = new DosageWriter(dosages);

        Assert.assertEquals(-6, dw.write(buffer, 0, buffer.length));
        Assert.assertTrue(dw.done());
        Assert.assertEquals(0x00, buffer[0] & 0xff);
        Assert.assertEquals(0x00, buffer[1] & 0xff);
        Assert.assertEquals(0x00, buffer[2] & 0xff);
        Assert.assertEquals(0x40, buffer[3] & 0xff);
        Assert.assertEquals(0x00, buffer[4] & 0xff);
        Assert.assertEquals(0x80, buffer[5] & 0xff);
    }
}
