package org.gorpipe.gor.driver.genotypeutilities;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.gorpipe.gor.driver.genotypeutilities.ValueColumnParsing.*;

public class UTestValueColumnParsing {

    @Test
    public void test_parseBiAllelicHardCalls() {
        final CharSequence values = "0123";
        final byte[] hc = new byte[4];
        parseBiAllelicHardCalls(values, hc);
        Assert.assertArrayEquals(new byte[]{0, 1, 2, 3}, hc);
    }

    @Test
    public void test_parseImputedGenotypes() {
        final CharSequence values = new String(new char[]{126, 126, 33, 126, 126, 33, 95, 95});
        final byte[] hc = new byte[4];
        final float[] dosages = new float[4];
        parseImputedGenotypes(values, 0.9f, hc, dosages);
        Assert.assertArrayEquals(new byte[]{0, 1, 2, 3}, hc);
        Assert.assertArrayEquals(new float[]{0f, 1f, 2f, 1f}, dosages, 1e-6f);
    }

    @Test
    public void test_parseMultiAllelicHardCalls_2() {
        final CharSequence values1 = "0101203";
        final CharSequence values2 = "0110023";
        final List<CharSequence> list = new ArrayList<>();
        list.add(values1);
        list.add(values2);
        final int[] gt1 = new int[7];
        final int[] gt2 = new int[7];
        parseMultiAllelicHardCalls(list, gt1, gt2);
        Assert.assertArrayEquals(new int[]{0, 1, 0, 0, 1, 2, -1}, gt1);
        Assert.assertArrayEquals(new int[]{0, 2, 2, 1, 1, 2, -1}, gt2);
    }

    @Test
    public void test_parseMultiAllelicHardCalls_3() {
        final CharSequence values1 = "00111002003";
        final CharSequence values2 = "01010100203";
        final CharSequence values3 = "01100010023";
        final List<CharSequence> list = new ArrayList<>();
        list.add(values1);
        list.add(values2);
        list.add(values3);
        final int[] gt1 = new int[11];
        final int[] gt2 = new int[11];
        parseMultiAllelicHardCalls(list, gt1, gt2);
        Assert.assertArrayEquals(new int[]{0, 2, 1, 1, 0, 0, 0, 1, 2, 3, -1}, gt1);
        Assert.assertArrayEquals(new int[]{0, 3, 3, 2, 1, 2, 3, 1, 2, 3, -1}, gt2);
    }

    @Test
    public void test_fillHC() {
        final int[] gt1 = {0, 0, 1, -1};
        final int[] gt2 = {0, 1, 1, -1};
        final byte[] hc = new byte[4];
        fillHC(gt1, gt2, hc);
        Assert.assertArrayEquals(new byte[]{0, 1, 2, 3}, hc);
    }

    @Test
    public void test_parseBiAllelicHardCalls_2() {
        final CharSequence values = "0123";
        final int[] gt1 = new int[4];
        final int[] gt2 = new int[4];
        parseBiAllelicHardCalls(values, gt1, gt2);
        Assert.assertArrayEquals(new int[]{0, 0, 1, -1}, gt1);
        Assert.assertArrayEquals(new int[]{0, 1, 1, -1}, gt2);
    }

    @Test
    public void test_parseImputedGenotypes_2() {
        final CharSequence values = "~~!~~!  ";
        final float[][] probs = new float[4][3];
        parseImputedGenotypes(values, probs);
        Assert.assertArrayEquals(new float[] {1f, 0f, 0f}, probs[0], 0);
        Assert.assertArrayEquals(new float[] {0f, 1f, 0f}, probs[1], 0);
        Assert.assertArrayEquals(new float[] {0f, 0f, 1f}, probs[2], 0);
        Assert.assertArrayEquals(new float[] {0f, 0f, 0f}, probs[3], 0);
    }
}
