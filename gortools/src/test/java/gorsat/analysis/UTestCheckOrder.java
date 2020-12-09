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

package gorsat.analysis;

import gorsat.Analysis.CheckOrder;
import gorsat.TestUtils;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Test;

public class UTestCheckOrder {

    private static final String[] VALID_INPUT_DATA = {
            "chr1\t1",
            "chr1\t2",
            "chr1\t3",
            "chr1\t4",
            "chr2\t1",
            "chr2\t1",
            "chr2\t2",
            "chr2\t3"};

    private static final String[] INVALID_POSITION_INPUT_DATA = {
            "chr1\t1",
            "chr1\t2",
            "chr1\t1",
            "chr1\t4",
            "chr2\t1",
            "chr2\t1",
            "chr2\t2",
            "chr2\t3"};

    private static final String[] INVALID_CHROMOSOME_INPUT_DATA = {
            "chr1\t1",
            "chr1\t2",
            "chr2\t1",
            "chr1\t4",
            "chr2\t1",
            "chr2\t1",
            "chr2\t2",
            "chr2\t3"};

    @Test
    public void testValidOrder() {
        CheckOrder analysis = new CheckOrder("");
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, VALID_INPUT_DATA, VALID_INPUT_DATA);
    }

    @Test(expected = GorDataException.class)
    public void testInvalidPositionOrder() {
        CheckOrder analysis = new CheckOrder("");
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INVALID_POSITION_INPUT_DATA, VALID_INPUT_DATA);
    }

    @Test(expected = GorDataException.class)
    public void testInvalidChromosomeOrder() {
        CheckOrder analysis = new CheckOrder("");
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INVALID_CHROMOSOME_INPUT_DATA, VALID_INPUT_DATA);
    }

    @Test
    public void testErrorMessage() {
        try {
            CheckOrder analysis = new CheckOrder("Message to test");
            AnalysisTestEngine engine = new AnalysisTestEngine();
            engine.run(analysis, INVALID_POSITION_INPUT_DATA, VALID_INPUT_DATA);
        } catch (Exception e) {
            Assert.assertTrue("Contains message text", e.getMessage().contains("Message to test"));
            return;
        }

        Assert.fail("This should never fail");
    }

    @Test
    public void testCheckOrderWithCreate() {
        try {
            TestUtils.runGorPipe("create xxx = gorrow chr1,2 | calc npos '2,1' | split npos | select chrom,npos; gor [xxx]");
            Assert.fail("Query should failed on gor order");
        } catch(Exception ignored) {}
    }
}
