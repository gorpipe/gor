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

import org.apache.commons.math3.special.Erf;

class RegressionUtilities {
    private final static double SQRT_2 = 1.4142135623730951D;

    private RegressionUtilities() {}

    /**
     * Takes in a z-statistic and returns the corresponding p-value.
     */
    static double computePValue(double z) {
        return 1 - Erf.erf(Math.abs(z) / SQRT_2);
    }

    static void computePValues(double[] z, double[] pValues) {
        for (int i = 0; i < z.length; ++i) {
            pValues[i] = computePValue(z[i]);
        }
    }

    static double[][] getTriangularMatrix(int dim) {
        final double[][] toReturn = new double[dim][];
        int i = 0;
        while (i < dim) {
            toReturn[i++] = new double[i];
        }
        return toReturn;
    }
}
