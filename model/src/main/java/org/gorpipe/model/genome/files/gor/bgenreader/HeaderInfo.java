package org.gorpipe.model.genome.files.gor.bgenreader;

class HeaderInfo {
    int variantDataBlockCount;
    int numberOfSamples;
    LayoutType layoutType;
    CompressionType compressionType;
    boolean hasSampleIdentifiers;

    HeaderInfo(int vdbCount, int ns, LayoutType lt, CompressionType ct, boolean hsi) {
        this.variantDataBlockCount = vdbCount;
        this.numberOfSamples = ns;
        this.layoutType = lt;
        this.compressionType = ct;
        this.hasSampleIdentifiers = hsi;
    }
}

enum LayoutType {
    LAYOUT_ONE,
    LAYOUT_TWO
}

enum CompressionType {
    NONE,
    ZLIB,
    ZSTD
}
