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

package gorsat.Regression;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static gorsat.Regression.ComputationTestUtils.getRandomBooleanVector;
import static gorsat.Regression.ComputationTestUtils.getRandomMatrix;

public class UTestLogisticRegression {

    //The Hessian will be singular. We do not want things to explode if that happens.
    @Test
    public void testLinearlyDependentCovariates() {
        final double[][] X = {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};
        final boolean[] y = {true, false, true, false, true, false, true, false, true, false};
        final LogisticRegression lr = LogisticRegression.getGorLogisticRegressionObject(2, 10);
        final boolean converged = lr.runRegression(X, y, 10, 1e-5, 20);
        Assert.assertFalse(converged);
    }

    @Test
    public void generalTest() {
        final double[][] studyHours = {{0.5f, 0.75f, 1, 1.25f, 1.5f, 1.75f, 1.75f, 2, 2.25f, 2.5f, 2.75f, 3, 3.25f, 3.5f, 4, 4.25f, 4.5f, 4.75f, 5, 5.5f}};
        final boolean[] passOrFail = {false, false, false, false, false, false, true, false, true, false, true, false, true, false, true, true, true, true, true, true};
        LogisticRegression glr = LogisticRegression.getGorLogisticRegressionObject(1, passOrFail.length);
        final boolean converged = glr.runRegression(studyHours, passOrFail, passOrFail.length, 1e-4, 10);
        Assert.assertTrue(converged);
        Assert.assertEquals(-4.0777, glr.beta[0], 1e-4);
        Assert.assertEquals(1.5046, glr.beta[1], 1e-4);
        Assert.assertEquals(-2.316, glr.zStats[0], 1e-3);
        Assert.assertEquals(2.393, glr.zStats[1], 1e-3);
        Assert.assertEquals(0.0206, glr.pValues[0], 1e-4);
        Assert.assertEquals(0.0167, glr.pValues[1], 1e-4);
        final int numberOfSamples = 100000;
        final double[][] x = getRandomMatrix(10, numberOfSamples, 0, 100);
        final boolean[] y = getRandomBooleanVector(numberOfSamples);
        final LogisticRegression glr2 = LogisticRegression.getGorLogisticRegressionObject(10, numberOfSamples);
        final boolean converged2 = glr2.runRegression(x, y, numberOfSamples, 1e-6, 20);
        Assert.assertTrue(converged2); //This should always converge. A correct value for beta is 0.
        final double[] beta2 = Arrays.copyOf(glr2.beta, glr2.beta.length);
        //Try run this again. Everything should be EXACTLY the same.
        final boolean converged3 = glr2.runRegression(x, y, numberOfSamples, 1e-6, 20);
        final double[] beta3 = Arrays.copyOf(glr2.beta, glr2.beta.length);
        Assert.assertTrue(converged3);
        Assert.assertArrayEquals(beta2, beta3, 0);
    }

    @Test
    public void generalTestSingleCovariate() {
        final double[][] x = {{-24.087, 24.363, 5.322, -41.106, 0.861, 6.006, 28.906, 4.990, -2.064, 42.520, -2.360, 21.635, -47.002, -3.236, 19.639, -49.442, 33.665, 25.869, 48.999, 15.649, -13.529, 34.842, 8.851, -17.395, -33.688, -41.427, 7.891, 32.655, -28.831, 20.174, -21.875, -29.408, 27.868, 2.730, -26.620, -21.906, -49.489, -11.146, 33.083, -7.870, -27.778, -32.197, 33.854, 35.251, 32.954, -16.500, 22.687, 49.642, -8.592, -1.581, 49.770, -9.968, 17.438, -21.887, -3.530, 11.940, 12.856, -40.952, 24.545, -48.849, 3.174, 48.457, -43.633, 27.974, 40.554, 19.082, 32.728, 25.508, -40.851, 3.961, -15.235, -14.979, 27.993, -33.181, -0.409, 33.507, -46.259, 22.170, -44.579, 8.579, -4.191, 5.884, -48.713, -25.909, -30.153, -48.238, -41.109, 26.229, 31.984, 12.586, -20.295, -25.746, -44.104, -24.875, -0.895, 42.741, 37.528, -42.259, -8.822, -17.202}};
        final boolean[] y = {false, true, false, false, true, true, true, true, false, true, false, true, false, true, true, false, true, true, true, true, false, true, true, false, false, false, true, true, false, true, false, false, true, false, false, false, false, false, true, false, false, false, true, true, true, false, true, true, false, true, true, false, true, false, true, true, true, false, true, false, true, true, false, true, true, true, true, true, false, true, false, false, true, false, false, true, false, true, false, true, true, true, false, false, false, false, false, true, true, true, false, false, false, false, true, true, true, false, false, false};
        final double[] beta = {0.55561, 0.28966};
        final double[] zStats = {1.052, 3.238};

        final LogisticRegression glr = LogisticRegression.getGorLogisticRegressionObject(1, 100);

        Assert.assertTrue(glr.runRegression(x, y, 100, 1e-5, 10));
        Assert.assertArrayEquals(beta, glr.beta, 1e-5);
        Assert.assertArrayEquals(zStats, glr.zStats, 1e-3);
    }

