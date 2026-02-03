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

package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.util.Log;

import java.io.Closeable;

/**
 * Used to represent a CRAM reference, the backing source for which can either be
 * a file or the EBI ENA reference service.
 *
 * NOTE: In a future release, this class will be renamed and the functionality it
 * contains will be refactored and distributed into one or more separate reference
 * source implementations, each corresponding to the type of resource backing the
 * reference.
 */

public class SharedFastaReferenceSource extends SharedCachedReferenceSource implements Closeable {

    private static final Log log = Log.getInstance(htsjdk.samtools.cram.ref.ReferenceSource.class);
    private final ReferenceSequenceFile rsFile;

    public SharedFastaReferenceSource(final ReferenceSequenceFile rsFile, String referenceFileKey) {
        super(referenceFileKey);
        this.rsFile = rsFile;
    }

    @Override
    protected byte[] loadReference(String name) {
        if (rsFile == null || !rsFile.isIndexed())
            return new byte[0];

        ReferenceSequence sequence = null;
        try {
            sequence = rsFile.getSequence(name);
        } catch (final SAMException e) {
            // the only way to test if rsFile contains the sequence is to try and catch exception.
            log.warn("Sequence not found: " + name);
        }
        if (sequence != null)
            return sequence.getBases();

        return new byte[0];
    }

    @Override
    public byte[] getReferenceBasesByRegion(SAMSequenceRecord sequenceRecord, int zeroBasedStart, int requestedRegionLength) {
        if (rsFile != null && rsFile.isIndexed()) {
            var name = sequenceRecord.getContig();
            ReferenceSequence sequence = null;
            try {
                sequence = rsFile.getSubsequenceAt(name, zeroBasedStart + 1, zeroBasedStart + requestedRegionLength);
            } catch (final SAMException e) {
                // the only way to test if rsFile contains the sequence is to try and catch exception.
                log.warn("Sequence not found: " + name);
            }
            if (sequence != null) {
                var bases = sequence.getBases();
                toUpperCase(bases);
                return bases;
            }
        }
        return new byte[0];
    }

    private static final byte UPPER_CASE_OFFSET = 'A' - 'a';

    public static byte toUpperCase(final byte b) {
        if (b < 'a' || b > 'z') {
            return b;
        }
        return (byte) (b + UPPER_CASE_OFFSET);
    }

    /**
     * Converts in place all lower case letters to upper case in the byte array provided.
     */
    public static void toUpperCase(final byte[] bytes) {
        final int length = bytes.length;
        for (int i = 0; i < length; ++i) {
            if (bytes[i] >= 'a' && bytes[i] <= 'z') {
                bytes[i] = (byte) (bytes[i] + UPPER_CASE_OFFSET);
            }
        }
    }

}
