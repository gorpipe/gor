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

package org.gorpipe.gor.driver.pgen;

import java.util.ArrayList;
import java.util.List;

/**
 * A stream like interface for writing a pgen variant record to a buffer.
 */
abstract class VariantRecord {
    private final List<DataTrackWriter> dataTrackWriters;
    private final int numberOfSamples;
    private int dtwIdx = 0;

    protected VariantRecord(byte[] hc) {
        this.dataTrackWriters = new ArrayList<>();
        this.dataTrackWriters.add(new MainTrackWriter(hc));
        this.numberOfSamples = hc.length;
    }

    int write(byte[] buffer, int offset, int len) {
        final int upTo = offset + len;
        int bufferIdx = offset;
        while (this.dtwIdx < this.dataTrackWriters.size()) {
            final DataTrackWriter dtw = this.dataTrackWriters.get(this.dtwIdx);
            if (!dtw.done()) {
                final int written = dtw.write(buffer, bufferIdx, upTo - bufferIdx);
                if (written < 0) bufferIdx -= written;
                else return bufferIdx + written - offset;
            }
            ++this.dtwIdx;
        }
        return offset - bufferIdx;
    }

    abstract byte getType();

    int getNumberOfSamples() {
        return this.numberOfSamples;
    }

    protected void addDataTrackWriter(DataTrackWriter dtw) {
        this.dataTrackWriters.add(dtw);
    }
}
