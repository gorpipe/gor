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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sigmar on 18/04/16.
 */
public class UTestGorTryMethods {
    private static final Logger log = LoggerFactory.getLogger(UTestGorTryMethods.class);

    @Test
    public void testTryHide() {
        String query = "gor ../tests/data/external/samtools/serialization_test.bam | tryhide SEQ,QUAL,DUMMY | tryhide 3,100 | top 1";
        TestUtils.runGorPipeCount(query);

        // succsesfully hide non-existing column
    }

    @Test
    public void testTrySelect() {
        String query = "gor ../tests/data/external/samtools/serialization_test.bam | top 2 | tryselect 1,2,DUMMY";
        TestUtils.runGorPipeCount(query);
    }

    @Test
    public void testTryWhere() {
        String query = "gor ../tests/data/external/samtools/serialization_test.bam | trywhere contains(notpresent,'unknown')";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals("trywhere not ignored on unknown column", 1, count);
    }

    @Test
    public void testTryWherePresent() {
        String query = "gor ../tests/data/external/samtools/serialization_test.bam | where contains(Cigar,'unknown')";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals("trywhere not ignored on unknown column", 0, count);
    }
}
