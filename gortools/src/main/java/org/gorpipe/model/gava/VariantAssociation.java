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
package org.gorpipe.model.gava;

import cern.jet.stat.Probability;
import org.gorpipe.model.genome.CancelMonitor;

import java.util.*;

/**
 * Variant association algorithm, based on the VAAST algorithm.
 * http://www.ncbi.nlm.nih.gov/pubmed/21700766
 * <p>
 * Computes a p-value for a genomic feature (typically a single gene) using a
 * randomization test, where the affection status of the subjects is permuted
 * and the test statistic computed using a composite likelihood ratio test
 * as described in the VAAST paper.
 * <p>
 * Note: This is not an exact implementation and for example we have a bailout
 * parameter to avoid spending to much computing time on uninteresting features.
 * <p>
 * Usage: Need to first set the case and control lists, the disease model (regular,
 * dominant, or recessive), and other parameters. Then the procedure is as follows:
 * - Before processing a feature, call initializeGroup() to clear the counts.
 * - Iterate through the data for the feature and for each variant found for some
 * subject, add the call counts using addFeature().
 * - Calculate the p-value (and a few other values) using calculateValues().
 * Repeat the process for the next feature.
 *
 * @author gfj
 */
public class VariantAssociation {

    // Ignore difference in values below this threshold when comparing test statistics.
    private static final Double EPSILON = 1e-16;

    /**
     * Number of random permutations.
     */
    private int maxIterations = 1000;
    private int bailOutAfter = 51;

    private int collapseThreshold = 5;
    protected boolean includeProtective = false;

    private GavaLogLikelihood gavaLogLikelihood;

    private CancelMonitor cancelMonitor;
    private boolean debug;

    private String[] pns;
    private int numCases;
    private int numControls;
    private HashMap<String, Integer> pn2index;

    private HashMap<String, VariantCounts> site2counts;

    /**
     * Instantiate the class.
     */
    @SuppressWarnings("squid:S1186") /* Added suppress warning on sonarqube since we are only moving this class */
    public VariantAssociation() {
    }

    /**
     * @param cases    the cases
     * @param controls the controls
     */
    public void setPnLists(String[] cases, String[] controls) {
        numCases = cases.length;
        numControls = controls.length;
        pns = new String[numCases + numControls];
        System.arraycopy(controls, 0, pns, 0, numControls);
        System.arraycopy(cases, 0, pns, numControls, numCases);
        pn2index = new HashMap<>(pns.length);
        for (int i = 0; i < pns.length; ++i) {
            pn2index.put(pns[i], i);
        }
        site2counts = new HashMap<>();
    }

    /**
     * Set the disease model (inheritance and penetrance patterns).
     *
     * @param recessiveDisease   true if recessive disease
     * @param dominantDisease    true if dominant disease
     * @param controlPenetrance  maximum count among controls, -1 for no constraint
     * @param casePenetrance     true if recessive/dominant with no locus heterogeneity
     * @param noMaxAlleleCounts  true if no max allele count for each case in dominant/recessive models
     * @param upperFreqThreshold upper threshold for the allele frequency
     * @param includeProtective  true to include protective variants, false to exclude
     */
    public void setModel(boolean recessiveDisease, boolean dominantDisease,
                         int controlPenetrance, int casePenetrance, boolean noMaxAlleleCounts,
                         double upperFreqThreshold, boolean includeProtective) {
        if (recessiveDisease) {
            gavaLogLikelihood = new RecessiveLogLikelihood();
        } else if (dominantDisease) {
            gavaLogLikelihood = new DominantLogLikelihood();
        } else {
            gavaLogLikelihood = new RegularLogLikelihood();
        }
        gavaLogLikelihood.setPenetrance(controlPenetrance, casePenetrance, noMaxAlleleCounts);
        gavaLogLikelihood.setUpperFreqThreshold(upperFreqThreshold);
        this.includeProtective = includeProtective;
        gavaLogLikelihood.setIncludeProtective(includeProtective);
    }

    /**
     * @param maxIterations the maximum number of random permutations for the p-value
     * @param bailOutAfter  number of hits to trigger bailout
     */
    public void setNumRandomIterations(int maxIterations, int bailOutAfter) {
        this.maxIterations = maxIterations;
        this.bailOutAfter = bailOutAfter;
    }

    /**
     * @param collapseThreshold collapse variants with fewer than this number of alleles among affecteds
     */
    public void setCollapseThreshold(int collapseThreshold) {
        this.collapseThreshold = collapseThreshold;
    }

    /**
     * @param cancelMonitor cancel monitor
     */
    public void setCancelMonitor(CancelMonitor cancelMonitor) {
        this.cancelMonitor = cancelMonitor;
    }


    /**
     * @param debug if true, print out debug information
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Initialize the algorithm for the next feature.
     * (Clears the data containers.)
     */
    public void initializeGroup() {
        site2counts.clear();
    }

    /**
     * Add a variant.
     *
     * @param variantString a tab delimited string with variantId,pn,callCopies,nullScore,altScore
     */
    public void addVariant(String variantString) {
        String[] cols = variantString.split("\t");
        String hash = cols[0];
        String pn = cols[1];
        int callCopies = Integer.parseInt(cols[2]);
        double nullScore = (cols.length > 4) ? Double.parseDouble(cols[4]) : 1.0;
        double altScore = (cols.length > 5) ? Double.parseDouble(cols[5]) : 1.0;
        if (pn2index.containsKey(pn)) {
            int index = pn2index.get(pn);
            if (!site2counts.containsKey(hash)) {
                site2counts.put(hash, new VariantCounts(new int[pns.length], nullScore, altScore));
            }
            site2counts.get(hash).callCounts[index] += callCopies;
        }
    }

