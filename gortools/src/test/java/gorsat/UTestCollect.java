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

import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 17/05/2017.
 */
public class UTestCollect {
    @Test
    public void testCollectOnRandom() {
        String query = "norrows 10000 | calc a random() | collect a 1000 -sum -ave -std";
        var lines = TestUtils.runGorPipeLines(query);

        Assert.assertEquals(10001, lines.length);

        var r500 = RowObj.StoR(lines[500]);
        var r4000 = RowObj.StoR(lines[4000]);
        var r9000 = RowObj.StoR(lines[9000]);

        // Row 500
        var sum500 = r500.colAsDouble(4);
        var ave500 = r500.colAsDouble(5);
        var std500 = r500.colAsDouble(6);
        Assert.assertTrue( sum500 > 220.0 && sum500 < 280.0);
        Assert.assertTrue( ave500 > 0.44 && ave500 < 0.56);
        Assert.assertTrue( std500 > 0.24 && std500 < 0.36);

        // Row 4000
        var sum4000 = r4000.colAsDouble(4);
        var ave4000 = r4000.colAsDouble(5);
        var std4000 = r4000.colAsDouble(6);
        Assert.assertTrue( sum4000 > 440.0 && sum4000 < 560.0);
        Assert.assertTrue( ave4000 > 0.44 && ave4000 < 0.56);
        Assert.assertTrue( std4000 > 0.24 && std4000 < 0.36);

        // Row 9000
        var sum9000 = r9000.colAsDouble(4);
        var ave9000 = r9000.colAsDouble(5);
        var std9000 = r9000.colAsDouble(6);
        Assert.assertTrue( sum9000 > 440.0 && sum9000 < 560.0);
        Assert.assertTrue( ave9000 > 0.44 && ave9000 < 0.56);
        Assert.assertTrue( std9000 > 0.24 && std9000 < 0.36);
    }

    @Test
    public void testCollectOnIncremental() {
        String query = "norrows 1000 | collect rownum 100 -sum -ave -std";
        var lines = TestUtils.runGorPipeLines(query);

        Assert.assertEquals(1001, lines.length);

        var r50 = RowObj.StoR(lines[50]);
        var r400 = RowObj.StoR(lines[400]);

        // Row 50
        var sum50 = r50.colAsDouble(3);
        var ave50 = r50.colAsDouble(4);
        var std50 = r50.colAsDouble(5);
        Assert.assertEquals(1225, sum50, 0.1);
        Assert.assertEquals(24.5, ave50, 0.1);
        Assert.assertEquals( 14.43, std50, 0.1);

        // Row 400
        var sum400 = r400.colAsDouble(3);
        var ave400 = r400.colAsDouble(4);
        var std400 = r400.colAsDouble(5);
        Assert.assertEquals(34950.0, sum400, 0.1);
        Assert.assertEquals(349.5, ave400, 0.1);
        Assert.assertEquals( 28.44, std400, 0.1);
    }
}
