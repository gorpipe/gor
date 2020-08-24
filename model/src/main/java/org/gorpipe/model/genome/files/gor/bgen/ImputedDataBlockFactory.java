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

import static org.gorpipe.model.genome.files.gor.genotypeutilities.ValueColumnParsing.parseImputedGenotypes;

class ImputedDataBlockFactory extends DataBlockFactory<ImputedDataBlock> {
    private float[][] pr;
    private boolean set = false;

    ImputedDataBlockFactory() {
        this.dataBlock = new ImputedDataBlock();
    }

    private void setIfNull(CharSequence values) {
        if (!this.set) {
            final int len = values.length() / 2;
            this.pr = new float[len][3];
            this.existing = new boolean[len];
            this.set = true;
        }
    }

    @Override
    ImputedDataBlock parse(CharSequence chr, int pos, CharSequence ref, CharSequence alt, CharSequence rsId, CharSequence varId, CharSequence values) {
        setIfNull(values);
        parseImputedGenotypes(values, this.pr);
        fillExisting(values);
        this.dataBlock.setVariables(chr, pos, rsId, varId, ref, alt, this.existing, this.pr);
        return this.dataBlock;
    }

    private void fillExisting(CharSequence values) {
        for (int i = 0; i < this.existing.length; ++i) {
            this.existing[i] = values.charAt(2 * i) != ' ';
        }
    }
}
