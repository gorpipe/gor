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

package org.gorpipe.gor.model;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class UTestVcfHeader {

    @Test
    public void test10ColumnFile() {
        final String query = "gor -p chr2 ../tests/data/external/gvcf/test.gvcf";
        var results = TestUtils.runGorPipeLines(query);

        Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tVALUES\n", results[0]);
    }

    @Test
    public void test10PlusColumnFile() {
        final String query = "gor -p chr2 ../tests/data/external/samtools/test.vcf";
        var results = TestUtils.runGorPipeLines(query);

        Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tNA00001\tNA00002\tNA00003\n", results[0]);
    }
}
