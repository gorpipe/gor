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

package gorsat.parser;

import gorsat.TestUtils;
import org.junit.Test;

public class UTestListSecond {
    @Test
    public void givenTwoItemListReturnsSecondItem() {
        TestUtils.assertCalculated("listsecond('A,B')", "B");
    }

    @Test
    public void givenTwoItemListWithCustomDelimiterReturnsSecondItem() {
        TestUtils.assertCalculated("listsecond('A::B', '::')", "B");
    }

    @Test
    public void givenMultipleSingleDigitItemListReturnsSecondItem() {
        TestUtils.assertCalculated("listsecond('1,2,3,4,5')", "2");
    }

    @Test
    public void givenMultipleTextItemsReturnsSecondItem() {
        TestUtils.assertCalculated("listsecond('one,two,three')", "two");
    }

    @Test
    public void givenEmptyListReturnsEmptyString() {
        TestUtils.assertCalculated("listsecond('')", "");
    }

    @Test
    public void givenSingleItemListReturnsEmptyString() {
        TestUtils.assertCalculated("listsecond('one')", "");
    }
}
