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

package org.gorpipe.gor.model;

import junit.framework.TestCase;
import org.gorpipe.gor.model.ChromoCache;

/**
 * Test the ChromoCache
 *
 * @version $Id$
 */
public class UTestChromoCache extends TestCase {
    /**
     * Test that standard human chromosomes work
     */
    public void testStandardHumanChromosomes() {
        final ChromoCache cache = new ChromoCache();
        assertEquals(26, cache.getChromoCnt());
        checkStandardHumanChromosomes(cache);
        checkLexicoOrder(cache);
    }

    /**
     * Test that adding chromosomes work
     */
    public void testAddingChromosomes() {
        final ChromoCache cache = new ChromoCache();
        final String[] newchromos = {"chrKalli", "chr100", "chrPalli", "chrA", "chr30", "chr76", "chr220"};
        for (String chromo : newchromos) {
            final int id = cache.addChromosome(chromo);
            checkLexicoOrder(cache);
            checkChromo(cache, chromo, id);
        }

        checkStandardHumanChromosomes(cache);
    }

    /**
     * Test that adding chromosomes through lookup
     */
    public void testAddingChromosomesDuringLookup() {
        final ChromoCache cache = new ChromoCache();
        final String[] newchromos = {"chrKalli", "chr100", "chrPalli", "chrA", "chr30", "chr76", "chr220"};
        for (String chromo : newchromos) {
            final int id = cache.prefixToChromosomeIdOrUnknown(chromo, true);
            checkLexicoOrder(cache);
            checkChromo(cache, chromo, id);
        }

        checkStandardHumanChromosomes(cache);
    }


    /**
     * Test that ischromo methods work
     */
    public void testIsChromo() {
        assertTrue(ChromoCache.isChr('c', 'h', 'r'));
        assertFalse(ChromoCache.isChr('c', 'H', 'r'));
        assertFalse(ChromoCache.isChr('c', 'h', 'q'));
        assertTrue(ChromoCache.isChrIgnoreCase('c', 'H', 'r'));
        assertFalse(ChromoCache.isChrIgnoreCase('c', 'H', 'q'));
    }

    /**
     * Test toIdOrUnknown
     */
    public void testToIdOrUnknown() {
        final ChromoCache cache = new ChromoCache();
        assertEquals(1, cache.toIdOrUnknown("chr1", 4, false));
        assertEquals(1, cache.toIdOrUnknown("CHR1", 4, false));
        assertEquals(15, cache.toIdOrUnknown("chr15", 5, false));
        assertEquals(23, cache.toIdOrUnknown("chrX", 4, false));
        assertEquals(24, cache.toIdOrUnknown("chrXY", 5, false));
        assertEquals(0, cache.toIdOrUnknown("chrM", 4, false));

        assertEquals(1, cache.toIdOrUnknown("chr1\t12345\tA", 12, false));
        assertEquals(1, cache.toIdOrUnknown("CHR1\t12345\tA", 12, false));
        assertEquals(15, cache.toIdOrUnknown("chr15\t12345\tA", 13, false));
        assertEquals(23, cache.toIdOrUnknown("chrX\t12345\tA", 12, false));
        assertEquals(24, cache.toIdOrUnknown("chrXY\t12345\tA", 13, false));
        assertEquals(0, cache.toIdOrUnknown("chrM\t12345\tA", 12, false));

    }

