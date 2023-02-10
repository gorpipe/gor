package org.gorpipe.util;

import org.junit.Assert;
import org.junit.Test;

public class UTestStrings {

    @Test
    public void testIsNullOrEmpty() {
        Assert.assertEquals(true, Strings.isNullOrEmpty(null));
        Assert.assertEquals(true, Strings.isNullOrEmpty(""));
        Assert.assertEquals(false, Strings.isNullOrEmpty(" "));
        Assert.assertEquals(false, Strings.isNullOrEmpty("abc"));
        Assert.assertEquals(false, Strings.isNullOrEmpty("abc "));
    }

    public void testIsNullOrBlank() {
        Assert.assertEquals(true, Strings.isNullOrBlank(null));
        Assert.assertEquals(true, Strings.isNullOrBlank(""));
        Assert.assertEquals(true, Strings.isNullOrBlank(" "));
        Assert.assertEquals(false, Strings.isNullOrBlank("abc"));
        Assert.assertEquals(false, Strings.isNullOrBlank("abc "));
    }

    /**
     * Test if a String is blank or null
     */
    @Test
    public void testBlankNull() {
        Assert.assertEquals(null, Strings.blankNull(null));
        Assert.assertEquals(null, Strings.blankNull(""));
        Assert.assertEquals(null, Strings.blankNull(" "));
        Assert.assertEquals("abc", Strings.blankNull("abc"));
        Assert.assertNotSame("abc", Strings.blankNull("abc "));
    }
}