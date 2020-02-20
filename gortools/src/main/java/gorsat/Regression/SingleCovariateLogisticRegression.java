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

import static gorsat.Regression.LinearAlgebra.dotProd;
import static gorsat.Regression.LinearAlgebra.sum;
import static gorsat.Regression.LinearAlgebra.tripleDotProd;

class SingleCovariateLogisticRegression extends LogisticRegression {

    SingleCovariateLogisticRegression(int maximumNumberOfSamples) {
        super(1, maximumNumberOfSamples);
    }

    @Override
    protected void computeTestStats() {
        final double det = hessian[0][0] * hessian[1][1] - hessian[1][0] * hessian[1][0];
        zStats[0] = beta[0] / Math.sqrt(hessian[1][1] / det);
        zStats[1] = beta[1] / Math.sqrt(hessian[0][0] / det);
    }

    @Override
    protected void updateGradientAndHessian(double[][] x, boolean[] y) {
        double tmp1, tmp2;
        final double[] xi = x[0];
        final double beta0 = beta[0];
        final double beta1 = beta[1];
        for (int i = 0; i < numberOfSamples; ++i) {
            tmp1 = sf.sigmoid(beta0 + beta1 * xi[i]);
            oneOver[i] = tmp1;
            tmp2 = 1 - tmp1;
            prod[i] = tmp2 * tmp1;
            gradMult[i] = y[i] ? tmp2 : -tmp1;
        }
        gradient[0] = sum(gradMult, numberOfSamples);
        gradient[1] = dotProd(gradMult, xi, numberOfSamples);
        hessian[0][0] = sum(prod, numberOfSamples);
        hessian[1][0] = dotProd(xi, prod, numberOfSamples);
        hessian[1][1] = tripleDotProd(xi, xi, prod, numberOfSamples);
    }

    @Override
    protected void updateGradientAndHessianFirstRun(double[][] x, boolean[] y) {
        for (int j = 0; j < numberOfSamples; ++j) {
            gradMult[j] = y[j] ? 0.5 : -0.5;
        }
        gradient[0] = sum(gradMult, numberOfSamples);
        gradient[1] = dotProd(gradMult, x[0], numberOfSamples);
        hessian[0][0] = numberOfSamples * 0.25;
        hessian[1][0] = sum(x[0], numberOfSamples) * 0.25;
        hessian[1][1] = dotProd(x[0], x[0], numberOfSamples) * 0.25;
    }
}
