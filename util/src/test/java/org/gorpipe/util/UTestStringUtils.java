package org.gorpipe.util;

import org.junit.Assert;
import org.junit.Test;

public class UTestStringUtils {

    /**
     * Test if a String is blank or null
     */
    @Test
    public void testBlankNull() {
        Assert.assertEquals(null, StringUtils.blankNull(null));
        Assert.assertEquals(null, StringUtils.blankNull(""));
        Assert.assertEquals(null, StringUtils.blankNull(" "));
        Assert.assertEquals("abc", StringUtils.blankNull("abc"));
        Assert.assertNotSame("abc", StringUtils.blankNull("abc "));
    }
}