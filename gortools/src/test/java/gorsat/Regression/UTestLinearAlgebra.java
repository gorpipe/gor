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

import org.apache.commons.math3.linear.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static gorsat.Regression.ComputationTestUtils.getRandomVector;
import static gorsat.Regression.LinearAlgebra.*;
import static gorsat.Regression.RegressionUtilities.getTriangularMatrix;

public class UTestLinearAlgebra {

    @Test
    public void testCholesky_illegalArgument() {
        final double[][] badMatrix = {{-1}};
        boolean success = false;
        try {
            choleskyFactorize(badMatrix, 1);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void testCholesky_emptyMatrix() {
        choleskyFactorize(new double[0][0], 0); //This should be ok.
    }

    @Test
    public void testCholesky_general() {
        for (int dim = 1; dim < 10; ++dim) {
            final Array2DRowRealMatrix rm = getRandomUpperTriangularPosMatrix(dim);
            final RealMatrix rmT = rm.transpose();
            final RealMatrix toDecMatrix = rm.multiply(rmT);
            final double[][] toDecompose = takeHalfOfSymmetricMatrix(toDecMatrix.getData());
            choleskyFactorize(toDecompose, dim);
            final double delta = getDelta(toDecMatrix);
            final double[][] resultMatrix = takeHalfOfSymmetricMatrix(rmT.getData());
            matrixAssertEquals(resultMatrix, toDecompose, delta);
        }
    }

    @Test
    public void testSolveSymmetric_illegalArgument() {
        final double[][] badMatrix = {{-1}};
        boolean success = false;
        try {
            solveSymPosDef(badMatrix, new double[1], new double[1]);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void testSolveSymmetric_terribleArgument() {
        final double[][] terribleMatrix = {{-1, -1}, {-1, -1}};
        boolean success = false;
        try {
            solveSymPosDef(terribleMatrix, new double[2], new double[2]);
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void testSolveSymmetric_emptyMatrix() {
        solveSymPosDef(new double[0][0], new double[0], new double[0]); //This should be ok.
    }

    @Test
    public void testSolveSymmetric_general() {
        for (int dim = 1; dim < 10; ++dim) {
            final Array2DRowRealMatrix matrixA = getRandomPositiveDefiniteMatrix(dim);
            final double[][] A = takeHalfOfSymmetricMatrix(matrixA.getData());
            final double[] expected = getRandomVector(dim, -50, 50);
            final double[] b = matrixA.operate(expected);
            final double[] x = new double[dim];
            solveSymPosDef(A, x, b);
            final double delta = getDelta(matrixA);
            Assert.assertArrayEquals(expected, x, delta);
        }
    }

    @Test
    public void testQRFactorize_emptyMatrix() {
        QRFactorize(new double[0][0], new double[0], 0); //This should be ok.
    }

    @Test
    public void testQRFactorize() {
        final double delta = 1e-10;
        for (int cols = 1; cols <= 25; ++cols) {
            final double[][] QR = new double[cols][2 * cols];
            final double[][] A = getRandomUpperTri(cols);

            for (int rows = cols; rows <= 2 * cols; ++rows) {
                runTest(delta, cols, QR, A, rows);
            }
        }
    }

    private void runTest(double delta, int cols, double[][] QR, double[][] a, int rows) {
        copySubmatrix(cols, QR, a, rows);
        final double[] diag = new double[cols];
        QRFactorize(QR, diag, rows);
        final double[][] Q = getQ(cols, QR, rows);
        validateOrthogonality(delta, cols, rows, Q);
        final double[][] computedA = getComputedA(cols, QR, rows, diag, Q);
        compare(delta, cols, a, rows, computedA);
    }

    private void copySubmatrix(int cols, double[][] QR, double[][] a, int rows) {
        for (int i = 0; i < cols; ++i) {
            System.arraycopy(a[i], 0, QR[i], 0, rows);
        }
    }

    private double[][] getRandomUpperTri(int cols) {
        final double[][] A = new double[cols][];
        for (int i = 0; i < cols; ++i) {
            A[i] = getRandomVector(2 * cols, -50, 50);
        }
        return A;
    }

    private double[][] getQ(int cols, double[][] QR, int rows) {
        final double[][] Q = new double[cols][rows];
        for (int i = 0; i < cols; ++i) {
            Q[i][i] = 1;
        }
        for (int j = cols - 1; j != -1; --j) {
            final double[] QR_j = QR[j];
            final double norm2 = dotProd(QR_j, QR_j, j, rows);
            for (int col = cols - 1; col >= j; --col) {
                final double[] Q_c = Q[col];
                final double scale = -2 * dotProd(Q_c, QR_j, j, rows) / norm2;
                addMultipleOf(Q_c, scale, QR_j, j, rows);
            }
        }
        return Q;
    }

    private void compare(double delta, int cols, double[][] a, int rows, double[][] computedA) {
        for (int col = 0; col < cols; ++col) {
            for (int row = 0; row < rows; ++row) {
                final String message = "First differed at (" + col + ", " + row + ")";
                Assert.assertEquals(message, a[col][row], computedA[col][row], delta);
            }
        }
    }

    private double[][] getComputedA(int cols, double[][] QR, int rows, double[] diag, double[][] q) {
        final double[][] computedA = new double[cols][rows];
        for (int col = 0; col < cols; ++col) {
            final double[] A_c = computedA[col];
            final double[] R_c = QR[col];
            for (int i = 0; i < col; ++i) {
                addMultipleOf(A_c, R_c[i], q[i], 0, rows);
            }
            addMultipleOf(A_c, diag[col], q[col], 0, rows);
        }
        return computedA;
    }

    private void validateOrthogonality(double delta, int cols, int rows, double[][] q) {
        for (int i = 0; i < cols; ++i) {
            final double[] Q_i = q[i];
            Assert.assertEquals(1, dotProd(Q_i, Q_i,  rows), delta);

            for (int j = i + 1; j < cols; ++j) {
                Assert.assertEquals(0, dotProd(Q_i, q[j], rows), delta);
            }
        }
    }

    @Test
    public void testInvertUpperTriangular() {
        for (int n = 0; n < 25; ++n) {
            final double[][] U = getRandomInvertibleUpperTriangularMatrix(n);
            final double[][] Uinv = getTriangularMatrix(n);
            invertUpperTriangular(U, Uinv, n);
            final double[][] prod = multiplyUpperTrianglular(U, Uinv, n);
            verifyUpperIsIdentity(prod, n);
        }
    }

    private void verifyUpperIsIdentity(double[][] A, int dim) {
        final double delta = 1e-8;
        for (int i = 0; i < dim; ++i) {
            final double[] A_i = A[i];
            for (int j = 0; j < i; ++j) {
                Assert.assertEquals(0, A_i[j], delta);
            }
            Assert.assertEquals(1, A_i[i], delta);
        }
    }

    private double[][] multiplyUpperTrianglular(double[][] U1, double[][] U2, int n) {
        final double[][] toReturn = new double[n][];
        for (int j = 0; j < n; ++j) {
            final double[] toReturn_j = new double[j + 1];
            for (int i = 0; i <=j; ++i) {
                double toReturn_ij = 0;
                for (int k = i; k <= j; ++k) {
                    toReturn_ij += U1[k][i] * U2[j][k];
                }
                toReturn_j[i] = toReturn_ij;
            }
            toReturn[j] = toReturn_j;
        }
        return toReturn;
    }

    private double[][] getRandomInvertibleUpperTriangularMatrix(int dim) {
        final double delta = 1;
        final Random r = new Random();
        final double[][] toReturn = new double[dim][];
        for (int i = 0; i < dim; ++i) {
            final double[] toReturn_i = new double[i + 1];
            for (int j = 0; j < i; ++j) {
                toReturn_i[j] = 20 * r.nextDouble() - 10;
            }
            toReturn_i[i] = (10 - delta) * r.nextDouble() + delta;
            if (r.nextDouble() > 0.5) toReturn_i[i] = -toReturn_i[i];
            toReturn[i] = toReturn_i;
        }
        return toReturn;
    }

    private double[][] takeHalfOfSymmetricMatrix(double[][] A) {
        final double[][] toReturn = new double[A.length][];
        for (int i = 0; i < toReturn.length; ++i) {
            toReturn[i] = Arrays.copyOf(A[i], i + 1);
        }
        return toReturn;
    }

    private static Array2DRowRealMatrix getRandomUpperTriangularPosMatrix(int dim) {
        final double[][] U = new double[dim][dim];
        for (int i = 0; i < dim; ++i) {
            U[i][i] = 100 * Math.random() + 1; //Don't want almost zero.
            for (int j = i + 1; j < dim; ++j) {
                U[i][j] = 100 * Math.random() - 50;
            }
        }
        return new Array2DRowRealMatrix(U);
    }

    private static Array2DRowRealMatrix getRandomPositiveDefiniteMatrix(int dim) {
        final Array2DRowRealMatrix matrixU = getRandomUpperTriangularPosMatrix(dim);
        return (Array2DRowRealMatrix) matrixU.transpose().multiply(matrixU);
    }

    private static double getConditionNumber(RealMatrix A) {
        return new SingularValueDecomposition(A).getConditionNumber();
    }

    private static double getDelta(RealMatrix A) {
        return getConditionNumber(A) * 1e-13;
    }

    @Test
    public void testSum() {
        double[] x0 = new double[0];
        Assert.assertEquals(0, sum(x0, x0.length), 0);

        final double[] x1 = new double[100];
        double sum1 = 0;
        for (int i = 0; i < x1.length; ++i) {
            x1[i] = Math.random();
            sum1 += x1[i];
        }
        Assert.assertEquals(sum1, sum(x1, x1.length), 1e-10);

        final double[] x2 = new double[96];
        double sum2 = 0;
        for (int i = 0; i < x2.length; ++i) {
            x2[i] = Math.random();
            sum2 += x2[i];
        }
        Assert.assertEquals(sum2, sum(x2, x2.length), 1e-10);
    }

    @Test
    public void testDotProd() {
        double[] x0 = new double[0];
        double[] y0 = new double[0];
        Assert.assertEquals(0, dotProd(x0, y0, 0), 0);

        double[] x1 = getRandomVector(100, 0, 1);
        double[] y1 = getRandomVector(100, 0, 1);
        Assert.assertEquals(dotProduct(x1, y1), dotProd(x1, y1, 100), 1e-10);

        final double[] x2 = getRandomVector(96, 0, 1);
        final double[] y2 = getRandomVector(96, 0, 1);
        Assert.assertEquals(dotProduct(x2, y2), dotProd(x2, y2, 96), 1e-10);
    }

    @Test
    public void testTripleDotProd() {
        double[] x0 = new double[0];
        double[] y0 = new double[0];
        double[] z0 = new double[0];
        Assert.assertEquals(0, tripleDotProd(x0, y0, z0,0), 0);

        double[] x1 = getRandomVector(100, 0, 1);
        double[] y1 = getRandomVector(100, 0, 1);
        double[] z1 = getRandomVector(100, 0, 1);
        Assert.assertEquals(tripleDotProduct(x1, y1, z1), tripleDotProd(x1, y1, z1, 100), 1e-10);

        final double[] x2 = getRandomVector(96, 0, 1);
        final double[] y2 = getRandomVector(96, 0, 1);
        final double[] z2 = getRandomVector(96, 0, 1);
        Assert.assertEquals(tripleDotProduct(x2, y2, z2), tripleDotProd(x2, y2, z2, 96), 1e-10);
    }

    private static void matrixAssertEquals(double[][] expected, double[][] actual, double delta) {
        Assert.assertEquals("The dimensions do not match.", expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            final double[] x_i = expected[i];
            final double[] y_i = actual[i];
            Assert.assertEquals("The dimensions do not match.", x_i.length, y_i.length);
            for (int j = 0; j <= i; ++j) {
                Assert.assertEquals("Entries (" + i + ", " + j + ") are not equal.\n Expected:\t" + x_i[j] + "\tActual:\t" + y_i[j], x_i[j], y_i[j], delta);
            }
        }
    }

    private static double dotProduct(double[] x, double[] y) {
        double sum = 0;
        assert x.length == y.length;
        for (int i = 0; i < x.length; ++i) {
            sum += x[i] * y[i];
        }
        return sum;
    }

    private static double tripleDotProduct(double[] x, double[] y, double[] z) {
        double sum = 0;
        assert x.length == y.length && y.length == z.length;
        for (int i = 0; i < x.length; ++i) {
            sum += x[i] * y[i] * z[i];
        }
        return sum;
    }
}
