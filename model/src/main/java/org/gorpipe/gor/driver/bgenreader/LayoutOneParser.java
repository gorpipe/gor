package org.gorpipe.gor.driver.bgenreader;

import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;

import static org.gorpipe.gor.driver.bgenreader.Utils.*;

public class LayoutOneParser extends VariantDataBlockParser {
    private byte[] probBuffer;

    LayoutOneParser(int numberOfSamples, Unzipper unzipper, CompressionType compressionType) {
        super(numberOfSamples, unzipper, compressionType);
        if (compressionType == CompressionType.NONE) {
            this.probBuffer = null;
        } else {
            this.probBuffer = new byte[2 * numberOfSamples];
        }
    }

    @Override
    Row parse(byte[] in, int inOffset, int len) {
        int outOffset = 0;
        final int nsInRow = (int) parseUnsignedInt(in, inOffset);
        inOffset += 4;
        if (nsInRow != this.numberOfSamples) {
            throw new IllegalArgumentException();
        }

        final int varIdLen = parseUnsignedShort(in, inOffset);
        inOffset += 2;
        final int varIdOffset = inOffset;
        inOffset += varIdLen;

        final int rsIdLen = parseUnsignedShort(in, inOffset);
        inOffset += 2;
        final int rsIdOffset = inOffset;
        inOffset += rsIdLen;

        final int chrLen = parseUnsignedShort(in, inOffset);
        inOffset += 2;
        outOffset = this.writeChr(in, inOffset, outOffset, chrLen);
        inOffset += chrLen;
        outOffset = this.writeTabToBuffer(outOffset);

        final long pos = parseUnsignedInt(in, inOffset);
        inOffset += 4;
        outOffset = this.writeIntToBuffer(pos, outOffset);
        outOffset = this.writeTabToBuffer(outOffset);

        final int refAlleleLen = (int) parseUnsignedInt(in, inOffset);
        inOffset += 4;
        outOffset = this.writeRawToBuffer(in, inOffset, outOffset, refAlleleLen);
        inOffset += refAlleleLen;
        outOffset = this.writeTabToBuffer(outOffset);

        final int altAlleleLen = (int) parseUnsignedInt(in, inOffset);
        inOffset += 4;
        outOffset = this.writeRawToBuffer(in, inOffset, outOffset, altAlleleLen);
        inOffset += altAlleleLen;
        outOffset = this.writeTabToBuffer(outOffset);

        outOffset = this.writeRawToBuffer(in, rsIdOffset, outOffset, rsIdLen);
        outOffset = this.writeTabToBuffer(outOffset);
        outOffset = this.writeRawToBuffer(in, varIdOffset, outOffset, varIdLen);
        outOffset = this.writeTabToBuffer(outOffset);

        final int probOffset;
        if (this.compressionType == CompressionType.NONE) {
            if (len - inOffset != 6 * numberOfSamples) {
                throw new IllegalArgumentException();
            }
            this.probBuffer = in;
            probOffset = inOffset;
        } else {
            final int lenLeft = (int) parseUnsignedInt(in, inOffset);
            inOffset += 4;
            if (lenLeft + inOffset != len) {
                throw new IllegalArgumentException();
            }
            this.probBuffer = ensureCapacity(this.probBuffer, 6 * this.numberOfSamples);
            unzip(in, inOffset, lenLeft, this.probBuffer, 0, 6 * this.numberOfSamples);
            probOffset = 0;
        }

        this.buffer = ensureCapacity(this.buffer,outOffset + 2 * this.numberOfSamples);
        final float scale = 1.0f / 32_768f;
        for (int i = 0; i < this.numberOfSamples; ++i, outOffset += 2) {
            final float p0 = parseUnsignedShort(this.probBuffer, probOffset + 6 * i) * scale;
            final float p1 = parseUnsignedShort(this.probBuffer, probOffset + 6 * i + 2) * scale;
            final float p2 = parseUnsignedShort(this.probBuffer, probOffset + 6 * i + 4) * scale;
            if (p0 == 0.0f && p1 == 0.0f && p2 == 0.0f) {
                this.buffer[outOffset] = ' ';
                this.buffer[outOffset + 1] = ' ';
            } else {
                this.buffer[outOffset] = (byte) Math.round((1 - p1) * 93 + 33);
                this.buffer[outOffset + 1] = (byte) Math.round((1 - p2) * 93 + 33);
            }
        }
        return new RowBase(new String(this.buffer, 0, outOffset));
    }
}
