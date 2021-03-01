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

import gorsat.Analysis.InRange;
import org.junit.Test;

public class UTestInRange {

    @Test
    public void testAllLinesInRange() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr1\t3",
                "chr1\t4",
                "chr1\t5"};

        InRange analysis = new InRange("chr1", 0, "chr1", 10);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, input);
    }

    @Test
    public void testAllLinesInRangeOverMultipleChromosomes() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr11\t1",
                "chr11\t2",
                "chr3\t1",
                "chr3\t2",
                "chr4\t1",
                "chr4\t2",
                "chr5\t1",
                "chr5\t2"};

        InRange analysis = new InRange("chr1", 0, "chr5", 10);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, input);
    }

    @Test
    public void testAllLinesInRangeFromAtoZ() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr11\t1",
                "chr11\t2",
                "chr3\t1",
                "chr3\t2",
                "chr4\t1",
                "chr4\t2",
                "chr5\t1",
                "chr5\t2",
                "qqq\t1"};

        InRange analysis = new InRange("a", 0, "z", 10);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, input);
    }

    @Test
    public void testSingleChromosomeFilterInRange() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr11\t1",
                "chr11\t2",
                "chr3\t1",
                "chr3\t2",
                "chr4\t1",
                "chr4\t2"};
        String[] output = {
                input[4],
                input[5]};

        InRange analysis = new InRange("chr3", 0, "chr3", 10);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }

    @Test
    public void testSingleChromosomeFilterNotInRange() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr11\t1",
                "chr11\t2",
                "chr3\t1",
                "chr3\t2",
                "chr4\t1",
                "chr4\t2"};
        String[] output = {};

        InRange analysis = new InRange("chr2", 0, "chr2", 10);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }

    @Test
    public void testAtoZChromosomeNotInPositionRange() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr11\t1",
                "chr11\t2",
                "chr3\t1",
                "chr3\t2",
                "chr4\t1",
                "chr4\t2"};

        InRange analysis = new InRange("a", 10, "z", 100);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, input);
    }

    @Test
    public void testSingleChromosomeNotPositionInRange() {
        String[] input = {
                "chr1\t1",
                "chr1\t2",
                "chr11\t1",
                "chr11\t2",
                "chr3\t1",
                "chr3\t2",
                "chr4\t1",
                "chr4\t2"};
        String[] output = {};

        InRange analysis = new InRange("chr11", 10, "chr11", 100);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
