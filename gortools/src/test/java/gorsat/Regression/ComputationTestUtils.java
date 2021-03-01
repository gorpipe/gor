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

public class ComputationTestUtils {
    public static boolean[] getRandomBooleanVector(int len) {
        final boolean[] toReturn = new boolean[len];
        for (int i = 0; i < len; ++i) {
            toReturn[i] = Math.random() < 0.5;
        }
        return toReturn;
    }

    public static double[] getRandomVector(int dim, double a, double b) {
        final double d = b - a;
        final double[] toReturn = new double[dim];
        for (int i = 0; i < dim; ++i) {
            toReturn[i] = a + d * Math.random();
        }
        return toReturn;
    }

    public static double[][] getRandomMatrix(int dim1, int dim2, double a, double b) {
        final double[][] x = new double[dim1][];
        for (int i = 0; i < dim1; ++i) {
            x[i] = getRandomVector(dim2, a, b);
        }
        return x;
    }
}
