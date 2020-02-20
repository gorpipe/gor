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

import gorsat.Analysis.NClosest;
import org.junit.Test;

public class UTestNClosest {

    @Test
    public void closestGroupOfChromosomePositionsOneClosest() {
        String[] input = {
                "chr1\t1\tLeftRow\t0\t1\tRightRow",
                "chr2\t20\tLeftRow\t-1\t19\tRightRow",
                "chr2\t20\tLeftRow\t0\t20\tRightRow",
                "chr2\t20\tLeftRow\t1\t21\tRightRow",
                "chr2\t20\tLeftRow\t3\t23\tRightRow"};

        String[] output = {
                "chr1\t1\tLeftRow\t0\t1\tRightRow",
                "chr2\t20\tLeftRow\t0\t20\tRightRow"};

        performTest(input, output, 1);
    }

    @Test
    public void closestGroupOfChromosomePositionsTwoClosest() {
        String[] input = {
                "chr1\t1\tLeftRow\t0\t1\tRightRow",
                "chr2\t10\tLeftRow\t-1\t9\tRightRow",
                "chr2\t10\tLeftRow\t0\t10\tRightRow",
                "chr2\t10\tLeftRow\t1\t11\tRightRow",
                "chr2\t10\tLeftRow\t2\t12\tRightRow"};

        String[] output = {
                "chr1\t1\tLeftRow\t0\t1\tRightRow",
                "chr2\t10\tLeftRow\t0\t10\tRightRow",
                "chr2\t10\tLeftRow\t-1\t9\tRightRow"};

        performTest(input, output, 2);
    }

    @Test
    public void closestGroupOfChromosomePositionsTenClosest() {
        String[] input = {
                "chr1\t1\tLeftRow\t0\t1\tRightRow",
                "chr2\t10\tLeftRow\t-1\t9\tRightRow",
                "chr2\t10\tLeftRow\t0\t10\tRightRow",
                "chr2\t10\tLeftRow\t1\t11\tRightRow",
                "chr2\t10\tLeftRow\t3\t13\tRightRow",
                "chr3\t100\tLeftRow\t-1\t99\tRightRow",
                "chr3\t100\tLeftRow\t0\t100\tRightRow",
                "chr3\t100\tLeftRow\t1\t101\tRightRow",
                "chr3\t100\tLeftRow\t2\t102\tRightRow",
                "chr3\t100\tLeftRow\t3\t103\tRightRow",
                "chr3\t100\tLeftRow\t4\t104\tRightRow",
                "chr3\t100\tLeftRow\t5\t105\tRightRow",
                "chr3\t100\tLeftRow\t6\t106\tRightRow",
                "chr3\t100\tLeftRow\t7\t107\tRightRow",
                "chr3\t100\tLeftRow\t20\t120\tRightRow",
                "chr3\t100\tLeftRow\t21\t121\tRightRow"};

        String[] output = {
                "chr1\t1\tLeftRow\t0\t1\tRightRow",
                "chr2\t10\tLeftRow\t0\t10\tRightRow",
                "chr2\t10\tLeftRow\t-1\t9\tRightRow",
                "chr2\t10\tLeftRow\t1\t11\tRightRow",
                "chr2\t10\tLeftRow\t3\t13\tRightRow",
                "chr3\t100\tLeftRow\t0\t100\tRightRow",
                "chr3\t100\tLeftRow\t-1\t99\tRightRow",
                "chr3\t100\tLeftRow\t1\t101\tRightRow",
                "chr3\t100\tLeftRow\t2\t102\tRightRow",
                "chr3\t100\tLeftRow\t3\t103\tRightRow",
                "chr3\t100\tLeftRow\t4\t104\tRightRow",
                "chr3\t100\tLeftRow\t5\t105\tRightRow",
                "chr3\t100\tLeftRow\t6\t106\tRightRow",
                "chr3\t100\tLeftRow\t7\t107\tRightRow",
                "chr3\t100\tLeftRow\t20\t120\tRightRow"};

        performTest(input, output, 10);
    }

    private void performTest(String[] input, String[] output, int nClosest) {
        NClosest analysis = new NClosest(1, nClosest);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
