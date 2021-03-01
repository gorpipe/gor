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

package gorsat;

import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by sigmar on 01/10/15.
 */
public class UTestGorUntilThrow {
    /**
     * Test gor until method
     *
     */
    @Test
    public void testGorUntil() {

        String query = "gor ../tests/data/external/samtools/index_test.bam | until Chromo == 'chr11'";
        Assert.assertEquals("Gor UNTIL not stopping at right position", 1362, TestUtils.runGorPipeCount(query));
    }

    /**
     * Test gor throw method
     *
     */
    @Test
    public void testGorThrow() {
        String query = "gor ../tests/data/external/samtools/index_test.bam | throwif Chromo == 'chr11'";

        try {
            TestUtils.runGorPipeCount(query);
            Assert.fail();
        } catch (GorDataException ignored) {
        }
    }

    @Test
    @Ignore("No longer valid as division is now always floating point, see GOR-581")
    public void testGorCatchFormula() {
        String query = "gor ../tests/data/external/samtools/index_test.bam | top 2 | calc y catch(1/0,'/ by zero error')";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);

        for (String line : lines) {
            Assert.assertTrue(line.contains("by zero"));
        }
    }

    @Test
    @Ignore("No longer valid as division is now always floating point, see GOR-581")
    public void testGorCatchWithErrorMessageFormula() {
        String query = "gor ../tests/data/external/samtools/index_test.bam | top 2 | calc y catch(1/0,'some error #{e}')";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);

        for (String line : lines) {
            Assert.assertTrue(line.contains("by zero"));
        }
    }
}
