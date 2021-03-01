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

package gorsat.analysis;

import gorsat.Analysis.BugN;
import org.junit.Assert;
import org.junit.Test;

public class UTestBugN {

    private static final String[] INPUT_DATA = {
            "chr1\t1", "chr1\t2"};

    @Test
    public void testBugSetupWith100Percent() {
        runTestWitFailure("setup", 1);
    }

    @Test
    public void testBugProcessWith100Percent() {
        runTestWitFailure("process", 1);
    }

    @Test
    public void testBugFinishWith100Percent() {
        runTestWitFailure("finish", 1);
    }

    @Test
    public void testBugSetupWith0Percent() {
        runTest("setup", 0);
    }

    @Test
    public void testBugProcessWith0Percent() {
        runTest("process", 0);
    }

    @Test
    public void testBugFinishWith0Percent() {
        runTest("finish", 0);
    }

    private void runTest(String process, double value) {
        BugN analysis = new BugN(process, value);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT_DATA, INPUT_DATA);
    }

    private void runTestWitFailure(String process, double value) {

        String assertText = String.format("Bug %1$.0f%% for %2$s", value * 100.0, process);

        try {
            BugN analysis = new BugN(process, value);
            AnalysisTestEngine engine = new AnalysisTestEngine();
            engine.run(analysis, INPUT_DATA, INPUT_DATA);
        } catch (Throwable t) {
            Assert.assertEquals(assertText, "BUG " + process,  t.getMessage());
            return;
        }

        Assert.fail(assertText);
    }
}
