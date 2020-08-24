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

class BitUtilities {

    private BitUtilities() {}

    static int writeBoolArray(byte[] buffer, int bOffset, int bUpTo, boolean[] array, int aOffset) {
        int bIdx = bOffset;
        byte currByte;
        final int arrayIdxUpTo = array.length - 7;
        int aIdx = aOffset;
        while (bIdx < bUpTo && aIdx < arrayIdxUpTo) {
            currByte = 0;
            if (array[aIdx]) currByte |= 0x01;
            if (array[aIdx + 1]) currByte |= 0x02;
            if (array[aIdx + 2]) currByte |= 0x04;
            if (array[aIdx + 3]) currByte |= 0x08;
            if (array[aIdx + 4]) currByte |= 0x10;
            if (array[aIdx + 5]) currByte |= 0x20;
            if (array[aIdx + 6]) currByte |= 0x40;
            if (array[aIdx + 7]) currByte |= 0x80;
            buffer[bIdx++] = currByte;
            aIdx += 8;
        }
        if (bIdx < bUpTo && aIdx < array.length) {
            currByte = 0;
            byte toXorWith = 1;
            do {
                if (array[aIdx++]) currByte |= toXorWith;
                toXorWith <<= 1;
            } while (aIdx < array.length);
            buffer[bIdx++] = currByte;
        }
        return bIdx - bOffset;
    }

    static int write1Bit(byte[] buffer, int bOffset, int bUpTo, int[] array, int aOffset) {
        int bIdx = bOffset;
        final int arrayIdxUpTo = array.length - 7;
        int aIdx = aOffset;
        while (bIdx < bUpTo && aIdx < arrayIdxUpTo) {
            buffer[bIdx++] = (byte) (array[aIdx] | (array[aIdx + 1] << 1) | (array[aIdx + 2] << 2) | (array[aIdx + 3] << 3)
                    | (array[aIdx + 4] << 4) | (array[aIdx + 5] << 5) | (array[aIdx + 6] << 6) | (array[aIdx + 7] << 7));
            aIdx += 8;
        }
        if (bIdx < bUpTo && aIdx < array.length) {
            byte currByte = 0;
            byte toOrWith = 1;
            do {
                if (array[aIdx++] == 1) currByte |= toOrWith;
                toOrWith <<= 1;
            } while (aIdx < array.length);
            buffer[bIdx++] = currByte;
        }
        return bIdx - bOffset;
    }

    static int write2Bits(byte[] buffer, int bOffset, int bUpTo, int[] array, int aOffset) {
        int bIdx = bOffset;
        final int arrayIdxUpTo = array.length - 3;
        int aIdx = aOffset;
        while (bIdx < bUpTo && aIdx < arrayIdxUpTo) {
            buffer[bIdx++] = (byte) (array[aIdx] | (array[aIdx + 1] << 2) | (array[aIdx + 2] << 4) | (array[aIdx + 3] << 6));
            aIdx += 4;
        }
        if (bIdx < bUpTo) {
            switch (array.length - aIdx) {
                case 0: break;
                case 1: {
                    buffer[bIdx++] = (byte) array[aIdx];
                    break;
                }
                case 2: {
                    buffer[bIdx++] = (byte) (array[aIdx] | (array[aIdx + 1] << 2));
                    break;
                }
                default: {
                    buffer[bIdx++] = (byte) (array[aIdx] | (array[aIdx + 1] << 2) | (array[aIdx + 2] << 4));
                    break;
                }
            }
        }
        return bIdx - bOffset;
    }

    static int write4Bits(byte[] buffer, int bOffset, int bUpTo, int[] array, int aOffset) {
        int bIdx = bOffset;
        final int arrayIdxUpTo = array.length - 1;
        int aIdx = aOffset;
        while (bIdx < bUpTo && aIdx < arrayIdxUpTo) {
            buffer[bIdx++] = (byte) (array[aIdx] | (array[aIdx + 1] << 4));
            aIdx += 2;
        }
        if (bIdx < bUpTo && aIdx != array.length) {
            buffer[bIdx++] = (byte) array[aIdx];
        }
        return bIdx - bOffset;
    }

    static int write8Bits(byte[] buffer, int bOffset, int bUpTo, int[] array, int aOffset) {
        int bIdx = bOffset;
        int aIdx = aOffset;
        while (bIdx < bUpTo && aIdx < array.length) {
            buffer[bIdx++] = (byte) array[aIdx++];
        }
        return bIdx - bOffset;
    }

    static int write16Bits(byte[] buffer, int bOffset, int bUpTo, int[] array, int aOffset) {
        int bIdx = bOffset;
        int aIdx = aOffset;
        final int bIdxUpTo = bUpTo - 1;
        while (bIdx < bIdxUpTo && aIdx < array.length) {
            buffer[bIdx] = (byte) (array[aIdx] & 0xff);
            buffer[bIdx + 1] = (byte) ((array[aIdx] >>> 8) & 0xff);
            ++aIdx;
            bIdx += 2;
        }
        return bIdx - bOffset;
    }

    static int write24Bits(byte[] buffer, int bOffset, int bUpTo, int[] array, int aOffset) {
        int bIdx = bOffset;
        int aIdx = aOffset;
        final int bIdxUpTo = bUpTo - 2;
        while (bIdx < bIdxUpTo && aIdx < array.length) {
            buffer[bIdx] = (byte) (array[aIdx] & 0xff);
            buffer[bIdx + 1] = (byte) ((array[aIdx] >>> 8) & 0xff);
            buffer[bIdx + 2] = (byte) ((array[aIdx] >>> 16) & 0xff);
            ++aIdx;
            bIdx += 3;
        }
        return bIdx - bOffset;
    }
}
