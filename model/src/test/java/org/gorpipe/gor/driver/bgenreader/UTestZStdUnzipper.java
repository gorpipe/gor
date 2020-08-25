package org.gorpipe.gor.driver.bgenreader;

import com.github.luben.zstd.ZstdOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UTestZStdUnzipper {

    @Test
    public void test_basic() throws IOException {
        final byte[] unzipped1 = "miglangarihadegismatvonandiereitthvadgottimatinn".getBytes();
        final ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        final ZstdOutputStream def1 = new ZstdOutputStream(baos1);
        def1.write(unzipped1);
        def1.flush();
        final byte[] zipped1 = baos1.toByteArray();

        final ZStdUnzipper unzipper = new ZStdUnzipper();

        unzipper.setInput(zipped1, 0, zipped1.length);
        final byte[] actualUnzipped1 = new byte[unzipped1.length];
        final int len1 = unzipper.unzip(actualUnzipped1, 0, actualUnzipped1.length);

        Assert.assertEquals(actualUnzipped1.length, len1);
        Assert.assertArrayEquals(unzipped1, actualUnzipped1);

        unzipper.reset();

        final byte[] unzipped2 = "maturinnvarmjogfinn".getBytes();
        final ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        final ZstdOutputStream def2 = new ZstdOutputStream(baos2);
        def2.write(unzipped2);
        def2.flush();
        final byte[] zipped2 = baos2.toByteArray();

        unzipper.setInput(zipped2, 0, zipped2.length);
        final byte[] actualUnzipped2 = new byte[unzipped2.length];
        final int len2 = unzipper.unzip(actualUnzipped2, 0, actualUnzipped2.length);

        Assert.assertEquals(actualUnzipped2.length, len2);
        Assert.assertArrayEquals(unzipped2, actualUnzipped2);
    }
}
