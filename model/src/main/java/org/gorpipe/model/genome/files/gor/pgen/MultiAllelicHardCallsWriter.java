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

import static org.gorpipe.model.genome.files.gor.pgen.BitUtilities.*;

class MultiAllelicHardCallsWriter extends DataTrackWriter {
    private final int[] gt1, gt2;
    private final byte[] hc;
    private boolean[] flags1, flags2, homFlags;
    private boolean doneWritingFirstByte = false;
    private int flag1Count = 0, flag2Count = 0;
    private int[] cat1AltAll, cat2RefAltAlls;
    private int flag1Idx = 0, flag2Idx = 0, cat1Idx = 0, cat2Idx = 0, homFlagsIdx = 0;
    private final int numberOfAltAlleles;
    private boolean done = false;

    MultiAllelicHardCallsWriter(int[] gt1, int[] gt2, byte[] hc, int numberOfAltAlleles) {
        this.gt1 = gt1;
        this.gt2 = gt2;
        this.hc = hc;
        this.numberOfAltAlleles = numberOfAltAlleles;
        compute();
    }

    @Override
    int write(byte[] buffer, int offset, int len) {
        final int upTo = offset + len;
        int bufferIdx = offset;

        if (bufferIdx < upTo && !this.doneWritingFirstByte) {
            buffer[bufferIdx++] = getFirstByte(this.flag1Count, this.flag2Count);
            this.doneWritingFirstByte = true;
        }

        if (this.flag1Count != 0) {
            bufferIdx += writeFlags1(buffer, bufferIdx, upTo);
            if (this.flag1Idx < this.flags1.length) return bufferIdx - offset;

            if (this.numberOfAltAlleles > 2) {
                bufferIdx += writeCat1(buffer, bufferIdx, upTo);
                if (this.cat1Idx < this.cat1AltAll.length) return bufferIdx - offset;
            }
        }

        if (this.flag2Count != 0) {
            bufferIdx += writeFlags2(buffer, bufferIdx, upTo);
            if (this.flag2Idx < this.flags2.length) return bufferIdx - offset;

            if(this.numberOfAltAlleles == 2) {
                bufferIdx += writeHomFlags(buffer, bufferIdx, upTo);
                if (this.homFlagsIdx < this.homFlags.length) return bufferIdx - offset;
            } else if (this.numberOfAltAlleles > 2) {
                bufferIdx += writeCat2(buffer, bufferIdx, upTo);
                if (this.cat2Idx < this.cat2RefAltAlls.length) return bufferIdx - offset;
            }
        }
        this.done = true;
        return offset - bufferIdx;
    }

    @Override
    boolean done() {
        return this.done;
    }

    private byte getFirstByte(int count1, int count2) {
        byte toReturn = 0;
        if (count1 == 0) toReturn |= 0x0f;
        if (count2 == 0) toReturn |= 0xf0;
        return toReturn;
    }

    int count(byte[] buffer, byte byteToCount) {
        int count = 0;
        for (int i = 0; i < buffer.length; ++i) {
            if (buffer[i] == byteToCount) ++count;
        }
        return count;
    }

    private void compute() {
        setFlags();
        setPatchArrays();
    }

    private void setPatchArrays() {
        if (this.numberOfAltAlleles == 2) {
            setHomFlags();
        } else if (this.numberOfAltAlleles > 2) {
            setCatArrays();
        }
    }

    private void setCatArrays() {
        setCat1Array();
        setCat2Array();
    }

    private void setCat1Array() {
        this.cat1AltAll = new int[this.flag1Count];
        int flagIdx = 0;
        int catIdx = 0;

        for (int i = 0; i < this.hc.length && flagIdx < this.flags1.length; ++i) {
            if (this.hc[i] == 1 && this.flags1[flagIdx++]) {
                this.cat1AltAll[catIdx++] = this.gt2[i] - 2;
            }
        }
    }
    
    private void setCat2Array() {
        this.cat2RefAltAlls = new int[2 * this.flag2Count];
        int flagIdx = 0;
        int catIdx = 0;

        for (int i = 0; i < this.hc.length && flagIdx < this.flags2.length; ++i) {
            if (this.hc[i] == 2 && this.flags2[flagIdx++]) {
                this.cat2RefAltAlls[2 * catIdx] = this.gt1[i] - 1;
                this.cat2RefAltAlls[2 * catIdx + 1] = this.gt2[i] - 1;
                ++catIdx;
            }
        }
    }

    private void setHomFlags() {
        this.homFlags = new boolean[this.flag2Count];
        int homIdx = 0;
        int flagIdx = 0;
        for (int i = 0; i < this.hc.length && flagIdx < this.flags2.length; ++i) {
            if (this.hc[i] == 2 && this.flags2[flagIdx++]) {
                this.homFlags[homIdx++] = this.gt1[i] == 2;
            }
        }
    }

    private void setFlags() {
        final int count1 = count(this.hc, (byte) 1);
        final int count2 = count(this.hc, (byte) 2);

        this.flags1 = new boolean[count1];
        this.flags2 = new boolean[count2];

        int localFlagIdx1 = 0;
        int localFlagIdx2 = 0;

        for (int i = 0; i < this.hc.length; ++i) {
            if (this.hc[i] == 1 && (this.flags1[localFlagIdx1++] = this.gt2[i] > 1)) ++this.flag1Count;
            else if (this.hc[i] == 2 && (this.flags2[localFlagIdx2++] = this.gt2[i] > 1)) ++this.flag2Count;
        }
    }

