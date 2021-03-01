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

import gorsat.Analysis.ColumnSplit;
import org.junit.Test;

public class UTestColumnSplit {

    @Test
    public void testBasicSplit() {
        String[] input = {"chr1\t1\tA,B,C"};
        String[] output = {
                "chr1\t1\tA",
                "chr1\t1\tB",
                "chr1\t1\tC"};

        performTest(input, output, 3, ",");
    }

    @Test
    public void testBasicSplitWithIcelandicChars() {
        String[] input = {"chr1\t1\tA,B,C\tÞetta er próf áéíúýö"};
        String[] output = {
                "chr1\t1\tA\tÞetta er próf áéíúýö",
                "chr1\t1\tB\tÞetta er próf áéíúýö",
                "chr1\t1\tC\tÞetta er próf áéíúýö"};

        performTest(input, output, 4, ",");
    }

    @Test
    public void testBasicSplitDifferentSeparator() {
        String[] input = {"chr1\t1\tA:B:C"};
        String[] output = {
                "chr1\t1\tA",
                "chr1\t1\tB",
                "chr1\t1\tC"};

        performTest(input, output, 3, ":");
    }

    @Test
    public void testCplitWithExtraRightSideColumns() {
        String[] input = {"chr1\t1\tA,B,C\tFoo\tBar"};
        String[] output = {
                "chr1\t1\tA\tFoo\tBar",
                "chr1\t1\tB\tFoo\tBar",
                "chr1\t1\tC\tFoo\tBar"};

        performTest(input, output, 5, ",");
    }

    @Test
    public void testCplitWithExtraRightSideColumnsSkipLastColumn() {
        String[] input = {"chr1\t1\tA,B,C\tFoo\tBar"};
        String[] output = {
                "chr1\t1\tA\tFoo",
                "chr1\t1\tB\tFoo",
                "chr1\t1\tC\tFoo"};

        performTest(input, output, 4, ",");
    }

    @Test
    public void splitShouldMaintainEmptyColumnAtEnd() {
        String[] input = {"chr1\t1\tA,B,C\t"};
        String[] output = {
                "chr1\t1\tA\t",
                "chr1\t1\tB\t",
                "chr1\t1\tC\t"};

        performTest(input, output, 4, ",");
    }

    private void performTest(String[] input, String[] output, int totalNumberOfColumns, String separator) {
        ColumnSplit analysis = new ColumnSplit(totalNumberOfColumns, 2, separator);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
