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

package gorsat;

import org.junit.Assert;
import org.junit.Test;

public class UTestIOTest {
    @Test
    public void testIoTest() {
        String[] args = new String[]{"gor ../tests/data/gor/genes.gor | top 1 | calc filepath '../tests/data/gor/genes.gor' | calc seeknum 10 | calc streamsize 1000 | iotest"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals(1, count);
    }

    @Test
    public void testIoTestWrite() {
        String[] args = new String[]{"gor ../tests/data/gor/genes.gor | top 1 | calc filepath '../tests/data/gor/genes.gor' | calc seeknum 10 | calc streamsize 1000 | calc write 'true' | iotest"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals(1, count);
    }

    @Test
    public void testParallelIoTest() {
        String[] args = new String[]{"gor ../tests/data/gor/genes.gor | top 1 | calc filepath '../tests/data/gor/genes.gor' | calc seeknum 10 | calc streamsize 1000 | iotest -p"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals(1, count);
    }
}
