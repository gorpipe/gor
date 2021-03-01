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

/**
 * PGenWriter is a class which takes in gor rows containing genotype data, and writes them to a pgen file and a
 * corresponding pvar file.
 */
public abstract class PGenWriter<T extends VariantRecord> implements AutoCloseable {
    protected int refIdx, altIdx, rsIdIdx, valIdx;
    protected final PVarWriter writer;
    protected final PGenOutputStream<T> os;
    protected final VariantRecordFactory<? extends T> vrFact;

    /**
     * @param os The pgen output stream to which the pgen writer should write the variant records. The pgen writer closes the pgen output stream.
     * @param writer The pvar writer to which the pgen writer should write the variant info. The pgen writer closes the pvar writer.
     */
    PGenWriter(PGenOutputStream<T> os, PVarWriter writer, VariantRecordFactory<? extends T> vrFact, int refIdx, int altIdx, int rsIdIdx, int valIdx) {
        this.refIdx = refIdx;
        this.altIdx = altIdx;
        this.vrFact = vrFact;
        this.rsIdIdx = rsIdIdx;
        this.valIdx = valIdx;
        this.writer = writer;
        this.os = os;
    }

    public abstract void write(Row r) throws IOException;

    @Override
    public void close() throws Exception {
        this.os.close();
        this.writer.close();
    }
}
