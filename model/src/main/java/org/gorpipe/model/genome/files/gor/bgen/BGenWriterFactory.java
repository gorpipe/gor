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

public class BGenWriterFactory {

    private BGenWriterFactory() {}

    public static BGenWriter getBGenWriter(String fileName, boolean group, boolean imputed, int refIdx, int altIdx, int rsIdIdx, int varIdIdx, int valueIdx) {
        if (imputed) {
            return new BGenWriter<>(fileName, new ImputedDataBlockFactory(), refIdx, altIdx, rsIdIdx, varIdIdx, valueIdx);
        } else {
            if (group) {
                return new BGenWriter<>(fileName, new VariantGrouper(), refIdx, altIdx, rsIdIdx, varIdIdx, valueIdx);
            } else {
                return new BGenWriter<>(fileName, new HardCallDataBlockFactory(), refIdx, altIdx, rsIdIdx, varIdIdx, valueIdx);
            }
        }
    }
}
