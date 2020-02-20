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

public class LinearAlgebra {
    private final static double EPSILON = Double.longBitsToDouble(0x3CA0000000000000L); //2^(-53)

    private LinearAlgebra() {}

    /**
     * Solves Ax = b and writes the results to x. The matrix A must be symmetric and positively determined.
     */
    public static void solveSymPosDef(double[][] A, double[] x, double[] b) {
        if (b.length == 2) {
            final double det = A[0][0] * A[1][1] - A[1][0] * A[1][0];
            if (det <= EPSILON) { //Note that the det should be > 0 if A is positively determined since 2 is even.
                throw new IllegalArgumentException("Singular matrix.");
            }
            x[0] = (b[0] * A[1][1] - b[1] * A[1][0]) / det;
            x[1] = (A[0][0] * b[1] - A[1][0] * b[0]) / det;
        } else {
            final int n = b.length;
            choleskyFactorize(A, n);

            solveUpper(A, b, b, n);

            solveLower(A, x, b, n);
        }
    }

    /**
     * Decomposes the matrix to A = UU^T where U is upper triangular. Writes the result to A.
     */
    public static void choleskyFactorize(double[][] A, int dim) {
        double sqrt, invSqrt, tmp;
        double[] A_i, A_k;
        for (int i = dim - 1; i != -1; --i) {
            A_i = A[i];
            if (A_i[i] <= EPSILON) {
                throw new IllegalArgumentException("Not a positive definite matrix.");
            }
            sqrt = Math.sqrt(A_i[i]);
            A_i[i] = sqrt;
            invSqrt = 1 / sqrt;
            for (int j = i - 1; j != -1; --j) {
                A_i[j] *= invSqrt;
            }
            for (int k = i - 1; k != -1; --k) {
                tmp = A_i[k];
                A_k = A[k];
                for (int j = k; j != -1; --j) {
                    A_k[j] -= tmp * A_i[j];
                }
            }
        }
    }

    /**
     * Solves Ux = y and writes the result to x. It is ok if x and y are the same vector.
     * The matrix U should be transposed.
     */
    public static void solveUpper(double[][] U, double[] x, double[] y, int n) {
        double[] U_i;
        double y_i;
        for (int i = n - 1; i != -1; --i) {
            U_i = U[i];
            y_i = (y[i] /= U_i[i]);
            for (int j = i - 1; j != -1; --j) {
                x[j] -= y_i * U_i[j];
            }
        }
    }

    /**
     * Solves Lx = y and writes the result to x. The vector x must be different from y.
     */
    public static void solveLower(double[][] L, double[] x, double[] y, int n) {
        double[] L_i;
        double x_i;
        for (int i = 0; i < n; ++i) {
            L_i = L[i];
            x_i = y[i];
            for (int j = 0; j < i; ++j) {
                x_i -= L_i[j] * x[j];
            }
            x[i] = x_i / L_i[i];
        }
    }

    /**
     * QR decomposes X. The matrix X is indexed (column, row) (it is transposed). The results are written to
     * X s.t. in X[i] we write r_0i, r_1i,...,r_i-1i, h_i0, h_i1, ...,h_im-i-1 where h_i is the i-th Householder
     * reflector vector. In rDiag we write r_00, r_11,...,r_nn where n is the number of columns in X. The number m is
     * the number of rows in X.
     */
    public static void QRFactorize(double[][] X, double[] rDiag, int m) {
        double[] X_j;
        for (int j = 0; j < X.length; ++j) {
            X_j = X[j];
            final double x2 = dotProd(X_j, X_j, j, m);
            final double a = X_j[j] > 0 ? -Math.sqrt(x2) : Math.sqrt(x2);
            X_j[j] -= a;
            rDiag[j] = a;
            final double X_jj = X_j[j];

            for (int k = j + 1; k < X.length; ++k) {
                final double scale = dotProd(X[k], X_j, j, m) / (a * X_jj);
                addMultipleOf(X[k], scale, X_j, j, m);
            }
        }
    }

    /**
     * Inverts the upper triangular matrix n x n matrix U. Writes the results to X.
     */
    public static void invertUpperTriangular(double[][] U, double[][] X, int n) {
        for (int colIdx = n - 1; colIdx != -1; --colIdx) {
            solveUxEqualsKthElem(U, X[colIdx], colIdx);
        }
    }

    private static void solveUxEqualsKthElem(double[][] U, double[] x, int k) {
        double[] U_j;
        double x_j;

        U_j = U[k];
        x[k] = (x_j = 1 / U_j[k]);

        for (int i = k - 1; i != -1; --i) {
            x[i] = -U_j[i] * x_j;
        }

        for (int j = k - 1; j != -1; --j) {
            U_j = U[j];
            x_j = (x[j] /= U_j[j]);
            for (int i = j - 1; i != -1; --i) {
                x[i] -= U_j[i] * x_j;
            }
        }
    }

