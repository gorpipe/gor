package org.gorpipe.gor.driver.pgen;

import org.gorpipe.gor.driver.bgenreader.BitStream;
import org.gorpipe.util.collection.ByteArray;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class UTestVariableWidthPGenOutputStream {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void test_writeOneVariant() throws IOException {
        final File file = tf.newFile("writeOne.pgen");
        final VariableWidthPGenOutputStream os = new VariableWidthPGenOutputStream(file.getAbsolutePath());
        os.write(new BiAllelicHardCalls(new byte[]{3, 3, 3, 3}));
        os.close();

        Assert.assertEquals(26, file.length());
        final byte[] buffer = new byte[26];
        final FileInputStream fis = new FileInputStream(file);
        Assert.assertEquals(buffer.length, fis.read(buffer));

        final byte[] wanted = {0x6c, 0x1b, 0x10, 1, 0, 0, 0, 4, 0, 0, 0, 0x07, 0x19, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, (byte) 0xff};
        Assert.assertArrayEquals(wanted, buffer);
    }

    @Test
    public void test_mergeLogic() throws IOException {
        final File file = tf.newFile("many.pgen");
        final VariableWidthPGenOutputStream os = new VariableWidthPGenOutputStream(file.getAbsolutePath());
        final int numberOfVariants = 131_072;
        final byte[][] hc = new byte[numberOfVariants][4];

        final Random r = new Random();
        for (int i = 0; i < hc.length; ++i) {
            final byte[] variant = hc[i];
            variant[0] = (byte) r.nextInt(4);
            variant[1] = (byte) r.nextInt(4);
            variant[2] = (byte) r.nextInt(4);
            variant[3] = (byte) r.nextInt(4);
            os.write(new BiAllelicHardCalls(variant));
        }
        os.close();

        final FileInputStream fis = new FileInputStream(file);
        Assert.assertEquals(12, fis.skip(12));

        final byte[] offsets = new byte[16];
        Assert.assertEquals(offsets.length, fis.read(offsets));
        Assert.assertEquals(file.length() - 2 * 65_536, ByteArray.readUnsignedInt(offsets, 0, ByteOrder.LITTLE_ENDIAN));
        Assert.assertEquals(file.length() - 65_536, ByteArray.readUnsignedInt(offsets, 8, ByteOrder.LITTLE_ENDIAN));

        final byte[] info = new byte[5 * numberOfVariants];
        Assert.assertEquals(info.length, fis.read(info));
        int infoIdx = 0;
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 65_536; ++j) {
                Assert.assertEquals(0, info[infoIdx++]);
            }
            for (int j = 0; j < 65_536; ++j) {
                Assert.assertEquals(1, info[infoIdx]);
                Assert.assertEquals(0, info[infoIdx + 1]);
                Assert.assertEquals(0, info[infoIdx + 2]);
                Assert.assertEquals(0, info[infoIdx + 3]);
                infoIdx += 4;
            }
        }

        final byte[] buffer = new byte[numberOfVariants];
        Assert.assertEquals(buffer.length, fis.read(buffer));
        final BitStream bs = new BitStream(2, buffer, 0);
        for (byte[] variant : hc) {
            Assert.assertEquals(variant[0], bs.next());
            Assert.assertEquals(variant[1], bs.next());
            Assert.assertEquals(variant[2], bs.next());
            Assert.assertEquals(variant[3], bs.next());
        }
        Assert.assertEquals(0, fis.available());
        fis.close();
    }

    @Test
    public void test_writeEmptyFile() throws IOException {
        final Path path = tf.getRoot().toPath().resolve("empty.pgen");
        final VariableWidthPGenOutputStream os = new VariableWidthPGenOutputStream(path.toString());
        os.close();

        Assert.assertFalse(Files.exists(path));
    }
}
