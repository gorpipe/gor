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

public class UTestColType {
    @Test
    public void chromPosOnly() {
        String query = "gorrow 1,2 | coltype";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\n" +
                "chr1\t2\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void multipleColumns() {
        String query = "gorrow 1,2 | calc x 42 | calc y 3.14 | calc s 'bingo' | coltype";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\tx\ty\ts\n" +
                "chr1\t2\tI(42)\tD(3.14)\tS(bingo)\n";
        Assert.assertEquals(expected, res);
    }
}
