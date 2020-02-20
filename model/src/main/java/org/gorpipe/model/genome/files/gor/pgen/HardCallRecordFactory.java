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

package org.gorpipe.model.genome.files.gor.pgen;

import org.gorpipe.model.genome.files.gor.genotypeutilities.ValueColumnParsing;

class HardCallRecordFactory extends VariantRecordFactory<BiAllelicHardCalls> {

    @Override
    void add(CharSequence values) {
        setValueColsIfNull();
        this.valueCols.add(values);
    }

    @Override
    BiAllelicHardCalls parse(CharSequence values) {
        setHcIfNull(values.length());
        ValueColumnParsing.parseBiAllelicHardCalls(values, this.hc);
        return new BiAllelicHardCalls(this.hc);
    }

    @Override
    MultiAllelicHardCalls merge() {
        if (this.valueCols != null && !this.valueCols.isEmpty()) {
            return privateMerge();
        } else throw new IllegalStateException();
    }

    private MultiAllelicHardCalls privateMerge() {
        setHcIfNull(this.valueCols.get(0).length());
        setGtsIfNull(this.hc.length);
        ValueColumnParsing.parseMultiAllelicHardCalls(this.valueCols, this.gt1, this.gt2);
        ValueColumnParsing.fillHC(this.gt1, this.gt2, this.hc);
        final MultiAllelicHardCalls toReturn = new MultiAllelicHardCalls(this.gt1, this.gt2, this.hc, this.valueCols.size());
        this.valueCols.clear();
        return toReturn;
    }
}
