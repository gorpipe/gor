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
package org.gorpipe.gor.haplotype;

/**
 * Checks the correctness of phase of haplotypes/genotypes using basic rules.
 *
 * @author hersir
 * @version $Id$
 */
public class GBasicRulesCalculator {

    /**
     * Indicates that the phase cannot be determined.
     * This happens when neither parent is known,
     * or neither parent is genotyped at the marker.
     * Note that double and triple congruency is indicated specially
     * with {@link #DOUBLE_CONGRUENCY} and {@link #TRIPLE_CONGRUENCY}.
     */
    private static final byte UNDETERMINED_PHASE = 0;

    /**
     * Indicates that the phase is known and correct.
     */
    private static final byte CORRECT_PHASE = 1;

    /**
     * Indicates that the phase is known and incorrect.
     */
    private static final byte INCORRECT_PHASE = 2;

    /**
     * Indicates that the phase cannot be determined because of a double congruency.
     */
    private static final byte DOUBLE_CONGRUENCY = 3;

    /**
     * Indicates that the phase cannot be determined because of a triple congruency.
     */
    private static final byte TRIPLE_CONGRUENCY = 4;

    /**
     * Indicates that the genotypes of the trio are incompatible in some way.
     */
    public static final byte INCOMPATIBLE_GENOTYPES = 5;

    protected byte unknown = Byte.MIN_VALUE;

    /**
     * Set the value for an unknown allele (default is Byte.MIN_VALUE) .
     *
     * @param unknown the unknonw value
     */
    public void setUnknownAllele(byte unknown) {
        this.unknown = unknown;
    }

    /**
     * Check basic rules (inhertance error and phase) at an single autosomal locus for a child in a triad.
     *
     * @param child  the child genotypes
     * @param father the father genotypes
     * @param mother the mother genotypes
     * @return A byte value indicating the phase
     * {@link #UNDETERMINED_PHASE}, {@link #CORRECT_PHASE}, {@link #INCORRECT_PHASE},
     * {@link #DOUBLE_CONGRUENCY}, {@link #TRIPLE_CONGRUENCY}, {@link #INCOMPATIBLE_GENOTYPES}.
     */
    public byte checkBasicRules(byte[] child, byte[] father, byte[] mother) {

        boolean childUnknown = (child[0] == unknown || child[1] == unknown);
        boolean fatherUnknown = (father[0] == unknown || father[1] == unknown);
        boolean motherUnknown = (mother[0] == unknown || mother[1] == unknown);

        if (childUnknown || (fatherUnknown && motherUnknown)) {
            return UNDETERMINED_PHASE;
        }

        boolean childHeterozygous = child[0] != child[1];
        boolean fatherLikeChild = likeChild(father, child);
        boolean motherLikeChild = likeChild(mother, child);

        if (childHeterozygous) {
            if ((fatherUnknown && motherLikeChild) || (motherUnknown && fatherLikeChild)) {
                return DOUBLE_CONGRUENCY;
            }
            if (fatherLikeChild && motherLikeChild) {
                // if both parents share the same heterozygous genotype as the child
                // the phase cannot be determined (triple-heterozygosity)
                return TRIPLE_CONGRUENCY;
            }
        }

        // If here we don't have double- or triple-heterozygosity.
        // Check if the phase is correct or incorrect, or else the genotypes are incompatible.
        // Note that this handles child homozygosity correctly,
        // and checks for compatibility in that case too.
        if ((fatherUnknown || (child[0] == father[0] || child[0] == father[1])) &&
                (motherUnknown || (child[1] == mother[0] || child[1] == mother[1]))) {
            return CORRECT_PHASE;
        }

        if ((motherUnknown || (child[0] == mother[0] || child[0] == mother[1])) &&
                (fatherUnknown || (child[1] == father[0] || child[1] == father[1]))) {
            return INCORRECT_PHASE;
        }

        return INCOMPATIBLE_GENOTYPES;
    }

    private boolean likeChild(byte[] parent, byte[] child) {
        if (parent[0] == child[0] && parent[1] == child[1]) return true;
        return parent[0] == child[1] && parent[1] == child[0];
    }

}
