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

import gorsat.Analysis.ColNumAnalysis;
import org.junit.Test;

public class UTestColNumAnalysis {

    @Test
    public void testColNumTwoColumns() {
        String[] input = {"chr1\t1"};
        String[] output = {"chr1\t1"};

        performTest(input, output);
    }

    @Test
    public void testColNumThreeColumns() {
        String[] input = {"chr1\t1\t2"};
        String[] output = {"chr1\t1\t3(2)"};

        performTest(input, output);
    }

    @Test
    public void testColNumFourColumns() {
        String[] input = {"chr1\t1\t2\tA"};
        String[] output = {"chr1\t1\t3(2)\t4(A)"};

        performTest(input, output);
    }

    @Test
    public void testColNumMoreThanFourColumns() {
        String[] input = {"chr1\t1\t2\tA\tB\tC\tD"};
        String[] output = {"chr1\t1\t3(2)\t4(A)\t5(B)\t6(C)\t7(D)"};

        performTest(input, output);
    }

    private void performTest(String[] input, String[] output) {
        ColNumAnalysis analysis = new ColNumAnalysis();
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }

}