    @Test
    public void generalTestTwoCovariates() {
        final double[][] x = {{-0.094, -2.124, 3.487, -1.598, -1.599, 4.863, -0.856, 1.727, -4.509, -2.666, 2.351, -1.636, -2.475, 3.609, 4.157, 2.651, -2.947, 3.198, -1.492, -1.878, -0.216, -0.527, -1.018, 2.164, 4.664, 4.408, -2.674, -4.510, -2.444, 4.007, 0.548, 1.312, 1.560, -4.428, -4.177, 1.604, -0.549, 0.421, 4.975, 0.319, -3.738, -1.787, 2.791, -4.032, 2.522, 1.487, -4.076, -1.580, 1.134, 1.578, 1.638, -2.680, 3.431, -0.488, -1.839, 2.008, 2.395, -1.968, 0.904, -4.689, 3.555, 3.386, 1.295, -3.896, -3.426, -1.997, -2.507, -1.989, 3.032, -2.305, -1.266, 4.143, -1.594, -0.486, -2.419, 0.235, -1.234, -1.094, -3.185, 1.088, -4.818, -2.722, 0.237, -2.317, 1.365, 3.627, -0.857, 4.855, 2.533, -3.639, -1.279, 1.910, -3.972, 3.929, 1.124, 1.067, -3.329, -4.579, 4.496, 1.109},
                {3.118, 2.120, -0.201, 2.313, 1.466, 1.032, -1.189, 0.696, 4.866, 1.385, -0.914, 3.668, -0.892, -3.724, -4.598, 1.729, 0.234, -3.691, -4.846, -4.461, 0.567, 2.760, -3.150, -3.704, -1.055, 0.214, -4.588, 4.759, -1.518, -4.851, 3.856, 1.624, -2.322, -0.604, 3.411, 4.432, 3.196, -0.071, -3.340, -1.279, -4.868, -1.078, 0.760, 3.607, -1.916, 0.527, 0.329, 4.489, 3.860, 3.312, 2.048, -1.916, 4.131, -0.193, -4.432, -2.528, 3.851, -2.898, -2.222, -2.642, -1.227, 4.946, 0.020, 2.240, 2.871, 0.287, 0.301, 2.686, 3.995, -0.300, -4.908, -2.599, -1.429, 3.652, -2.230, 1.297, -1.317, -0.954, 3.279, 2.248, -2.662, -1.015, 1.087, 1.730, 0.243, 1.212, 0.993, -2.959, 4.918, -0.834, -1.128, -4.698, 0.463, 2.088, 1.911, 1.918, 3.788, 3.150, -4.572, -1.284}};
        final boolean[] y = {false, false, false, true, false, true, true, true, false, false, false, false, false, true, true, true, false, true, false, false, false, true, false, true, true, true, false, false, false, true, true, true, true, false, false, true, true, false, true, true, false, true, true, false, true, true, false, false, true, true, true, true, true, true, true, true, true, false, true, false, true, true, true, false, false, true, false, true, true, false, false, true, false, false, false, true, true, false, false, true, false, false, true, true, true, true, true, true, true, false, false, false, false, true, true, true, false, false, true, true};
        final double[] beta = {0.6266, 0.8391, 0.1300};
        final double[] zStats = {1.995, 5.413, 1.152};

        final LogisticRegression glr = LogisticRegression.getGorLogisticRegressionObject(2, 100);

        Assert.assertTrue(glr.runRegression(x, y, 100, 1e-5, 10));
        Assert.assertArrayEquals(beta, glr.beta, 1e-4);
        Assert.assertArrayEquals(zStats, glr.zStats, 1e-3);
    }

