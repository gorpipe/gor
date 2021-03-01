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

package org.gorpipe.model.util;

import org.gorpipe.gor.util.GFisherExact2by2;
import org.junit.Assert;
import org.junit.Test;

public class UTestGFisherExact2by2 {
    @Test
    public void compute() {
        final double v = GFisherExact2by2.compute(1, 9, 11, 3);
        Assert.assertEquals(0.9999663480953112, v, 1e-8);
    }

    @Test
    public void computeOneTailed() {
        final double v = GFisherExact2by2.computeOneTailed(1, 9, 11, 3);
        Assert.assertEquals(0.001379728092610052, v, 1e-8);
    }

    @Test
    public void computeTwoTailed() {
        final double v = GFisherExact2by2.computeTwoTailed(1, 9, 11, 3);
        Assert.assertEquals(0.0027594561852201044, v, 1e-8);
    }
}