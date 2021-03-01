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

import gorsat.Analysis.TopN;
import org.junit.Test;

public class UTestTopNAnalysis {

    private String inputRows = "chr1\t1\t1\nchr1\t2\t2\nchr1\t3\t3\nchr1\t4\t4";

    @Test
    public void testTop0() {
        AnalysisTestEngine engine = new AnalysisTestEngine();
        String outputRows = "";
        engine.run(new TopN(0), inputRows, outputRows);
    }

    @Test
    public void testTop1() {
        AnalysisTestEngine engine = new AnalysisTestEngine();
        String outputRows = "chr1\t1\t1";
        engine.run(new TopN(1), inputRows, outputRows);
    }

    @Test
    public void testTop10With3Lines() {
        AnalysisTestEngine engine = new AnalysisTestEngine();
        String outputRows = "chr1\t1\t1\nchr1\t2\t2\nchr1\t3\t3\nchr1\t4\t4";
        engine.run(new TopN(10), inputRows, outputRows);
    }
}
