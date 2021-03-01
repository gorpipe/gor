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

package gorsat.process;

import cern.jet.stat.Probability;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static gorsat.process.StatisticalAdjustment.*;

public class UTestStatisticalAdjustment {
    private final static double[] zeros = {0, 0, 0, 0, 0};
    private final static double[] ones = {1, 1, 1, 1, 1};
    private final static double[] p1 = {0.00017, 0.00146, 0.00191, 0.00549, 0.00761, 0.02960, 0.03758, 0.04329, 0.04947, 0.08747, 0.09710, 0.12406, 0.39329, 0.50489, 0.54049, 0.59839, 0.67695, 0.68907, 0.71670, 0.99819};
    private final static double[] p2 = {0.00001, 0.00009, 0.00070, 0.00187, 0.00193, 0.00334, 0.02226, 0.02953, 0.05390, 0.08870, 0.14441, 0.16196, 0.21637, 0.25317, 0.43925, 0.68289, 0.74283, 0.88603, 0.89843, 0.92992};
    private final static double[] p3 = {0.00012, 0.00076, 0.00491, 0.00625, 0.00650, 0.00841, 0.01177, 0.03530, 0.04668, 0.07616, 0.07757, 0.21173, 0.29232, 0.30787, 0.36301, 0.36960, 0.42550, 0.50121, 0.61608, 0.89518};

    @Test
    public void testSquared() {
        final int rounds = 1_000;
        final int upperBound = 100_000;
        final Random r = new Random();
        for (int i = 0; i < rounds; ++i) {
            final double a = upperBound * r.nextDouble();
            Assert.assertEquals(a * a, squared(a), 0);
        }
    }

    @Test
    public void testHarmonic_general() {
        final int upTo = 100;
        double sum = 0;
        for (int n = 1; n < upTo; ++n) {
            sum += 1.0 / n;
            Assert.assertEquals(sum, harmonic(n), 1e-14);
        }
    }

    @Test
    public void testHarmonic_illegalArgument() {
        boolean success = false;
        try {
            harmonic(0);
        } catch (IllegalArgumentException e) {
            success = true;
        } catch (Exception e) {}
        Assert.assertTrue(success);
    }

    @Test
    public void testPow() {
        final double baseUpper = 100;
        final int expUpper = 100;
        final int numberOfBases = 1000;
        final Random r = new Random();
        for (int i = 0; i < numberOfBases; ++i) {
            final double b = baseUpper * r.nextDouble();
            double wanted = 1.0;
            for (int e = 0; e < expUpper; ++e) {
                Assert.assertEquals(wanted, pow(b, e), 1e-14 * wanted);
                wanted *= b;
            }
        }
    }

    //#################################################################################
    //
    // In the nontrivial tests, the correct values are computed using R and Plink.
    //
    //##################################################################################

    @Test
    public void testBonferroni() {
        for (int i = 0; i < p1.length; ++i) {
            Assert.assertEquals(Math.min(1, p1.length * p1[i]), bonferroni(p1[i], p1.length), 0);
        }
        for (int i = 0; i < p2.length; ++i) {
            Assert.assertEquals(Math.min(1, p2.length * p2[i]), bonferroni(p2[i], p2.length), 0);
        }
        for (int i = 0; i < p3.length; ++i) {
            Assert.assertEquals(Math.min(1, p3.length * p3[i]), bonferroni(p3[i], p3.length), 0);
        }
    }

    @Test
    public void testHolmBonferroni() {
        final double[] wanted_1 = {0.00340, 0.02774, 0.03438, 0.09333, 0.12176, 0.44400, 0.52612, 0.56277, 0.59364, 0.96217, 0.97100, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000};
        final double[] wanted_2 = {0.00020, 0.00171, 0.01260, 0.03179, 0.03179, 0.05010, 0.31164, 0.38389, 0.64680, 0.97570, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000};
        final double[] wanted_3 = {0.00240, 0.01444, 0.08838, 0.10625, 0.10625, 0.12615, 0.16478, 0.45890, 0.56016, 0.83776, 0.83776, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000};

        final double[] actual_1 = new double[wanted_1.length];
        final double[] actual_2 = new double[wanted_2.length];
        final double[] actual_3 = new double[wanted_3.length];

        holm_bonferroni(p1, actual_1);
        holm_bonferroni(p2, actual_2);
        holm_bonferroni(p3, actual_3);

        Assert.assertArrayEquals(wanted_1, actual_1, 1e-5);
        Assert.assertArrayEquals(wanted_2, actual_2, 1e-5);
        Assert.assertArrayEquals(wanted_3, actual_3, 1e-5);
    }

    @Test
    public void testHolmBonferroni_edgeCases() {
        final double[] actual_zeros = new double[zeros.length];
        final double[] actual_ones = new double[ones.length];

        holm_bonferroni(zeros, actual_zeros);
        holm_bonferroni(ones, actual_ones);

        Assert.assertArrayEquals(zeros, actual_zeros, 0);
        Assert.assertArrayEquals(ones, actual_ones, 0);
    }

