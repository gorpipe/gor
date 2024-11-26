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

/**
 * Created by sigmar on 10/12/15.
 */
public class UTestVariantBamCigarValidation {
    @Test
    public void testVariantBamCigarValidation() {
        String[] args = new String[]{"gor ../tests/data/external/samtools/serialization_test.bam | variants", "-config", "../tests/config/gor_unittests_config.txt"};
        Assert.assertEquals(225, TestUtils.runGorPipeCount(args));
    }

    @Test
    public void testEmptyBamQualPileup() {
        String[] args = new String[]{"gor -p chr22 ../tests/data/external/samtools/index_test.bam | pileup", "-config", "../tests/config/gor_unittests_config.txt"};
        Assert.assertEquals(5965, TestUtils.runGorPipeCount(args));
    }

    @Test
    public void testOverlapPileup() {
        String[] args = new String[]{"gor ../tests/data/external/samtools/bam.gorz | where qname = 'LH00430:208:22CMM2LT4:1:1194:15833:24435' | pileup -q 0 -bq 0 -depth | where depth = 1", "-config", "../tests/config/gor_unittests_config.txt"};
        Assert.assertEquals(191, TestUtils.runGorPipeCount(args));
    }

    @Test
    public void testNonOverlapPileup() {
        String[] args = new String[]{"gor ../tests/data/external/samtools/bam.gorz | where qname = 'LH00430:208:22CMM2LT4:1:1194:15833:24435' | pileup -soc -q 0 -bq 0 -depth | where depth = 1", "-config", "../tests/config/gor_unittests_config.txt"};
        Assert.assertEquals(246, TestUtils.runGorPipeCount(args));
    }
}
