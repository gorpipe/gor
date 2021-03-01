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

import gorsat.Analysis.ChopSegment;
import org.junit.Test;

public class UTestChopSegment {

    @Test
    public void testSingleRow1000basesChoppedTo100Bases() {
        String[] input = {"chr1\t1\t1000"};
        String[] output = {"chr1\t1\t63",
            "chr1\t63\t125",
            "chr1\t125\t187",
            "chr1\t187\t250",
            "chr1\t250\t312",
            "chr1\t312\t375",
            "chr1\t375\t437",
            "chr1\t437\t500",
            "chr1\t500\t562",
            "chr1\t562\t625",
            "chr1\t625\t687",
            "chr1\t687\t750",
            "chr1\t750\t812",
            "chr1\t812\t875",
            "chr1\t875\t937",
            "chr1\t937\t1000"};
        ChopSegment analysis = new ChopSegment(100);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }

    @Test
    public void testSegmentSizeCoversRange() {
        String[] input = {"chr1\t1\t1000"};
        String[] output = {"chr1\t1\t1000"};
        ChopSegment analysis = new ChopSegment(10000);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
