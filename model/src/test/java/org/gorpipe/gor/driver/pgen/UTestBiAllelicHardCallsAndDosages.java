package org.gorpipe.gor.driver.pgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestBiAllelicHardCallsAndDosages {

    @Test
    public void test_getType() {
        final BiAllelicHardCallsAndDosages vr = new BiAllelicHardCallsAndDosages(new byte[0], new float[0]);
        Assert.assertEquals(0x40, vr.getType());
    }

    @Test
    public void test_emptyArguments() {
        final BiAllelicHardCallsAndDosages vr = new BiAllelicHardCallsAndDosages(new byte[0], new float[0]);
        final byte[] buffer = new byte[0];
        final int bytesWritten = vr.write(buffer, 0, 0);
        Assert.assertEquals(0, bytesWritten);
    }

    @Test
    public void test_callWriteWithEmptyBuffer() {
        final BiAllelicHardCallsAndDosages vr = new BiAllelicHardCallsAndDosages(new byte[1], new float[1]);
        final byte[] buffer = new byte[0];
        final int bytesWritten = vr.write(buffer, 0, 0);
        Assert.assertEquals(0, bytesWritten);
    }

    @Test
    public void test_write_tooSmallBuffer() {
        final byte[] buffer = new byte[2];
        final float[] dos = {0f};
        final byte[] hc = {0};
        final BiAllelicHardCallsAndDosages vr = new BiAllelicHardCallsAndDosages(hc, dos);
        int bytesWritten = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(1, bytesWritten);
        Assert.assertEquals(0, buffer[0]);

        bytesWritten = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-buffer.length, bytesWritten);
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
    }

    @Test
    public void test_tooSmallBuffer_2() {
        final byte[] buffer = new byte[2];
        final float[] dos = {1.14f, 0.14f, 1.85f, 1.5f, 1.9f};
        final byte[] hc = {1, 0, 2, 3, 2};

        final BiAllelicHardCallsAndDosages vr = new BiAllelicHardCallsAndDosages(hc, dos);

        Assert.assertEquals(buffer.length, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0xe1, buffer[0] & 0xff);
        Assert.assertEquals(0x02, buffer[1] & 0xff);

        final int dos0 = Math.round(dos[0] * 16_384);
        final int dos1 = Math.round(dos[1] * 16_384);
        final int dos2 = Math.round(dos[2] * 16_384);
        final int dos3 = Math.round(dos[3] * 16_384);
        final int dos4 = Math.round(dos[4] * 16_384);

        Assert.assertEquals(buffer.length, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(dos0 & 0xff, buffer[0] & 0xff);
        Assert.assertEquals((dos0 >>> 8) & 0xff, buffer[1] & 0xff);

        Assert.assertEquals(buffer.length, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(dos1 & 0xff, buffer[0] & 0xff);
        Assert.assertEquals((dos1 >>> 8) & 0xff, buffer[1] & 0xff);

        Assert.assertEquals(buffer.length, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(dos2 & 0xff, buffer[0] & 0xff);
        Assert.assertEquals((dos2 >>> 8) & 0xff, buffer[1] & 0xff);

        Assert.assertEquals(buffer.length, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(dos3 & 0xff, buffer[0] & 0xff);
        Assert.assertEquals((dos3 >>> 8) & 0xff, buffer[1] & 0xff);

        //Finishing.
        Assert.assertEquals(-buffer.length, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(dos4 & 0xff, buffer[0] & 0xff);
        Assert.assertEquals((dos4 >>> 8) & 0xff, buffer[1] & 0xff);
    }

    @Test
    public void test_write_perfectFitBuffer() {
        final byte[] buffer = new byte[9];
        final float[] dos = {1.14f, 0.14f, 1.85f, 1.5f};
        final byte[] hc = {1, 0, 2, 3};

        final BiAllelicHardCallsAndDosages vr = new BiAllelicHardCallsAndDosages(hc, dos);
        final int bytesWritten = vr.write(buffer, 0, buffer.length);

        Assert.assertEquals(-9, bytesWritten);
        Assert.assertEquals(0xe1, buffer[0] & 0xff);

        final int dos0 = Math.round(dos[0] * 16_384);
        final int dos1 = Math.round(dos[1] * 16_384);
        final int dos2 = Math.round(dos[2] * 16_384);
        final int dos3 = Math.round(dos[3] * 16_384);

        Assert.assertEquals(dos0 & 0xff, buffer[1] & 0xff);
        Assert.assertEquals((dos0 >>> 8) & 0xff, buffer[2] & 0xff);
        Assert.assertEquals(dos1 & 0xff, buffer[3] & 0xff);
        Assert.assertEquals((dos1 >>> 8) & 0xff, buffer[4] & 0xff);
        Assert.assertEquals(dos2 & 0xff, buffer[5] & 0xff);
        Assert.assertEquals((dos2 >>> 8) & 0xff, buffer[6] & 0xff);
        Assert.assertEquals(dos3 & 0xff, buffer[7] & 0xff);
        Assert.assertEquals((dos3 >>> 8) & 0xff, buffer[8] & 0xff);
    }

    @Test
    public void test_write_bigBuffer() {
        final byte[] buffer = new byte[10];
        final float[] dos = {1.14f, 0.14f, 1.85f, 1.5f};
        final byte[] hc = {1, 0, 2, 3};

        final BiAllelicHardCallsAndDosages vr = new BiAllelicHardCallsAndDosages(hc, dos);
        final int bytesWritten = vr.write(buffer, 0, buffer.length);

        Assert.assertEquals(-9, bytesWritten);
        Assert.assertEquals(0xe1, buffer[0] & 0xff);

        final int dos0 = Math.round(dos[0] * 16_384);
        final int dos1 = Math.round(dos[1] * 16_384);
        final int dos2 = Math.round(dos[2] * 16_384);
        final int dos3 = Math.round(dos[3] * 16_384);

        Assert.assertEquals(dos0 & 0xff, buffer[1] & 0xff);
        Assert.assertEquals((dos0 >>> 8) & 0xff, buffer[2] & 0xff);
        Assert.assertEquals(dos1 & 0xff, buffer[3] & 0xff);
        Assert.assertEquals((dos1 >>> 8) & 0xff, buffer[4] & 0xff);
        Assert.assertEquals(dos2 & 0xff, buffer[5] & 0xff);
        Assert.assertEquals((dos2 >>> 8) & 0xff, buffer[6] & 0xff);
        Assert.assertEquals(dos3 & 0xff, buffer[7] & 0xff);
        Assert.assertEquals((dos3 >>> 8) & 0xff, buffer[8] & 0xff);
    }
}
