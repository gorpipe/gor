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

import gorsat.Analysis.ColSed;
import org.junit.Test;

public class UTestColSed {

    @Test
    public void testNoMatchingPatternAndNoReplacementPattern() {
        String[] input = {"chr1\t1\tFoo\tbar"};
        String[] output = {"chr1\t1\tFoo\tbar"};
        boolean[] replaceColumns = {false, false, false, false};

        performTest(input, output, replaceColumns, "", true, false);
    }

    @Test
    public void testMatchingPatternAndReplacementPatternCaseInsensitive() {
        String[] input = {
                "chr1\t1\tFoo\tbar",
                "chr1\t1\tFoo\tBar"};
        String[] output = {
                "chr1\t1\tFoo\txxx",
                "chr1\t1\tFoo\txxx"};
        boolean[] replaceColumns = {false, false, true, true};

        performTest(input, output, replaceColumns, "bar", true, false);
    }

    @Test
    public void testMatchingPatternAndReplacementPatternCaseSensitive() {
        String[] input = {
                "chr1\t1\tFoo\tbar",
                "chr1\t1\tFoo\tBar"};
        String[] output = {
                "chr1\t1\tFoo\txxx",
                "chr1\t1\tFoo\tBar"};
        boolean[] replaceColumns = {false, false, true, true};

        performTest(input, output, replaceColumns, "bar", false, false);
    }

    @Test
    public void testReplaceAnyTextInAllColumns() {
        String[] input = {"chr1\t1\tFoo\tbar"};
        String[] output = {"chr1\t1\txxx\txxx"};
        boolean[] replaceColumns = {false, false, true, true};

        performTest(input, output, replaceColumns, ".*", true, false);
    }

    @Test
    public void testReplaceOnlyFirst() {
        String[] input = {"chr1\t1\tFoo\tbar and then there was a bar in a bar"};
        String[] output = {"chr1\t1\txxx\txxx and then there was a bar in a bar"};
        boolean[] replaceColumns = {false, false, true, true};

        performTest(input, output, replaceColumns, "foo|bar", true, false);
    }

    @Test
    public void testReplaceAll() {
        String[] input = {"chr1\t1\tFoo\tbar and then there was a bar in a bar"};
        String[] output = {"chr1\t1\txxx\txxx and then there was a xxx in a xxx"};
        boolean[] replaceColumns = {false, false, true, true};

        performTest(input, output, replaceColumns, "foo|bar", true, true);
    }

    private void performTest(String[] input, String[] output, boolean[] replaceColumns, String matchingPattern, boolean caseInsensitive, boolean replaceAll) {
        ColSed analysis = new ColSed(matchingPattern, "xxx", replaceColumns, caseInsensitive, replaceAll);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
