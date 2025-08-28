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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 04/12/2016.
 */
public class UTestVarnorm {
    @Test
    public void testVarnorm() {
        String query = "gor -p chr2 ../tests/data/gor/dbsnp_test.gorz | calc oldpos pos | calc oldref #3 | calc oldalt #4 | varnorm #3 #4 -right | calc oldposx pos | calc oldrefx #3 | calc oldaltx #4 | varnorm #3 #4 -left | where gtshare(chrom,pos,#3,#4,oldpos,oldref,oldalt) = 0 or gtshare(chrom,oldpos,oldref,oldalt,oldposx,oldrefx,oldaltx)=0 or gtshare(chrom,pos,#3,#4,oldposx,oldrefx,oldaltx)=0 | rownum | throwif rownum = 1";
        Assert.assertEquals(0, TestUtils.runGorPipeCount(query));
    }
}