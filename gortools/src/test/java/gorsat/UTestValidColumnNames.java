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

package gorsat;

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;


public class UTestValidColumnNames {
    @Test
    public void testHeaderNameStartingWithNumber() {
        String[] result = TestUtils.runGorPipeLines("gorrow chr1,1,1 | calc abc 'foo' | calc aabc 'bar' | where abc = aabc");
        Assert.assertEquals(1, result.length);

        result = TestUtils.runGorPipeLines("gorrow chr1,1,1 | calc abc 'foo' | calc 1abc 'bar' | where abc = 1abc");
        Assert.assertEquals(1, result.length);
    }

    @Test
    public void testHeaderNameAsNumber() {
        try {
            TestUtils.runGorPipeLines("gorrow chr1,1,1 | calc abc 'foo' | calc 111 'bar' | where abc = 111");
            Assert.fail();
        } catch (GorParsingException ex) {
            // This should happen
        }
    }
}
