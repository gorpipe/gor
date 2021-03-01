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

package gorsat;

/**
 * Created by sigmar on 11/12/15.
 * from the htsjdk test source
 */

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;

import java.io.IOException;
import java.util.*;

/**
 * Created by vadim on 17/03/2015.
 */
public class FakeReferenceSequenceFile implements
        ReferenceSequenceFile {
    Map<String, SAMSequenceRecord> map = new HashMap<String, SAMSequenceRecord>();
    List<String> index = new ArrayList<String>();
    int current = 0;

    public FakeReferenceSequenceFile(List<SAMSequenceRecord> sequences) {
        for (SAMSequenceRecord s : sequences) {
            map.put(s.getSequenceName(), s);
            index.add(s.getSequenceName());
        }
    }

    private static ReferenceSequence buildReferenceSequence(SAMSequenceRecord samSequenceRecord) {
        byte[] bases = new byte[samSequenceRecord.getSequenceLength()];
        Arrays.fill(bases, (byte) 'N');
        return new ReferenceSequence(samSequenceRecord.getSequenceName(), samSequenceRecord.getSequenceIndex(), bases);
    }

    @Override
    public void reset() {
        current = 0;
    }

    @Override
    public ReferenceSequence nextSequence() {
        if (current >= index.size()) return null;
        return buildReferenceSequence(map.get(index.get(current++)));
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public ReferenceSequence getSubsequenceAt(final String contig, final long start,
                                              final long stop) {
        byte[] bases = new byte[(int) (stop - start + 1)];
        Arrays.fill(bases, (byte) 'N');
        return new ReferenceSequence(contig, bases.length, bases);
    }

    @Override
    public SAMSequenceDictionary getSequenceDictionary() {
        return null;
    }

    @Override
    public ReferenceSequence getSequence(final String contig) {
        return buildReferenceSequence(map.get(contig));
    }

    @Override
    public void close() throws IOException {
        map.clear();
        index.clear();
        current = 0;
    }
}
