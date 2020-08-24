package org.gorpipe.model.genome.files.gor.pgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestMultiAllelicHardCallsWriter {

    @Test
    public void test_emptyArguments() {
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(new int[0], new int[0], new byte[0], 2);
        final byte[] buffer = new byte[0];
        final int bytesWritten = vr.write(buffer, 0, 0);
        Assert.assertEquals(0, bytesWritten);
    }

    @Test
    public void test_callWriteWithEmptyBuffer() {
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(new int[1], new int[1], new byte[1], 2);
        final byte[] buffer = new byte[0];
        final int bytesWritten = vr.write(buffer, 0, 0);
        Assert.assertEquals(0, bytesWritten);
    }

    @Test
    public void test_write_2Alts() {
        final int[] gt1 = {0, 1, 2, 0, 1, 0, -1};
        final int[] gt2 = {0, 1, 2, 1, 2, 2, -1};
        final byte[] hc = {0, 2, 2, 1, 2, 1, 3};
        final byte[] buffer = new byte[4];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 2);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-4, written);
        Assert.assertEquals(0x00, buffer[0] & 0xff);
        Assert.assertEquals(0x02, buffer[1] & 0xff);
        Assert.assertEquals(0x06, buffer[2] & 0xff);
        Assert.assertEquals(0x01, buffer[3] & 0xff);
    }

    @Test
    public void test_write_3Alts() {
        final int[] gt1 = {0, 1, 2, 3, 0, 1, 2, 0, 1, 0, -1};
        final int[] gt2 = {0, 1, 2, 3, 1, 2, 3, 2, 3, 3, -1};
        final byte[] hc = {0, 2, 2, 2, 1, 2, 2, 1, 2, 1, 3};
        final byte[] buffer = new byte[7];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 3);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-7, written);
        Assert.assertEquals(0x00, buffer[0] & 0xff); //Storage mode
        Assert.assertEquals(0x06, buffer[1] & 0xff); //Cat 1 flags
        Assert.assertEquals(0x02, buffer[2] & 0xff); //Cat 1 types
        Assert.assertEquals(0x3e, buffer[3] & 0xff); //Cat 2 flags
        Assert.assertEquals(0xa5, buffer[4] & 0xff); //Cat 2 types
        Assert.assertEquals(0x94, buffer[5] & 0xff); //Cat 2 types
        Assert.assertEquals(0x08, buffer[6] & 0xff); //Cat 2 types
    }

    @Test
    public void test_write_3Alts_smallBuffer() {
        final int[] gt1 = {0, 1, 2, 3, 0, 1, 2, 0, 1, 0, -1};
        final int[] gt2 = {0, 1, 2, 3, 1, 2, 3, 2, 3, 3, -1};
        final byte[] hc = {0, 2, 2, 2, 1, 2, 2, 1, 2, 1, 3};
        final byte[] buffer = new byte[1];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 3);

        Assert.assertEquals(1, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0x00, buffer[0] & 0xff); //Storage mode

        Assert.assertEquals(1, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0x06, buffer[0] & 0xff); //Cat 1 flags

        Assert.assertEquals(1, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0x02, buffer[0] & 0xff); //Cat 1 types

        Assert.assertEquals(1, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0x3e, buffer[0] & 0xff); //Cat 2 flags

        Assert.assertEquals(1, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0xa5, buffer[0] & 0xff); //Cat 2 types

        Assert.assertEquals(1, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0x94, buffer[0] & 0xff); //Cat 2 types

        Assert.assertEquals(-1, vr.write(buffer, 0, buffer.length));
        Assert.assertEquals(0x08, buffer[0] & 0xff); //Cat 2 types
    }

    @Test
    public void test_write_5alts() {
        final int[] gt1 = {0, 1, -1};
        final int[] gt2 = {5, 5, -1};
        final byte[] hc = {1, 2, 3};
        final byte[] buffer = new byte[5];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 5);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-5, written);
        Assert.assertEquals(0x00, buffer[0] & 0xff); //Storage mode
        Assert.assertEquals(0x01, buffer[1] & 0xff); //Cat 1 flags
        Assert.assertEquals(0x03, buffer[2] & 0xff); //Cat 1 types
        Assert.assertEquals(0x01, buffer[3] & 0xff); //Cat 2 flags
        Assert.assertEquals(0x40, buffer[4] & 0xff); //Cat 2 types
    }

    @Test
    public void test_write_17alts() {
        final int[] gt1 = {0, 1, -1};
        final int[] gt2 = {15, 12, -1};
        final byte[] hc = {1, 2, 3};
        final byte[] buffer = new byte[6];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 17);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-6, written);
        Assert.assertEquals(0x00, buffer[0] & 0xff); //Storage mode
        Assert.assertEquals(0x01, buffer[1] & 0xff); //Cat 1 flags
        Assert.assertEquals(0x0d, buffer[2] & 0xff); //Cat 1 types
        Assert.assertEquals(0x01, buffer[3] & 0xff); //Cat 2 flags
        Assert.assertEquals(0x00, buffer[4] & 0xff); //Cat 2 types
        Assert.assertEquals(0x0b, buffer[5] & 0xff); //Cat 2 types
    }

    @Test
    public void test_write_257alts() {
        final int[] gt1 = {0, 1, -1};
        final int[] gt2 = {257, 63, -1};
        final byte[] hc = {1, 2, 3};
        final byte[] buffer = new byte[8];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 257);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-8, written);
        Assert.assertEquals(0x00, buffer[0] & 0xff); //Storage mode
        Assert.assertEquals(0x01, buffer[1] & 0xff); //Cat 1 flags
        Assert.assertEquals(0xff, buffer[2] & 0xff); //Cat 1 types
        Assert.assertEquals(0x01, buffer[3] & 0xff); //Cat 2 flags
        Assert.assertEquals(0x00, buffer[4] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[5] & 0xff); //Cat 2 types
        Assert.assertEquals(0x3e, buffer[6] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[7] & 0xff); //Cat 2 types
    }

    @Test
    public void test_write_65537alts() {
        final int[] gt1 = {0, 1, -1};
        final int[] gt2 = {65537, 443, -1};
        final byte[] hc = {1, 2, 3};
        final byte[] buffer = new byte[11];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 65537);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-11, written);
        Assert.assertEquals(0x00, buffer[0] & 0xff); //Storage mode
        Assert.assertEquals(0x01, buffer[1] & 0xff); //Cat 1 flags
        Assert.assertEquals(0xff, buffer[2] & 0xff); //Cat 1 types
        Assert.assertEquals(0xff, buffer[3] & 0xff); //Cat 1 types
        Assert.assertEquals(0x01, buffer[4] & 0xff); //Cat 2 flags
        Assert.assertEquals(0x00, buffer[5] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[6] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[7] & 0xff); //Cat 2 types
        Assert.assertEquals(0xba, buffer[8] & 0xff); //Cat 2 types
        Assert.assertEquals(0x01, buffer[9] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[10] & 0xff); //Cat 2 types
    }

    @Test
    public void test_write_65538alts() {
        final int[] gt1 = {0, 1, -1};
        final int[] gt2 = {65537, 443, -1};
        final byte[] hc = {1, 2, 3};
        final byte[] buffer = new byte[12];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 65538);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-12, written);
        Assert.assertEquals(0x00, buffer[0] & 0xff); //Storage mode
        Assert.assertEquals(0x01, buffer[1] & 0xff); //Cat 1 flags
        Assert.assertEquals(0xff, buffer[2] & 0xff); //Cat 1 types
        Assert.assertEquals(0xff, buffer[3] & 0xff); //Cat 1 types
        Assert.assertEquals(0x00, buffer[4] & 0xff); //Cat 1 types
        Assert.assertEquals(0x01, buffer[5] & 0xff); //Cat 2 flags
        Assert.assertEquals(0x00, buffer[6] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[7] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[8] & 0xff); //Cat 2 types
        Assert.assertEquals(0xba, buffer[9] & 0xff); //Cat 2 types
        Assert.assertEquals(0x01, buffer[10] & 0xff); //Cat 2 types
        Assert.assertEquals(0x00, buffer[11] & 0xff); //Cat 2 types
    }

    @Test
    public void test_write_noFlags() {
        final int[] gt1 = {0, 1, 0, -1};
        final int[] gt2 = {0, 1, 1, -1};
        final byte[] hc = {0, 2, 1, 3};
        final byte[] buffer = new byte[1];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 2);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-1, written);
        Assert.assertEquals(0xff, buffer[0] & 0xff);
    }

    @Test
    public void test_write_noCat1Flags() {
        final int[] gt1 = {0, 1, 0, 1};
        final int[] gt2 = {0, 1, 1, 2};
        final byte[] hc = {0, 2, 1, 2};
        final byte[] buffer = new byte[3];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 2);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-3, written);
        Assert.assertEquals(0x0f, buffer[0] & 0xff);
        Assert.assertEquals(0x02, buffer[1] & 0xff);
        Assert.assertEquals(0x00, buffer[2] & 0xff);
    }

    @Test
    public void test_write_noCat2Flags() {
        final int[] gt1 = {0, 1, 0, 0};
        final int[] gt2 = {0, 1, 1, 2};
        final byte[] hc = {0, 2, 1, 1};
        final byte[] buffer = new byte[2];
        final MultiAllelicHardCallsWriter vr = new MultiAllelicHardCallsWriter(gt1, gt2, hc, 2);
        final int written = vr.write(buffer, 0, buffer.length);
        Assert.assertEquals(-2, written);
        Assert.assertEquals(0xf0, buffer[0] & 0xff);
        Assert.assertEquals(0x02, buffer[1] & 0xff);
    }
}
