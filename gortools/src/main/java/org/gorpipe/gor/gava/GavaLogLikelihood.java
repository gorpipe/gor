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
 * Log-likelihood ratio calculator for variant association.
 *
 * @author gfj
 */
public abstract class GavaLogLikelihood {

    /**
     * Upper threshold for the control allele frequency.
     */
    protected double upperFreqThreshold = 0.3;
    /**
     * Control penetrance (0 for complete penetrance, -1 for no constraint).
     */
    protected int controlPenetrance = -1;
    /**
     * Case penetrance (0 for no locus hetreogeneity, -1 for no constraint).
     */
    protected int casePenetrance = -1;
    /**
     * No max allele count for each case in dominant/recessive models.
     */
    protected boolean noMaxAlleleCounts = false;
    /**
     * Include protective variants in calculations.
     */
    protected boolean includeProtective = false;

    protected int numCases;
    protected int numControls;


    // ---------------------- Setters ---------------------------

    /**
     * @param numCases
     * @param numControls
     */
    public void setSubjectCounts(int numCases, int numControls) {
        this.numCases = numCases;
        this.numControls = numControls;
    }

    /**
     * @param controlPenetrance maximum count among controls, -1 for no constraint
     * @param casePenetrance    true if recessive/dominant with no locus heterogeneity
     * @param noMaxAlleleCounts true if no max allele count for each case in dominant/recessive models
     */
    public void setPenetrance(int controlPenetrance, int casePenetrance, boolean noMaxAlleleCounts) {
        this.controlPenetrance = controlPenetrance;
        this.casePenetrance = casePenetrance;
        this.noMaxAlleleCounts = noMaxAlleleCounts;
    }

    /**
     * @param upperFreqThreshold upper threshold for the allele frequency
     */
    public void setUpperFreqThreshold(double upperFreqThreshold) {
        this.upperFreqThreshold = upperFreqThreshold;
    }

    /**
     * Set if protective variants should be included (default is false).
     *
     * @param includeProtective true to include protective variants, false to exclude
     */
    public void setIncludeProtective(boolean includeProtective) {
        this.includeProtective = includeProtective;
    }


    // ---------------------- Computation methods ---------------------------

    /**
     * Compute the log-likelihood ratio for a feature.
     * The first numControls subjects in the given permutation are used as controls,
     * and the remaining perm.length-numControls are used as cases.
     *
     * @param collapsedCounts the call counts for the subjects (with very rare variants collapsed)
     * @param perm            permutation of the subjects
     * @param debug           if true, print out debug information
     * @return the log-likelihood
     */
    public abstract double computeLogLikelihood(List<CollapsedCounts> collapsedCounts, int[] perm, boolean debug);

    /**
     * Calculate the log-likelihood for a single variant (using the regular inheritance model).
     *
     * @param counts the call counts for the variant
     * @param perm   permutation of the subjects
     * @return the log-likelihood
     */
    public double calcVariantLogLikelihood(VariantCounts counts, int[] perm) {

        int numSubjects = perm.length;

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
        double p = allCopies / (double) (2 * numSubjects);
        double pA = caseCopies / (double) (2 * numCases);
        double pU = Math.min(controlCopies / (double) (2 * numControls), upperFreqThreshold);
        double logDiff = 0.0;
        if (includeProtective || pA > pU) {
            logDiff = Math.log(counts.altScore) - Math.log(counts.nullScore);
            if (pA > 0.0 && pA < 1.0) {
                logDiff += caseCopies * Math.log(pA) + (2 * numCases - caseCopies) * Math.log(1.0 - pA);
            }
            if (pU > 0.0 && pU < 1.0) {
                logDiff += controlCopies * Math.log(pU) + (2 * numControls - controlCopies) * Math.log(1.0 - pU);
            }
            if (p > 0.0 && p < 1.0) {
                logDiff -= allCopies * Math.log(p) + (2 * numSubjects - allCopies) * Math.log(1.0 - p);
            }
        }
        return logDiff;
    }

}
