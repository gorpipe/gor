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

import gorsat.Analysis.DistinctRows;
import org.junit.Test;

public class UTestDistinctRows {

    @Test
    public void testAllUniqueRows() {
        String[] input = {
                "chr1\t1\tA",
                "chr1\t1\tB",
                "chr1\t2\tA",
                "chr1\t3\tA",
                "chr11\t1\tA"};

        performTest(input, input);
    }

    @Test
    public void testNonUniqueRows() {
        String[] input = {
                "chr1\t1\tA",
                "chr1\t1\tA",
                "chr1\t2\tA",
                "chr1\t2\tA",
                "chr11\t1\tA"};

        String[] output = {
                input[0],
                input[2],
                input[4]};

        performTest(input, output);
    }

    @Test
    public void testLongNonUniequeRows() {
        String[] input = {
                "chr1\t1\tAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\tBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
                "chr1\t1\tAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\tBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
                "chr1\t1\tAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\tBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBC"};

        String[] output = {
                input[0],
                input[2]};

        performTest(input, output);
    }

    private void performTest(String[] input, String[] output) {
        DistinctRows analysis = new DistinctRows();
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
