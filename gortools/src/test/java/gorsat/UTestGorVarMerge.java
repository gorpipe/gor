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

package gorsat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class UTestGorVarMerge {
    private static final Logger log = LoggerFactory.getLogger(UTestGorVarMerge.class);

    @Test
    public void testVarMerge() {
        TestUtils.assertGorpipeResults("Chrom\tPOS\treference\tallele\nchr10\t60803\tT\tG\nchr10\t61023\tC\tG\n",
                "gor -p chr10 ../tests/data/gor/dbsnp_test.gorz | select 1-4 | varmerge #3 #4");
    }

    @Test
    public void testVarMergeSort() {
        // Throws an exception if fails (Wrong order in command).  Test works even though we don't have config (no chromseq).
        TestUtils.assertGorpipeResults("Chrom\tPOS\treference\tallele\n",
                "gor -p chr21 ../tests/data/gor/dbsnp_varmerge_sort.gorz | select 1-4 | varmerge #3 #4 " +
                        "| verifyorder | where 2 = 3");
    }
}
