/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.model.genome.files.gor.bgen;

import org.gorpipe.util.collection.ByteArray;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.Deflater;

abstract class VariantDataBlock {
    private final Deflater deflater = new Deflater();
    protected byte[] uncompressed;
    protected byte[] compressed = new byte[32 * 1024];
    private int compressedLen;
    private int totalLen;
    protected boolean[] existing;

    protected int numberOfSamples;
    protected int probLenInBytes;
    protected int uncompressedLen;
    protected byte bitsPerProb;

    CharSequence chr;
    int pos;
    CharSequence rsId;
    CharSequence varId;
    CharSequence[] alleles;

    private int varIdLenIdx;
    private int varIdIdx;
    private int rsIdLenIdx;
    private int rsIdIdx;
    private int chrLenIdx;
    private int chrIdx;
    private int varPosIdx;
    private int alleleCountIdx;
    private int allIdx;
    private int allLenIdx;
    private int subAllIdx;
    private int totalLenIdx;
    private int uncompressedLenIdx;
    private int gtIdx;
    private boolean computed;

    void setVariables(CharSequence chr, int pos, CharSequence rsId, CharSequence varId, boolean[] existing, CharSequence... alleles) {
        this.chr = chr;
        this.pos = pos;
        final String id = rsId == null || varId == null ? String.join(":", chr, String.valueOf(pos), String.join(":", alleles)) : null;
        this.rsId = rsId == null ? id : rsId;
        this.varId = varId == null ? id : varId;
        this.existing = existing;
        this.alleles = alleles;
        this.varIdLenIdx = 0;
        this.varIdIdx = 0;
        this.rsIdLenIdx = 0;
        this.rsIdIdx = 0;
        this.chrLenIdx = 0;
        this.chrIdx = 0;
        this.varPosIdx = 0;
        this.alleleCountIdx = 0;
        this.allIdx = 0;
        this.allLenIdx = 0;
        this.subAllIdx = 0;
        this.totalLenIdx = 0;
        this.uncompressedLenIdx = 0;
        this.gtIdx = 0;
        this.computed = false;
    }