    @Test
    public void generalTestThreeCovariates() {
        final double[][] x = {{0.794, 2.185, 4.842, 2.345, 1.363, -4.502, 3.870, 3.822, -1.413, 3.582, -3.937, 4.675, -1.450, -1.172, -0.514, 4.598, 0.183, -0.398, -2.096, -0.983, 3.673, 1.307, -2.449, -3.745, 1.814, -0.228, -2.809, 4.374, -4.458, 2.650, 1.124, 4.238, -2.983, -0.343, -1.309, -1.469, 1.663, -3.907, -1.440, 2.988, 4.594, -3.876, 1.808, -3.854, -3.241, 2.784, 4.466, -2.657, 2.025, 0.574, -2.223, -0.409, -1.777, 3.884, 2.934, 4.265, 2.912, 0.259, -3.452, -1.689, -1.682, 1.752, 3.480, 1.876, -0.496, 0.291, -4.988, 4.905, -4.592, 4.062, 2.618, -4.710, 3.848, 1.474, 0.780, -1.285, -0.947, -2.753, -3.501, 1.247, 3.638, 1.566, -1.835, 0.126, -3.759, -1.884, 2.546, -3.969, 1.298, 0.090, -4.428, 1.346, -1.409, -4.992, 0.510, 3.666, -2.634, -0.685, -4.038, 1.895},
                {-2.553, -4.126, -4.674, -4.271, 4.840, -0.858, 0.902, -3.528, 4.301, 3.243, 1.398, -2.101, 2.766, 4.402, -3.346, -3.918, -1.533, -0.548, -2.541, 4.086, 4.992, -0.376, -2.781, -3.392, -1.801, -0.081, -4.418, 3.726, 0.160, 4.249, -2.875, 1.265, 1.808, 3.941, -3.777, -0.525, -3.079, 0.605, -4.410, -3.221, 4.446, 3.637, 0.896, 1.384, 4.013, -2.111, -2.920, 0.509, 0.720, -1.769, 3.779, 4.886, -0.011, 4.816, -2.582, -4.975, 1.412, -4.040, -3.896, -4.491, 2.098, 4.497, 0.554, 4.309, -4.987, -1.564, -0.297, -4.261, -3.709, -2.398, 3.607, -1.724, -2.666, 4.744, -1.421, 1.988, 2.877, 2.355, 0.300, 4.780, 4.938, -4.898, -0.134, 2.063, 4.779, 2.424, -4.586, 0.792, 0.616, -3.633, -1.952, -3.905, -2.074, -3.746, -4.637, -3.071, 1.672, 4.209, -1.200, -3.659},
                {4.972, 4.249, -1.680, 2.706, -3.536, 2.339, -1.468, -0.087, -2.431, 2.737, 0.986, -1.004, -4.276, 4.974, 1.758, -1.247, 1.869, -0.327, -0.735, -4.273, -4.960, -4.440, 4.885, -4.292, 4.740, -3.475, 1.974, -4.362, -3.645, -3.888, 0.819, -2.577, -1.548, 2.198, -0.151, -4.027, -4.806, 3.318, 2.048, 2.964, 2.575, 0.332, 0.200, -4.442, 3.456, 2.748, -4.899, -4.652, 3.705, -1.909, -2.614, -2.618, -0.072, -4.778, -3.940, 2.082, -1.561, -1.560, -4.829, 4.295, -4.795, -1.388, 4.392, 3.556, -2.469, 0.386, -0.499, -2.050, 0.193, 0.782, 4.944, 0.677, 1.666, 4.197, 2.780, 3.593, -0.119, 1.945, 0.373, -4.395, 1.772, 1.072, 3.168, -4.167, -3.451, -2.848, -1.156, 1.950, -0.701, -0.737, 4.992, 2.993, -0.892, 2.171, 4.277, 3.319, 4.408, 1.141, -1.211, 0.174}};
        final boolean[] y = {false, true, true, false, false, true, true, false, true, false, false, true, true, false, false, true, true, true, false, true, true, true, false, false, true, false, true, true, false, true, true, false, true, true, true, true, true, true, false, true, true, false, true, false, false, true, true, false, true, true, true, true, false, true, false, false, true, false, true, true, false, true, true, true, false, false, false, false, false, true, true, false, true, true, true, true, true, true, false, true, true, true, false, true, true, false, false, false, false, false, false, false, false, true, true, true, true, true, false, true};
        final double[] beta = {0.44925, 0.25787, 0.18284, 0.05986};
        final double[] zStats = {1.991, 3.149, 2.411, 0.79};

        final LogisticRegression glr = LogisticRegression.getGorLogisticRegressionObject(3, 100);

        Assert.assertTrue(glr.runRegression(x, y, 100, 1e-5, 10));
        Assert.assertArrayEquals(beta, glr.beta, 1e-5);
        Assert.assertArrayEquals(zStats, glr.zStats, 1e-3);
    }

