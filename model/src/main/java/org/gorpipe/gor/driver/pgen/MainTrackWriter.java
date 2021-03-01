/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.gor.driver.pgen;

class MainTrackWriter extends DataTrackWriter {
    private final byte[] hc;
    private int idx = 0;

    MainTrackWriter(byte[] hc) {
        this.hc = hc;
    }

    @Override
    int write(byte[] buffer, int offset, int len) {
        final int bUpTo = offset + len;
        final int iUpTo = this.hc.length - 3;
        int bufferIdx = offset;
        while (bufferIdx < bUpTo && this.idx < iUpTo) {
            buffer[bufferIdx++] = (byte) (this.hc[this.idx] | (this.hc[this.idx + 1] << 2) | (this.hc[this.idx + 2] << 4) | (this.hc[this.idx + 3] << 6));
            this.idx += 4;
        }

        if (this.idx < this.hc.length) {
            if (bufferIdx < bUpTo) {
                switch (this.hc.length & 3) {
                    case 1:
                        buffer[bufferIdx++] = this.hc[this.idx];
                        this.idx += 1;
                        break;
                    case 2:
                        buffer[bufferIdx++] = (byte) (this.hc[this.idx] | (this.hc[this.idx + 1] << 2));
                        this.idx += 2;
                        break;
                    default:
                        buffer[bufferIdx++] = (byte) (this.hc[this.idx] | (this.hc[this.idx + 1] << 2) | (this.hc[this.idx + 2] << 4));
                        this.idx += 3;
                        break;
                }
            } else return bufferIdx - offset;
        }
        return offset - bufferIdx;
    }

    @Override
    boolean done() {
        return this.idx == this.hc.length;
    }
}
