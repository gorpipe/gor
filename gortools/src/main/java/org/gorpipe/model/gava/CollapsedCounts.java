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
package org.gorpipe.model.gava;

/**
 * Collapsed counts for a group of variants.
 *
 * @author gfj
 */
class CollapsedCounts extends VariantCounts {

    /**
     * The number of variants collapsed to one.
     */
    int numVariants;

    /**
     * Construct collapsed counts with a single variant.
     *
     * @param varCallCounts the call counts of the variant for each subject
     */
    CollapsedCounts(VariantCounts varCallCounts) {
        super(varCallCounts.callCounts, varCallCounts.nullScore, varCallCounts.altScore);
        this.numVariants = 1;
    }

    /**
     * Add a variant to the collapsed group.
     *
     * @param varCallCounts the call counts of the variant for each subject
     */
    void addVariant(VariantCounts varCallCounts) {
        if (numVariants == 1) {
            // Create a copy only when needed.
            int[] newCallCounts = new int[callCounts.length];
            System.arraycopy(callCounts, 0, newCallCounts, 0, callCounts.length);
            callCounts = newCallCounts;
        }
        for (int i = 0; i < varCallCounts.callCounts.length; ++i) {
            callCounts[i] += varCallCounts.callCounts[i]; // Todo What if recessive?
        }
        nullScore = (numVariants * nullScore + varCallCounts.nullScore) / (numVariants + 1);
        altScore = (numVariants * altScore + varCallCounts.altScore) / (numVariants + 1);
        ++numVariants;
    }
}
