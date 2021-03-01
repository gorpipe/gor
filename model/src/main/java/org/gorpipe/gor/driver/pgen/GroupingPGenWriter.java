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

import org.gorpipe.gor.model.Row;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class GroupingPGenWriter extends PGenWriter<VariantRecord> {
    private String lastChr = null;
    private int lastPos = -1;
    private CharSequence lastRef, lastAlt, lastRsId, lastValues;
    private List<CharSequence> alts;
    private boolean lazyMode = true;
    private boolean firstRow = true;

    GroupingPGenWriter(VariableWidthPGenOutputStream os, PVarWriter writer, VariantRecordFactory<? extends VariantRecord> vrFact,
                       int refIdx, int altIdx, int rsIdIdx, int valIdx) {
        super(os, writer, vrFact, refIdx, altIdx, rsIdIdx, valIdx);
    }

    @Override
    public void write(Row r) throws IOException {
        if (firstRow) {
            saveFields(r);
            this.firstRow = false;
        } else if (lastPos != r.pos || !lastChr.equals(r.chr) || !lastRef.equals(r.colAsString(refIdx))) {
            flush();
            lazyMode = true;
            saveFields(r);
        } else {
            if (lazyMode) {
                vrFact.add(lastValues);
                if (alts == null) this.alts = new ArrayList<>();
                alts.add(lastAlt);
                lazyMode = false;
            }
            vrFact.add(r.colAsString(valIdx));
            alts.add(r.colAsString(altIdx));
        }
    }

    @Override
    public void close() throws Exception {
        if (!this.firstRow) flush();
        super.close();
    }

    private void flush() throws IOException {
        final CharSequence rsId, alt;
        if (lazyMode) {
            os.write(vrFact.parse(lastValues));
            rsId = rsIdIdx == -1 ? String.join(":", lastChr, String.valueOf(lastPos), lastAlt, lastRef) : lastRsId;
            alt = lastAlt;
        } else {
            os.write(vrFact.merge());
            alt = String.join(",", alts);
            alts.clear();
            rsId = rsIdIdx == -1 ? String.join(":", lastChr, String.valueOf(lastPos), alt, lastRef) : lastRsId;
        }
        writer.write(lastChr, lastPos, rsId, lastRef, alt);
    }

    private void saveFields(Row r) {
        this.lastChr = r.chr;
        this.lastPos = r.pos;
        this.lastRef = r.colAsString(refIdx);
        this.lastAlt = r.colAsString(altIdx);
        this.lastRsId = rsIdIdx == -1 ? null : r.colAsString(rsIdIdx);
        this.lastValues = r.colAsString(valIdx);
    }
}
