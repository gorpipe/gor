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
package org.gorpipe.gor.util;

/**
 * Fisher's exact test for 2-by-2 tables.
 *
 * @author gfj
 * @version $Id$
 */
@SuppressWarnings("squid:S00117")
public class GFisherExact2by2 {
    private GFisherExact2by2() {}

    /**
     * Compute the probability for a specific tail for the table {{A, B}, {C, D}},
     * that is, towards large A and D and small B and C.
     * (This was factored out of the old Haplo Analyzer and is used when computing association).
     *
     * @param A the a
     * @param B the b
     * @param C the c
     * @param D the d
     * @return the p-value
     */
    public static double compute(int A, int B, int C, int D) {
        int n = A + B + C + D;

        double F = GLogGamma.get(A + B) + GLogGamma.get(C + D) + GLogGamma.get(A + C) + GLogGamma.get(B + D) - GLogGamma.get(n);
        double P = 0;

        int steps = Math.min(C, B);

        for (int i = steps; i >= 0; --i) {
            P += Math.exp(F - GLogGamma.get(A + i) - GLogGamma.get(D + i) - GLogGamma.get(B - i) - GLogGamma.get(C - i));
        }

        return P;
    }

    /**
     * Compute the one-tailed probability for the table {{A, B}, {C, D}}.
     *
     * @param A the a
     * @param B the b
     * @param C the c
     * @param D the d
     * @return the p-value
     */
    public static double computeOneTailed(int A, int B, int C, int D) {
        int n = A + B + C + D;

        double F = GLogGamma.get(A + B) + GLogGamma.get(C + D) + GLogGamma.get(A + C) + GLogGamma.get(B + D) - GLogGamma.get(n);

        int minAD = Math.min(A, D);
        int minBC = Math.min(B, C);

        double Pleft = 0.0;
        for (int i = minAD; i >= 0; --i) {
            Pleft += Math.exp(F - GLogGamma.get(A - i) - GLogGamma.get(D - i) - GLogGamma.get(B + i) - GLogGamma.get(C + i));
        }
        double Pright = 0.0;
        for (int i = minBC; i >= 0; --i) {
            Pright += Math.exp(F - GLogGamma.get(A + i) - GLogGamma.get(D + i) - GLogGamma.get(B - i) - GLogGamma.get(C - i));
        }

        return Math.min(Pleft, Pright);
    }

    /**
     * Compute the two-tailed probability for the table {{A, B}, {C, D}}.
     *
     * @param A the a
     * @param B the b
     * @param C the c
     * @param D the d
     * @return the p-value
     */
    public static double computeTwoTailed(int A, int B, int C, int D) {
        int n = A + B + C + D;

        double F = GLogGamma.get(A + B) + GLogGamma.get(C + D) + GLogGamma.get(A + C) + GLogGamma.get(B + D) - GLogGamma.get(n);
        double Pbase = Math.exp(F - GLogGamma.get(A) - GLogGamma.get(D) - GLogGamma.get(B) - GLogGamma.get(C));

        int minAD = Math.min(A, D);
        int minBC = Math.min(B, C);

        double P;
        double Ptails = 0.0;
        for (int i = -minAD; i <= minBC; ++i) {
            P = Math.exp(F - GLogGamma.get(A + i) - GLogGamma.get(D + i) - GLogGamma.get(B - i) - GLogGamma.get(C - i));
            if (P <= Pbase) {
                Ptails += P;
            }
        }
        return Ptails;
    }

}
