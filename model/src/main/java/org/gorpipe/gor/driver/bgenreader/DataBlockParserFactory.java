package org.gorpipe.gor.driver.bgenreader;

class DataBlockParserFactory {

    static VariantDataBlockParser getParser(HeaderInfo headerInfo) {
        if (headerInfo.compressionType == CompressionType.ZSTD && headerInfo.layoutType == LayoutType.LAYOUT_ONE) {
            throw new IllegalArgumentException();
        }

        final Unzipper unzipper;
        switch (headerInfo.compressionType) {
            case NONE: {
                unzipper = null;
                break;
            }
            case ZLIB: {
                unzipper = new ZLibUnzipper();
                break;
            }
            case ZSTD: {
                unzipper = new ZStdUnzipper();
                break;
            }
            default: throw new IllegalStateException();
        }

        switch (headerInfo.layoutType) {
            case LAYOUT_ONE: return new LayoutOneParser(headerInfo.numberOfSamples, unzipper, headerInfo.compressionType);
            case LAYOUT_TWO: return new LayoutTwoParser(headerInfo.numberOfSamples, unzipper, headerInfo.compressionType);
            default: throw new IllegalStateException();
        }
    }
}
