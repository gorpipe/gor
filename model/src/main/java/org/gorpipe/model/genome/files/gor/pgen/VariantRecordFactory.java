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

import java.util.ArrayList;
import java.util.List;

/**
 * A class that provides methods for
 *
 * 1) Parsing genotype data, encoded as charsequence in some way, to a variant record.
 * 2) Merging genotype data of the form mentioned above, to a multi-allelic variant record.
 */
abstract class VariantRecordFactory<T extends VariantRecord> {
    protected byte[] hc;
    protected int[] gt1, gt2;
    protected List<CharSequence> valueCols;

    abstract void add(CharSequence values);

    abstract T parse(CharSequence values);

    abstract MultiAllelicVariantRecord merge();

    protected void setHcIfNull(int len) {
        if (this.hc == null) {
            this.hc = new byte[len];
        }
    }

    protected void setGtsIfNull(int len) {
        if (this.gt1 == null && this.gt2 == null) {
            this.gt1 = new int[len];
            this.gt2 = new int[len];
        }
    }

    protected void setValueColsIfNull() {
        if (this.valueCols == null) {
            this.valueCols = new ArrayList<>();
        }
    }
}