    @Test
    public void testHolmBonferroni_emptyArgument() {
        holm_bonferroni(new double[0], new double[0]); //This should not throw an error.
    }

    @Test
    public void testBenjaminiHochberg() {
        final double[] wanted_1 = {0.00340, 0.01273, 0.01273, 0.02745, 0.03044, 0.09867, 0.10737, 0.10822, 0.10993, 0.17494, 0.17655, 0.20677, 0.60506, 0.72065, 0.72065, 0.74799, 0.75442, 0.75442, 0.75442, 0.99819};
        final double[] wanted_2 = {0.00020, 0.00090, 0.00467, 0.00772, 0.00772, 0.01113, 0.06360, 0.07382, 0.11978, 0.17740, 0.26256, 0.26993, 0.33288, 0.36167, 0.58567, 0.85361, 0.87392, 0.92992, 0.92992, 0.92992};
        final double[] wanted_3 = {0.00240, 0.00760, 0.02600, 0.02600, 0.02600, 0.02803, 0.03363, 0.08825, 0.10373, 0.14104, 0.14104, 0.35288, 0.43981, 0.43981, 0.46200, 0.46200, 0.50059, 0.55690, 0.64851, 0.89518};

        final double[] actual_1 = new double[wanted_1.length];
        final double[] actual_2 = new double[wanted_2.length];
        final double[] actual_3 = new double[wanted_3.length];

        benjamini_hochberg(p1, actual_1);
        benjamini_hochberg(p2, actual_2);
        benjamini_hochberg(p3, actual_3);

        Assert.assertArrayEquals(wanted_1, actual_1, 1e-5);
        Assert.assertArrayEquals(wanted_2, actual_2, 1e-5);
        Assert.assertArrayEquals(wanted_3, actual_3, 1e-5);
    }

    @Test
    public void testBenjaminiHochberg_edgeCases() {
        final double[] actual_zeros = new double[zeros.length];
        final double[] actual_ones = new double[ones.length];

        benjamini_hochberg(zeros, actual_zeros);
        benjamini_hochberg(ones, actual_ones);

        Assert.assertArrayEquals(zeros, actual_zeros, 0);
        Assert.assertArrayEquals(ones, actual_ones, 0);
    }

    @Test
    public void testBenjaminiHochberg_emptyArgument() {
        benjamini_hochberg(new double[0], new double[0]); //This should not throw an error.
    }

    @Test
    public void testBenjaminiYekutieli() {
        final double[] wanted_1 = {0.01223, 0.04581, 0.04581, 0.09876, 0.10952, 0.35498, 0.38629, 0.38937, 0.39551, 0.62939, 0.63516, 0.74389, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000};
        final double[] wanted_2 = {0.00072, 0.00324, 0.01679, 0.02777, 0.02777, 0.04005, 0.22882, 0.26560, 0.43093, 0.63824, 0.94464, 0.97115, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000};
        final double[] wanted_3 = {0.00863, 0.02734, 0.09354, 0.09354, 0.09354, 0.10086, 0.12099, 0.31750, 0.37321, 0.50741, 0.50741, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000};

        final double[] actual_1 = new double[wanted_1.length];
        final double[] actual_2 = new double[wanted_2.length];
        final double[] actual_3 = new double[wanted_3.length];

        benjamini_yekutieli(p1, actual_1);
        benjamini_yekutieli(p2, actual_2);
        benjamini_yekutieli(p3, actual_3);

        Assert.assertArrayEquals(wanted_1, actual_1, 1e-5);
        Assert.assertArrayEquals(wanted_2, actual_2, 1e-5);
        Assert.assertArrayEquals(wanted_3, actual_3, 1e-5);
    }

    @Test
    public void testBenjaminiYekutieli_edgeCases() {
        final double[] actual_zeros = new double[zeros.length];
        final double[] actual_ones = new double[ones.length];

        benjamini_yekutieli(zeros, actual_zeros);
        benjamini_yekutieli(ones, actual_ones);

        Assert.assertArrayEquals(zeros, actual_zeros, 0);
        Assert.assertArrayEquals(ones, actual_ones, 0);
    }

    @Test
    public void testBenjaminiYekutieli_emptyArgument() {
        benjamini_yekutieli(new double[0], new double[0]); //This should not throw an error.
    }

    @Test
    public void test_sidak_ss() {
        final int len = 20;
        for (int i = 0; i < len; ++i) {
            Assert.assertEquals(1 - Math.pow(1.0 - p1[i], len), sidak_ss(p1[i], len), 1e-10);
            Assert.assertEquals(1 - Math.pow(1.0 - p2[i], len), sidak_ss(p2[i], len), 1e-10);
            Assert.assertEquals(1 - Math.pow(1.0 - p3[i], len), sidak_ss(p3[i], len), 1e-10);
        }
    }

