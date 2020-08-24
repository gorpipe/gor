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

package org.gorpipe.model.genome.files.gor.bgen;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.model.genome.files.gor.Row;

import java.io.IOException;
import java.sql.SQLException;

class GroupingBGenWriter extends BGenWriter<VariantGrouper> {
    private String chr;
    private int pos;
    private CharSequence ref;

    GroupingBGenWriter(String fileName, VariantGrouper dbFact, int refIdx, int altIdx, int rsIdIdx, int varIdIdx, int valIdx) {
        super(fileName, dbFact, refIdx, altIdx, rsIdIdx, varIdIdx, valIdx);
    }

    @Override
    public void write(Row r) {
        if (this.chr == null) {
            initialize(r);
        } else if (!(this.chr.equals(r.chr) && this.pos == r.pos && this.ref.equals(r.colAsString(this.refIdx)))) {
            try {
                this.os.write(this.dbFact.merge());
            } catch (IOException | SQLException e) {
                throw new GorSystemException(e);
            }
            initialize(r);
        }
        this.dbFact.add(r.colAsString(this.altIdx), r.colAsString(this.valIdx));
    }

    @Override
    public void close() throws Exception {
        if (this.dbFact.hasSome()) this.os.write(this.dbFact.merge());
        super.close();
    }

    private void initialize(Row r) {
        this.dbFact.initialize(r.chr, r.pos, r.colAsString(this.refIdx), getRsId(r), getVarId(r));
        this.chr = r.chr;
        this.pos = r.pos;
        this.ref = r.colAsString(this.refIdx);
    }
}
