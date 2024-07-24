package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;

public class UTestVcfFile {

    @Test
    public void testFixContigsOrderWithLexicalOrderNoChrNoChange() {
        List<String> input = Arrays.asList("1", "10", "11", "2", "M", "X", "Y");
        List<String> expected = Arrays.asList("1", "10", "11", "2", "M", "X", "Y");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithNumericalOrderNoChrNoChange() {
        List<String> input = Arrays.asList("1", "2", "3", "10", "X", "Y", "M");
        List<String> expected = Arrays.asList("1", "2", "3", "10",  "X", "Y", "M");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithLexicalOrderNoChange() {
        List<String> input = Arrays.asList("chr1", "chr10", "chr11", "chr2", "chrM", "chrX", "chrY");
        List<String> expected = Arrays.asList("chr1", "chr10", "chr11", "chr2", "chrM", "chrX", "chrY");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithNumericalOrderNoChange() {
        List<String> input = Arrays.asList("chr1", "chr2", "chr3", "chr10", "chrX", "chrY", "chrM");
        List<String> expected = Arrays.asList("chr1", "chr2", "chr3", "chr10", "chrX", "chrY", "chrM");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }


    @Test
    public void testFixContigsOrderWithLexicalOrderNoChr() {
        List<String> input = Arrays.asList("1", "10", "4", "11", "2", "X", "Y", "unknown");
        List<String> expected = Arrays.asList("1", "10", "11", "2", "4", "X", "Y", "unknown");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithNumericalOrderNoChr() {
        List<String> input = Arrays.asList("1", "2", "3", "10", "4", "X", "Y", "unknown");
        List<String> expected = Arrays.asList("1", "2", "3", "4", "10",  "X", "Y", "unknown");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithLexicalOrder() {
        List<String> input = Arrays.asList("chr1", "chr10", "chr4", "chr11", "chr2", "chrX", "chrY", "chrM");
        List<String> expected = Arrays.asList("chr1", "chr10", "chr11", "chr2", "chr4", "chrM", "chrX", "chrY");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithNumericalOrder() {
        List<String> input = Arrays.asList("chr1", "chr2", "chr3", "chr10", "chr4", "chrX", "chrM", "chrY");
        List<String> expected = Arrays.asList("chr1", "chr2", "chr3", "chr4", "chr10", "chrX", "chrY", "chrM");
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithEmptyList() {
        List<String> input = Arrays.asList();
        List<String> expected = Arrays.asList();
        assertEquals(expected, VcfFile.fixContigsOrder(input));
    }

    @Test
    public void testFixContigsOrderWithNullInput() {
        assertEquals(null, VcfFile.fixContigsOrder(null));
    }
}