    /**
     * Calculate the p-value and other values for the current genetic feature.
     *
     * @return the values as a tab delimited string
     */
    @SuppressWarnings("squid:S3776") /* Added suppress warning on sonarqube since we are only moving this class */
    public String calculateValues() {

        VariantCounts[] variantCounts = site2counts.values().toArray(new VariantCounts[site2counts.size()]);
        if (!includeProtective) {
            variantCounts = filterVariants(variantCounts);
        }

        // Prioritize by score.
        Arrays.sort(variantCounts, Comparator.comparingDouble(o -> o.nullScore));

        gavaLogLikelihood.setSubjectCounts(numCases, numControls);
        int[] perm = new int[pns.length];
        for (int i = 0; i < pns.length; ++i) {
            perm[i] = i;
        }
        double baseValue = computeTestStatistic(variantCounts, perm, debug);
        int counter = 0;
        int iter = 0;
        double permPval;
        if (-baseValue <= 0.05 && -baseValue != 0.0) {
            if (pns.length > 1000 || CombinationIterator.getNumberOfCombinations(pns.length, numControls) > Math.min(maxIterations, 10000)) {
                Random random = new Random();
                while (iter < maxIterations && counter < bailOutAfter) {
                    ++iter;
                    for (int i = pns.length; i > 1; --i) {
                        int r = random.nextInt(i);
                        int temp = perm[r];
                        perm[r] = perm[i - 1];
                        perm[i - 1] = temp;
                    }
                    double randomValue = computeTestStatistic(variantCounts, perm, false);

                    if (randomValue + EPSILON >= baseValue) ++counter;
                    if (cancelMonitor != null && cancelMonitor.isCancelled()) {
                        break;
                    }
                }
            } else {
                CombinationIterator combit = new CombinationIterator(pns.length, numControls);
                while (combit.next()) {
                    ++iter;
                    double combValue = computeTestStatistic(variantCounts, combit.getPermutation(), false);
                    if (combValue + EPSILON >= baseValue) ++counter;
                    if (cancelMonitor != null && cancelMonitor.isCancelled()) {
                        break;
                    }
                }
            }
            permPval = counter / (double) iter;
        } else {
            permPval = -baseValue;

            if (baseValue == -0.0) {
                baseValue = -1.0;
            }
        }

        double chiPval = -baseValue;
        if (debug) {
            System.err.println("Returning values:\t" + chiPval + "\t" + permPval + "\t" + iter);
        }

        if (permPval < chiPval) {
            permPval = chiPval;
        }
        if (permPval == 0.0) {
            permPval = chiPval;
        }
        return chiPval + "\t" + permPval + "\t" + iter;
    }


    private double computeTestStatistic(VariantCounts[] variantCounts, int[] perm, boolean isDebug) {
        ArrayList<CollapsedCounts> collapsedCounts = groupVariants(variantCounts, perm);
        double chi2 = -2 * gavaLogLikelihood.computeLogLikelihood(collapsedCounts, perm, isDebug);
        return -Probability.chiSquareComplemented(collapsedCounts.size(), chi2);
    }

    private VariantCounts[] filterVariants(VariantCounts[] variantCounts) {
        ArrayList<VariantCounts> filteredCounts = new ArrayList<>();
        for (VariantCounts varCounts : variantCounts) {
            int[] counts = varCounts.callCounts;
            int caseCopies = 0;
            int controlCopies = 0;
            for (int i = 0; i < numControls + numCases; ++i) {
                if (i < numControls) {
                    controlCopies += counts[i];
                } else {
                    caseCopies += counts[i];
                }
            }
            double pA = caseCopies / (double) (2 * numCases);
            double pU = controlCopies / (double) (2 * numControls);
            if (pA > pU) {
                filteredCounts.add(varCounts);
            }
        }
        return filteredCounts.toArray(new VariantCounts[filteredCounts.size()]);
    }

    private ArrayList<CollapsedCounts> groupVariants(VariantCounts[] variantCounts, int[] perm) {
        int[] counts = getCaseCounts(variantCounts, perm);
        CollapsedCounts lastTooFew = null;
        int tooFewSum = 0;
        ArrayList<CollapsedCounts> collapsedCounts = new ArrayList<>();
        for (int j = 0; j < variantCounts.length; ++j) {
            double lrValue = gavaLogLikelihood.calcVariantLogLikelihood(variantCounts[j], perm);
            if (lrValue > 0.0) {
                if (counts[j] < collapseThreshold) {
                    if (lastTooFew != null && tooFewSum < collapseThreshold) {
                        lastTooFew.addVariant(variantCounts[j]);
                        tooFewSum += counts[j];
                    } else {
                        lastTooFew = new CollapsedCounts(variantCounts[j]);
                        collapsedCounts.add(lastTooFew);
                        tooFewSum = counts[j];
                    }
                } else {
                    collapsedCounts.add(new CollapsedCounts(variantCounts[j]));
                }
            }
        }
        return collapsedCounts;
    }

    private int[] getCaseCounts(VariantCounts[] variantCounts, int[] perm) {
        int[] alleleCounts = new int[variantCounts.length];
        for (int j = 0; j < variantCounts.length; ++j) {
            int[] counts = variantCounts[j].callCounts;
            for (int i = numControls; i < perm.length; ++i) {
                alleleCounts[j] += counts[perm[i]];
            }
        }
        return alleleCounts;
    }
}
