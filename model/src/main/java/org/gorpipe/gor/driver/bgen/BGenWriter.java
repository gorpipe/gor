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

package org.gorpipe.gor.driver.bgen;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.Row;

import java.io.IOException;
import java.sql.SQLException;

public class BGenWriter<T extends DataBlockFactory<? extends VariantDataBlock>> implements AutoCloseable {
    protected final BGenOutputStream os;
    protected final int refIdx;
    protected final int altIdx;
    protected final int rsIdIdx;
    protected final int varIdIdx;
    protected final int valIdx;
    protected final T dbFact;

    BGenWriter(String fileName, T dbFact, int refIdx, int altIdx, int rsIdIdx, int varIdIdx, int valIdx) {
        this.os = new BGenOutputStream(fileName);
        this.refIdx = refIdx;
        this.altIdx = altIdx;
        this.rsIdIdx = rsIdIdx;
        this.varIdIdx = varIdIdx;
        this.valIdx = valIdx;
        this.dbFact = dbFact;
    }

    public void write(Row r) {
        try {
            this.os.write(this.dbFact.parse(r.chr, r.pos, r.colAsString(this.refIdx), r.colAsString(this.altIdx), getRsId(r), getVarId(r), r.colAsString(this.valIdx)));
        } catch (IOException | SQLException e) {
            throw new GorSystemException(e);
        }
    }

    protected CharSequence getRsId(Row r) {
        return this.rsIdIdx < 0 ? null : r.colAsString(this.rsIdIdx);
    }

    protected CharSequence getVarId(Row r) {
        return this.varIdIdx < 0 ? null : r.colAsString(this.varIdIdx);
    }

    @Override
    public void close() throws Exception {
        this.os.close();
    }
}
