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

package org.gorpipe.gor.driver.bgen;

import java.util.ArrayList;
import java.util.List;

import static org.gorpipe.gor.driver.genotypeutilities.ValueColumnParsing.parseMultiAllelicHardCalls;

class VariantGrouper extends HardCallDataBlockFactory {
    private CharSequence chr;
    private int pos;
    private CharSequence ref;
    private CharSequence alt;
    private List<CharSequence> alleles;
    private CharSequence rsId;
    private CharSequence varId;
    private CharSequence values;
    private List<CharSequence> valuesList;

    void initialize(CharSequence chr, int pos, CharSequence ref, CharSequence rsId, CharSequence varId) {
        this.chr = chr;
        this.pos = pos;
        this.ref = ref;
        this.alt = null;
        this.alleles = null;
        this.rsId = rsId;
        this.varId = varId;
        this.valuesList = null;
        this.values = null;
    }

    void add(CharSequence alt, CharSequence values) {
        if (this.alleles == null) {
            if (this.alt == null) {
                this.alt = alt;
                this.values = values;
            } else {
                this.alleles = new ArrayList<>();
                this.alleles.add(this.ref);
                this.alleles.add(this.alt);
                this.alleles.add(alt);
                this.valuesList = new ArrayList<>();
                this.valuesList.add(this.values);
                this.valuesList.add(values);
                this.alt = null;
                this.values = null;
            }
        } else {
            this.alleles.add(alt);
            this.valuesList.add(values);
        }
    }

    HardCallDataBlock merge() {
        if (this.alleles == null) {
            return super.parse(this.chr, this.pos, this.ref, this.alt, this.rsId, this.varId, this.values);
        } else {
            setIfNull(this.valuesList.get(0));
            parseMultiAllelicHardCalls(this.valuesList, this.gt1, this.gt2);
            fillExisting(this.gt1);
            this.dataBlock.setVariables(this.chr, this.pos, this.rsId, this.varId, this.existing, this.gt1, this.gt2, this.alleles.toArray(new CharSequence[0]));
            return this.dataBlock;
        }
    }

    boolean hasSome() {
        return this.alt != null || (this.alleles != null && !this.alleles.isEmpty());
    }

    private void fillExisting(int[] gt) {
        for (int i = 0; i < gt.length; ++i) {
            this.existing[i] = gt[i] != -1;
        }
    }
}
