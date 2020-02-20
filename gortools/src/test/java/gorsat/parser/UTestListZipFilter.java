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

public class UTestListZipFilter {
    @Test
    public void singleItemLists() {
        TestUtils.assertCalculated("listzipfilter('A', 'B', 'x!=\\'A\\'')", "A");
    }

    @Test
    public void typicalCommaSeparatedLists() {
        TestUtils.assertCalculated("listzipfilter('1,2,3,4,5','5,4,3,2,1','int(x)>2')", "1,2,3");
    }

    @Test
    public void indexInCondition() {
        TestUtils.assertCalculated("listzipfilter('1,2,3,4,5','5,4,3,2,1','i>2')", "3,4,5");
    }

    @Test
    public void conditionAlwaysFails() {
        TestUtils.assertCalculated("listzipfilter('1,2,3,4,5','5,4,3,2,1','i>2')", "3,4,5");
    }

    @Test
    public void conditionListIsShorterThanDataList() {
        TestUtils.assertCalculated("listzipfilter('1,2,3,4,5','5,4','int(x)>2')", "1,2");
    }

    @Test
    public void conditionListIsLongerThanDataList() {
        TestUtils.assertCalculated("listzipfilter('1,2,3,4','5,4,3,2,1','int(x)>2')", "1,2,3");
    }

    @Test
    public void listsWithNoDelimiter() {
        TestUtils.assertCalculated("listzipfilter('12345','54321','int(x)>2', '')", "123");
    }

    @Test
    public void listsWithTwoCharDelimiter() {
        TestUtils.assertCalculated("listzipfilter('1::2::3::4::5','5::4::3::2::1','int(x)>2', '::')", "1::2::3");
    }

    @Test
    public void listsWithTwoCharDelimiterWhereConditionFailsForFirst() {
        TestUtils.assertCalculated("listzipfilter('1::2::3::4::5','5::4::3::2::1','int(x)<3', '::')", "4::5");
    }
}
