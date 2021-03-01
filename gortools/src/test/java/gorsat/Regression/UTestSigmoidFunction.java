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

package gorsat.Regression;

import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class UTestSigmoidFunction {
    @Test
    public void testSigmoidFunction() {
        final SigmoidFunction sf = SigmoidFunction.getSigmoidFunction();
        final Random r = new Random();
        final double maxError = r.doubles(10000).map(x -> 100 * x - 50)
                .map(x -> Math.abs(sf.sigmoid(x) - 1 / (1 + FastMath.exp(-x)))).max().getAsDouble();
        Assert.assertEquals(0, maxError, 1e-10);
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        SigmoidFunction[] sfs = new SigmoidFunction[2];
        final Thread t1 = new Thread(() -> sfs[0] = SigmoidFunction.getSigmoidFunction());
        final Thread t2 = new Thread(() -> sfs[1] = SigmoidFunction.getSigmoidFunction());
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assert.assertSame(sfs[0], sfs[1]);
    }

    @Test
    public void testReuse() {
        final SigmoidFunction sf1 = SigmoidFunction.getSigmoidFunction();
        final SigmoidFunction sf2 = SigmoidFunction.getSigmoidFunction();
        Assert.assertSame(sf1, sf2);
    }
}
