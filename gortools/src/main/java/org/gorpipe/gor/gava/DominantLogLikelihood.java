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
 * Log likelihood calculator for dominant variant association.
 *
 * @author gfj
 */
public class DominantLogLikelihood extends GavaLogLikelihood {

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

        double[] altValues = calcVariantLikelihoods(collapsedCounts, perm);
        int[] order = MathArrayUtils.getSortingPermutationInt(altValues);
        int[] pnAlleleCounts = new int[numSubjects];
        boolean[] hasDiseaseVariant = new boolean[numSubjects];
        int numAffWithDiseaseVariant = 0;
        double logSum = 0.0;
        for (int j = order.length - 1; j >= 0; --j) {
            CollapsedCounts counts = collapsedCounts.get(order[j]);
            int caseCopies = 0;
            int controlCopies = 0;
            for (int i = 0; i < numSubjects; ++i) {
                int count = counts.callCounts[perm[i]];
                if (count > 0) {
                    if (i < numControls) {
                        ++controlCopies;
                    } else if (pnAlleleCounts[i] == 0 || noMaxAlleleCounts) {
                        ++caseCopies;
                    }
                    ++pnAlleleCounts[i];
                }
            }
            if (controlPenetrance >= 0 && controlCopies > controlPenetrance) {
                break;
            }
            int allCopies = caseCopies + controlCopies;
            double p = allCopies / (double) numSubjects;
            double pA = caseCopies / (double) numCases;
            double pU = Math.min(controlCopies / (double) numControls, upperFreqThreshold);
            if (includeProtective || pA > pU) {
                double logDiff = Math.log(counts.nullScore) - Math.log(counts.altScore);
                if (p > 0.0 && p < 1.0) {
                    logDiff += allCopies * Math.log(p) + (numSubjects - allCopies) * Math.log(1.0 - p);
                }
                if (pA > 0.0 && pA < 1.0) {
                    logDiff -= caseCopies * Math.log(pA) + (numCases - caseCopies) * Math.log(1.0 - pA);
                }
                if (pU > 0.0 && pU < 1.0) {
                    logDiff -= controlCopies * Math.log(pU) + (numControls - controlCopies) * Math.log(1.0 - pU);
                }
                if (logDiff < 0.0) { // Exclude if alt is less likely than null (can happen if frequency contraint)
                    logSum += logDiff;
                    if (casePenetrance >= 0) {
                        for (int i = numControls; i < numSubjects; ++i) {
                            int count = counts.callCounts[perm[i]];
                            if (count > 0) {
                                if (!hasDiseaseVariant[i]) {
                                    hasDiseaseVariant[i] = true;
                                    ++numAffWithDiseaseVariant;
                                }
                            }
                        }
                    }
                }
            }
            if (debug) {
                String prefix = (includeProtective || pA > pU) ? "incl" : "excl";
                System.err.println(prefix + "\t" + caseCopies + "\t" + controlCopies + "\t" + pA + "\t" + pU + "\t" + (-2 * logSum));
            }
        }
        if (casePenetrance >= 0 && numAffWithDiseaseVariant < numCases - casePenetrance) {
            return 0;
        }
        return logSum;
    }


    private double[] calcVariantLikelihoods(List<CollapsedCounts> collapsedCounts, int[] perm) {

        int numSubjects = perm.length;

        double[] altValues = new double[collapsedCounts.size()];
        for (int j = 0; j < collapsedCounts.size(); ++j) {
            CollapsedCounts counts = collapsedCounts.get(j);
            int caseCopies = 0;
            int controlCopies = 0;
            for (int i = 0; i < numSubjects; ++i) {
                if (counts.callCounts[perm[i]] > 0) {
                    if (i < numControls) {
                        ++controlCopies;
                    } else {
                        ++caseCopies;
                    }
                }
            }
            int allCopies = caseCopies + controlCopies;
            double p = allCopies / (double) numSubjects;
            double pA = caseCopies / (double) numCases;
            double pU = Math.min(controlCopies / (double) numControls, upperFreqThreshold);
            if (includeProtective || pA > pU) {
                altValues[j] = Math.log(counts.altScore) - Math.log(counts.nullScore);
                if (pA > 0.0 && pA < 1.0) {
                    altValues[j] += caseCopies * Math.log(pA) + (numCases - caseCopies) * Math.log(1.0 - pA);
                }
                if (pU > 0.0 && pU < 1.0) {
                    altValues[j] += controlCopies * Math.log(pU) + (numControls - controlCopies) * Math.log(1.0 - pU);
                }
                if (p > 0.0 && p < 1.0) {
                    altValues[j] -= allCopies * Math.log(p) + (numSubjects - allCopies) * Math.log(1.0 - p);
                }
            }
        }
        return altValues;

    }

}
