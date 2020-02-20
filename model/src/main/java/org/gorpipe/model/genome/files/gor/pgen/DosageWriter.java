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

package org.gorpipe.model.genome.files.gor.pgen;

class DosageWriter extends DataTrackWriter {
    private final float[] dosages;
    private int idx = 0;

    DosageWriter(float[] dosages) {
        this.dosages = dosages;
    }

    @Override
    int write(byte[] buffer, int offset, int len) {
        final int upTo = offset + len - 1;

        int bufferIdx = offset;
        int dos;
        while (bufferIdx < upTo && this.idx < this.dosages.length) {
            dos = Math.round(this.dosages[this.idx++] * 16_384);
            buffer[bufferIdx] = (byte) (dos & 0xff);
            buffer[bufferIdx + 1] = (byte) ((dos >>> 8) & 0xff);
            bufferIdx += 2;
        }
        if (this.idx < this.dosages.length) return bufferIdx - offset;
        else return offset - bufferIdx;
    }

    @Override
    boolean done() {
        return this.idx == this.dosages.length;
    }
}