    /**
     * Test prefixToChromosomeIdOrUnknown
     */
    public void testPrefixedChrToIdOrUnknown() {
        final ChromoCache cache = new ChromoCache();
        assertEquals(1, cache.prefixedChrToIdOrUnknown("chr1\t".getBytes(), 0, false));
        assertEquals(-1, cache.prefixedChrToIdOrUnknown("CHR1\t".getBytes(), 0, false));  // not case insensitive
        assertEquals(15, cache.prefixedChrToIdOrUnknown("chr15\t".getBytes(), 0, false));
        assertEquals(23, cache.prefixedChrToIdOrUnknown("chrX\t".getBytes(), 0, false));
        assertEquals(24, cache.prefixedChrToIdOrUnknown("chrXY\t".getBytes(), 0, false));
        assertEquals(0, cache.prefixedChrToIdOrUnknown("chrM\t".getBytes(), 0, false));

        assertEquals(1, cache.prefixedChrToIdOrUnknown("chr1\t12345\tA".getBytes(), 0, false));
        assertEquals(-1, cache.prefixedChrToIdOrUnknown("CHR1\t12345\tA".getBytes(), 0, false));  // not case insensitive
        assertEquals(15, cache.prefixedChrToIdOrUnknown("chr15\t12345\tA".getBytes(), 0, false));
        assertEquals(23, cache.prefixedChrToIdOrUnknown("chrX\t12345\tA".getBytes(), 0, false));
        assertEquals(24, cache.prefixedChrToIdOrUnknown("chrXY\t12345\tA".getBytes(), 0, false));
        assertEquals(0, cache.prefixedChrToIdOrUnknown("chrM\t12345\tA".getBytes(), 0, false));

        assertEquals(1, cache.prefixedChrToIdOrUnknown("1\t".getBytes(), 0, false));
        assertEquals(15, cache.prefixedChrToIdOrUnknown("15\t".getBytes(), 0, false));
        assertEquals(23, cache.prefixedChrToIdOrUnknown("X\t".getBytes(), 0, false));
        assertEquals(24, cache.prefixedChrToIdOrUnknown("XY\t".getBytes(), 0, false));
        assertEquals(0, cache.prefixedChrToIdOrUnknown("M\t".getBytes(), 0, false));

        assertEquals(1, cache.prefixedChrToIdOrUnknown("1\t12345\tA".getBytes(), 0, false));
        assertEquals(15, cache.prefixedChrToIdOrUnknown("15\t12345\tA".getBytes(), 0, false));
        assertEquals(23, cache.prefixedChrToIdOrUnknown("X\t12345\tA".getBytes(), 0, false));
        assertEquals(24, cache.prefixedChrToIdOrUnknown("XY\t12345\tA".getBytes(), 0, false));
        assertEquals(0, cache.prefixedChrToIdOrUnknown("M\t12345\tA".getBytes(), 0, false));

        // Add some special usecases.
        assertEquals(2, cache.prefixedChrToIdOrUnknown("2\t\t".getBytes(), 0, false));
        assertEquals(17, cache.prefixedChrToIdOrUnknown("17\t22\tA".getBytes(), 0, false));
        assertEquals(2, cache.prefixedChrToIdOrUnknown("2\t1\tA".getBytes(), 0, false));

    }

    private void checkStandardHumanChromosomes(final ChromoCache cache) {
        checkChromo(cache, "chrM", 0);
        for (int i = 1; i < 23; i++) {
            checkChromo(cache, "chr" + i, i);
        }
        checkChromo(cache, "chrX", 23);
        checkChromo(cache, "chrXY", 24);
        checkChromo(cache, "chrY", 25);
    }

    private void checkChromo(ChromoCache cache, String chr, int id) {
        assertEquals(chr, cache.toName(id));
        assertEquals(id, cache.prefixToChromosomeId(chr));
        assertEquals(id, cache.prefixToChromosomeId(chr.getBytes()));
        assertEquals(id, cache.prefixToChromosomeIdOrUnknown(chr, false));
        assertEquals(id, cache.prefixedChrToIdOrUnknown(chr.getBytes(), 0, false));
    }

    private void checkLexicoOrder(ChromoCache cache) {
        final int[] lexico = cache.getChrInLexicoOrder();
        for (int i = 1; i < lexico.length; i++) {
            assertTrue(cache.isChrLexicoPrior(lexico[i - 1], lexico[i]));
            final String l = cache.toName(lexico[i - 1]);
            final String r = cache.toName(lexico[i]);
            assertTrue(l + " is not lexicographically prior to " + r, l.compareTo(r) <= 0);
        }
    }

}
