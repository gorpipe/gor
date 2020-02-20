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

package org.gorpipe.model.genome.files.gor;

/**
 * Assume that we take the bits from the bytes of an array and arrange them in row such that the least significant
 * bit from the first byte comes first, then the second least significant from the first and so on. Next we take the
 * first {@code bits} bits and interpret them as the integer which we obtain by multiplying the first bit by 2^0, the second
 * with 2^0,..., and {@code bits} with 2^{{@code bits} - 1} and finally summing up the result. Then we take the next {@code bits} bits and so
 * on. In this way we obtain a stream of integers and BitStream is an interface for accessing those integers.
 *
 * Note: {@code bits} must be < 64.
 *
 * @author Hjalti Thor Isleifsson
 */

public class BitStream {
    private final int bits;
    private final int begin;
    private int idx;
    private int offset = 0;
    private final byte[] array;
    private final NextFunction nf;

    public BitStream(int bits, byte[] array, int idx) {
        this.array = array;
        this.idx = idx;
        this.begin = idx;
        this.bits = bits;
        this.nf = getNextFunction();
    }

    public void skip(int n) {
        final int tmp = (n * this.bits + this.offset);
        this.idx = (tmp >> 3) + this.idx;
        this.offset = (byte) (tmp & 7);
    }

    public void seek(int n) {
        this.idx = this.begin + ((this.bits * n) >> 3);
        this.offset = (byte) ((this.bits * n) & 7);
    }

    public long next() {
        return this.nf.apply();
    }

    abstract class NextFunction {
        abstract long apply();
    }

    private NextFunction getNextFunction() {
        if ((this.bits & 7) == 0) {
            switch (this.bits >>> 3) {
                case 1:
                    return new NextFunction() {
                        @Override
                        long apply() {
                            return array[idx++] & 0xff;
                        }
                    };
                case 2:
                    return new NextFunction() {
                        @Override
                        long apply() {
                            final long toReturn = (array[idx] & 0xffL) | ((array[idx + 1] & 0xffL) << 8);
                            idx += 2;
                            return toReturn;
                        }
                    };
                case 3:
                    return new NextFunction() {
                        @Override
                        long apply() {
                            final long toReturn = (array[idx] & 0xffL) | ((array[idx + 1] & 0xffL) << 8)
                                    | ((array[idx + 2] & 0xffL) << 16);
                            idx += 3;
                            return toReturn;
                        }
                    };
                case 4:
                    return new NextFunction() {
                        @Override
                        long apply() {
                            final long toReturn = (array[idx] & 0xffL) | ((array[idx + 1] & 0xffL) << 8)
                                    | ((array[idx + 2] & 0xffL) << 16) | ((array[idx + 3] & 0xffL) << 24);
                            idx += 4;
                            return toReturn;
                        }
                    };
                case 5:
                    return new NextFunction() {
                        @Override
                        long apply() {
                            final long toReturn = (array[idx] & 0xffL) | ((array[idx + 1] & 0xffL) << 8)
                                    | ((array[idx + 2] & 0xffL) << 16) | ((array[idx + 3] & 0xffL) << 24)
                                    | ((array[idx + 4] & 0xffL) << 32);
                            idx += 5;
                            return toReturn;
                        }
                    };
                case 6:
                    return new NextFunction() {
                        @Override
                        long apply() {
                            final long toReturn = (array[idx] & 0xffL) | ((array[idx + 1] & 0xffL) << 8)
                                    | ((array[idx + 2] & 0xffL) << 16) | ((array[idx + 3] & 0xffL) << 24)
                                    | ((array[idx + 4] & 0xffL) << 32) | ((array[idx + 5] & 0xffL) << 40);
                            idx += 6;
                            return toReturn;
                        }
                    };
                case 7:
                    return new NextFunction() {
                        @Override
                        long apply() {
                            final long toReturn = (array[idx] & 0xffL) | ((array[idx + 1] & 0xffL) << 8)
                                    | ((array[idx + 2] & 0xffL) << 16) | ((array[idx + 3] & 0xffL) << 24)
                                    | ((array[idx + 4] & 0xffL) << 32) | ((array[idx + 5] & 0xffL) << 40)
                                    | ((array[idx + 6] & 0xffL) << 48);
                            idx += 7;
                            return toReturn;
                        }
                    };
                default:
                    throw new IllegalArgumentException();
            }
        } else if ((bits & (bits - 1)) == 0) { //Is this.bits a power of two (less than eight)?
            return new NextFunction() {
                final int toAndWith = (1 << bits) - 1;
                @Override
                long apply() {
                    final int toReturn = ((array[idx] & 0xff) >>> offset) & toAndWith;
                    offset = (offset + bits) & 7;
                    if (offset == 0) idx++;
                    return toReturn;
                }
            };
        } else {
            return new NextFunction() {
                final long toAndWith = (1L << bits) - 1L;
                @Override
                long apply() {
                    long toReturn;
                    final int newIdx = ((bits + offset) >>> 3) + idx;
                    final int newOffset = (offset + bits) & 7;
                    if (newIdx == idx) {
                         toReturn = ((array[idx] & 0xffL) >>> offset) & toAndWith;
                    } else {
                        toReturn = (array[idx++] & 0xffL) >>> offset;
                        int slide = 8 - offset;
                        while (idx < newIdx) {
                            toReturn |= ((array[idx++] & 0xffL) << slide);
                            slide += 8;
                        }
                        if (newOffset != 0) toReturn = (toReturn | ((array[idx] & 0xffL) << slide)) & toAndWith;
                    }
                    offset = newOffset;
                    return toReturn;
                }
            };
        }
    }
}
