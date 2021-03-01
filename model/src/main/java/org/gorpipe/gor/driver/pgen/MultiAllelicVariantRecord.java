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

abstract class MultiAllelicVariantRecord extends VariantRecord {
    protected final int numberOfAltAlleles;

    MultiAllelicVariantRecord(int[] gt1, int[] gt2, byte[] hc, int numberOfAltAlleles) {
        super(hc);
        super.addDataTrackWriter(new MultiAllelicHardCallsWriter(gt1, gt2, hc, numberOfAltAlleles));
        this.numberOfAltAlleles = numberOfAltAlleles;
    }
}
