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

package org.gorpipe.model.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class UTestGChiSquared2by2 {

    @Test
    public void computePearsonChiSquared() {
        final double v = GChiSquared2by2.computePearsonChiSquared(473, 1257, 2206, 384);
        Assert.assertEquals(1470.181685015177, v, 1e-8);
    }

    @Test
    public void computeLogLikelihoodChiSquared() {
        final double v = GChiSquared2by2.computeLogLikelihoodChiSquared(473, 1257, 2206, 384);
        Assert.assertEquals(999.9, v, 1e-8);
    }

    @Test
    public void getPValue() {
        final double v = GChiSquared2by2.getPValue(1470.181685015177);
        Assert.assertEquals(0.0, v, 1e-8);
    }

    @Test
    public void getChiSqValue() {
        Assert.assertTrue(Double.isInfinite(GChiSquared2by2.getChiSqValue(0.0)));
        Assert.assertTrue(Double.isInfinite(GChiSquared2by2.getChiSqValue(Double.MIN_VALUE)));
        Assert.assertTrue(Double.isNaN(GChiSquared2by2.getChiSqValue(-1.0)));
        Assert.assertEquals(0.0, GChiSquared2by2.getChiSqValue(1.0), 1e-8);
    }

    @Test
    public void getFiniteChiSqValue() {
        final double v1 = GChiSquared2by2.getFiniteChiSqValue(0.95, 473, 1257, 2206, 384);
        Assert.assertEquals(0.003932140000019528, v1, 1e-8);

        final double v2 = GChiSquared2by2.getFiniteChiSqValue(-1.0, 473, 1257, 2206, 384);
        Assert.assertEquals(1470.181685015177, v2, 1e-8);
    }
}