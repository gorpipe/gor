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

import static org.gorpipe.gor.driver.genotypeutilities.ValueColumnParsing.parseBiAllelicHardCalls;

class HardCallDataBlockFactory extends DataBlockFactory<HardCallDataBlock> {
    protected int[] gt1, gt2;
    protected byte[] uncompressed;
    private boolean set = false;

    HardCallDataBlockFactory() {
        this.dataBlock = new HardCallDataBlock();
    }

    void setIfNull(CharSequence values) {
        if (!this.set) {
            final int len = values.length();
            this.gt1 = new int[len];
            this.gt2 = new int[len];
            this.uncompressed = new byte[(len & 3) == 0 ? (len >>> 2) : ((len >>> 2) + 1)];
            this.existing = new boolean[len];
            this.set = true;
        }
    }

    @Override
    HardCallDataBlock parse(CharSequence chr, int pos, CharSequence ref, CharSequence alt, CharSequence rsId, CharSequence varId, CharSequence values) {
        setIfNull(values);
        parseBiAllelicHardCalls(values, this.gt1, this.gt2);
        fillExisting(values);
        this.dataBlock.setVariables(chr, pos, rsId, varId, this.existing, this.gt1, this.gt2, ref, alt);
        return this.dataBlock;
    }

    private void fillExisting(CharSequence values) {
        for (int i = 0; i < this.existing.length; ++i) {
            this.existing[i] = values.charAt(i) != '3';
        }
    }
}
