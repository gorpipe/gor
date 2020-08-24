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
package org.gorpipe.gor.gava;

import java.util.List;

/**
 * Log likelihood calculator for regular variant association.
 *
 * @author gfj
 */
public class RegularLogLikelihood extends GavaLogLikelihood {

    /**
     * @param collapsedCounts the call counts for the subjects (with very rare variants collapsed)
     * @param perm            permutation of the subjects
     * @param debug           if true, print out debug information
     * @return
     */
    @SuppressWarnings("squid:S3776") /* Added suppress warning on sonarqube since we are only moving this class */
    @Override
    public double computeLogLikelihood(List<CollapsedCounts> collapsedCounts, int[] perm, boolean debug) {

        int numSubjects = perm.length;
        assert numSubjects == numCases + numControls;

        double logSum = 0.0;
        for (CollapsedCounts counts : collapsedCounts) {
            int caseCopies = 0;
            int controlCopies = 0;
            for (int i = 0; i < numSubjects; ++i) {
                if (i < numControls) {
                    controlCopies += counts.callCounts[perm[i]];
                } else {
                    caseCopies += counts.callCounts[perm[i]];
                }
            }
            int allCopies = caseCopies + controlCopies;
            int groupSize = counts.numVariants;
            double p = allCopies / (double) (2 * groupSize * numSubjects);
            double pA = caseCopies / (double) (2 * groupSize * numCases);
            double pU = Math.min(controlCopies / (double) (2 * groupSize * numControls), upperFreqThreshold);
            if (includeProtective || pA > pU) {
                double logDiff = Math.log(counts.nullScore) - Math.log(counts.altScore);
                if (p > 0.0 && p < 1.0) {
                    logDiff += allCopies * Math.log(p) + (2 * groupSize * numSubjects - allCopies) * Math.log(1.0 - p);
                }
                if (pA > 0.0 && pA < 1.0) {
                    logDiff -= caseCopies * Math.log(pA) + (2 * groupSize * numCases - caseCopies) * Math.log(1.0 - pA);
                }
                if (pU > 0.0 && pU < 1.0) {
                    logDiff -= controlCopies * Math.log(pU) + (2 * groupSize * numControls - controlCopies) * Math.log(1.0 - pU);
                }
                if (logDiff < 0.0) { // Exclude if alt is less likely than null (can happen if frequency contraint)
                    logSum += logDiff;
                }
            }
            if (debug) {
                String prefix = (includeProtective || pA > pU) ? "incl" : "excl";
                System.err.println(prefix + "\t" + caseCopies + "\t" + controlCopies + "\t" + pA + "\t" + pU + "\t" + (-2 * logSum));
            }
        }
        return logSum;
    }


}
