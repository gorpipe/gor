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

package org.gorpipe.gor.driver.providers.stream.datatypes.cram;

import java.util.Arrays;
import java.util.List;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTag;

public class CramUtils {

    // Below code taken and revised from htsjdk  library
    public static final byte a = 'a', c = 'c', g = 'g', t = 't', n = 'n', A = 'A', C = 'C', G = 'G', T = 'T', N = 'N';

    private static final byte A_MASK = 1;
    private static final byte C_MASK = 2;
    private static final byte G_MASK = 4;
    private static final byte T_MASK = 8;

    private static final byte[] bases = new byte[127];
    private static final byte NON_IUPAC_CODE = 0;
    /*
     * Definition of IUPAC codes:
     * http://www.bioinformatics.org/sms2/iupac.html
     */
    static {
        Arrays.fill(bases, NON_IUPAC_CODE);
        bases[A] = A_MASK;
        bases[C] = C_MASK;
        bases[G] = G_MASK;
        bases[T] = T_MASK;
        bases['M'] = A_MASK | C_MASK;
        bases['R'] = A_MASK | G_MASK;
        bases['W'] = A_MASK | T_MASK;
        bases['S'] = C_MASK | G_MASK;
        bases['Y'] = C_MASK | T_MASK;
        bases['K'] = G_MASK | T_MASK;
        bases['V'] = A_MASK | C_MASK | G_MASK;
        bases['H'] = A_MASK | C_MASK | T_MASK;
        bases['D'] = A_MASK | G_MASK | T_MASK;
        bases['B'] = C_MASK | G_MASK | T_MASK;
        bases['N'] = A_MASK | C_MASK | G_MASK | T_MASK;
        // Also store the bases in lower case
        for (int i = 'A'; i <= 'Z'; i++) {
            bases[(byte) i + 32] = bases[(byte) i];
        }
        bases['.'] = A_MASK | C_MASK | G_MASK | T_MASK;
    }

    public static void calculateMdAndNmTags(final SAMRecord record, final byte[] ref,
                                            final boolean calcMD, final boolean calcNM) {
        if (!calcMD && !calcNM)
            return;

        try {
            final Cigar cigar = record.getCigar();
            final List<CigarElement> cigarElements = cigar.getCigarElements();
            final byte[] seq = record.getReadBases();
            int cigarIndex, blockRefPos, blockReadStart, matchCount = 0;
            int nmCount = 0;
            final StringBuilder mdString = new StringBuilder();

            final int nElements = cigarElements.size();
            for (cigarIndex = blockReadStart = 0, blockRefPos = 0; cigarIndex < nElements; ++cigarIndex) {
                final CigarElement ce = cigarElements.get(cigarIndex);
                int inBlockOffset;
                final int blockLength = ce.getLength();
                final CigarOperator op = ce.getOperator();
                if (op == CigarOperator.MATCH_OR_MISMATCH || op == CigarOperator.EQ
                        || op == CigarOperator.X) {
                    for (inBlockOffset = 0; inBlockOffset < blockLength; ++inBlockOffset) {
                        final int readOffset = blockReadStart + inBlockOffset;

                        if (ref.length <= blockRefPos + inBlockOffset) break; // out of boundary

                        final byte readBase = seq[readOffset];
                        final byte refBase = ref[blockRefPos + inBlockOffset];

                        if ((bases[readBase] == bases[refBase]) || readBase == 0) {
                            // a match
                            ++matchCount;
                        } else {
                            mdString.append(matchCount);
                            mdString.appendCodePoint(refBase);
                            matchCount = 0;
                            ++nmCount;
                        }
                    }
                    if (inBlockOffset < blockLength) break;
                    blockRefPos += blockLength;
                    blockReadStart += blockLength;
                } else if (op == CigarOperator.DELETION) {
                    mdString.append(matchCount);
                    mdString.append('^');
                    for (inBlockOffset = 0; inBlockOffset < blockLength; ++inBlockOffset) {
                        if (ref[blockRefPos + inBlockOffset] == 0) break;
                        mdString.appendCodePoint(ref[blockRefPos + inBlockOffset]);
                    }
                    matchCount = 0;
                    if (inBlockOffset < blockLength) break;
                    blockRefPos += blockLength;
                    nmCount += blockLength;
                } else if (op == CigarOperator.INSERTION
                        || op == CigarOperator.SOFT_CLIP) {
                    blockReadStart += blockLength;
                    if (op == CigarOperator.INSERTION) nmCount += blockLength;
                } else if (op == CigarOperator.SKIPPED_REGION) {
                    blockRefPos += blockLength;
                }
            }
            mdString.append(matchCount);

            if (calcMD) record.setAttribute(SAMTag.MD.name(), mdString.toString());
            if (calcNM) record.setAttribute(SAMTag.NM.name(), nmCount);
        } catch (Exception ex) {

        }
    }
}
