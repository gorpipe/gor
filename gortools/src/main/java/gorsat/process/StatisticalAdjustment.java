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

package gorsat.process;

import cern.jet.stat.Probability;
import org.apache.commons.math3.util.FastMath;

public class StatisticalAdjustment {
    private static final double CHI_INV_HALF = 0.4549364231195736D;
    private static final double EULER_MASCHERONI = 0.5772156649015328606065121D;

    private StatisticalAdjustment() {}

    /**
     * The genomic control corrected chi-2 statistic is computed by dividing the initial chi-2 statistic by lambda
     * (the genomic inflation estimate).
     *
     * If we have a z statistic &gt; 0, then the corresponding the chi statistic is z^2. The correctied chi-statistic is
     * z^2 / lambda so the corrected p-value is
     *
     *  {@literal P(CHI_2(1) > z^2/lambda) = P(N(0,1) > z / sqrt(lambda) or N(0,1) < -z / sqrt(lambda)) = 2 * P(N(0,1) < -z / sqrt(lambda))}
     *
     * Which explains the formulas in the next two methods.
     */
    public static double genomic_control_correct_z(double z, double invSqrtLambda) {
        return 2 * Probability.normal(-z * invSqrtLambda);
    }

    public static double genomic_control_correct_p(double pValue, double invSqrtLambda) {
        return genomic_control_correct_z(Math.abs(Probability.normalInverse(pValue / 2)), invSqrtLambda);
    }

    /**
     * Returns 1 / sqrt(lambda) where lambda is the genomic inflation estimate. The genomic inflation estimate
     * is computed by taking the median of the chi stats (with 1 df) from which the p values are computed and dividing
     * it by chi_inv_half.
     *
     * Let C be a CHI_2(1) random variable and N a N(0,1) random variable. Then C ~ N^2 so
     *
     *    {@literal P(C > x^2) = P(N > |x| or N < -|x|) = 2 * P(N < -|x|).}
     *
     * Thus, we can compute the chi-stats from the p values by normalInverse(p / 2)^2.
     */
    public static double getInvSqrtLambda_p(double[] p) {
        final int n = p.length;
        final int mIdx = n >>> 1;
        final double denom = (n & 1) == 0 ?
                (squared(Probability.normalInverse(p[mIdx - 1] / 2)) + squared(Probability.normalInverse(p[mIdx] / 2))) / 2
                : squared(Probability.normalInverse(p[mIdx] / 2));
        return Math.sqrt(CHI_INV_HALF / denom);
    }

    public static double bonferroni(double pValue, int len) {
        return Math.min(pValue * len, 1.0);
    }

    /**
     * Runs a Holm-Bonferroni correction on an ordered vector of p values.
     */
    public static void holm_bonferroni(double[] pValues, double[] holm) {
        int i = 0, mult = pValues.length;
        double adjP = Double.NEGATIVE_INFINITY;
        while (i < pValues.length) {
            adjP = Math.max(adjP, mult * pValues[i]);
            if (adjP > 1.0) break;
            holm[i] = adjP;
            --mult;
            ++i;
        }

        while (i < pValues.length) {
            holm[i++] = 1.0;
        }
    }

    public static double sidak_ss(double pValue, int len) {
        return 1.0 - pow(1.0 - pValue, len);
    }

    /**
     * Runs a Sidak step down correction on an ordered vector of p values.
     */
    public static void sidak_sd(double[] pValues, double[] sidak) {
        final int n = pValues.length;
        int k = n;
        double last = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; ++i) {
            sidak[i] = (last = Math.max(1 - pow(1 - pValues[i], k--), last));
        }
    }

    /**
     * Runs a Benjamini Hochberg correction on an ordered vector of p values.
     */
    public static void benjamini_hochberg(double[] pValues, double[] bh) {
        final int m = pValues.length;
        if (m == 0) return;

        double last_bh = Double.POSITIVE_INFINITY;
        int n = m;
        while (n != 0) {
            last_bh = Math.min(last_bh, (double) m / n * pValues[--n]);
            bh[n] = last_bh;
        }
    }

    /**
     * Runs a Benjamini-Yekutieli correction on an ordered vector of p values.
     */
    public static void benjamini_yekutieli(double[] pValues, double[] by) {
        final int m = pValues.length;
        if (m == 0) return;

        final double mcm = m * harmonic(m);
        int n = m;
        double last_by;
        while ((last_by = mcm / n * pValues[--n]) > 1.0) {
            by[n] = 1.0;
            if (n == 0) return;
        }
        by[n] = last_by;
        while (n != 0) {
            last_by = Math.min(last_by, mcm / n * pValues[--n]);
            by[n] = last_by;
        }
    }

    /**
     * Computes b^e in log_2(e) time.
     */
    static double pow(double b, int e) {
        if (e == 0) return 1.0;
        double res = 1.0;
        double mult = b;
        do {
            if ((e & 1) == 1) {
                res *= mult;
            }
            e >>>= 1;
            if (e == 0) break;
            mult *= mult;
        } while (true);
        return res;
    }

    static double squared(double a) {
        return a * a;
    }

    /**
     * Computes 1/1 + 1/2 + 1/3 + ... + 1/n.
     */
    static double harmonic(int n) {
        if (n < 10) {
            switch (n) {
                case 1: return 1;
                case 2: return 1.5;
                case 3: return 11.0 / 6.0;
                case 4: return 25.0 / 12.0;
                case 5: return 137.0 / 60.0;
                case 6: return 49.0 / 20.0;
                case 7: return 363.0 / 140.0;
                case 8: return 761.0 / 280.0;
                case 9: return 7129.0 / 2520.0;
                default: throw new IllegalArgumentException();
            }
        } else {
            final double rec_n2 = 1.0 / (n * n);
            return FastMath.log(n) + EULER_MASCHERONI + 1.0 / (2 * n) +
                    rec_n2 * (-1.0 / 12.0 + rec_n2 * (1.0 / 120.0 + rec_n2 * (-1.0 / 252.0 + rec_n2 * (1.0 / 240.0 + rec_n2 * (-1.0 / 132.0 + rec_n2 * 691.0 / 32760.0)))));
        }
    }

    /**
     * Inverts a permutation.
     */
    public static void invert(int[] perm) {
        final boolean[] covered = new boolean[perm.length];
        int i = 0;
        while (i < perm.length) {
            int j = i;
            int jth = perm[j];
            while (jth != i) {
                final int tmp = perm[jth];
                perm[jth] = j;
                covered[jth] = true;
                j = jth;
                jth = tmp;
            }
            perm[i] = j;
            covered[i++] = true;
            while (i < perm.length && covered[i]) ++i;
        }
    }
}
