package org.gorpipe.model.genome.files.gor.bgen;

import org.gorpipe.util.collection.ByteArray;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static org.gorpipe.model.genome.files.gor.bgen.HardCallDataBlock.*;

public class UTestHardCallDataBlock {

    @Test
    public void test_write_smallSteps() throws DataFormatException {
        final boolean[] flags = {true, true, true, false};
        final int[] gt1 = {0, 0, 1, -1};
        final int[] gt2 = {0, 1, 1, -1};
        final HardCallDataBlock db = new HardCallDataBlock();
        db.setVariables("chr1", 1, null, null, flags, gt1, gt2, "A", "C");

        final byte[] buffer = new byte[100]; //Just big enough.
        int idx = 0;
        int written;
        while ((written = db.write(buffer, idx, 1)) > 0) {
            idx += written;
        }
        idx -= written;

        Assert.assertEquals(0, db.write(buffer, 0, buffer.length));

        Assert.assertEquals(10, ByteArray.readShort(buffer, 0, ByteOrder.LITTLE_ENDIAN));
        for (int i = 0; i < 10; ++i) {
            Assert.assertEquals(db.varId.charAt(i), buffer[i + 2]);
        }
        Assert.assertEquals(10, ByteArray.readShort(buffer, 12, ByteOrder.LITTLE_ENDIAN));
        for (int i = 0; i < 10; ++i) {
            Assert.assertEquals(db.rsId.charAt(i), buffer[i + 14]);
        }
        Assert.assertEquals(4, ByteArray.readShort(buffer, 24, ByteOrder.LITTLE_ENDIAN));
        for (int i = 0; i < 4; ++i) {
            Assert.assertEquals(db.chr.charAt(i), buffer[i + 26]);
        }
        Assert.assertEquals(1, ByteArray.readInt(buffer, 30, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals(2, ByteArray.readShort(buffer, 34, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals(1, ByteArray.readInt(buffer, 36, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals('A', buffer[40]);
        Assert.assertEquals(1, ByteArray.readInt(buffer, 41, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals('C', buffer[45]);
        Assert.assertEquals(idx - 50, ByteArray.readInt(buffer, 46, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals(db.uncompressedLen, ByteArray.readInt(buffer, 50, ByteOrder.LITTLE_ENDIAN));

        final byte[] uncompressed = new byte[db.uncompressedLen];
        final Inflater inf = new Inflater();
        inf.setInput(buffer, 54, buffer.length - 54);
        inf.inflate(uncompressed);
        Assert.assertArrayEquals(db.uncompressed, uncompressed);

        Assert.assertEquals(4, ByteArray.readInt(uncompressed, 0, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals(2, ByteArray.readShort(uncompressed, 4, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals(2, uncompressed[6]);
        Assert.assertEquals(2, uncompressed[7]);
        Assert.assertEquals(2, uncompressed[8]);
        Assert.assertEquals(2, uncompressed[9]);
        Assert.assertEquals(2, uncompressed[10]);
        Assert.assertEquals(-126, uncompressed[11]);
        Assert.assertEquals(0, uncompressed[12]);
        Assert.assertEquals(1, uncompressed[13]);
        Assert.assertEquals(9, uncompressed[14]);
    }

    @Test
    public void test_setVariables() {
        final HardCallDataBlock db = new HardCallDataBlock();
        final boolean[] flags = new boolean[0];
        final int[] gt1 = new int[0];
        final int[] gt2 = new int[1];
        db.setVariables("chr1", 1, null, null, flags, gt1, gt2, "A", "C");
        Assert.assertEquals("chr1", db.chr);
        Assert.assertEquals(1, db.pos);
        Assert.assertEquals("chr1:1:A:C", db.rsId);
        Assert.assertEquals("chr1:1:A:C", db.varId);
        Assert.assertEquals(0, db.numberOfSamples);
        Assert.assertArrayEquals(new String[] {"A", "C"}, db.alleles);
        Assert.assertEquals(1, db.bitsPerProb);
        Assert.assertEquals(2, db.gtBitsPerSample);
        Assert.assertSame(flags, db.existing);
        Assert.assertSame(gt1, db.gt1);
        Assert.assertSame(gt2, db.gt2);
        Assert.assertEquals(0, db.probLenInBytes);
        Assert.assertEquals(10, db.uncompressedLen);
    }

    @Test
    public void test_getBiPr() {
        Assert.assertEquals(1, getBiPr(0, 0));
        Assert.assertEquals(2, getBiPr(0, 1));
        Assert.assertEquals(0, getBiPr(1, 1));
        Assert.assertEquals(0, getBiPr(-1, -1));
    }

    @Test
    public void test_fillBiAllelic_empty() {
        fillBiAllelic(new byte[0], 0, new int[0], new int[0], 0); //This should be fine.
    }

    @Test
    public void test_fillBiAllelic_1() {
        final byte[] buffer = new byte[1];
        final int[] gt1 = {0};
        final int[] gt2 = {0};
        fillBiAllelic(buffer, 0, gt1, gt2, 1);
        Assert.assertEquals(0x01, buffer[0]);
    }

    @Test
    public void test_fillBiAllelic_2() {
        final byte[] buffer = new byte[1];
        final int[] gt1 = {0, 0};
        final int[] gt2 = {0, 1};
        fillBiAllelic(buffer, 0, gt1, gt2, 2);
        Assert.assertEquals(0x09, buffer[0]);
    }

    @Test
    public void test_fillBiAllelic_3() {
        final byte[] buffer = new byte[1];
        final int[] gt1 = {0, 0, 1};
        final int[] gt2 = {0, 1, 1};
        fillBiAllelic(buffer, 0, gt1, gt2, 3);
        Assert.assertEquals(0x09, buffer[0]);
    }

    @Test
    public void test_fillBiAllelic_4() {
        final byte[] buffer = new byte[1];
        final int[] gt1 = {0, 0, 1, -1};
        final int[] gt2 = {0, 1, 1, -1};
        fillBiAllelic(buffer, 0, gt1, gt2, 4);
        Assert.assertEquals(0x09, buffer[0]);
    }

    @Test
    public void test_getCoLexIdx() {
        Assert.assertEquals(0, getCoLexIdx(0, 0));
        Assert.assertEquals(1, getCoLexIdx(0, 1));
        Assert.assertEquals(2, getCoLexIdx(1, 1));
        Assert.assertEquals(3, getCoLexIdx(0, 2));
        Assert.assertEquals(4, getCoLexIdx(1, 2));
        Assert.assertEquals(5, getCoLexIdx(2, 2));
    }

    @Test
    public void test_fillMultiAllelic_empty() {
        fillMultiAllelic(new byte[0], 0, new int[0], new int[0], 0, 117); //This should be fine.
    }

    @Test
    public void test_fillMultiAllelic_1_3() {
        final byte[] buffer = new byte[1];
        fillMultiAllelic(buffer, 0, new int[] {0}, new int[] {0}, 1, 5);
        Assert.assertEquals(0x01, buffer[0] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {0}, new int[] {1}, 1, 5);
        Assert.assertEquals(0x02, buffer[0] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {1}, new int[] {1}, 1, 5);
        Assert.assertEquals(0x04, buffer[0] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {0}, new int[] {2}, 1, 5);
        Assert.assertEquals(0x08, buffer[0] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {1}, new int[] {2}, 1, 5);
        Assert.assertEquals(0x10, buffer[0] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {2}, new int[] {2}, 1, 5);
        Assert.assertEquals(0x00, buffer[0] & 0xff);
    }

    @Test
    public void test_fillMultiAllelic_2_3() {
        final byte[] buffer = new byte[2];
        fillMultiAllelic(buffer, 0, new int[] {0, 0}, new int[] {0, 0}, 2, 5);
        Assert.assertEquals(0x21, buffer[0] & 0xff);
        Assert.assertEquals(0x00, buffer[1] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {0, 0}, new int[] {0, 1}, 2, 5);
        Assert.assertEquals(0x41, buffer[0] & 0xff);
        Assert.assertEquals(0x00, buffer[1] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {0, 1}, new int[] {0, 1}, 2, 5);
        Assert.assertEquals(0x81, buffer[0] & 0xff);
        Assert.assertEquals(0x00, buffer[1] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {0, 0}, new int[] {0, 2}, 2, 5);
        Assert.assertEquals(0x01, buffer[0] & 0xff);
        Assert.assertEquals(0x01, buffer[1] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {0, 1}, new int[] {0, 2}, 2, 5);
        Assert.assertEquals(0x01, buffer[0] & 0xff);
        Assert.assertEquals(0x02, buffer[1] & 0xff);

        fillMultiAllelic(buffer, 0, new int[] {0, 2}, new int[] {0, 2}, 2, 5);
        Assert.assertEquals(0x01, buffer[0] & 0xff);
        Assert.assertEquals(0x00, buffer[1] & 0xff);
    }
}
