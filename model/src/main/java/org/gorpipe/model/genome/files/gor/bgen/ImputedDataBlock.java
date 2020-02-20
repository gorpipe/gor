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

class ImputedDataBlock extends VariantDataBlock {
    float[][] prob;

    ImputedDataBlock() {
        this.bitsPerProb = 8;
    }

    void setVariables(CharSequence chr, int pos, CharSequence rsId, CharSequence varId, CharSequence ref, CharSequence alt, boolean[] existing, float[][] prob) {
        super.setVariables(chr, pos, rsId, varId, existing, ref, alt);
        this.prob = prob;
        this.numberOfSamples = this.prob.length;
        this.uncompressedLen = 3 * this.numberOfSamples + 10;
        this.probLenInBytes = 2 * this.numberOfSamples;
    }

    @Override
    protected void fillProbs() {
        fillProbs(this.uncompressed, this.numberOfSamples + 10, this.existing, this.prob);
    }

    static void fillProbs(byte[] buffer, int offset, boolean[] existing, float[][] prob) {
        for (int i = 0, bufferIdx = offset; i < existing.length; ++i, bufferIdx += 2) {
            if (existing[i]) {
                writeProbs(buffer, prob[i], bufferIdx);
            } else {
                buffer[bufferIdx] = 0;
                buffer[bufferIdx + 1] = 0;
            }
        }
    }

    static void writeProbs(byte[] buffer, float[] pr, int bufferIdx) {
        final float v0 = pr[0] * 255;
        final float v1 = pr[1] * 255;
        final float v2 = pr[2] * 255;
        int i0 = (int) v0;
        int i1 = (int) v1;
        int i2 = (int) v2;
        final int f = 255 - i0 - i1 - i2;
        final float f0 = v0 - i0;
        final float f1 = v1 - i1;
        final float f2 = v2 - i2;
        if (f == 1) {
            if (f0 > f1 && f0 > f2) i0++;
            else if (f1 > f0 && f1 > f2) i1++;
        } else if (f == 2) {
            if (f0 > f1 || f0 > f2) i0++;
            if (f1 > f0 || f1 > f2) i1++;
        }
        buffer[bufferIdx] = (byte) i0;
        buffer[bufferIdx + 1] = (byte) i1;
    }
}
