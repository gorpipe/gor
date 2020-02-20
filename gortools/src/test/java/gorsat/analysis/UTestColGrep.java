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

import gorsat.Analysis.ColGrep;
import org.junit.Test;

import java.util.regex.PatternSyntaxException;

public class UTestColGrep {

    private static final String[] INPUT = {
            "chr1\t1\tGroupA\tThis is a text with 'quotes'",
            "chr1\t1\tGroupB\tThis is a text with number 123456"
    };

    private static final int[] NO_COLUMNS = {};

    @Test
    public void testGrepOnEmptyPattern() {
        performTest("", true, NO_COLUMNS, true, false, INPUT);
    }

    @Test(expected = PatternSyntaxException.class)
    public void testGrepInvalidPatern() {
        String[] output = {INPUT[1]};

        performTest("[0-9]{1-6}", true, NO_COLUMNS, true, false, output);
    }

    @Test
    public void testGrepOnGroupACaseInsensitive() {
        String[] output = {INPUT[0]};

        performTest("groupa", true, NO_COLUMNS, true, false, output);

    }

    @Test
    public void testGrepOnGroupACaseInsensitiveInverted() {
        String[] output = {INPUT[1]};

        performTest("groupa", true, NO_COLUMNS, true, true, output);

    }

    @Test
    public void testGrepOnGroupACaseSensitive() {
        String[] output = {INPUT[0]};

        performTest("GroupA", true, NO_COLUMNS, false, false, output);

    }

    @Test
    public void testGrepOnGroupAUpperCaseCaseSensitive() {
        String[] output = {};

        performTest("GROUPA", true, NO_COLUMNS, false, false, output);

    }

    @Test
    public void testGrepNonMatchingPattern() {
        String[] output = {};

        performTest("foo", true, NO_COLUMNS, true, false, output);
    }

    @Test
    public void testGrepMatchPatternInSpecificColumn() {
        String[] output = {INPUT[1]};
        int[] columns = {3};

        performTest("123456", false, columns, true, false, output);
    }

    @Test
    public void testGrepMatchPatternInSpecificColumnInverted() {
        String[] output = {INPUT[0]};
        int[] columns = {3};

        performTest("123456", false, columns, true, true, output);
    }

    @Test
    public void testGrepNoMatchingPatternInSpecificColumn() {
        String[] output = {};
        int[] columns = {2};

        performTest("123456", false, columns, true, false, output);
    }

    @Test
    public void testGrepFromFullRowColumnsShouldNorIntervene() {
        String[] output = {INPUT[1]};
        int[] columns = {2};

        performTest("123456", true, columns, true, false, output);
    }

    @Test
    public void testGrepNumber() {
        String[] output = {INPUT[1]};

        performTest("[0-9]{6}", true, NO_COLUMNS, true, false, output);
    }

    @Test
    public void testGrepNumberInverted() {
        String[] output = {INPUT[0]};

        performTest("[0-9]{6}", true, NO_COLUMNS, true, true, output);
    }

    private void performTest(String pattern, boolean useAllCols, int[] columns, boolean caseInsensitive, boolean inverted, String[] result) {
        ColGrep analysis = new ColGrep(pattern, useAllCols, columns, caseInsensitive, inverted);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, INPUT, result);
    }
}
