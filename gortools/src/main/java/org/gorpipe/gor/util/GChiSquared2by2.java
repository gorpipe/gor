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
package org.gorpipe.gor.util;

import cern.jet.stat.Probability;

/**
 * Chi-squared for 2-by-2 contingency tables.
 *
 * @author gfj
 * @version $Id$
 */
public class GChiSquared2by2 {

    private GChiSquared2by2() {}

    private static final boolean USE_CONTINUITY_CORRECTION = true;

    /**
     * Compute the Pearson's chi-squared value for the 2-by-2 table
     * [o11 o12]
     * [o21 o22].
     *
     * @param o11 entry (1,1)
     * @param o12 entry (1,2)
     * @param o21 entry (2,1)
     * @param o22 entry (2,2)
     * @return the chi squared value
     */
    public static double computePearsonChiSquared(int o11, int o12, int o21, int o22) {
        // Use double for row/column sums to avoid overflow in multiplication below
        double row1 = o11 + o12;
        double row2 = o21 + o22;
        double col1 = o11 + o21;
        double col2 = o12 + o22;
        double total = row1 + row2;
        if (total < 0.5) return 0;
        double D = o11 * o22 - o12 * o21;
        if (USE_CONTINUITY_CORRECTION) {
            D = Math.abs(D) - total * 0.5;
        }
        return total * D * D / (row1 * row2 * col1 * col2);
    }

    /**
     * Compute the likelihood ratio chi-squared value for the 2-by-2 table
     * [o11 o12]
     * [o21 o22].
     *
     * @param o11 entry (1,1)
     * @param o12 entry (1,2)
     * @param o21 entry (2,1)
     * @param o22 entry (2,2)
     * @return the chi squared value
     */
    public static double computeLogLikelihoodChiSquared(int o11, int o12, int o21, int o22) {
        double chisq = Math.min(2 * (logLikelihood(o11, o12) + logLikelihood(o21, o22)
                - logLikelihood(o11 + o21, o12 + o22)), 999.9);
        return (chisq < 1e-12) ? 0.0 : chisq;
    }

    /**
     * Binomial log-likelihood.
     */
    private static double logLikelihood(double a, double b) {
        double p = b / (a + b);
        return alogx(a, 1.0 - p) + alogx(b, p);
    }

    /**
     * Compute a * log(x)
     */
    private static double alogx(double a, double x) {
        return (a == 0.0) ? 0.0 : a * Math.log(x);
    }

    /**
     * Get the p-value from a chi-squared value for a 2-by-2 table.
     * That is, get the p-value for a chi-squared distribution with 1 degree of freedom.
     *
     * @param chisq the chi-squared value
     * @return the p-value
     */
    public static double getPValue(double chisq) {
        return Probability.chiSquareComplemented(1, chisq);
    }

    /**
     * Get the chi-squared value corresponding to a p-value (assuming 1 degree of freedom).
     *
     * @param pvalue the p-value
     * @return the chi-squared value
     */
    public static double getChiSqValue(double pvalue) {
        if (pvalue / 2.0 > 0.0) {
            double ninv = Probability.normalInverse(pvalue / 2.0);
            return ninv * ninv;
        } else if (pvalue > -1e-10) {
            return Double.POSITIVE_INFINITY;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Get the chi-squared value corresponding to a p-value (assuming 1 degree of freedom),
     * but if the p-value is zero compute the chi-squared value directly from the table.
     * This is preferable to returning an infinite value, if the chi-squared values are used
     * for correcting the p-values (genomic control).
     *
     * @param pvalue the p-value
     * @param o11    entry (1,1)
     * @param o12    entry (1,2)
     * @param o21    entry (2,1)
     * @param o22    entry (2,2)
     * @return the chi-squared value
     */
    public static double getFiniteChiSqValue(double pvalue, int o11, int o12, int o21, int o22) {
        if (pvalue / 2.0 > 0.0) {
            double ninv = Probability.normalInverse(pvalue / 2.0);
            return ninv * ninv;
        } else {
            return computePearsonChiSquared(o11, o12, o21, o22);
        }
    }
}
