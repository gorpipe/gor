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

import gorsat.Analysis.ColSplitAnalysis;
import org.junit.Test;

public class UTestColSplitAnalysis {



    @Test
    public void testBasicColumnSplit() {
        String[] input = {"chr1\t1\tA,B,C"};
        String[] output = {"chr1\t1\tA,B,C\tA\tB\tC"};

        performTest(input, output, 3, ",", "", false);
    }



    @Test
    public void testBasicColumnSplitWithColumnNumberOfOne() {
        String[] input = {"chr1\t1\tA,B,C"};
        String[] output = {"chr1\t1\tA,B,C\tA"};

        performTest(input, output, 1, ",", "", false);
    }

    @Test
    public void testDifferentSeparator() {
        String[] input = {"chr1\t1\tA,B:C"};
        String[] output = {"chr1\t1\tA,B:C\tA,B\tC\t"};

        performTest(input, output, 3, ":", "", false);
    }

    @Test
    public void testEmptyRows() {
        String[] input = {"chr1\t1\tA,B,C"};
        String[] output = {"chr1\t1\tA,B,C\tA\tB\tC\t\t\t\t\t\t\t"};

        performTest(input, output, 10, ",", "", false);
    }

    @Test
    public void testEmptyRowsWithMissingValue() {
        String[] input = {"chr1\t1\tA,B,C"};
        String[] output = {"chr1\t1\tA,B,C\tA\tB\tC\tx\tx\tx\tx\tx\tx\tx"};

        performTest(input, output, 10, ",", "x", false);
    }

    @Test
    public void testEmptyRowsWithMissingValueAndColumnNumber() {
        String[] input = {
                "chr1\t1\tA,B,C",
                "chr1\t2\tD,E",
                "chr1\t3\tF,G,H,I,J,K,L"};
        String[] output = {
                "chr1\t1\tA,B,C\t3\tA\tB\tC\tx\tx",
                "chr1\t2\tD,E\t2\tD\tE\tx\tx\tx",
                "chr1\t3\tF,G,H,I,J,K,L\t7\tF\tG\tH\tI\tJ"};

        performTest(input, output, 5, ",", "x", true);
    }

    private void performTest(String[] input, String[] output, int numberOfColumns, String seoerator, String missingValue, boolean writeColums) {
        ColSplitAnalysis analysis = new ColSplitAnalysis(2, numberOfColumns, seoerator, missingValue, writeColums,
                AnalysisTestEngine.ROW_HEADER);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }

}
