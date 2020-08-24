package org.gorpipe.gor.driver.bgenreader;

import org.junit.Assert;
import org.junit.Test;

import static org.gorpipe.gor.driver.bgenreader.Utils.*;

public class UTestUtils {

    @Test
    public void test_parseUnsignedInt() {
        final byte[] buffer = new byte[4];

        final long n1 = parseUnsignedInt(buffer, 0);
        Assert.assertEquals(0, n1);

        buffer[0] = (byte) 0xff;
        buffer[1] = (byte) 0xff;
        buffer[2] = (byte) 0xff;
        buffer[3] = (byte) 0xff;
        final long n2 = parseUnsignedInt(buffer, 0);
        Assert.assertEquals(0xffffffffL, n2);

        buffer[0] = (byte) 0x01;
        buffer[1] = (byte) 0x01;
        buffer[2] = (byte) 0x01;
        buffer[3] = (byte) 0x01;
        final long n3 = parseUnsignedInt(buffer, 0);
        Assert.assertEquals(0x01010101L, n3);

        buffer[0] = (byte) 0x01;
        buffer[1] = (byte) 0x02;
        buffer[2] = (byte) 0x03;
        buffer[3] = (byte) 0x04;
        final long n4 = parseUnsignedInt(buffer, 0);
        Assert.assertEquals(0x04030201L, n4);
    }

    @Test
    public void test_parseUnsignedShort() {
        final byte[] buffer = new byte[2];

        final int n1 = parseUnsignedShort(buffer, 0);
        Assert.assertEquals(0, n1);

        buffer[0] = (byte) 0xff;
        buffer[1] = (byte) 0xff;
        final int n2 = parseUnsignedShort(buffer, 0);
        Assert.assertEquals(0xffff, n2);

        buffer[0] = (byte) 0x01;
        buffer[1] = (byte) 0x01;
        final int n3 = parseUnsignedShort(buffer, 0);
        Assert.assertEquals(0x0101, n3);

        buffer[0] = (byte) 0x01;
        buffer[1] = (byte) 0x02;
        final int n4 = parseUnsignedShort(buffer, 0);
        Assert.assertEquals(0x0201, n4);
    }

    @Test
    public void test_ensureCapacity() {
        final byte[] buffer1 = ensureCapacity(null, 10);
        Assert.assertEquals(16, buffer1.length);

        final byte[] buffer2 = ensureCapacity(new byte[3], 10);
        Assert.assertEquals(12, buffer2.length);

        final byte[] buffer3 = new byte[10];
        final byte[] buffer4 = ensureCapacity(buffer3, 10);
        Assert.assertSame(buffer3, buffer4);

        final byte[] buffer5 = {1, 0, 0, 0, 2};
        final byte[] buffer6 = ensureCapacity(buffer5, 10);
        final byte[] buffer7 = {1, 0, 0, 0, 2, 0, 0, 0, 0, 0};
        Assert.assertArrayEquals(buffer7, buffer6);

        final byte[] buffer8 = ensureCapacity(new byte[0], 1);
        Assert.assertEquals(1, buffer8.length);
    }
}
