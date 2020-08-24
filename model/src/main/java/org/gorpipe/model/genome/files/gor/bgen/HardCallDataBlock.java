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

import java.util.Arrays;

class HardCallDataBlock extends VariantDataBlock {
    int gtBitsPerSample;
    int[] gt1, gt2;

    HardCallDataBlock() {
        this.bitsPerProb = 1;
    }

    void setVariables(CharSequence chr, int pos, CharSequence rsId, CharSequence varId, boolean[] existing, int[] gt1, int[] gt2, CharSequence... alleles) {
        super.setVariables(chr, pos, rsId, varId, existing, alleles);
        this.gt1 = gt1;
        this.gt2 = gt2;
        this.numberOfSamples = this.gt1.length;
        this.gtBitsPerSample = alleles.length * (alleles.length + 1) / 2 - 1;
        final int probLenInBits = this.gtBitsPerSample * this.numberOfSamples;
        this.probLenInBytes = (probLenInBits & 7) == 0 ? probLenInBits >>> 3 : ((probLenInBits >>> 3) + 1);
        this.uncompressedLen = this.probLenInBytes + this.numberOfSamples + 10;
    }

    @Override
    protected void fillProbs() {
        if (this.alleles.length == 2) {
            fillBiAllelic(this.uncompressed, this.numberOfSamples + 10, this.gt1, this.gt2, this.numberOfSamples);
        } else {
            fillMultiAllelic(this.uncompressed,this.numberOfSamples + 10, this.gt1, this.gt2, this.numberOfSamples, this.gtBitsPerSample);
        }
    }

    static void fillBiAllelic(byte[] buffer, int offset, int[] gt1, int[] gt2, int len) {
        int bufferIdx = offset;
        int i = 0;
        final int upTo = len - 3;
        while (i < upTo) {
            buffer[bufferIdx++] = (byte) (getBiPr(gt1[i], gt2[i]) | (getBiPr(gt1[i + 1], gt2[i + 1]) << 2)
                    | (getBiPr(gt1[i + 2], gt2[i + 2]) << 4) | (getBiPr(gt1[i + 3], gt2[i + 3]) << 6));
            i += 4;
        }
        switch (len & 3) {
            case 0: break;
            case 1: {
                buffer[bufferIdx] = getBiPr(gt1[i], gt2[i]);
                break;
            }
            case 2: {
                buffer[bufferIdx] = (byte) (getBiPr(gt1[i], gt2[i]) | (getBiPr(gt1[i + 1], gt2[i + 1]) << 2));
                break;
            }
            default: {
                buffer[bufferIdx] = (byte) (getBiPr(gt1[i], gt2[i]) | (getBiPr(gt1[i + 1], gt2[i + 1]) << 2) | (getBiPr(gt1[i + 2], gt2[i + 2]) << 4));
                break;
            }
        }
    }

    static byte getBiPr(int gt1, int gt2) {
        if (gt1 == -1 || gt1 == 1) return 0;
        if (gt2 == 0) return 1;
        else return 2;
    }

    static void fillMultiAllelic(byte[] buffer, int offset, int[] gt1, int[] gt2, int len, int gtBitsPerSample) {
        Arrays.fill(buffer, offset, buffer.length, (byte) 0);
        for (int i = 0; i < len; ++i) {
            final int coLexIdx = getCoLexIdx(gt1[i], gt2[i]);
            if (coLexIdx != gtBitsPerSample) {
                final int bitIdx = gtBitsPerSample * i + coLexIdx;
                buffer[offset + (bitIdx >>> 3)] |= (1 << (bitIdx & 7));
            }
        }
    }

    static int getCoLexIdx(int gt1, int gt2) {
        if (gt2 == 0) return 0;
        return gt1 + gt2 * (gt2 + 1) / 2;
    }
}
