package org.gorpipe.model.genome.files.gor.bgenreader;

import static org.gorpipe.model.genome.files.gor.bgenreader.CompressionType.*;
import static org.gorpipe.model.genome.files.gor.bgenreader.LayoutType.*;
import static org.gorpipe.model.genome.files.gor.bgenreader.Utils.parseUnsignedInt;

class HeaderBlockParser {

    static HeaderInfo parse(byte[] array, int offset, int len) {
        final int variantDataBlockCount = (int) parseUnsignedInt(array, offset);
        final int numberOfSamples = (int) parseUnsignedInt(array, offset + 4);
        //Skip magic bytes and free data area
        final int flagOffset = offset + len - 4;
        final byte compressionByte = getCompressionBits(array[flagOffset]);
        final CompressionType ct = getCompressionType(compressionByte);
        final byte layoutByte = getLayoutBits(array[flagOffset]);
        final LayoutType lt = getLayoutType(layoutByte);
        final boolean hasSampleIds = getHasSampleIds(array[flagOffset + 3]);
        return new HeaderInfo(variantDataBlockCount, numberOfSamples, lt, ct, hasSampleIds);
    }

    protected static byte getLayoutBits(byte b) {
        return (byte) ((b & 0x3c) >>> 2);
    }

    protected static byte getCompressionBits(byte b) {
        return (byte) (b & 0x3);
    }

    protected static boolean getHasSampleIds(byte b) {
        return ((b & 0x80) >>> 7) == 1;
    }

    protected static CompressionType getCompressionType(byte compressionByte) {
        /* Use when we upgrade to java 11.
        return switch (compressionByte) {
            case 0 -> NONE;
            case 1 -> ZLIB;
            case 2 -> ZSTD;
            default -> throw new IllegalArgumentException("Unknown compression type: " + compressionByte + ".");
        };
        */
        switch (compressionByte) {
            case 0: return NONE;
            case 1: return ZLIB;
            case 2: return ZSTD;
            default: throw new IllegalArgumentException("Unknown compression type: " + compressionByte + ".");
        }
    }

    protected static LayoutType getLayoutType(byte ltByte) {
        /* Use when we upgrade to java 11.
        return switch (ltByte) {
            case 0 -> throw new IllegalArgumentException("Value 0 is unsupported.");
            case 1 -> LAYOUT_ONE;
            case 2 -> LAYOUT_TWO;
            default -> throw new IllegalArgumentException("Unknown value: " + ltByte + ".");
        }; */
        switch (ltByte) {
            case 0: throw new IllegalArgumentException("Value 0 is unsupported.");
            case 1: return LAYOUT_ONE;
            case 2: return LAYOUT_TWO;
            default: throw new IllegalArgumentException("Unknown value: " + ltByte + ".");
        }
    }
}
