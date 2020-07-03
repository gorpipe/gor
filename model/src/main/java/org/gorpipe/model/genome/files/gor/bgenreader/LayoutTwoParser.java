package org.gorpipe.model.genome.files.gor.bgenreader;

import org.gorpipe.model.genome.files.gor.BitStream;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.genome.files.gor.RowBase;

import static org.gorpipe.model.genome.files.gor.bgenreader.Utils.*;

public class LayoutTwoParser extends VariantDataBlockParser {
    private byte[] uncompressed;

    LayoutTwoParser(int numberOfSamples, Unzipper unzipper, CompressionType compressionType) {
        super(numberOfSamples, unzipper, compressionType);
    }

    @Override
    Row parse(byte[] in, int inOffset, int len) {
        int outOffset = 0;

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

        final int numberOfAlleles = parseUnsignedShort(in, inOffset);
        inOffset += 2;
        if (numberOfAlleles != 2) {
            throw new IllegalArgumentException("Biallelic markers are only supported.");
        }

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

        final int lenLeft = (int) parseUnsignedInt(in, inOffset);
        inOffset += 4;
        if (inOffset + lenLeft != len) {
            throw new IllegalArgumentException();
        }

        int uncompressedIdx;
        if (this.compressionType == CompressionType.NONE) {
            this.uncompressed = in;
            uncompressedIdx = inOffset;
        } else {
            final int uncompressedLen = (int) parseUnsignedInt(in, inOffset);
            inOffset += 4;
            this.uncompressed = ensureCapacity(this.uncompressed, uncompressedLen);

            unzip(in, inOffset, lenLeft - 4, this.uncompressed, 0, uncompressedLen);
            uncompressedIdx = 0;
        }

        final int nsInBlock = (int) parseUnsignedInt(this.uncompressed, uncompressedIdx);
        uncompressedIdx += 4;

        if (nsInBlock != this.numberOfSamples) {
            throw new IllegalArgumentException();
        }

        final int alleleCount = parseUnsignedShort(this.uncompressed, uncompressedIdx);
        uncompressedIdx += 2;
        if (alleleCount != 2) {
            throw new IllegalArgumentException("Biallelic markers are only supported. Number of alleles: " + alleleCount);
        }

        final byte minPloidy = this.uncompressed[uncompressedIdx++];
        final byte maxPloidy = this.uncompressed[uncompressedIdx++];
        if (minPloidy != 2 || maxPloidy != 2) {
            throw new IllegalArgumentException("Ploidy must equal 2");
        }
        int ploidyIdx = uncompressedIdx;
        uncompressedIdx += this.numberOfSamples;
        final byte phased = this.uncompressed[uncompressedIdx++];
        if (phased != 0) {
            throw new IllegalArgumentException("Unphased genotypes are only supported.");
        }
        final byte bitsPerProb = this.uncompressed[uncompressedIdx++];
        if (bitsPerProb < 1 || bitsPerProb > 32) {
            throw new IllegalArgumentException("The number of bits used to store each probability must lie between" +
                    "1 and 32 inclusive. Current value: " + bitsPerProb);
        }

        final float scale = 1.0f / ((1L << bitsPerProb) - 1);
        final BitStream bs = new BitStream(bitsPerProb, this.uncompressed, uncompressedIdx);
        this.buffer = ensureCapacity(this.buffer, outOffset + 2 * this.numberOfSamples);
        for (int i = 0; i < this.numberOfSamples; ++i, outOffset += 2, ++ploidyIdx) {
            final byte ploidyAndMissingnessByte = this.uncompressed[ploidyIdx];
            final int ploidy = ploidyAndMissingnessByte & 0x3f;
            final boolean existing = ((ploidyAndMissingnessByte & 0x80) >>> 7) == 0;
            if (ploidy != 2) {
                throw new IllegalArgumentException("Ploidy must equal 2");
            }
            if (existing) {
                final float p0 = bs.next() * scale;
                final float p1 = bs.next() * scale;
                final float p2 = 1.0f - p0 - p1;
                this.buffer[outOffset] = (byte) Math.round((1.0 - p1) * 93 + 33);
                this.buffer[outOffset + 1] = (byte) Math.round((1.0 - p2) * 93 + 33);
            } else {
                this.buffer[outOffset] = ' ';
                this.buffer[outOffset + 1] = ' ';
            }
        }
        return new RowBase(new String(this.buffer, 0, outOffset));
    }
}
