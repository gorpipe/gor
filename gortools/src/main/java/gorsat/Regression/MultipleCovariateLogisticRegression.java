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

import java.util.Arrays;

import static gorsat.Regression.LinearAlgebra.*;

class MultipleCovariateLogisticRegression extends LogisticRegression {

    MultipleCovariateLogisticRegression(int numberOfDepVars, int maximumNumberOfSamples) {
        super(numberOfDepVars, maximumNumberOfSamples);
    }

    @Override
    protected void computeTestStats() {
        final double[] x = zStats;
        final double[] y = pValues;

        double[] u;
        double z;
        for (int k = dim - 1; k != -1; --k) {
            //Solve Uy = e_k.
            //We know that y_i = 0 for i > k since U is upper triangular and invertible.
            u = hessian[k];
            z = (y[k] = 1 / u[k]);
            for (int i = k - 1; i != -1; --i) {
                y[i] = -u[i] * z;
            }
            for (int j = k - 1; j != -1; --j) {
                u = hessian[j];
                z = (y[j] /= u[j]);
                for (int i = j - 1; i != -1; --i) {
                    y[i] -= u[i] * z;
                }
            }

            //Solve U^Tx = y.
            for (int i = 0; i <= k; ++i) {
                u = hessian[i];
                z = y[i];
                for (int j = 0; j < i; ++j) {
                    z -= u[j] * x[j];
                }
                x[i] = z / u[i];
            }
            zStats[k] = beta[k] / (Math.sqrt(x[k]));
        }
    }

    @Override
    protected void updateGradientAndHessian(double[][] x, boolean[] y) {
        Arrays.fill(oneOver, beta[0]);
        double[] xi;
        double tmp;
        for (int i = 0; i < numberOfDepVars; ++i) {
            xi = x[i];
            tmp = beta[i + 1];
            for (int j = 0; j < numberOfSamples; ++j) {
                oneOver[j] += tmp * xi[j];
            }
        }
        double tmp1, tmp2;
        for (int j = 0; j < numberOfSamples; ++j) {
            tmp1 = sf.sigmoid(oneOver[j]);
            tmp2 = 1 - tmp1;
            oneOver[j] = tmp1;
            prod[j] = tmp2 * oneOver[j];
            gradMult[j] = y[j] ? tmp2 : -tmp1;
        }
        gradient[0] = sum(gradMult, numberOfSamples);
        for (int i = 1; i < dim; ++i) {
            gradient[i] = dotProd(gradMult, x[i - 1], numberOfSamples);
        }
        hessian[0][0] = sum(prod, numberOfSamples);
        double[] hess_i;
        for (int i = 1; i < dim; ++i) {
            hess_i = hessian[i];
            hess_i[0] = dotProd(x[i - 1], prod, numberOfSamples);
            for (int j = 1; j <= i; ++j) {
                hess_i[j] = tripleDotProd(x[i - 1], x[j - 1], prod, numberOfSamples);
            }
        }
    }

    @Override
    protected void updateGradientAndHessianFirstRun(double[][] x, boolean[] y) {
        for (int j = 0; j < numberOfSamples; ++j) {
            gradMult[j] = y[j] ? 0.5 : -0.5;
        }
        gradient[0] = sum(gradMult, numberOfSamples);
        for (int i = 1; i < dim; ++i) {
            gradient[i] = dotProd(gradMult, x[i - 1], numberOfSamples);
        }
        hessian[0][0] = numberOfSamples * 0.25;
        double[] hess_i, x_i;
        for (int i = 1; i < dim; ++i) {
            hess_i = hessian[i];
            x_i = x[i - 1];
            hess_i[0] = sum(x_i, numberOfSamples) * 0.25;
            for (int j = 1; j <= i; ++j) {
                hess_i[j] = dotProd(x_i, x[j - 1], numberOfSamples) * 0.25;
            }
        }
    }
}
