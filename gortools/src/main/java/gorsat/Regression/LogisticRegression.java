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

import static gorsat.Regression.LinearAlgebra.solveSymPosDef;
import static gorsat.Regression.RegressionUtilities.computePValues;
import static gorsat.Regression.RegressionUtilities.getTriangularMatrix;

/**
 * This program fits a logistic regression model on a given data set.
 *
 * We find the maximum likelihood estimators by using Newton's method to find the zero of the gradient of the
 * likelihood function.
 */

abstract public class LogisticRegression {
    final protected int numberOfDepVars;
    final protected int maximumNumberOfSamples;
    final protected int dim;

    public double[] beta;
    public double[] zStats;
    public double[] pValues;
    public int numberOfIterations;

    final protected double[] gradient;
    final protected double[][] hessian;
    final protected double[] delta;

    protected int numberOfSamples;
    final protected double[] oneOver;
    final protected double[] gradMult;
    final protected double[] prod;

    private double error;
    private boolean betaClean;

    final protected SigmoidFunction sf;

    protected LogisticRegression(int numberOfDepVars, int maximumNumberOfSamples) {
        this.numberOfDepVars = numberOfDepVars;
        this.maximumNumberOfSamples = maximumNumberOfSamples;
        this.dim = numberOfDepVars + 1;
        this.beta = new double[dim];
        this.zStats = new double[dim];
        this.pValues = new double[dim];
        this.gradient = new double[dim];
        this.hessian = getTriangularMatrix(dim);
        this.delta = new double[dim];
        this.oneOver = new double[this.maximumNumberOfSamples];
        this.gradMult = new double[this.maximumNumberOfSamples];
        this.prod = new double[this.maximumNumberOfSamples];
        this.betaClean = true;
        this.sf = SigmoidFunction.getSigmoidFunction();
    }

    public static LogisticRegression getGorLogisticRegressionObject(int numberOfDepVars, int maximumNumberOfSamples) {
        assert numberOfDepVars > 0;
        assert maximumNumberOfSamples > 0;
        return numberOfDepVars == 1 ? new SingleCovariateLogisticRegression(maximumNumberOfSamples) : new MultipleCovariateLogisticRegression(numberOfDepVars, maximumNumberOfSamples);
    }

    public boolean runRegression(double[][] x, boolean[] y, int numberOfSamples, double tol, int maxIter) {
        this.numberOfSamples = numberOfSamples;
        boolean converged;
        if (!betaClean) {
            Arrays.fill(beta, 0);
        }
        try {
            updateGradientAndHessianFirstRun(x, y);
            updateBeta();
            converged = error < tol;
            numberOfIterations = 1;
            while (!converged && numberOfIterations < maxIter) {
                updateGradientAndHessian(x, y);
                updateBeta();
                converged = error < tol;
                numberOfIterations++;
            }
            betaClean = false;
        } catch (IllegalArgumentException e) {
            //The hessian is not positive definite. Just want to keep on.
            betaClean = false;
            converged = false;
        }
        if (converged) {
            computeTestStats();
            computePValues(zStats, pValues);
        }
        return converged;
    }

    private void updateBeta() {
        error = 0;
        double tmpError;
        solveSymPosDef(hessian, delta, gradient);
        for (int i = 0; i < dim; ++i) {
            tmpError = Math.abs(delta[i]);
            if (tmpError > error) error = tmpError;
            beta[i] += delta[i];
        }
    }

    abstract protected void computeTestStats();
    abstract protected void updateGradientAndHessian(double[][] x, boolean[] y);
    abstract protected void updateGradientAndHessianFirstRun(double[][] x, boolean[] y);
}
