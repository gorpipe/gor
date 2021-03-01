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

package gorsat.parser;

import gorsat.TestUtils;
import org.junit.Test;

public class UTestListZip {
    @Test
    public void givenTwoEqualLengthListsReturnsZippedList() {
        TestUtils.assertCalculated("listzip('a,b,c', '1,2,3')", "a;1,b;2,c;3");
    }

    @Test
    public void givenTwoListsWhereSecondListIsShorterReturnsZippedList() {
        TestUtils.assertCalculated("listzip('1,2,3','A,B')", "1;A,2;B");
    }

    @Test
    public void givenTwoListsWhereFirstListIsShorterReturnsZippedList() {
        TestUtils.assertCalculated("listzip('one,two,three','1,2,3,4,5')", "one;1,two;2,three;3");
    }

    @Test
    public void givenTwoListsWithCustomSingleCharDelimiterReturnsZippedList() {
        TestUtils.assertCalculated("listzip('one:two:three', '1:2:3:4:5', ':', '|')", "one|1:two|2:three|3");
    }

    @Test
    public void givenTwoListsWithCustomMultiCharDelimiterReturnsZippedList() {
        TestUtils.assertCalculated("listzip('onexxxtwoxxxthree', '1xxx2xxx3xxx4xxx5', 'xxx', '|')", "one|1xxxtwo|2xxxthree|3");
    }

    @Test
    public void givenTwoListsWithEmptyDelimiterReturnsZippedList() {
        TestUtils.assertCalculated("listzip('onetwothree', '12345', '', '')", "o1n2e3t4w5");
    }
}