    @Test
    public void generalTestFourCovariates() {
        final double[][] x = {{1.820, 0.187, -2.054, -3.479, 0.409, 3.094, -3.537, -2.935, -0.973, -4.727, -4.222, 4.056, 0.797, -3.364, 4.418, 4.973, -0.551, -4.478, 4.438, 0.789, -2.875, 1.112, 3.775, 3.573, 2.530, -2.540, 1.453, -1.809, 4.440, 4.113, 0.639, 3.099, -4.438, -1.660, 4.550, -4.743, 1.739, 3.958, 0.217, 4.124, 1.513, 1.768, -4.904, -0.233, 1.040, -3.421, 1.527, 0.069, -0.653, -4.153, 4.054, 2.040, -1.045, 4.907, 4.587, 2.219, -3.156, 3.808, 0.424, 1.198, 0.766, 0.754, 0.118, -2.774, -4.075, -2.859, 2.195, 3.928, -3.009, -0.799, 1.735, -2.879, 0.476, 1.239, -0.213, -4.265, 2.459, -4.519, 0.652, 1.826, -3.335, 3.521, 2.003, 4.706, -2.114, 4.365, 2.872, 4.160, -4.232, 4.079, 4.473, -2.227, 3.939, 2.043, -1.110, -2.647, -0.509, -1.199, 0.604, -0.536},
                {-2.135, -4.410, 3.914, -3.214, -0.074, 3.641, 4.674, 2.571, 2.904, 4.415, -0.357, -1.082, -4.260, -2.484, -0.621, 0.385, 0.784, -3.768, -0.154, -4.187, -1.104, 2.645, -4.649, -3.108, 1.461, 0.906, 0.885, -2.609, 4.315, 3.675, -1.112, -1.943, -1.408, -3.280, -4.578, 2.171, 3.477, -3.311, 0.759, -3.229, -4.123, -2.968, -0.613, 4.544, 2.007, -2.897, 4.420, 1.373, -4.110, -0.584, 1.436, 4.554, -0.233, 0.067, -3.280, -3.323, 1.425, -2.679, 4.920, 0.168, 3.440, 3.365, 1.122, -2.503, -0.671, 4.218, 2.340, 2.052, 1.621, 2.816, 3.410, 4.672, -3.161, 0.250, 2.315, 0.849, 2.400, 0.175, -0.095, 3.592, 0.424, 1.381, -0.954, -3.046, 3.458, 3.530, -1.824, -4.775, -2.880, 1.051, 4.275, 2.787, 4.647, 0.207, 0.253, -0.363, -1.633, 4.321, 1.749, -2.431},
                {-2.322, -1.466, 4.442, -4.703, -2.427, -4.675, 4.870, 4.679, -0.072, -0.640, 0.618, -0.908, 4.417, -1.727, -3.122, 1.664, 2.036, 4.821, -3.981, 1.848, -2.319, -4.527, 2.522, -2.988, -1.305, -3.755, -3.606, 3.752, 4.395, 0.745, 0.061, 3.932, 1.577, -4.544, 3.730, 0.450, 0.994, -2.762, 4.235, -2.266, -0.189, -1.937, 2.356, 1.960, -4.957, 1.970, 3.614, -4.001, 2.960, -0.749, -2.454, 4.091, -0.528, 2.866, 0.452, 1.243, -3.501, 2.075, -3.378, 2.358, -0.700, -1.694, 3.040, -3.916, -0.202, -1.754, 2.559, -1.602, 0.864, -0.842, 0.562, 0.909, -3.405, 1.426, 0.088, -1.112, 3.549, 3.325, 0.447, 4.035, -2.022, 3.018, 4.299, -3.043, 4.016, 2.783, -1.779, 4.383, -2.137, 0.458, 1.743, -3.896, 2.929, 3.124, -0.136, -3.364, 0.970, -1.997, 4.040, -3.384},
                {-3.387, -2.916, 3.019, -2.805, 3.544, -3.067, 4.583, -1.062, -3.674, 3.136, 1.367, -1.581, 1.746, -2.363, 4.778, -2.517, 0.530, -2.611, -3.717, 4.026, -4.155, 0.515, 4.055, 1.939, -0.659, 1.276, -0.335, 2.334, -0.265, -1.189, -0.196, 1.061, -1.069, 4.680, -1.952, -2.484, 0.678, 4.187, 1.667, -0.265, -3.227, -3.507, -1.535, 0.780, -4.206, -1.475, -1.335, 1.413, -0.616, -2.333, -3.175, 4.056, 1.060, -1.759, -1.191, 3.046, 2.572, -3.222, -1.225, -3.828, 2.798, 1.082, 1.224, 2.510, 4.175, -1.924, 3.072, 1.537, 1.675, -4.558, 0.768, -1.458, -0.688, 2.046, -2.548, 1.324, -0.804, 2.161, -4.852, 3.173, -1.879, -1.536, -3.001, -2.849, -3.306, -1.162, 4.542, -3.346, -3.995, 1.951, -4.984, -2.736, -3.171, -3.368, 3.718, 4.893, -3.622, 0.032, -3.610, -4.786}};
        final boolean[] y = {false, false, true, false, true, true, true, true, true, true, true, true, true, false, true, true, true, false, false, false, true, false, false, false, true, false, false, false, true, true, false, false, false, false, false, true, true, false, true, true, false, false, false, true, true, false, true, true, false, false, true, true, true, true, true, false, true, false, true, false, false, true, true, false, false, true, true, true, true, true, true, true, true, false, true, false, true, false, true, true, false, true, true, false, true, true, false, false, false, true, true, false, true, true, true, false, false, true, true, false};
        final double[] beta = {0.261585, 0.254247, 0.723626, 0.201670, 0.004409};
        final double[] zStats = {0.911, 2.516, 5.201, 1.871, 0.043};

        final LogisticRegression glr = LogisticRegression.getGorLogisticRegressionObject(4, 100);

        Assert.assertTrue(glr.runRegression(x, y, 100, 1e-6, 10));
        Assert.assertArrayEquals(beta, glr.beta, 1e-6);
        Assert.assertArrayEquals(zStats, glr.zStats, 1e-3);
    }

    @Test
    public void testGetGorLogisticRegressionObject() {
        LogisticRegression glr;
        boolean success;

        success = false;
        try {
            LogisticRegression.getGorLogisticRegressionObject(0, 1);
        } catch (AssertionError e) {
            success = true;
        }
        Assert.assertTrue(success);

        success = false;
        try {
            LogisticRegression.getGorLogisticRegressionObject(1, 0);
        } catch (AssertionError e) {
            success = true;
        }
        Assert.assertTrue(success);

        glr = LogisticRegression.getGorLogisticRegressionObject(1, 1);
        Assert.assertTrue(glr instanceof SingleCovariateLogisticRegression);

        glr = LogisticRegression.getGorLogisticRegressionObject(2, 1);
        Assert.assertTrue(glr instanceof MultipleCovariateLogisticRegression);
    }
}