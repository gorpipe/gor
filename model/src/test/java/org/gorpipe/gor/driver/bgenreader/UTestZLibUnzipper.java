package org.gorpipe.gor.driver.bgenreader;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;

public class UTestZLibUnzipper {

    @Test
    public void test_basic() throws IOException, DataFormatException {
        final byte[] unzipped1 = "miglangarihadegismatvonandiereitthvadgottimatinn".getBytes();
        final ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        final DeflaterOutputStream def1 = new DeflaterOutputStream(baos1);
        def1.write(unzipped1);
        def1.finish();
        final byte[] zipped1 = baos1.toByteArray();

        final ZLibUnzipper unzipper = new ZLibUnzipper();

        unzipper.setInput(zipped1, 0, zipped1.length);
        final byte[] actualUnzipped1 = new byte[unzipped1.length];
        final int len1 = unzipper.unzip(actualUnzipped1, 0, actualUnzipped1.length);

        Assert.assertEquals(actualUnzipped1.length, len1);
        Assert.assertArrayEquals(unzipped1, actualUnzipped1);
        Assert.assertEquals(0, unzipper.unzip(actualUnzipped1, 0, actualUnzipped1.length));

        unzipper.reset();

        final byte[] unzipped2 = "maturinnvarmjogfinn".getBytes();
        final ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        final DeflaterOutputStream def2 = new DeflaterOutputStream(baos2);
        def2.write(unzipped2);
        def2.finish();
        final byte[] zipped2 = baos2.toByteArray();

        unzipper.setInput(zipped2, 0, zipped2.length);
        final byte[] actualUnzipped2 = new byte[unzipped2.length];
        final int len2 = unzipper.unzip(actualUnzipped2, 0, actualUnzipped2.length);

        Assert.assertEquals(actualUnzipped2.length, len2);
        Assert.assertArrayEquals(unzipped2, actualUnzipped2);
        Assert.assertEquals(0, unzipper.unzip(actualUnzipped2, 0, actualUnzipped2.length));
    }
}
