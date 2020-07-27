package org.gorpipe.model.genome.files.gor.bgen;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A simple bgen driver for testing purposes only.
 */
public class SimpleBGenDriver {
    private final RandomAccessFile raf;
    final int offset;
    final int headerLength;
    final int numberOfVariants;
    final int numberOfSamples;
    final int layOut;
    final int compressionType;
    final boolean noSampleIdentifiers;
    int doneReading = 0;


    public SimpleBGenDriver(final String fileName) throws IOException {
        this.raf = new RandomAccessFile(fileName, "r");
        this.offset = readInt(raf);
        final byte[] buffer = new byte[this.offset];
        this.raf.read(buffer);
        this.headerLength = readInt(buffer, 0);
        this.numberOfVariants = readInt(buffer, 4);
        this.numberOfSamples = readInt(buffer, 8);
        this.compressionType = buffer[buffer.length - 4] & 3;
        this.layOut = (buffer[buffer.length - 4] >>> 2) & 3;
        this.noSampleIdentifiers = (buffer[buffer.length - 1] >>> 7) == 0;
    }

    public BGenBlock nextBlock() throws IOException, DataFormatException {
        final BGenBlock block = new BGenBlock();
        block.offsetInFile = this.raf.getFilePointer();
        final int varIdLen = readShort(raf);
        block.varId = new String(getNext(raf, varIdLen));
        final int rsIdLen = readShort(raf);
        block.rsId = new String(getNext(raf, rsIdLen));
        final int chrLen = readShort(raf);
        block.chr = new String(getNext(raf, chrLen));
        block.pos = readInt(raf);
        block.allCount = readShort(raf);
        block.alleles = new String[block.allCount];
        for (int i = 0; i < block.allCount; ++i) {
            final int allLen = readInt(raf);
            block.alleles[i] = new String(getNext(raf, allLen));
        }

        final int compressedLen = readInt(raf) - 4;
        final int unCompressedLen = readInt(raf);
        final byte[] compressed = getNext(raf, compressedLen);
        final byte[] unCompressed = new byte[unCompressedLen];
        final Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        inflater.inflate(unCompressed);
        inflater.end();
        block.probs = unCompressed;
        block.sizeInBytes = raf.getFilePointer() - block.offsetInFile;
        doneReading++;
        return block;
    }

    public boolean hasMore() {
        return doneReading < numberOfVariants;
    }

    public static int readInt(byte[] buffer, int offset) {
        return (buffer[offset] & 0xff)
                | ((buffer[offset + 1] & 0xff) << 8)
                | ((buffer[offset + 2] & 0xff) << 16)
                | ((buffer[offset + 3] & 0xff) << 24);
    }

    public static int readShort(byte[] buffer, int offset) {
        return (buffer[offset] & 0xff) | ((buffer[offset + 1] & 0xff) << 8);
    }

    public static int readInt(RandomAccessFile is) throws IOException {
        final byte[] buffer = new byte[4];
        is.read(buffer);
        return readInt(buffer, 0);
    }

    public static int readShort(RandomAccessFile is) throws IOException {
        final byte[] buffer = new byte[2];
        is.read(buffer);
        return readShort(buffer, 0);
    }

    public static byte[] getNext(RandomAccessFile is, int len) throws IOException {
        final byte[] buffer = new byte[len];
        is.read(buffer);
        return buffer;
    }

    class BGenBlock {
        public String[] alleles;
        String chr;
        int pos;
        String varId;
        String rsId;
        int allCount;
        byte[] probs;
        long offsetInFile;
        long sizeInBytes;
    }
}

