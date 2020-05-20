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

package org.gorpipe.util.string;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests for StringUtil.
 *
 * @author vilm
 * @version $Id$
 */
public class UTestStringUtil {

    /**
     * Test comparing strings with numbers
     */
    @Test
    public void testCompareNumStrings() {
        final String s1 = "String 9";
        final String s2 = "String 10";
        final String s3 = "String -9";

        Assert.assertEquals(-1, StringUtil.compareNumStrings(s1, s2));
        Assert.assertEquals(1, StringUtil.compareNumStrings(s2, s1));
        Assert.assertEquals(0, StringUtil.compareNumStrings(s1, s1));

        Assert.assertEquals(-1, StringUtil.compareNumStrings(s3, s2));
        Assert.assertEquals(1, StringUtil.compareNumStrings(s2, s3));
        Assert.assertEquals(0, StringUtil.compareNumStrings(s3, s3));
    }

    /**
     * Test spliting string while reserving quotes
     */
    @Test
    public void testSplitReserveQuotes() {
        final String[] tokens = {"one", "\'qouteone\'", "two", "\"double quoute one\"", "\"double qoute with 'single quoute'\"", "\'single qoute with \"double quoute\"\'", "three"};
        final String text = tokens[0] + " " + tokens[1] + " " + tokens[2] + " " + tokens[3] + " " + tokens[4] + " " + tokens[5] + " " + tokens[6];
        final String[] found = StringUtil.splitReserveQuotesToArray(text);
        Assert.assertEquals(tokens.length, found.length);
        for (int i = 0; i < tokens.length; i++) {
            Assert.assertEquals(tokens[i], found[i]);
        }
    }

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

    /**
     * Test string join methods.
     */
    @Test
    public void testJoin() {
        List<String> l = new ArrayList<String>();
        l.add("abc");

        StringBuffer b = new StringBuffer("B");
        StringUtil.join(",", l, b);
        Assert.assertEquals("Babc", b.toString());

        String s = "(" + StringUtil.join("),(", l) + ")";
        Assert.assertEquals("(abc)", s);

        l.add("def");
        StringUtil.join(",", l, b);
        Assert.assertEquals("Babcabc,def", b.toString());

        s = "(" + StringUtil.join("),(", l) + ")";
        Assert.assertEquals("(abc),(def)", s);


        b = new StringBuffer("B");
        StringUtil.join(",", new String[]{"abc"}, b);
        Assert.assertEquals("Babc", b.toString());

        s = "(" + StringUtil.join("),(", (Object[]) new String[]{"abc"}) + ")";
        Assert.assertEquals("(abc)", s);

        l.add("def");
        StringUtil.join(",", new String[]{"abc", "def"}, b);
        Assert.assertEquals("Babcabc,def", b.toString());

        s = "(" + StringUtil.join("),(", (Object[]) new String[]{"abc", "def"}) + ")";
        Assert.assertEquals("(abc),(def)", s);


    }
}