    @Test
    public void test_gcc_p() {
        final int len = 20;
        final double[] wanted_1 = {0.13212, 0.20247, 0.21384, 0.26611, 0.28513, 0.38363, 0.40497, 0.41832, 0.43142, 0.49373, 0.50641, 0.53794, 0.73244, 0.78943, 0.80636, 0.83293, 0.86748, 0.87269, 0.88445, 0.99928};
        final double[] wanted_2 = {0.06024, 0.09574, 0.14933, 0.18583, 0.18715, 0.21191, 0.33085, 0.35455, 0.41222, 0.46898, 0.53468, 0.55190, 0.59897, 0.62691, 0.74214, 0.86204, 0.88900, 0.95138, 0.95670, 0.97016};
        final double[] wanted_3 = {0.14257, 0.19927, 0.28355, 0.29720, 0.29949, 0.31508, 0.33689, 0.42228, 0.44823, 0.49898, 0.50103, 0.63400, 0.68808, 0.69746, 0.72875, 0.73231, 0.76127, 0.79763, 0.84840, 0.95994};

        final double invSqrtLambda_1 = getInvSqrtLambda_p(p1);
        final double invSqrtLambda_2 = getInvSqrtLambda_p(p2);
        final double invSqrtLambda_3 = getInvSqrtLambda_p(p3);

        for (int i = 0; i < len; ++i) {
            Assert.assertEquals(wanted_1[i], genomic_control_correct_p(p1[i], invSqrtLambda_1), 1e-5);
            Assert.assertEquals(wanted_2[i], genomic_control_correct_p(p2[i], invSqrtLambda_2), 1e-5);
            Assert.assertEquals(wanted_3[i], genomic_control_correct_p(p3[i], invSqrtLambda_3), 1e-5);
        }
    }

    @Test
    public void test_sidak_sd() {
        final double[] wanted_1 = {0.00339451, 0.0273785, 0.0338275, 0.0893413, 0.115051, 0.36282, 0.415069, 0.437472, 0.456011, 0.634642, 0.639922, 0.696423, 0.981641, 0.992707, 0.992707, 0.992707, 0.992707, 0.992707, 0.992707, 0.99819};
        final double[] wanted_2 = {0.00020, 0.00170862, 0.0125253, 0.0313188, 0.0313188, 0.0489454, 0.270329, 0.322721, 0.485667, 0.640023, 0.789788, 0.796116, 0.857804, 0.870416, 0.96891, 0.996793, 0.996793, 0.99852, 0.99852, 0.99852};
        final double[] wanted_3 = {0.00239727, 0.0143417, 0.0847863, 0.1011, 0.1011, 0.118987, 0.152748, 0.373242, 0.436538, 0.581626, 0.581626, 0.88249, 0.937094, 0.937094, 0.937094, 0.937094, 0.937094, 0.937094, 0.937094, 0.937094};

        final double[] actual_1 = new double[wanted_1.length];
        final double[] actual_2 = new double[wanted_2.length];
        final double[] actual_3 = new double[wanted_3.length];

        sidak_sd(p1, actual_1);
        sidak_sd(p2, actual_2);
        sidak_sd(p3, actual_3);

        Assert.assertArrayEquals(wanted_1, actual_1, 1e-6);
        Assert.assertArrayEquals(wanted_2, actual_2, 1e-6);
        Assert.assertArrayEquals(wanted_3, actual_3, 1e-6);
    }

    @Test
    public void test_sidak_sd_edgeCases() {
        final double[] actual_zeros = new double[zeros.length];
        final double[] actual_ones = new double[ones.length];

        sidak_sd(zeros, actual_zeros);
        sidak_sd(ones, actual_ones);

        Assert.assertArrayEquals(zeros, actual_zeros, 0);
        Assert.assertArrayEquals(ones, actual_ones, 0);
    }

    @Test
    public void test_sidak_sd_emptyArgument() {
        sidak_sd(new double[0], new double[0]); //This should not throw an error.
    }

    @Test
    public void testInvSqrtLambda_p() {
        final double CHI_INV_HALF = 0.4549364231195736D;
        final double[] p1 = {2 * Probability.normal(1)};
        final double[] p2 = {2 * Probability.normal(1), 2 * Probability.normal(2)};
        final double wanted1 = Math.sqrt(CHI_INV_HALF);
        final double wanted2 = Math.sqrt(CHI_INV_HALF / 2.5);
        final double actual1 = getInvSqrtLambda_p(p1);
        final double actual2 = getInvSqrtLambda_p(p2);
        Assert.assertEquals(wanted1, actual1, 1e-10);
        Assert.assertEquals(wanted2, actual2, 1e-10);
    }

    @Test
    public void test_invert() {
        final Random r = new Random();

        for (int len = 0; len <= 100; ++len) {
            final int[] p = new int[len];
            for (int i = 0; i < len; ++i) {
                p[i] = i;
            }
            for (int i = 0; i < len; ++i) {
                final int pi = p[i];
                final int iIdx = i + r.nextInt(len - i);
                p[i] = p[iIdx];
                p[iIdx] = pi;
            }
            final int[] inv = Arrays.copyOf(p, p.length);
            invert(inv);
            validateInverse(p, inv);
        }
    }

    private void validateInverse(int[] p, int[] inv) {
        for (int i = 0; i < inv.length; ++i) {
            Assert.assertEquals(i, p[inv[i]]);
        }
    }
}
