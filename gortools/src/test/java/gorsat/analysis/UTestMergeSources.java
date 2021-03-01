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

import gorsat.Analysis.MergeSources;
import gorsat.Iterators.RowArrayIterator;
import org.junit.Test;

public class UTestMergeSources {

    @Test
    public void mergeTwoSimpleStreams() {
        String[] sourceInput = {
                "chr1\t1",
                "chr1\t10",
                "chr2\t1",
                "chr3\t1"};

        String[] mergeInput = {
                "chr1\t1",
                "chr1\t5",
                "chr1\t15",
                "chr2\t10"};

        String[] result = {
                sourceInput[0],
                mergeInput[0],
                mergeInput[1],
                sourceInput[1],
                mergeInput[2],
                sourceInput[2],
                mergeInput[3],
                sourceInput[3]};

        performTest(sourceInput, mergeInput, result, false, null, null, true);
    }

    @Test
    public void mergeTwoSourcesWithSourceColumn() {
        String[] sourceInput = {
                "chr1\t1",
                "chr1\t10",
                "chr2\t1",
                "chr3\t1"};

        String[] mergeInput = {
                "chr1\t1",
                "chr1\t5",
                "chr1\t15",
                "chr2\t10"};

        String[] result = {
                sourceInput[0] + "\tL",
                mergeInput[0] + "\tR",
                mergeInput[1] + "\tR",
                sourceInput[1] + "\tL",
                mergeInput[2] + "\tR",
                sourceInput[2] + "\tL",
                mergeInput[3] + "\tR",
                sourceInput[3] + "\tL"};

        performTest(sourceInput, mergeInput, result, true, null, null, true);
    }

    @Test
    public void mergeTwoSimpleStreamsAdditionalColumnsWhichAreSame() {
        String[] sourceInput = {
                "chr1\t1\tA\t1",
                "chr1\t10\tB\t2",
                "chr2\t1\tC\t1",
                "chr3\t1\tD\t1"};

        String[] mergeInput = {
                "chr1\t1\tE\t1",
                "chr1\t5\tF\t2",
                "chr1\t15\tG\t1",
                "chr2\t10\tH\t1"};

        String[] result = {
                sourceInput[0],
                mergeInput[0],
                mergeInput[1],
                sourceInput[1],
                mergeInput[2],
                sourceInput[2],
                mergeInput[3],
                sourceInput[3]};

        performTest(sourceInput, mergeInput, result, false, null, null, true);
    }

    @Test
    public void mergeTwoSimpleStreamsAdditionalColumnsNotTheSame() {
        String[] sourceInput = {
                "chr1\t1\tA\t1",
                "chr1\t10\tB\t2",
                "chr2\t1\tC\t1",
                "chr3\t1\tD\t1"};

        String[] mergeInput = {
                "chr1\t1\tE\t1",
                "chr1\t5\tF\t2",
                "chr1\t15\tG\t1",
                "chr2\t10\tH\t1"};

        String[] result = {
                sourceInput[0],
                "chr1\t1\tE",
                "chr1\t5\tF",
                sourceInput[1],
                "chr1\t15\tG",
                sourceInput[2],
                "chr2\t10\tH",
                sourceInput[3]};

        int[] leftColumns = {0,1,2,3};
        int[] rightColumns = {0,1,2};

        performTest(sourceInput, mergeInput, result, false, leftColumns, rightColumns, false);
    }

    private void performTest(String[] sourceInput, String[] mergeInput, String[] result, boolean addSourceColumn, int[] leftColumns, int[] rightColumns, boolean columnsAreSame) {
        MergeSources analysis = new MergeSources(RowArrayIterator.apply(mergeInput), "missing", addSourceColumn, leftColumns, rightColumns, columnsAreSame, null);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, sourceInput, result);
    }
}
