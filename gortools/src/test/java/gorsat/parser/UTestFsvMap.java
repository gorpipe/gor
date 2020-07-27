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
import org.gorpipe.exceptions.GorDataException;
import org.junit.Ignore;
import org.junit.Test;

public class UTestFsvMap {
    @Test
    public void givenSingleItemListWhenSizeIsOneReturnsMapping() {
        TestUtils.assertCalculated("fsvmap('A',1,'x+x+x','')", "AAA");
    }

    @Test
    public void givenMultipleItemsListWhenSizeIsOneReturnsMapping() {
        TestUtils.assertCalculated("fsvmap('12345',1,'x+\"a\"',',')", "1a,2a,3a,4a,5a");
    }

    @Test
    public void givenMultipleItemsListWhenSizeIsThreeReturnsMapping() {
        TestUtils.assertCalculated("fsvmap('onetwothr',3,'\\'item \\'+x',',')", "item one,item two,item thr");
    }

    @Test
    public void givenMultipleItemsListWhenSizeIsThreeAndEmptyDelimiterReturnsMapping() {
        TestUtils.assertCalculated("fsvmap('onetwothr',3,'\\'item \\'+x',',')", "item one,item two,item thr");
    }

    @Test(expected = GorDataException.class)
    @Ignore
    // This test is sometimes failing on the build server, looks like the GorDataException isn't always thrown.
    // I have a feeling this is more to do with the test environment than anything else, at least I can't reproduce
    // it locally.
    public void givenMultipleItemsWhenItemSizeDoesntMatchListSize() {
        TestUtils.assertCalculated("fsvmap('onetwothree',3,'Chromo+x','')", "chr1onechr1twochr1thr");
    }
}