    int write(byte[] buffer, int offset, int len) {
        if (!this.computed) {
            compute();
            this.computed = true;
        }

        final int upTo = offset + len;
        int bufferIdx = offset;

        if (bufferIdx == upTo) return len;
        if (varIdLenIdx == 0) {
            buffer[bufferIdx++] = (byte) varId.length();
            varIdLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (varIdLenIdx == 1) {
            buffer[bufferIdx++] = (byte) (varId.length() >>> 8);
            varIdLenIdx++;
        }

        while (varIdIdx < varId.length()) {
            if (bufferIdx == upTo) return len;
            buffer[bufferIdx++] = (byte) varId.charAt(varIdIdx++);
        }

        if (bufferIdx == upTo) return len;
        if (rsIdLenIdx == 0) {
            buffer[bufferIdx++] = (byte) rsId.length();
            rsIdLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (rsIdLenIdx == 1) {
            buffer[bufferIdx++] = (byte) (rsId.length() >>> 8);
            rsIdLenIdx++;
        }

        while (rsIdIdx < rsId.length()) {
            if (bufferIdx == upTo) return len;
            buffer[bufferIdx++] = (byte) rsId.charAt(rsIdIdx++);
        }

        if (bufferIdx == upTo) return len;
        if (chrLenIdx == 0) {
            buffer[bufferIdx++] = (byte) chr.length();
            chrLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (chrLenIdx == 1) {
            buffer[bufferIdx++] = (byte) (chr.length() >>> 8);
            chrLenIdx++;
        }

        while (chrIdx < chr.length()) {
            if (bufferIdx == upTo) return len;
            buffer[bufferIdx++] = (byte) chr.charAt(chrIdx++);
        }

        if (bufferIdx == upTo) return len;
        if (varPosIdx == 0) {
            buffer[bufferIdx++] = (byte) pos;
            varPosIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (varPosIdx == 1) {
            buffer[bufferIdx++] = (byte) (pos >>> 8);
            varPosIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (varPosIdx == 2) {
            buffer[bufferIdx++] = (byte) (pos >>> 16);
            varPosIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (varPosIdx == 3) {
            buffer[bufferIdx++] = (byte) (pos >>> 24);
            varPosIdx++;
        }

        if (bufferIdx == upTo) return len;
        if (alleleCountIdx == 0) {
            buffer[bufferIdx++] = (byte) alleles.length;
            alleleCountIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (alleleCountIdx == 1) {
            buffer[bufferIdx++] = (byte) (alleles.length >>> 8);
            alleleCountIdx++;
        }

        while (allIdx < alleles.length) {
            final CharSequence alli = alleles[allIdx];
            if (bufferIdx == upTo) return len;
            if (allLenIdx == 0) {
                buffer[bufferIdx++] = (byte) alli.length();
                allLenIdx++;
            }
            if (bufferIdx == upTo) return len;
            if (allLenIdx == 1) {
                buffer[bufferIdx++] = (byte) (alli.length() >>> 8);
                allLenIdx++;
            }
            if (bufferIdx == upTo) return len;
            if (allLenIdx == 2) {
                buffer[bufferIdx++] = (byte) (alli.length() >>> 16);
                allLenIdx++;
            }
            if (bufferIdx == upTo) return len;
            if (allLenIdx == 3) {
                buffer[bufferIdx++] = (byte) (alli.length() >>> 24);
                allLenIdx++;
            }

            while (subAllIdx < alli.length()) {
                if (bufferIdx == upTo) return len;
                buffer[bufferIdx++] = (byte) alli.charAt(subAllIdx++);
            }
            allIdx++;
            allLenIdx = 0;
            subAllIdx = 0;
        }

        if (bufferIdx == upTo) return len;
        if (totalLenIdx == 0) {
            buffer[bufferIdx++] = (byte) totalLen;
            totalLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (totalLenIdx == 1) {
            buffer[bufferIdx++] = (byte) (totalLen >>> 8);
            totalLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (totalLenIdx == 2) {
            buffer[bufferIdx++] = (byte) (totalLen >>> 16);
            totalLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (totalLenIdx == 3) {
            buffer[bufferIdx++] = (byte) (totalLen >>> 24);
            totalLenIdx++;
        }

        if (bufferIdx == upTo) return len;
        if (uncompressedLenIdx == 0) {
            buffer[bufferIdx++] = (byte) this.uncompressedLen;
            uncompressedLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (uncompressedLenIdx == 1) {
            buffer[bufferIdx++] = (byte) (this.uncompressedLen >>> 8);
            uncompressedLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (uncompressedLenIdx == 2) {
            buffer[bufferIdx++] = (byte) (this.uncompressedLen >>> 16);
            uncompressedLenIdx++;
        }
        if (bufferIdx == upTo) return len;
        if (uncompressedLenIdx == 3) {
            buffer[bufferIdx++] = (byte) (this.uncompressedLen >>> 24);
            uncompressedLenIdx++;
        }

        if (bufferIdx == upTo) return len;

        if (this.gtIdx < compressedLen) {
            if (compressedLen - gtIdx <= upTo - bufferIdx) {
                System.arraycopy(compressed, this.gtIdx, buffer, bufferIdx, compressedLen - gtIdx);
                bufferIdx += compressedLen - gtIdx;
                this.gtIdx = compressedLen;
                return offset - bufferIdx;
            } else {
                System.arraycopy(compressed, this.gtIdx, buffer, bufferIdx, upTo - bufferIdx);
                this.gtIdx += upTo - bufferIdx;
                return len;
            }
        } else {
            return 0;
        }
    }

    protected abstract void fillProbs();

    private void compress() {
        this.deflater.setInput(this.uncompressed, 0, this.uncompressedLen);
        this.deflater.finish();
        this.compressedLen = 0;
        int written;
        while ((written = this.deflater.deflate(this.compressed, this.compressedLen, this.compressed.length - this.compressedLen)) > 0) {
            this.compressedLen += written;
            if (this.deflater.finished()) break;
            if (this.compressedLen == this.compressed.length) {
                this.compressed = Arrays.copyOf(this.compressed, 2 * this.compressed.length);
            }
        }
        this.deflater.reset();
        this.totalLen = this.compressedLen + 4;
    }

    private void compute() {
        ensureCapacity();
        ByteArray.writeInt(this.uncompressed, 0, ByteOrder.LITTLE_ENDIAN, this.numberOfSamples);
        ByteArray.writeShort(this.uncompressed, 4, ByteOrder.LITTLE_ENDIAN, (short) this.alleles.length);
        this.uncompressed[6] = 2; //Min ploidy.
        this.uncompressed[7] = 2; //Max ploidy.
        for (int i = 0; i < this.numberOfSamples; ++i) {
            this.uncompressed[i + 8] = (byte) (this.existing[i] ? 0x02 : 0x82);
        }
        this.uncompressed[this.numberOfSamples + 8] = 0; //Un-phased data.
        this.uncompressed[this.numberOfSamples + 9] = this.bitsPerProb;
        fillProbs();
        compress();
    }

    private void ensureCapacity() {
        final int requiredLen = 10 + this.numberOfSamples + this.probLenInBytes;
        if (this.uncompressed == null || requiredLen > this.uncompressed.length) {
            this.uncompressed = new byte[requiredLen];
        }
    }
}
