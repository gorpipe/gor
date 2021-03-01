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

import gorsat.Analysis.DistLocAnalysis;
import org.junit.Test;

public class UTestDistLocAnalysis {

    @Test
    public void testContinuousPositionsTop10() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr1\t3",
                "chr2\t1",
                "chr2\t2"};

        performTest(input, input, 10);
    }

    @Test
    public void testContinuousPositionsTop2() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr1\t3",
                "chr2\t1",
                "chr2\t2"};

        String[] output = {
                input[0],
                input[1]};

        performTest(input, output, 2);
    }

    @Test
    public void testContinuousPositionsSamePositionTop2() {
        String[] input = {
                "chr1\t1\tA",
                "chr1\t1\tB",
                "chr1\t3\tC",
                "chr2\t1\tD",
                "chr2\t1\tE",
                "chr2\t1\tF",
                "chr2\t1\tG",
                "chr2\t10\tH"};

        String[] output = {
                input[0],
                input[1],
                input[2],
                input[3],
                input[4],
                input[5],
                input[6]};

        performTest(input, output, 3);
    }

    private void performTest(String[] input, String[] output, int length) {
        DistLocAnalysis analysis = new DistLocAnalysis(length);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
