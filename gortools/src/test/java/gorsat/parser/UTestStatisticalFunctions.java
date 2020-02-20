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
import org.junit.Assert;
import org.junit.Test;

public class UTestStatisticalFunctions {
    @Test
    public void testChi() {
        TestUtils.assertCalculated("chi(0,0,0,0)",0.0);
        // todo test with meaningful data
    }

    @Test
    public void testChi2() {
        TestUtils.assertCalculated("chi2(3,3)",0.3916251762710878);
        TestUtils.assertCalculated("chi2(10,1.5)",0.9989353222272143);
        TestUtils.assertCalculated("chi2(5,10.11)",0.07217777195475643);
    }

    @Test
    public void testChiSquare() {
        TestUtils.assertCalculated("chisquare(0,0)",0.0);
        TestUtils.assertCalculated("chisquare(3,3)",0.608374823728911);
        TestUtils.assertCalculated("chisquare(10,1.5)",0.0010646777727857928);
        TestUtils.assertCalculated("chisquare(5,10.11)",0.9278222280452436);
    }

    @Test
    public void testChiSquareCompl() {
        TestUtils.assertCalculated("chiSquareCompl(3,3)",0.3916251762710878);
        TestUtils.assertCalculated("chiSquareCompl(10,1.5)",0.9989353222272143);
        TestUtils.assertCalculated("chiSquareCompl(5,10.11)",0.07217777195475643);
    }

    @Test
    public void testInvChiSquare() {
        for (int i = 0; i < 10; ++i) {
            final int degFree = (int) (10 * Math.random()) + 1;
            final double varValue = 1 + 10 * Math.random(); //The invChiSquare function seems to numerically unstable for small and large values.
            TestUtils.assertCalculated("invChiSquare(chiSquare(" + degFree + ", " + varValue + "), " + degFree + ")", varValue);
        }
    }

    @Test
    public void testStudent() {
        TestUtils.assertCalculated("student(1,.5)",0.6475836176504333);
        TestUtils.assertCalculated("student(10,1.5)",0.9177463367772799);
        TestUtils.assertCalculated("student(6,0.75)",0.7591910928439227);
    }

    @Test
    public void testInvStudent() {
        TestUtils.assertCalculated("invStudent(0.5,10)",0.6998121397488263);
        TestUtils.assertCalculated("invStudent(0.95,6)",0.06537400041241631);
        TestUtils.assertCalculated("invStudent(0.975,100)",0.031416552967902044);
    }

    @Test
    public void testNormal() {
        double normalHalf = 0.6914624612740131;
        double normalOne = 0.8413447460685429;
        TestUtils.assertCalculated("normal(0.5)", normalHalf);
        TestUtils.assertCalculated("normal(1)", normalOne);
    }

    @Test
    public void testInvNormal() {
        TestUtils.assertCalculated("InvNormal(0.6914624612740131)",0.5);
        TestUtils.assertCalculated("InvNormal(0.8413447460685429)",0.9999999999999998);
    }

    private static double Poisson(int n, double lambda) {
        double result = 1, pow = 1;
        int fact = 1;
        for (int i = 1; i <= n; ++i) {
            result += (pow *= lambda) / (fact *= i);
        }
        return result * Math.exp(-lambda);
    }

    @Test
    public void testPoisson() {
        for (int i = 0; i < 3; ++i) {
            final double lambda = 10 * Math.random();
            final int n = (int) (10 * Math.random());
            final String result = TestUtils.runGorPipe("gor 1.mem | select 1,2 | top 1 | calc NEWCOL poisson(" + n + "," + lambda + ") | top 1");
            final double poissonVal = Double.parseDouble(result.substring(result.lastIndexOf("\t") + 1));
            final double correctPoissonVal = Poisson(n, lambda);
            Assert.assertTrue(Math.abs(poissonVal - correctPoissonVal) < 1.0E-9);
        }
    }

    @Test
    public void testPoissonC() {
        for (int i = 0; i < 3; ++i) {
            final double lambda = 10 * Math.random();
            final int n = (int) (10 * Math.random());
            final String result = TestUtils.runGorPipe("gor 1.mem | select 1,2 | top 1 | calc NEWCOL poissonc(" + n + "," + lambda + ") | top 1");
            final double poissonVal = Double.parseDouble(result.substring(result.lastIndexOf("\t") + 1));
            final double correctPoissonVal = 1 - Poisson(n, lambda);
            Assert.assertTrue(Math.abs(poissonVal - correctPoissonVal) < 1.0E-9);
        }
    }

    @Test
    public void testPval() {
        TestUtils.assertCalculated("pval(0,0,0,0)",1.0);
        // todo test with meaningful data
    }

    @Test
    public void testPvalOne() {
        TestUtils.assertCalculated("pvalone(0,0,0,0)",1.0);
        // todo test with meaningful data
    }

    // The following tests aren't really testing the functionality - they use an external package
    // and we assume it does the right thing. The tests do assure us the functions can be called.

    @Test
    public void testBeta() {
        TestUtils.assertCalculated("beta(1, 1, 1)",1.0);
    }

    @Test
    public void testBetaC() {
        TestUtils.assertCalculated("betac(1, 1, 1)",1.0);
    }

    @Test
    public void testGamma() {
        TestUtils.assertCalculated("gamma(1, 1, 1)",0.6321205588285578);
    }

    @Test
    public void testGammaC() {
        TestUtils.assertCalculated("gammac(1, 1, 1)",0.36787944117144233);
    }

    @Test
    public void testBinomial() {
        TestUtils.assertCalculated("binomial(1, 1, 1)",1.0);
    }

    @Test
    public void testBinomialC() {
        TestUtils.assertCalculated("binomialc(1, 1, 1)",0.0);
    }

    @Test
    public void testNegBinomial() {
        TestUtils.assertCalculated("negbinomial(1, 1, 1)",1.0);
    }

    @Test
    public void testNegBinomialC() {
        TestUtils.assertCalculated("negbinomialc(1, 1, 1)",0.0);
    }

    @Test
    public void testErf() {
        TestUtils.assertCalculated("erf(0)",0.0);
        TestUtils.assertCalculated("erf(0.5)",0.5204998778130465);
    }

    @Test
    public void testErfC() {
        TestUtils.assertCalculated("erfc(0)",1.0);
        TestUtils.assertCalculated("erfc(0.5)",0.4795001221869535);
    }
}
