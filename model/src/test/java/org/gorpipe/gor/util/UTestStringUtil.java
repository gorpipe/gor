/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.gor.util;

import org.gorpipe.gor.util.StringUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Tests for StringUtil.
 *
 * @author vilm
 * @version $Id$
 */
public class UTestStringUtil {

    /**
     * Test split of strings
     */
    @Test
    public void testSplit() {
        final String emptyline = "";
        final String onlytab = "\t";
        final String oneline = "Kalli";
        final String threeparts = "Kalli\tPalli\tNalli";
        final String oneendswithtab = "Kalli\t";

        // Check empty
        ArrayList<String> list = StringUtil.split(emptyline);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("", list.get(0));

        // Check only tab
        list = StringUtil.split(onlytab);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("", list.get(0));
        Assert.assertEquals("", list.get(1));

        // Check single column
        list = StringUtil.split(oneline);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Kalli", list.get(0));

        // Check three parts
        list = StringUtil.split(threeparts);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("Kalli", list.get(0));
        Assert.assertEquals("Palli", list.get(1));
        Assert.assertEquals("Nalli", list.get(2));

        // Check starting inside the string
        list = StringUtil.split(threeparts, 6, '\t');
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Palli", list.get(0));
        Assert.assertEquals("Nalli", list.get(1));

        // Check ends with tab
        list = StringUtil.split(oneendswithtab);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Kalli", list.get(0));
        Assert.assertEquals("", list.get(1));
    }
}