    private int writeHomFlags(byte[] buffer, int offset, int upTo) {
        final int bytesWritten = writeBoolArray(buffer, offset, upTo, this.homFlags, this.homFlagsIdx);
        final int booleansWritten = bytesWritten << 3;
        if (booleansWritten + this.homFlagsIdx > this.homFlags.length) this.homFlagsIdx = this.homFlags.length;
        else this.homFlagsIdx += bytesWritten;
        return bytesWritten;
    }

    private int writeFlags1(byte[] buffer, int offset, int upTo) {
        final int bytesWritten = writeBoolArray(buffer, offset, upTo, this.flags1, this.flag1Idx);
        final int booleansWritten = bytesWritten << 3;
        if (booleansWritten + this.flag1Idx > this.flags1.length) this.flag1Idx = this.flags1.length;
        else this.flag1Idx += booleansWritten;
        return bytesWritten;
    }

    private int writeFlags2(byte[] buffer, int offset, int upTo) {
        final int bytesWritten = writeBoolArray(buffer, offset, upTo, this.flags2, this.flag2Idx);
        final int booleansWritten = bytesWritten << 3;
        if (booleansWritten + this.flag2Idx > this.flags2.length) this.flag2Idx = this.flags2.length;
        else this.flag2Idx += booleansWritten;
        return bytesWritten;
    }

    private int writeCat1(byte[] buffer, int offset, int upTo) {
        int bytesWritten;
        if (this.numberOfAltAlleles == 3) {
            bytesWritten = write1Bit(buffer, offset, upTo, this.cat1AltAll, this.cat1Idx);
            final int numbersWritten = 8 * bytesWritten;
            if (numbersWritten + this.cat1Idx > this.cat1AltAll.length) this.cat1Idx = this.cat1AltAll.length;
            else this.cat1Idx += numbersWritten;
        } else if (this.numberOfAltAlleles < 6) {
            bytesWritten = write2Bits(buffer, offset, upTo, this.cat1AltAll, this.cat1Idx);
            final int numbersWritten = 4 * bytesWritten;
            if (numbersWritten + this.cat1Idx > this.cat1AltAll.length) this.cat1Idx = this.cat1AltAll.length;
            else this.cat1Idx += numbersWritten;
        } else if (this.numberOfAltAlleles < 18) {
            bytesWritten = write4Bits(buffer, offset, upTo, this.cat1AltAll, this.cat1Idx);
            final int numbersWritten = 2 * bytesWritten;
            if (numbersWritten + this.cat1Idx > this.cat1AltAll.length) this.cat1Idx = this.cat1AltAll.length;
            else this.cat1Idx += numbersWritten;
        } else if (this.numberOfAltAlleles < 258) {
            bytesWritten = write8Bits(buffer, offset, upTo, this.cat1AltAll, this.cat1Idx);
            this.cat1Idx += bytesWritten;
        } else if (this.numberOfAltAlleles < 65538) {
            bytesWritten = write16Bits(buffer, offset, upTo, this.cat1AltAll, this.cat1Idx);
            final int numbersWritten = bytesWritten / 2;
            this.cat1Idx += numbersWritten;
        } else {
            bytesWritten = write24Bits(buffer, offset, upTo, this.cat1AltAll, this.cat1Idx);
            final int numbersWritten = bytesWritten / 3;
            this.cat1Idx += numbersWritten;
        }
        return bytesWritten;
    }

    private int writeCat2(byte[] buffer, int offset, int upTo) {
        int bytesWritten;
        if (this.numberOfAltAlleles < 5) {
            bytesWritten = write2Bits(buffer, offset, upTo, this.cat2RefAltAlls, this.cat2Idx);
            final int numbersWritten = 4 * bytesWritten;
            if (numbersWritten + this.cat2Idx > this.cat2RefAltAlls.length) this.cat2Idx = this.cat2RefAltAlls.length;
            else this.cat2Idx += numbersWritten;
        } else if (this.numberOfAltAlleles < 17) {
            bytesWritten = write4Bits(buffer, offset, upTo, this.cat2RefAltAlls, this.cat2Idx);
            final int numbersWritten = 2 * bytesWritten;
            if (numbersWritten + this.cat2Idx > this.cat2RefAltAlls.length) this.cat2Idx = this.cat2RefAltAlls.length;
            else this.cat2Idx += numbersWritten;
        } else if (this.numberOfAltAlleles < 257) {
            bytesWritten = write8Bits(buffer, offset, upTo, this.cat2RefAltAlls, this.cat2Idx);
            this.cat2Idx += bytesWritten;
        } else if (this.numberOfAltAlleles < 65537) {
            bytesWritten = write16Bits(buffer, offset, upTo, this.cat2RefAltAlls, this.cat2Idx);
            final int numbersWritten = bytesWritten / 2;
            this.cat2Idx += numbersWritten;
        } else {
            bytesWritten = write24Bits(buffer, offset, upTo, this.cat2RefAltAlls, this.cat2Idx);
            final int numbersWritten = bytesWritten / 3;
            this.cat2Idx += numbersWritten;
        }
        return bytesWritten;
    }
}
