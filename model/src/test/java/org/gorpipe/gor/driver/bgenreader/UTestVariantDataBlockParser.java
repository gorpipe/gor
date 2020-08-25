package org.gorpipe.gor.driver.bgenreader;

import org.gorpipe.gor.model.Row;
import org.junit.Assert;
import org.junit.Test;

public class UTestVariantDataBlockParser {

    @Test
    public void test_writeCharToBuffer() {
        final VariantDataBlockParser vdbp = new VariantDataBlockParser(0, null, CompressionType.NONE) {
            @Override
            Row parse(byte[] array, int offset, int len) {
                throw new UnsupportedOperationException();
            }
        };

        final int len = vdbp.buffer.length;
        vdbp.writeCharToBuffer(vdbp.buffer.length, '0');
        Assert.assertEquals('0', vdbp.buffer[len]);
    }

    @Test
    public void test_writeIntToBuffer() {
        final VariantDataBlockParser vdbp = new VariantDataBlockParser(0, null, CompressionType.NONE) {
            @Override
            Row parse(byte[] array, int offset, int len) {
                throw new UnsupportedOperationException();
            }
        };
        vdbp.writeIntToBuffer(100, 0);
        Assert.assertEquals('1', vdbp.buffer[0]);
        Assert.assertEquals('0', vdbp.buffer[1]);
        Assert.assertEquals('0', vdbp.buffer[2]);

        vdbp.writeIntToBuffer(0, 0);
        Assert.assertEquals('0', vdbp.buffer[0]);
    }

    @Test
    public void test_writeRawToBuffer() {
        final VariantDataBlockParser vdbp = new VariantDataBlockParser(0, null, CompressionType.NONE) {
            @Override
            Row parse(byte[] array, int offset, int len) {
                throw new UnsupportedOperationException();
            }
        };
        final byte[] in = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        vdbp.writeRawToBuffer(in, 0, 0, 10);
        Assert.assertEquals(1, vdbp.buffer[1]);
        Assert.assertEquals(2, vdbp.buffer[2]);
        Assert.assertEquals(3, vdbp.buffer[3]);
        Assert.assertEquals(4, vdbp.buffer[4]);
        Assert.assertEquals(5, vdbp.buffer[5]);
        Assert.assertEquals(6, vdbp.buffer[6]);
        Assert.assertEquals(7, vdbp.buffer[7]);
        Assert.assertEquals(8, vdbp.buffer[8]);
        Assert.assertEquals(9, vdbp.buffer[9]);

        vdbp.writeRawToBuffer(in, 2, 0, 3);
        Assert.assertEquals(2, vdbp.buffer[0]);
        Assert.assertEquals(3, vdbp.buffer[1]);
        Assert.assertEquals(4, vdbp.buffer[2]);
    }

    @Test
    public void test_writeChrToBuffer() {
        final VariantDataBlockParser vdbp = new VariantDataBlockParser(0, null, CompressionType.NONE) {
            @Override
            Row parse(byte[] array, int offset, int len) {
                throw new UnsupportedOperationException();
            }
        };
        final byte[] chr1 = {'c', 'h', 'r', '1'};
        final byte[] chr01 = {'c', 'h', 'r', '0', '1'};
        final byte[] chr0123 = {'0', '1', '2', '3'};

        final int offset1 = vdbp.writeChr(chr1, 0, 0, chr1.length);
        Assert.assertEquals(4, offset1);
        Assert.assertEquals('c', vdbp.buffer[0]);
        Assert.assertEquals('h', vdbp.buffer[1]);
        Assert.assertEquals('r', vdbp.buffer[2]);
        Assert.assertEquals('1', vdbp.buffer[3]);

        final int offset2 = vdbp.writeChr(chr01, 0, offset1, chr01.length);
        Assert.assertEquals(8, offset2);
        Assert.assertEquals('c', vdbp.buffer[offset1]);
        Assert.assertEquals('h', vdbp.buffer[offset1 + 1]);
        Assert.assertEquals('r', vdbp.buffer[offset1 + 2]);
        Assert.assertEquals('1', vdbp.buffer[offset1 + 3]);

        final int offset3 = vdbp.writeChr(chr0123, 0, offset2, chr0123.length);
        Assert.assertEquals(14, offset3);
        Assert.assertEquals('c', vdbp.buffer[offset2]);
        Assert.assertEquals('h', vdbp.buffer[offset2 + 1]);
        Assert.assertEquals('r', vdbp.buffer[offset2 + 2]);
        Assert.assertEquals('1', vdbp.buffer[offset2 + 3]);
        Assert.assertEquals('2', vdbp.buffer[offset2 + 4]);
        Assert.assertEquals('3', vdbp.buffer[offset2 + 5]);
    }
}
