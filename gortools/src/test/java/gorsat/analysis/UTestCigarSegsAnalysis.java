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

import org.gorpipe.exceptions.GorDataException;
import gorsat.Analysis.CigarSegsAnalysis;
import org.junit.Test;

public class UTestCigarSegsAnalysis {

    private static final int[] NO_GROUPING = {};

    // CigarSegs will split the input row based on ther cigar column. For each M, = or X the column
    // is split. The last column, the id is the count of splits from start of input data.
    // The gc options is not grouping as indicated but rather wich columns should be passed through.

    @Test
    public void testModificationCigarSegNoGrouping() {
        String[] input = {"chr1\t1\t2M"};
        String[] output = {"chr1\t0\t2\t1"};

        performTest(input, output, NO_GROUPING);
    }

    @Test(expected = GorDataException.class)
    public void testModificationInvalidCigarSegNoGrouping() {
        String[] input = {"chr1\t1\t2Y5J8K"};
        String[] output = {};

        performTest(input, output, NO_GROUPING);
    }

    @Test(expected = GorDataException.class)
    public void testModificationNoSizeNoGrouping() {
        String[] input = {"chr1\t1\t10"};
        String[] output = {};

        performTest(input, output, NO_GROUPING);
    }

    @Test
    public void testModificationDeleteCigarSegNoGrouping() {
        String[] input = {"chr1\t1\t2M2D"};
        String[] output = {"chr1\t0\t2\t1"};

        performTest(input, output, NO_GROUPING);
    }

    @Test
    public void testModificationDeleteModificationCigarSegNoGrouping() {
        String[] input = {"chr1\t1\t2M2D2M"};
        String[] output = {
                "chr1\t0\t2\t1",
                "chr1\t4\t6\t3"};

        performTest(input, output, NO_GROUPING);
    }

    @Test
    public void testAllOptionsCigarSegNoGrouping() {
        String[] input = {"chr1\t1\t2M2D2X2N2=2I2M2PH"};
        String[] output = {
                "chr1\t0\t2\t1",
                "chr1\t4\t6\t3",
                "chr1\t8\t10\t5",
                "chr1\t10\t12\t7"};

        performTest(input, output, NO_GROUPING);
    }

    @Test
    public void testActualCigarSegNoGrouping() {
        String[] input = {
                "chr1\t1000\t104M1D46M",
                "chr1\t2000\t32M2D106M12S"};
        String[] output = {
                "chr1\t999\t1103\t1",
                "chr1\t1104\t1150\t3",
                "chr1\t1999\t2031\t4",
                "chr1\t2033\t2139\t6"};

        performTest(input, output, NO_GROUPING);
    }

    @Test
    public void testActualCigarSegWithGrouping() {
        String[] input = {
                "chr1\t1000\t104M1D46M",
                "chr1\t2000\t32M2D106M12S"};
        String[] output = {
                "chr1\t999\t1103\t1\t104M1D46M\tchr1",
                "chr1\t1104\t1150\t3\t104M1D46M\tchr1",
                "chr1\t1999\t2031\t4\t32M2D106M12S\tchr1",
                "chr1\t2033\t2139\t6\t32M2D106M12S\tchr1"};
        int[] groups = {2,0};

        performTest(input, output, groups);
    }

    private void performTest(String[] input, String[] output, int[] groups) {
        CigarSegsAnalysis analysis = new CigarSegsAnalysis(2, groups);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
