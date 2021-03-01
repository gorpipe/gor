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

import gorsat.Analysis.MultiColumnSplit;
import org.junit.Test;

public class UTestMultiColumnSplit {

    @Test
    public void testBasicSplitSingleColumn() {
        String[] input = {"chr1\t1\tA,B,C"};
        String[] output = {
                "chr1\t1\tA",
                "chr1\t1\tB",
                "chr1\t1\tC"};
        int[] splitColumns = {2};

        performTest(input, output, splitColumns, 3, ",", "");
    }

    @Test
    public void testBasicSplitSingleColumnCustomSeparator() {
        String[] input = {"chr1\t1\tA,B:C"};
        String[] output = {
                "chr1\t1\tA,B",
                "chr1\t1\tC"};
        int[] splitColumns = {2};

        performTest(input, output, splitColumns, 3, ":", "");
    }

    @Test
    public void testBasicSplitTwoColumns() {
        String[] input = {"chr1\t1\tA,B,C\tD,E,F"};
        String[] output = {
                "chr1\t1\tA\tD",
                "chr1\t1\tB\tE",
                "chr1\t1\tC\tF"};
        int[] splitColumns = {2,3};

        performTest(input, output, splitColumns, 4, ",", "");
    }

    @Test
    public void testBasicSplitThreeColumns() {
        String[] input = {"chr1\t1\tA,B,C\tD,E,F\tG,H,I"};
        String[] output = {
                "chr1\t1\tA\tD\tG",
                "chr1\t1\tB\tE\tH",
                "chr1\t1\tC\tF\tI"};
        int[] splitColumns = {2,3,4};

        performTest(input, output, splitColumns, 5, ",", "");
    }

    @Test
    public void testBasicSplitTwoColumnsWithEmptyValues() {
        String[] input = {"chr1\t1\tA,B,C\tD,F"};
        String[] output = {
                "chr1\t1\tA\tD",
                "chr1\t1\tB\tF",
                "chr1\t1\tC\txxx"};
        int[] splitColumns = {2,3};

        performTest(input, output, splitColumns, 4, ",", "xxx");
    }

    @Test
    public void testBasicSplitThreeColumnsWithEmptyValues() {
        String[] input = {"chr1\t1\tA,B,C\tD,E\tF"};
        String[] output = {
                "chr1\t1\tA\tD\tF",
                "chr1\t1\tB\tE\txxx",
                "chr1\t1\tC\txxx\txxx"};
        int[] splitColumns = {2,3, 4};

        performTest(input, output, splitColumns, 5, ",", "xxx");
    }

    @Test
    public void testBasicSplitSingleColumnWithExtraColumns() {
        String[] input = {"chr1\t1\tA,B,C\tFoo\tBar"};
        String[] output = {
                "chr1\t1\tA\tFoo\tBar",
                "chr1\t1\tB\tFoo\tBar",
                "chr1\t1\tC\tFoo\tBar"};
        int[] splitColumns = {2};

        performTest(input, output, splitColumns, 5, ",", "");
    }

    @Test
    public void testBasicSplitTwoColumnsWithExtraColumns() {
        String[] input = {"chr1\t1\tA,B,C\tD,E,F\tFoo\tBar"};
        String[] output = {
                "chr1\t1\tA\tD\tFoo\tBar",
                "chr1\t1\tB\tE\tFoo\tBar",
                "chr1\t1\tC\tF\tFoo\tBar"};
        int[] splitColumns = {2,3};

        performTest(input, output, splitColumns, 6, ",", "");
    }

    @Test
    public void testBasicSplitTwoColumnsWithExtraColumnsWithCutoff() {
        String[] input = {"chr1\t1\tA,B,C\tD,E,F\tFoo\tBar"};
        String[] output = {
                "chr1\t1\tA\tD\tFoo",
                "chr1\t1\tB\tE\tFoo",
                "chr1\t1\tC\tF\tFoo"};
        int[] splitColumns = {2,3};

        performTest(input, output, splitColumns, 5, ",", "");
    }

    @Test (expected = Exception.class)
    public void testBasicSplitTwoColumnsWithExtraColumnsExceedingSplits() {
        String[] input = {"chr1\t1\tA,B,C\tD,E,F\tFoo\tBar"};
        String[] output = {};
        int[] splitColumns = {2,3};

        performTest(input, output, splitColumns, 10, ",", "");
    }

    private void performTest(String[] input, String[] output, int[] splitColumns, int totalNumberOfColumns, String separator, String emptyValue) {
        MultiColumnSplit analysis = new MultiColumnSplit(totalNumberOfColumns, splitColumns, separator, emptyValue);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
