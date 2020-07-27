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

class ImputedRecordFactory extends VariantRecordFactory<BiAllelicHardCallsAndDosages> {
    private float[] dosages;
    private final float threshold;

    ImputedRecordFactory(float threshold) {
        this.threshold = threshold;
    }

    @Override
    void add(CharSequence values) {
        throw new IllegalStateException("Not supported.");
    }

    @Override
    BiAllelicHardCallsAndDosages parse(CharSequence values) {
        final int len = values.length() / 2;
        setHcIfNull(len);
        setDosagesIfNull(len);
        ValueColumnParsing.parseImputedGenotypes(values, this.threshold, this.hc, this.dosages);
        return new BiAllelicHardCallsAndDosages(this.hc, this.dosages);
    }

    @Override
    MultiAllelicVariantRecord merge() {
        throw new IllegalStateException("Not supported.");
    }

    private void setDosagesIfNull(int len) {
        if (this.dosages == null) {
            this.dosages = new float[len];
        }
    }
}
