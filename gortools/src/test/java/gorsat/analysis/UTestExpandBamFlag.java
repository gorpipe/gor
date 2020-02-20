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

import gorsat.Analysis.ExpandBamFlag;
import org.junit.Test;

public class UTestExpandBamFlag {
    @Test(expected=NumberFormatException.class)
    public void testExpandingANonNumericValue() {
        String[] input = {"chr1\t1\tA"};
        String[] output = {};

        performTest(input, output);
    }

    private void performTest(String[] input, String[] output) {
        ExpandBamFlag analysis = new ExpandBamFlag(2);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }

    @Test
    public void testExpandingAZeroValue() {
        String[] input = {"chr1\t1\t0"};
        String[] output = {"chr1\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0"};

        performTest(input, output);
    }

    @Test
    public void testExpandingEveryOtherOnes() {
        short number = 0b101010101010101;
        String[] input = {"chr1\t1\t" + number};
        String[] output = {"chr1\t1\t" + number + "\t1\t0\t1\t0\t1\t0\t1\t0\t1\t0\t1"};

        performTest(input, output);
    }

    @Test
    public void testExpandingAllOnes() {
        short number = 0b111111111111111;
        String[] input = {"chr1\t1\t" + number + "\tA\tB"};
        String[] output = {"chr1\t1\t" + number + "\tA\tB\t1\t1\t1\t1\t1\t1\t1\t1\t1\t1\t1"};

        performTest(input, output);
    }
}
