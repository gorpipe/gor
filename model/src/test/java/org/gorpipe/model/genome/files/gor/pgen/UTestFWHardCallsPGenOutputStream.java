package org.gorpipe.model.genome.files.gor.pgen;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestFWHardCallsPGenOutputStream {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void test_getStorageModeByte() throws IOException {
        final FWHardCallsPGenOutputStream os = new FWHardCallsPGenOutputStream(tf.newFile("test.pgen").getAbsolutePath());
        Assert.assertEquals(0x02, os.getStorageModeByte());
    }

    @Test
    public void test_write() throws IOException {
        final File file = tf.newFile("testWrite.pgen");
        final FWHardCallsPGenOutputStream os = new FWHardCallsPGenOutputStream(file.getAbsolutePath());
        os.write(new BiAllelicHardCalls(new byte[]{0, 1, 2, 3}));
        os.close();

        Assert.assertEquals(13, file.length());
        final byte[] buffer = new byte[13];
        final FileInputStream fis = new FileInputStream(file);
        Assert.assertEquals(buffer.length, fis.read(buffer));

        final byte[] wanted = {0x6c, 0x1b, 0x02, 1, 0, 0, 0, 4, 0, 0, 0, 0, (byte) 0xe4};
        Assert.assertArrayEquals(wanted, buffer);
    }

    @Test
    public void test_writeEmpty() throws IOException {
        final Path path = tf.getRoot().toPath().resolve("empty.pgen");
        final FWHardCallsPGenOutputStream os = new FWHardCallsPGenOutputStream(path.toString());
        os.close();

        Assert.assertFalse(Files.exists(path));
    }
}
