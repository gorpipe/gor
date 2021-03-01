package org.gorpipe.gor.driver.pgen;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestFWUnPhasedPGenOutputStream {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void test_getStorageModeByte() throws IOException  {
        final FWUnPhasedPGenOutputStream os = new FWUnPhasedPGenOutputStream(tf.newFile("test.pgen").getAbsolutePath());
        Assert.assertEquals(3, os.getStorageModeByte());
    }

    @Test
    public void test_write() throws IOException {
        final File file = tf.newFile("testWrite.pgen");
        final FWUnPhasedPGenOutputStream os = new FWUnPhasedPGenOutputStream(file.getAbsolutePath());
        final byte[] hc = {0, 1, 2, 3};
        final float[] dos = {0, 1, 2, 0};
        os.write(new BiAllelicHardCallsAndDosages(hc, dos));
        os.close();

        final byte[] buffer = new byte[(int) file.length()];
        final FileInputStream is = new FileInputStream(file);
        Assert.assertEquals(buffer.length, is.read(buffer));

        final byte[] wanted = {0x6c, 0x1b, 0x03, 1, 0, 0, 0, 4, 0, 0, 0, 0, (byte) 0xe4, 0, 0, 0, 0x40, 0, (byte) 0x80, 0, 0};
        Assert.assertArrayEquals(wanted, buffer);
    }

    @Test
    public void test_writeEmpty() throws IOException {
        final Path path = tf.getRoot().toPath().resolve("empty.pgen");
        final FWUnPhasedPGenOutputStream os = new FWUnPhasedPGenOutputStream(path.toString());
        os.close();

        Assert.assertFalse(Files.exists(path));
    }
}
