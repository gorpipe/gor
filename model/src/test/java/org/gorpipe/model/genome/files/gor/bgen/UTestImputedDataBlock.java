package org.gorpipe.model.genome.files.gor.bgen;

import org.gorpipe.util.collection.ByteArray;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static org.gorpipe.model.genome.files.gor.bgen.ImputedDataBlock.fillProbs;
import static org.gorpipe.model.genome.files.gor.bgen.ImputedDataBlock.writeProbs;

public class UTestImputedDataBlock {

    @Test
    public void test_write_smallSteps() throws DataFormatException {
        final boolean[] flags = {true, true, true, false};
        final float[][] pr = {{1f, 0f, 0f}, {0f, 1f, 0f}, {0f, 0f, 1f}, {0f, 0f, 0f}};
        final ImputedDataBlock db = new ImputedDataBlock();
        db.setVariables("chr1", 1, null, null, "A", "C", flags, pr);

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
        Assert.assertEquals(8, uncompressed[13]);
        Assert.assertEquals(255, uncompressed[14] & 0xff);
        Assert.assertEquals(0, uncompressed[15]);
        Assert.assertEquals(0, uncompressed[16]);
        Assert.assertEquals(255, uncompressed[17] & 0xff);
        Assert.assertEquals(0, uncompressed[18]);
        Assert.assertEquals(0, uncompressed[19]);
        Assert.assertEquals(0, uncompressed[20]);
        Assert.assertEquals(0, uncompressed[21]);
    }

    @Test
    public void test_setVariables() {
        final ImputedDataBlock db = new ImputedDataBlock();
        final boolean[] flags = new boolean[0];
        final float[][] prob = new float[0][0];
        db.setVariables("chr1", 1, null, null, "A", "C", flags, prob);
        Assert.assertEquals("chr1", db.chr);
        Assert.assertEquals(1, db.pos);
        Assert.assertEquals("chr1:1:A:C", db.rsId);
        Assert.assertEquals("chr1:1:A:C", db.varId);
        Assert.assertEquals(0, db.numberOfSamples);
        Assert.assertArrayEquals(new String[] {"A", "C"}, db.alleles);
        Assert.assertEquals(8, db.bitsPerProb);
        Assert.assertSame(flags, db.existing);
        Assert.assertSame(prob, db.prob);
        Assert.assertEquals(0, db.probLenInBytes);
        Assert.assertEquals(10, db.uncompressedLen);
    }

    @Test
    public void test_writeProbs() {
        final byte[] buffer = new byte[2];

        writeProbs(buffer, new float[] {1f, 0f, 0f}, 0);
        Assert.assertEquals(255, buffer[0] & 0xff);
        Assert.assertEquals(0, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0f, 1f, 0f}, 0);
        Assert.assertEquals(0, buffer[0] & 0xff);
        Assert.assertEquals(255, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0f, 0f, 1f}, 0);
        Assert.assertEquals(0, buffer[0] & 0xff);
        Assert.assertEquals(0, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0.8f, 0.1f, 0.1f}, 0);
        Assert.assertEquals(204, buffer[0] & 0xff);
        Assert.assertEquals(25, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0.1f, 0.8f, 0.1f}, 0);
        Assert.assertEquals(25, buffer[0] & 0xff);
        Assert.assertEquals(204, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0.1f, 0.1f, 0.8f}, 0);
        Assert.assertEquals(25, buffer[0] & 0xff);
        Assert.assertEquals(25, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0.9f/255, 0.8f/255, 253.3f/255}, 0);
        Assert.assertEquals(1, buffer[0] & 0xff);
        Assert.assertEquals(1, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0.9f/255, 253.3f/255, 0.8f/255}, 0);
        Assert.assertEquals(1, buffer[0] & 0xff);
        Assert.assertEquals(253, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {253.3f/255, 0.9f/255, 0.8f/255}, 0);
        Assert.assertEquals(253, buffer[0] & 0xff);
        Assert.assertEquals(1, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {253.3f/255, 0.8f/255, 0.9f/255}, 0);
        Assert.assertEquals(253, buffer[0] & 0xff);
        Assert.assertEquals(1, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0.8f/255, 0.9f/255, 253.3f/255}, 0);
        Assert.assertEquals(1, buffer[0] & 0xff);
        Assert.assertEquals(1, buffer[1] & 0xff);

        writeProbs(buffer, new float[] {0.8f/255, 253.3f/255, 0.9f/255}, 0);
        Assert.assertEquals(1, buffer[0] & 0xff);
        Assert.assertEquals(253, buffer[1] & 0xff);
    }

    @Test
    public void test_fillProbs() {
        final byte[] buffer = new byte[2];
        fillProbs(buffer, 0, new boolean[] {false}, new float[][]{{0.8f/255, 0.9f/255, 253.3f/255}});
        Assert.assertEquals(0, buffer[0] & 0xff);
        Assert.assertEquals(0, buffer[1] & 0xff);

        fillProbs(buffer, 0, new boolean[] {true}, new float[][]{{0.8f/255, 0.9f/255, 253.3f/255}});
        Assert.assertEquals(1, buffer[0] & 0xff);
        Assert.assertEquals(1, buffer[1] & 0xff);
    }
}