    /*
     ************************
     * Operations on vectors.
     ************************
     */

    /**
     * Computes the dot product of the sub-vectors of x and y consisting of the elements whose index is < len.
     */
    public static double dotProd(double[] x, double[] y, int len) {
        return dotProd(x, y, 0, len);
    }

    /**
     * Computes the dot product of the sub-vectors of x and y consisting of the elements whose index is < upTo and
     * >= offset.
     */
    public static double dotProd(double[] x, double[] y, int offset, int upTo) {
        double s0 = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, s6 = 0, s7 = 0;
        final int unfoldUpTo = upTo - 7;
        int i = offset;
        while (i < unfoldUpTo) {
            s0 += x[i] * y[i];
            s1 += x[i + 1] * y[i + 1];
            s2 += x[i + 2] * y[i + 2];
            s3 += x[i + 3] * y[i + 3];
            s4 += x[i + 4] * y[i + 4];
            s5 += x[i + 5] * y[i + 5];
            s6 += x[i + 6] * y[i + 6];
            s7 += x[i + 7] * y[i + 7];
            i += 8;
        }
        double s = s0 + s1 + s2 + s3 + s4 + s5 + s6 + s7;
        while (i < upTo) {
            s += x[i] * y[i];
            ++i;
        }
        return s;
    }

    /**
     * Computes the triple dot product of the sub-vectors of x, y, z consisting of the elements whose index is < len.
     */
    public static double tripleDotProd(double[] x, double[] y, double[] z, int len) {
        return tripleDotProd(x, y, z, 0, len);
    }

    /**
     * Computes the tripte dot product of the sub-vectors of x, y, z consisting of the elements whose index is < upTo
     * and >= offset.
     */
    public static double tripleDotProd(double[] x, double[] y, double[] z, int offset, int upTo) {
        double s0 = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, s6 = 0, s7 = 0;
        final int unfoldUpTo = upTo - 7;
        int i = offset;
        while (i < unfoldUpTo) {
            s0 += x[i] * y[i] * z[i];
            s1 += x[i + 1] * y[i + 1] * z[i + 1];
            s2 += x[i + 2] * y[i + 2] * z[i + 2];
            s3 += x[i + 3] * y[i + 3] * z[i + 3];
            s4 += x[i + 4] * y[i + 4] * z[i + 4];
            s5 += x[i + 5] * y[i + 5] * z[i + 5];
            s6 += x[i + 6] * y[i + 6] * z[i + 6];
            s7 += x[i + 7] * y[i + 7] * z[i + 7];
            i += 8;
        }
        double s = s0 + s1 + s2 + s3 + s4 + s5 + s6 + s7;
        while (i < upTo) {
            s += x[i] * y[i] * z[i];
            ++i;
        }
        return s;
    }

    /**
     * Computes the sum of the elements of {@code x} with index < {@code len}.
     */
    public static double sum(double[] x, int len) {
        return sum(x, 0, len);
    }

    /**
     * Computes the sum the elements of x whose index is < upTo and >= offset.
     */
    public static double sum(double[] x, int offset, int upTo) {
        double s0 = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, s6 = 0, s7 = 0;
        final int unfoldUpTo = upTo - 7;
        int i = offset;
        while (i < unfoldUpTo) {
            s0 += x[i];
            s1 += x[i + 1];
            s2 += x[i + 2];
            s3 += x[i + 3];
            s4 += x[i + 4];
            s5 += x[i + 5];
            s6 += x[i + 6];
            s7 += x[i + 7];
            i += 8;
        }
        double s = s0 + s1 + s2 + s3 + s4 + s5 + s6 + s7;
        while (i < upTo) {
            s += x[i];
            ++i;
        }
        return s;
    }

    /**
     * Adds a * y to x.
     */
    public static void addMultipleOf(double[] x, double a, double[] y, int offset, int upTo) {
        final int unfoldUpTo = upTo - 7;
        int i = offset;
        while (i < unfoldUpTo) {
            x[i] += a * y[i];
            x[i + 1] += a * y[i + 1];
            x[i + 2] += a * y[i + 2];
            x[i + 3] += a * y[i + 3];
            x[i + 4] += a * y[i + 4];
            x[i + 5] += a * y[i + 5];
            x[i + 6] += a * y[i + 6];
            x[i + 7] += a * y[i + 7];
            i += 8;
        }
        while (i < upTo) {
            x[i] += a * y[i];
            ++i;
        }
    }
}
