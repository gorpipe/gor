package org.gorpipe.util;

import org.junit.Assert;
import org.junit.Test;

public class UTestNumericUtils {
    /**
     * Test if a String is an Integer
     */
    @Test
    public void testIsStringInt() {
        Assert.assertFalse(NumericUtils.isStringInt(null));
        Assert.assertFalse(NumericUtils.isStringInt("abc"));
        Assert.assertTrue(NumericUtils.isStringInt("5"));
        Assert.assertTrue(NumericUtils.isStringInt("-5"));
        Assert.assertTrue(NumericUtils.isStringInt("2147483647"));
        Assert.assertTrue(NumericUtils.isStringInt("-2147483648"));
        Assert.assertFalse(NumericUtils.isStringInt("2147483648"));
        Assert.assertFalse(NumericUtils.isStringInt("-2147483649"));
        Assert.assertFalse(NumericUtils.isStringInt("5.0"));
    }

    /**
     * Test if a String is an Long
     */
    @Test
    public void testIsStringLong() {
        Assert.assertFalse(NumericUtils.isStringLong(null));
        Assert.assertFalse(NumericUtils.isStringLong("abc"));
        Assert.assertTrue(NumericUtils.isStringLong("5"));
        Assert.assertTrue(NumericUtils.isStringLong("-5"));
        Assert.assertTrue(NumericUtils.isStringLong("-9223372036854775808"));
        Assert.assertTrue(NumericUtils.isStringLong("9223372036854775807"));
        Assert.assertFalse(NumericUtils.isStringLong("-9223372036854775809"));
        Assert.assertFalse(NumericUtils.isStringLong("9223372036854775808"));
        Assert.assertFalse(NumericUtils.isStringLong("5.0"));
    }
}