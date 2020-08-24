package org.gorpipe.model.genome.files.gor.bgenreader;

import org.junit.Assert;
import org.junit.Test;

import static org.gorpipe.model.genome.files.gor.bgenreader.CompressionType.*;
import static org.gorpipe.model.genome.files.gor.bgenreader.LayoutType.LAYOUT_ONE;
import static org.gorpipe.model.genome.files.gor.bgenreader.LayoutType.LAYOUT_TWO;

public class UTestDataBlockParserFactory {

    @Test
    public void test_basic() {
        final HeaderInfo hi0 = new HeaderInfo(0, 1, LAYOUT_ONE, NONE, false);
        final VariantDataBlockParser vdbp0 = DataBlockParserFactory.getParser(hi0);
        Assert.assertSame(NONE, vdbp0.compressionType);
        Assert.assertTrue(vdbp0 instanceof LayoutOneParser);
        Assert.assertEquals(1, vdbp0.numberOfSamples);
        Assert.assertNull(vdbp0.unzipper);

        final HeaderInfo hi1 = new HeaderInfo(0, 1, LAYOUT_ONE, ZLIB, false);
        final VariantDataBlockParser vdbp1 = DataBlockParserFactory.getParser(hi1);
        Assert.assertSame(ZLIB, vdbp1.compressionType);
        Assert.assertTrue(vdbp1 instanceof LayoutOneParser);
        Assert.assertEquals(1, vdbp1.numberOfSamples);
        Assert.assertTrue(vdbp1.unzipper instanceof ZLibUnzipper);

        final HeaderInfo hi2 = new HeaderInfo(0, 1, LAYOUT_TWO, NONE, false);
        final VariantDataBlockParser vdbp2 = DataBlockParserFactory.getParser(hi2);
        Assert.assertSame(NONE, vdbp2.compressionType);
        Assert.assertTrue(vdbp2 instanceof LayoutTwoParser);
        Assert.assertEquals(1, vdbp1.numberOfSamples);
        Assert.assertNull(vdbp2.unzipper);

        final HeaderInfo hi3 = new HeaderInfo(0, 1, LAYOUT_TWO, ZLIB, false);
        final VariantDataBlockParser vdbp3 = DataBlockParserFactory.getParser(hi3);
        Assert.assertSame(ZLIB, vdbp3.compressionType);
        Assert.assertTrue(vdbp3 instanceof LayoutTwoParser);
        Assert.assertEquals(1, vdbp1.numberOfSamples);
        Assert.assertTrue(vdbp3.unzipper instanceof ZLibUnzipper);

        final HeaderInfo hi4 = new HeaderInfo(0, 1, LAYOUT_TWO, ZSTD, false);
        final VariantDataBlockParser vdbp4 = DataBlockParserFactory.getParser(hi4);
        Assert.assertSame(ZSTD, vdbp4.compressionType);
        Assert.assertTrue(vdbp4 instanceof LayoutTwoParser);
        Assert.assertEquals(1, vdbp4.numberOfSamples);
        Assert.assertTrue(vdbp4.unzipper instanceof ZStdUnzipper);
    }

    @Test
    public void test_illegalArgument() {
        final HeaderInfo hi = new HeaderInfo(0, 1, LAYOUT_ONE, ZSTD, false);
        boolean success = false;
        try {
            DataBlockParserFactory.getParser(hi);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }
}
