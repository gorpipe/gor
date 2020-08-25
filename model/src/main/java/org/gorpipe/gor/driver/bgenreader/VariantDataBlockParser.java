package org.gorpipe.gor.driver.bgenreader;

import org.gorpipe.gor.model.Row;

import java.io.IOException;
import java.util.zip.DataFormatException;

import static org.gorpipe.gor.driver.bgenreader.Utils.ensureCapacity;

abstract class VariantDataBlockParser {
    protected byte[] buffer;
    protected final int numberOfSamples;
    protected final Unzipper unzipper;
    protected final CompressionType compressionType;

    protected VariantDataBlockParser(int numberOfSamples, Unzipper unzipper, CompressionType compressionType) {
        this.buffer = new byte[1024];
        this.numberOfSamples = numberOfSamples;
        this.unzipper = unzipper;
        this.compressionType = compressionType;
    }

    abstract Row parse(byte[] array, int offset, int len);

    protected int writeChr(byte[] in, int inOffset, int outOffset, int chrLen) {
        int idx = chrLen > 3 && (in[inOffset] == 'c' && in[inOffset + 1] == 'h' && in[inOffset + 2] == 'r') ? 3 : 0;
        while (idx < chrLen && in[inOffset + idx] == '0') ++idx;
        final int written = chrLen - idx + 3;
        final int newLen = outOffset + written;
        this.buffer = ensureCapacity(this.buffer, newLen);
        this.buffer[outOffset] = 'c';
        this.buffer[outOffset + 1] = 'h';
        this.buffer[outOffset + 2] = 'r';
        final int newInOffset = inOffset + idx;
        final int newOutOffset = outOffset + 3;
        final int upTo = chrLen - idx;
        for (idx = 0; idx < upTo; ++idx) {
            this.buffer[newOutOffset + idx] = in[newInOffset + idx];
        }
        return newLen;
    }

    protected int writeRawToBuffer(byte[] in, int inOffset, int outOffset, int len) {
        final int newLen = outOffset + len;
        this.buffer = ensureCapacity(this.buffer, newLen);
        System.arraycopy(in, inOffset, this.buffer, outOffset, len);
        return newLen;
    }

    protected int writeIntToBuffer(long toWrite, int outOffset) {
        int len = 0;
        long toWriteC = toWrite;
        do {
            toWriteC /= 10;
            len++;
        } while (toWriteC != 0);
        final int newLen = outOffset + len;
        this.buffer = ensureCapacity(this.buffer, newLen);
        int idx = newLen - 1;
        toWriteC = toWrite;
        do {
            final long div10 = toWriteC / 10;
            this.buffer[idx--] = (byte) (toWriteC - div10 * 10 + '0');
            toWriteC = div10;
        } while (toWriteC != 0);
        return newLen;
    }

    protected int writeCharToBuffer(int offset, char c) {
        final int newLen = offset + 1;
        this.buffer = ensureCapacity(this.buffer, newLen);
        this.buffer[offset] = (byte) c;
        return newLen;
    }

    protected int writeTabToBuffer(int offset) {
        return writeCharToBuffer(offset, '\t');
    }

    protected void unzip(byte[] in, int inOffset, int inLen, byte[] out, int outOffset, int outLen) {
        try {
            this.unzipper.setInput(in, inOffset, inLen);
            int total = 0;
            int unzipped;
            while ((unzipped = this.unzipper.unzip(out, outOffset + total, outLen - total)) > 0) {
                total += unzipped;
            }
            if (total != outLen) {
                throw new IllegalArgumentException();
            }
            this.unzipper.reset();
        } catch (IOException | DataFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
