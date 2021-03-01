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

import org.gorpipe.gor.model.Row;

import java.io.IOException;

class SimplePGenWriter<T extends VariantRecord> extends PGenWriter<T> {

    SimplePGenWriter(PGenOutputStream<T> os, PVarWriter writer, VariantRecordFactory<T> vrFact,
                     int refIdx, int altIdx, int rsIdIdx, int valIdx) {
        super(os, writer, vrFact, refIdx, altIdx, rsIdIdx, valIdx);
    }

    @Override
    public void write(Row r) throws IOException {
        final CharSequence rsId = rsIdIdx == -1 ? String.join(":", r.chr, String.valueOf(r.pos), r.colAsString(altIdx), r.colAsString(refIdx)) : r.colAsString(rsIdIdx);
        writer.write(r.chr,  r.pos,  rsId, r.colAsString(refIdx), r.colAsString(altIdx));
        os.write(vrFact.parse(r.colAsString(valIdx)));
    }
}
