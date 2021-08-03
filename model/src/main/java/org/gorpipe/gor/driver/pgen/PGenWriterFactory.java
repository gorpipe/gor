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

import org.gorpipe.gor.model.FileReader;

import java.io.IOException;

public class PGenWriterFactory {

    private PGenWriterFactory() {}

    public static PGenWriter getPGenWriter(String fileName, int refIdx, int altIdx, int rsIdIdx, int valIdx,
                                           boolean group, boolean imp, float threshold) throws IOException {
        return getPGenWriter(fileName, refIdx, altIdx, rsIdIdx, valIdx, group, imp, threshold, null);
    }

    public static PGenWriter getPGenWriter(String fileName, int refIdx, int altIdx, int rsIdIdx, int valIdx,
                                           boolean group, boolean imp, float threshold, FileReader fileReader) throws IOException {
        final String pVarName = fileName.substring(0, fileName.lastIndexOf('.')) + ".pvar";
        if (group) {
            if (imp) {
                throw new IllegalArgumentException("GOR does not support grouping of imputed genotypes.");
            } else {
                final VariableWidthPGenOutputStream os = new VariableWidthPGenOutputStream(fileName, fileReader);
                final PVarWriter pVarWriter = new PVarWriter(pVarName, fileReader);
                final VariantRecordFactory<? extends VariantRecord> vrFact = new HardCallRecordFactory();
                return new GroupingPGenWriter(os, pVarWriter, vrFact, refIdx, altIdx, rsIdIdx, valIdx);
            }
        } else {
            if (imp) {
                final FWUnPhasedPGenOutputStream os = new FWUnPhasedPGenOutputStream(fileName, fileReader);
                final PVarWriter pVarWriter = new PVarWriter(pVarName, fileReader);
                final VariantRecordFactory<BiAllelicHardCallsAndDosages> vrFact = new ImputedRecordFactory(threshold);
                return new SimplePGenWriter<>(os, pVarWriter, vrFact, refIdx, altIdx, rsIdIdx, valIdx);
            } else {
                final FWHardCallsPGenOutputStream os = new FWHardCallsPGenOutputStream(fileName, fileReader);
                final PVarWriter pVarWriter = new PVarWriter(pVarName, fileReader);
                final VariantRecordFactory<BiAllelicHardCalls> vrFact = new HardCallRecordFactory();
                return new SimplePGenWriter<>(os, pVarWriter, vrFact, refIdx, altIdx, rsIdIdx, valIdx);
            }
        }
    }
}
