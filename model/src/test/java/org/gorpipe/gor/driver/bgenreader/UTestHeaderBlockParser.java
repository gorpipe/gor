package org.gorpipe.gor.driver.bgenreader;

import org.junit.Assert;
import org.junit.Test;

import static org.gorpipe.gor.driver.bgenreader.HeaderBlockParser.*;

public class UTestHeaderBlockParser {

    @Test
    public void test_parse() {
        final byte[] header = {2, 0, 0, 0, 1, 0, 0, 0, 'b', 'g', 'e', 'n', 'f', 'r', 'e', 'e', 'd', 'a', 't', 'a', 'a', 'r', 'e', 'a', 5, 0, 0, -128};
        final HeaderInfo hi = HeaderBlockParser.parse(header, 0, header.length);
        Assert.assertEquals(1, hi.numberOfSamples);
        Assert.assertEquals(2, hi.variantDataBlockCount);
        Assert.assertSame(CompressionType.ZLIB, hi.compressionType);
        Assert.assertSame(LayoutType.LAYOUT_ONE, hi.layoutType);
        Assert.assertTrue(hi.hasSampleIdentifiers);
    }

    @Test
    public void test_getLayoutBits() {
        final byte b1 = 0b00000100;
        Assert.assertEquals(1, getLayoutBits(b1));

        final byte b2 = 0b00001000;
        Assert.assertEquals(2, getLayoutBits(b2));

        final byte b3 = 0b00111100;
        Assert.assertEquals(0b1111, getLayoutBits(b3));

        final byte b4 = (byte) 0b11111111;
        Assert.assertEquals(0b1111, getLayoutBits(b4));
    }

    @Test
    public void test_getCompressionBits() {
        final byte b1 = 0b00000000;
        Assert.assertEquals(0, getCompressionBits(b1));

        final byte b2 = 0b00000001;
        Assert.assertEquals(1, getCompressionBits(b2));

        final byte b3 = 0b00000010;
        Assert.assertEquals(2, getCompressionBits(b3));

        final byte b4 = 0b01111110;
        Assert.assertEquals(2, getCompressionBits(b4));
    }

    @Test
    public void test_getCompressionType() {
        final byte b1 = 0;
        final byte b2 = 1;
        final byte b3 = 2;
        final byte b4 = 3;

        Assert.assertSame(CompressionType.NONE, getCompressionType(b1));
        Assert.assertSame(CompressionType.ZLIB, getCompressionType(b2));
        Assert.assertSame(CompressionType.ZSTD, getCompressionType(b3));

        boolean success = false;
        try {
            getCompressionType(b4);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void test_getLayoutType() {
        final byte b1 = 1;
        final byte b2 = 2;
        final byte b3 = 0;
        final byte b4 = 3;

        Assert.assertSame(LayoutType.LAYOUT_ONE, getLayoutType(b1));
        Assert.assertSame(LayoutType.LAYOUT_TWO, getLayoutType(b2));

        boolean success1 = false;
        try {
            getLayoutType(b3);
        } catch (IllegalArgumentException e) {
            success1 = true;
        }
        Assert.assertTrue(success1);

        boolean success2 = false;
        try {
            getLayoutType(b4);
        } catch (IllegalArgumentException e) {
            success2 = true;
        }
        Assert.assertTrue(success2);
    }
}
