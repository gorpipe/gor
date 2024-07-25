package org.gorpipe.gor.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

public class UTestChrDataScheme {

    @Test
    public void testGetChrName() {
        ChrDataScheme scheme = ChrDataScheme.HG;
        assertEquals("1", scheme.getChrName(1));
        assertEquals("X", scheme.getChrName(23));
        assertThrows(RuntimeException.class, () -> scheme.getChrName(-1));
        assertThrows(RuntimeException.class, () -> scheme.getChrName(100));
    }

    @Test
    public void testChr2id() {
        ChrDataScheme scheme = ChrDataScheme.HG;
        assertEquals(1, scheme.chr2id("1"));
        assertEquals(23, scheme.chr2id("X"));
        //assertNull(scheme.chr2id("unknown"));
    }

    @Test
    public void testChr2order() {
        ChrDataScheme scheme = ChrDataScheme.HG;
        assertEquals(0, scheme.chr2order("1"));
        assertEquals(22, scheme.chr2order("X"));
    }

    @Test
    public void testFindChromosomeOrder() {
        ChrDataScheme scheme = ChrDataScheme.HG;
        int[] idsInOrder = {1, 2, 3, 4, 5, 0};
        String[] id2name = {"M", "1", "2", "3", "4", "5"};
        assertEquals(6, scheme.findChromosomeOrder("X", idsInOrder, id2name, 6));
        assertEquals(1, scheme.findChromosomeOrder("10", idsInOrder, id2name, 6));
    }

    @Test
    public void testUpdateDataScheme() {
        ChrDataScheme scheme = new ChrDataScheme(
                new String[]{"MT", "1", "2", "3", "4", "5", "6"},
                new int[]{6, 0, 1, 2, 3, 4, 5}
        );
        List<String> orderedContigList = Arrays.asList("1", "2", "3", "X", "Y");
        ChrDataScheme.updateDataScheme(scheme, orderedContigList);
        assertEquals("1", scheme.getChrName(0));
        assertEquals("X", scheme.getChrName(3));
    }

    @Test
    public void testSortUsingNumChrDataScheme() {
        ChrDataScheme scheme = ChrDataScheme.ChrNumerical;
        List<String> contigs = Arrays.asList("chr10", "chr1", "chr2", "chrX", "chrY");
        List<String> sortedContigs = ChrDataScheme.sortUsingChrDataScheme(contigs, scheme);
        List<String> expected = Arrays.asList("chr1", "chr2", "chr10", "chrX", "chrY");
        assertEquals(expected, sortedContigs);
    }

    @Test
    public void testSortUsingLexChrDataScheme() {
        ChrDataScheme scheme = ChrDataScheme.ChrLexico;
        List<String> contigs = Arrays.asList("chr10", "chr1", "chr2", "chrX", "chrY");
        List<String> sortedContigs = ChrDataScheme.sortUsingChrDataScheme(contigs, scheme);
        List<String> expected = Arrays.asList("chr1", "chr10", "chr2", "chrX", "chrY");
        assertEquals(expected, sortedContigs);
    }

    @Test
    public void testIsStrictLexicalOrder() {
        assertTrue(ChrDataScheme.isStrictLexicalOrder(Arrays.asList("1", "2", "3", "X", "Y")));
        assertTrue(ChrDataScheme.isStrictLexicalOrder(Arrays.asList("1", "10", "3", "X", "Y")));
        assertTrue(ChrDataScheme.isStrictLexicalOrder(Arrays.asList("1", "10", "20", "3", "X", "Y")));
        assertFalse(ChrDataScheme.isStrictLexicalOrder(Arrays.asList("1", "2", "3", "20", "X", "Y")));
        assertFalse(ChrDataScheme.isStrictLexicalOrder(Arrays.asList("1", "2", "10", "X", "Y")));
        assertFalse(ChrDataScheme.isStrictLexicalOrder(Arrays.asList("3", "1", "2", "X", "Y")));
    }

    @Test
    public void testIsMostLikelyLexicalOrder() {
        assertFalse(ChrDataScheme.isMostLikelyLexicalOrder(Arrays.asList("1", "2", "3", "X", "Y")));
        assertTrue(ChrDataScheme.isMostLikelyLexicalOrder(Arrays.asList("1", "10", "3", "X", "Y")));
        assertTrue(ChrDataScheme.isMostLikelyLexicalOrder(Arrays.asList("1", "10", "20", "3", "X", "Y")));
        assertFalse(ChrDataScheme.isMostLikelyLexicalOrder(Arrays.asList("1", "2", "3", "20", "X", "Y")));
        assertFalse(ChrDataScheme.isMostLikelyLexicalOrder(Arrays.asList("1", "2", "10", "X", "Y")));
        assertFalse(ChrDataScheme.isMostLikelyLexicalOrder(Arrays.asList("3", "1", "2", "X", "Y")));
    }
}
