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

import com.google.common.primitives.Ints;
import gorsat.Analysis.AtAnalysis;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UTestAtAnalysisNumber {

    static final String HEADER = "chrom\tpos\tnumber\tgroup";
    private static final String[] INPUT_DATA_NUMBER = {
            "chr1\t1\t1\tA",
            "chr1\t2\t1\tB",
            "chr1\t3\t1\tC",
            "chr1\t4\t2\tA",
            "chr1\t5\t2\tB",
            "chr1\t6\t3\tC",
            "chr2\t1\t3\tA",
            "chr2\t2\t3\tB",
            "chr2\t3\t4\tC",
            "chr2\t4\t4\tA",
            "chr2\t5\t5\tB",
            "chr2\t6\t5\tC"};

    private static final String[] INPUT_DATA_STRING = {
            "chr1\t1\ta\tA",
            "chr1\t2\ta\tB",
            "chr1\t3\ta\tC",
            "chr1\t4\tb\tA",
            "chr1\t5\tb\tB",
            "chr1\t6\tc\tC",
            "chr2\t1\tc\tA",
            "chr2\t2\tc\tB",
            "chr2\t3\td\tC",
            "chr2\t4\td\tA",
            "chr2\t5\te\tB",
            "chr2\t6\te\tC"};

    private class TestRun {

        private String binSize;
        private int testColumn;
        private int[] group;
        private AtAnalysis.Parameters parameters;
        private String[] result;

        TestRun(String binSize,
                int testColumn,
                int[] group,
                AtAnalysis.Parameters parameters,
                String[] result) {
            this.binSize = binSize;
            this.testColumn = testColumn;
            this.group = group;
            this.parameters = parameters;
            this.result = result;
        }
    }

    @Test
    public void testAtMinBinSize1NoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("1",2, groups(),
                createParameters(false, false), INPUT_DATA_NUMBER));
    }

    @Test
    public void testAtMaxBinSize1NoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("1",2, groups(),
                createParameters(true, false), INPUT_DATA_NUMBER));
    }

    @Test
    public void testAtMinBinSize1NoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("1",2, groups(),
                createParameters(false, true), INPUT_DATA_NUMBER));
    }

    @Test
    public void testAtMaxBinSize1NoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("1",2, groups(),
                createParameters(true, true), INPUT_DATA_NUMBER));
    }

    @Test
    public void testAtMinBinSize10NoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("10",2, groups(),
                createParameters(false, false), generateResult(0,6)));
    }

    @Test
    public void testAtMaxBinSize10NoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("10",2, groups(),
                createParameters(true, false), generateResult(5,10)));
    }

    @Test
    public void testAtMinBinSize10NoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("10",2, groups(),
                createParameters(false, true), generateResult(2,7)));
    }

    @Test
    public void testAtMaxBinSize10NoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("10",2, groups(),
                createParameters(true, true), generateResult(5,11)));
    }

    @Test
    public void testAtMinBinSizeChrNoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(),
                createParameters(false, false), generateResult(0,6)));
    }

    @Test
    public void testAtMaxBinSizeChrNoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(),
                createParameters(true, false), generateResult(5,10)));
    }

    @Test
    public void testAtMinBinSizeChrNoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(),
                createParameters(false, true), generateResult(2,7)));
    }

    @Test
    public void testAtMaxBinSizeChrNoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(),
                createParameters(true, true), generateResult(5,11)));
    }

    @Test
    public void testAtMinBinSizeGenomeNoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(),
                createParameters(false, false), generateResult(0)));
    }

    @Test
    public void testAtMaxBinSizeGenomeNoGroupUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(),
                createParameters(true, false), generateResult(10)));
    }

    @Test
    public void testAtMinBinSizeGenomeNoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(),
                createParameters(false, true), generateResult(2)));
    }

    @Test
    public void testAtMaxBinSizeGenomeNoGroupUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(),
                createParameters(true, true), generateResult(11)));
    }

    @Test
    public void testAtMinBinSizeChrGroupingOnSingleColumnUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(3),
                createParameters(false, false), generateResult(0, 1, 2, 6, 7, 8)));
    }

    @Test
    public void testAtMaxBinSizeChrGroupingOnSingleColumnUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(3),
                createParameters(true, false), generateResult(3, 4, 5, 9, 10, 11)));
    }

    @Test
    public void testAtMinBinSizeChrGroupingOnSingleColumnUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(3),
                createParameters(false, true), generateResult(0, 1, 2, 6, 7, 8)));
    }

    @Test
    public void testAtMaxBinSizeChrGroupingOnSingleColumnUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("chr",2, groups(3),
                createParameters(true, true), generateResult(3, 4, 5, 9, 10, 11)));
    }

    @Test
    public void testAtMinBinSizeGenomeGroupingOnSingleColumnUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(3),
                createParameters(false, false), generateResult(0, 1, 2)));
    }

    @Test
    public void testAtMaxBinSizeGenomeGroupingOnSingleColumnUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(3),
                createParameters(true, false), generateResult(9, 10, 11)));
    }

    @Test
    public void testAtMinBinSizeGenomeGroupingOnSingleColumnUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(3),
                createParameters(false, true), generateResult(0, 1, 2)));
    }

    @Test
    public void testAtMaxBinSizeGenomeGroupingOnSingleColumnUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(3),
                createParameters(true, true), generateResult(9, 10, 11)));
    }

    @Test
    public void testAtMinBinSizeGenomeGroupingOnMultipleColumnsUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(0,3),
                createParameters(false, false), generateResult(0, 1, 2, 6,7,8)));
    }

    @Test
    public void testAtMaxBinSizeGenomeGroupingOnMultipleColumnsUseFirst() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(0,3),
                createParameters(true, false), generateResult(3,4,5, 9, 10, 11)));
    }

    @Test
    public void testAtMinBinSizeGenomeGroupingOnMultipleColumnsUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(0,3),
                createParameters(false, true), generateResult(0, 1, 2, 6,7,8)));
    }

    @Test
    public void testAtMaxBinSizeGenomeGroupingOnMultipleColumnsUseLast() {
        performTest(INPUT_DATA_NUMBER, new TestRun("gen",2, groups(0,3),
                createParameters(true, true), generateResult(3,4,5,9, 10, 11)));
    }




    private void performTest(String[] data, TestRun testRun) {
        gorsat.Commands.Analysis analysis;

        if (testRun.binSize.equals("chr")) {
            AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
            analysis = new AtAnalysis.ChromAt(sessionFactory.create(), testRun.testColumn, testRun.group, testRun.parameters);
        } else if (testRun.binSize.equals("gen")) {
            analysis = new AtAnalysis.GenomeAt(testRun.testColumn, testRun.group, testRun.parameters);
        } else {
            analysis = new AtAnalysis.At(Integer.parseInt(testRun.binSize), testRun.testColumn, testRun.group, testRun.parameters);
        }

        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, data, testRun.result);
    }

    private AtAnalysis.Parameters createParameters(boolean useMax, boolean useLast, boolean compareString) {
        AtAnalysis.Parameters parameters = new AtAnalysis.Parameters();
        parameters.useLast_$eq(useLast);
        parameters.useMax_$eq(useMax);
        parameters.compareString_$eq(compareString);

        return parameters;
    }

    private AtAnalysis.Parameters createParameters(boolean useMax, boolean useLast) {
        return createParameters(useMax, useLast, false);
    }

    private String[] generateResult(int... indices) {
        List<String> list = new ArrayList<>();

        for (Integer index : indices) {
            list.add(INPUT_DATA_NUMBER[index]);
        }

        return list.toArray(new String[0]);
    }

    private int[] groups(int... indices) {
        List<Integer> list = new ArrayList<>();

        for (Integer index : indices) {
            list.add(index);
        }

        return Ints.toArray(list);
    }
}
