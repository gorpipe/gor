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

public class UTestListNth {
    @Test
    public void givenEmptyListReturnsEmptyString() {
        TestUtils.assertCalculated("listnth('', 100)", "");
    }

    @Test
    public void givenSingleItemListWhenRequestingFirstItemReturnsItem() {
        TestUtils.assertCalculated("listnth('one', 1)", "one");
    }

    @Test
    public void givenSingleItemListWhenRequestingOutOfBoundsReturnsEmptyString() {
        TestUtils.assertCalculated("listnth('one', 10)", "");
    }

    @Test
    public void givenMultipleItemListWhenRequesting3rdItemReturnsItem() {
        TestUtils.assertCalculated("listnth('one,two,three,four,five', 3)", "three");
    }

    @Test
    public void givenItemListShorterThanNReturnsEmptyString() {
        TestUtils.assertCalculated("listnth('one,two,three,four,five', 8)", "");
    }
}
