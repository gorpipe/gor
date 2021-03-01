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

import java.util.Arrays;

import static gorsat.Regression.LinearAlgebra.*;
import static gorsat.Regression.RegressionUtilities.computePValues;
import static gorsat.Regression.RegressionUtilities.getTriangularMatrix;

public class LinearRegression {
    final private int n;
    private int numberOfSamples;
    final private double[] rDiag;
    final private double[][] X;
    final private double[][] Rinv;

    final public double[] beta;
    final public double[] betaError;
    final public double[] tStats;
    final public double[] pValues;

    public LinearRegression(int maxNumberOfSamples, int numberOfCovariates) {
        validateDimensions(maxNumberOfSamples, numberOfCovariates);
        this.n = numberOfCovariates + 1;
        this.beta = new double[this.n];
        this.rDiag = new double[this.n];
        this.X = new double[this.n][];
        this.X[0] = new double[maxNumberOfSamples];
        this.Rinv = getTriangularMatrix(this.n);
        this.betaError = new double[this.n];
        this.tStats = new double[this.n];
        this.pValues = new double[this.n];
    }

    public void setData(double[][] X, int numberOfSamples) {
        validateDimensions(numberOfSamples, this.n - 1);
        this.numberOfSamples = numberOfSamples;
        System.arraycopy(X, 0, this.X, 1, X.length);
        Arrays.fill(this.X[0], 1);
        QRFactorize(this.X, this.rDiag, numberOfSamples);
    }

    private void validateDimensions(int numberOfSamples, int numberOfCovariates) {
        if (numberOfSamples <= numberOfCovariates) {
            throw new IllegalArgumentException("Number of samples must be greater than the number of covariates.");
        }
    }

    public void runRegression(double[] y) {
        //Have: R = H_n...H_2H_1A so Q = H_1...H_n.
        //Must compute <Q_1,y>,...,<Q_n,y> and solve Rb = (<Q_1,y>,...,<Q_n,y>).
        //Note that <Q_i,y> = <(H_i)_i, H_(i-1)...H_1y>.
        double[] X_j;
        double scale;
        for (int j = 0; j < this.n; ++j) {
            X_j = this.X[j];
            scale = dotProd(y, X_j, j, this.numberOfSamples) / (this.rDiag[j] * X_j[j]);
            addMultipleOf(y, scale, X_j, j, this.numberOfSamples);
        }

        final double error2 = dotProd(y, y, this.n, this.numberOfSamples);

        System.arraycopy(y, 0, this.beta, 0, this.n);
        double beta_j;
        for (int j = this.n - 1; j != -1; --j) {
            this.beta[j] = (beta_j = this.beta[j] / this.rDiag[j]);
            X_j = this.X[j];
            for (int i = j - 1; i != -1; --i) {
                this.beta[i] -= X_j[i] * beta_j;
            }
        }
        computeTestStats(error2);
    }

    private void computeTestStats(double error2) {
        setRInv();
        computeBetaVariance();
        computeBetaError(error2);
        computeZStats();
        computePValues(this.tStats, this.pValues);
    }

    private void computeZStats() {
        for (int i = 0; i < this.n; ++i) {
            this.tStats[i] = this.beta[i] / this.betaError[i];
        }
    }

    private void setRInv() {
        writeR(this.Rinv);
        invertUpperTriangular(this.Rinv, this.Rinv, this.n);
    }

    private void computeBetaError(double error2) {
        final double errorVar = error2 / (this.numberOfSamples - this.n);
        for (int i = 0; i < this.n; ++i) {
            this.betaError[i] = Math.sqrt(errorVar * this.betaError[i]);
        }
    }

    private void computeBetaVariance() {
        //We compute the diagonal of R^{-1} * T^R^{-1}.
        double[] Rinv_j;
        for (int j = 0; j < this.n; ++j) {
            Rinv_j = this.Rinv[j];
            for (int i = 0; i < j; ++i) {
                this.betaError[i] += Rinv_j[i] * Rinv_j[i];
            }
            this.betaError[j] = Rinv_j[j] * Rinv_j[j];
        }
    }

    private void writeR(double[][] R) {
        R[0][0] = this.rDiag[0];
        for (int i = 1; i < this.n; ++i) {
            final double[] R_i = R[i];
            System.arraycopy(this.X[i], 0, R_i, 0, i);
            R_i[i] = this.rDiag[i];
        }
    }
}
