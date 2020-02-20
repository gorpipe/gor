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

    /**
     * Test parse method.
     */
    @Test
    public void testParse() {
        String[] s = StringUtil.parse("File: abc.txt  Size: 300", "File:\\s+([\\w.]+)\\s+Size:\\s+([0-9]+)");
        Assert.assertEquals(2, s.length);
        Assert.assertEquals("abc.txt", s[0]);
        Assert.assertEquals("300", s[1]);

        s = StringUtil.parse("A10D14", "(A(10))?(B10)?(D14)?");
        Assert.assertEquals(4, s.length);
        Assert.assertEquals("A10", s[0]);
        Assert.assertEquals("10", s[1]);
        Assert.assertNull(s[2]);
        Assert.assertEquals("D14", s[3]);
    }

    /**
     * Test dotJoin method.
     */
    @Test
    public void testDotJoin() {
        Assert.assertEquals(null, StringUtil.dotJoin(null, null));
        Assert.assertEquals("parent", StringUtil.dotJoin("parent", null));
        Assert.assertEquals("child", StringUtil.dotJoin(null, "child"));
        Assert.assertEquals("root.parent.child", StringUtil.dotJoin("root.parent", "child"));
    }

    /**
     * Test dotLeaf method.
     */
    @Test
    public void testDotLeaf() {
        Assert.assertEquals(null, StringUtil.dotLeaf(null));
        Assert.assertEquals("child", StringUtil.dotLeaf("child"));
        Assert.assertEquals("child", StringUtil.dotLeaf("parent.child"));
        Assert.assertEquals("child", StringUtil.dotLeaf("root.parent.child"));
    }

    /**
     * Test leaf method
     */
    @Test
    public void testLeaf() {
        String sep = "/";
        Assert.assertEquals(null, StringUtil.leaf(null, sep));
        Assert.assertEquals("child", StringUtil.leaf("child", sep));
        Assert.assertEquals("child", StringUtil.leaf("parent/child", sep));
        Assert.assertEquals("child", StringUtil.leaf("root/parent/child", sep));
    }

    /**
     * Test dotParent method
     */
    @Test
    public void testDotParent() {
        Assert.assertEquals(null, StringUtil.dotParent(null));
        Assert.assertEquals(null, StringUtil.dotParent("child"));
        Assert.assertEquals("parent", StringUtil.dotParent("parent.child"));
        Assert.assertEquals("root.parent", StringUtil.dotParent("root.parent.child"));
    }

    /**
     * Test dotHead method.
     */
    @Test
    public void testDotHead() {
        Assert.assertEquals(null, StringUtil.dotHead(null));
        Assert.assertEquals("abc", StringUtil.dotHead("abc"));
        Assert.assertEquals("abc", StringUtil.dotHead("abc.def"));
        Assert.assertEquals("abc", StringUtil.dotHead("abc.def.ghi"));
    }

    /**
     * Test dotHead method.
     */
    @Test
    public void testDotTail() {
        Assert.assertEquals(null, StringUtil.dotTail(null));
        Assert.assertEquals(null, StringUtil.dotTail("abc"));
        Assert.assertEquals("def", StringUtil.dotTail("abc.def"));
        Assert.assertEquals("def.ghi", StringUtil.dotTail("abc.def.ghi"));
    }

    /**
     * Test parent method.
     */
    @Test
    public void testParent() {
        String sep = "/";
        Assert.assertEquals(null, StringUtil.parent(null, sep));
        Assert.assertEquals(null, StringUtil.parent("child", sep));
        Assert.assertEquals("parent", StringUtil.parent("parent/child", sep));
        Assert.assertEquals("root/parent", StringUtil.parent("root/parent/child", sep));
    }

    /**
     * Test toSentence method.
     */
    @Test
    public void testToSentence() {
        Assert.assertEquals("First Value", StringUtil.toSentence("firstValue"));
        Assert.assertEquals("Base Query", StringUtil.toSentence("BASE QUERY"));
        Assert.assertEquals("A Common String", StringUtil.toSentence(" A Common String "));
        Assert.assertEquals("A Common String", StringUtil.toSentence(" aCommonString "));
        Assert.assertEquals("A Common String", StringUtil.toSentence(" a_common_String "));
        Assert.assertEquals("A Common String", StringUtil.toSentence(" A_COMMON_STRING "));
    }

    /**
     * Test toSentence method.
     */
    @Test
    public void testToCamelCase() {
        Assert.assertEquals("firstValue", StringUtil.toCamelCase("First Value"));
        Assert.assertEquals("toCamelCase", StringUtil.toCamelCase(" To  Camel case "));
        Assert.assertEquals("aCommonString", StringUtil.toCamelCase(" A Common String"));
    }

    /**
     * Test toIdentifier method.
     */
    @Test
    public void testToIdentifier() {
        Assert.assertEquals("FIRST_VALUE", StringUtil.toIdentifier("firstValue"));
        Assert.assertEquals("TO_IDENTIFIER_STRING", StringUtil.toIdentifier("toIdentifierString"));
        Assert.assertEquals("A_COMMON_STRING", StringUtil.toIdentifier(" A Common String "));
        Assert.assertEquals("A_COMMON_STRING", StringUtil.toIdentifier(" aCommonString "));
        Assert.assertEquals("A_COMMON_STRING", StringUtil.toIdentifier(" a_common_String "));
        Assert.assertEquals("A_COMMON_STRING", StringUtil.toIdentifier(" A_COMMON_STRING "));
    }

    /**
     * Test startsWithPattern method.
     */
    @Test
    public void testStartsWithPattern() {
        Assert.assertTrue(StringUtil.startsWithPattern("ab xy", "[abc][abc]"));
        Assert.assertFalse(StringUtil.startsWithPattern("ab xy", "[xy][xy]"));
    }

    /**
     * Test converting string to not null
     */
    @Test
    public void testToNotNull() {
        Assert.assertEquals("", StringUtil.toNotNullString(null));
        String s = "abcDef";
        Assert.assertEquals(s, StringUtil.toNotNullString(s));
    }

    /**
     * Test truncate function
     */
    @Test
    public void testTruncate() {
        // Strings shorter or equal to maxlength should be unharmed
        Assert.assertEquals("abcdefg", StringUtil.truncate("abcdefg", 7, true));
        Assert.assertEquals("abcdef", StringUtil.truncate("abcdef", 7, false));


        Assert.assertEquals("abcdefg", StringUtil.truncate("abcdefghi", 7, false));
        Assert.assertEquals("abcd...", StringUtil.truncate("abcdefghi", 7, true));

    }

    /**
     * Test if a String is an Integer
     */
    @Test
    public void testIsStringInt() {
        Assert.assertFalse(StringUtil.isStringInt(null));
        Assert.assertFalse(StringUtil.isStringInt("abc"));
        Assert.assertTrue(StringUtil.isStringInt("5"));
        Assert.assertTrue(StringUtil.isStringInt("-5"));
        Assert.assertTrue(StringUtil.isStringInt("2147483647"));
        Assert.assertTrue(StringUtil.isStringInt("-2147483648"));
        Assert.assertFalse(StringUtil.isStringInt("2147483648"));
        Assert.assertFalse(StringUtil.isStringInt("-2147483649"));
        Assert.assertFalse(StringUtil.isStringInt("5.0"));
    }

    /**
     * Test if a String is an Long
     */
    @Test
    public void testIsStringLong() {
        Assert.assertFalse(StringUtil.isStringLong(null));
        Assert.assertFalse(StringUtil.isStringLong("abc"));
        Assert.assertTrue(StringUtil.isStringLong("5"));
        Assert.assertTrue(StringUtil.isStringLong("-5"));
        Assert.assertTrue(StringUtil.isStringLong("-9223372036854775808"));
        Assert.assertTrue(StringUtil.isStringLong("9223372036854775807"));
        Assert.assertFalse(StringUtil.isStringLong("-9223372036854775809"));
        Assert.assertFalse(StringUtil.isStringLong("9223372036854775808"));
        Assert.assertFalse(StringUtil.isStringLong("5.0"));
    }

    /**
     * Test if a String is blank or null
     */
    @Test
    public void testBlankNull() {
        Assert.assertEquals(null, StringUtil.blankNull(null));
        Assert.assertEquals(null, StringUtil.blankNull(""));
        Assert.assertEquals(null, StringUtil.blankNull(" "));
        Assert.assertEquals("abc", StringUtil.blankNull("abc"));
        Assert.assertNotSame("abc", StringUtil.blankNull("abc "));
    }

    /**
     * Test if base64 encoding and decoding of a key value pair map works.
     */
    @Test
    public void testBase64() {
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("projectRoot", "/mnt/csa_local/env/dev/projects/clinex85");
        stringMap.put("projectname", "clinex85");
        stringMap.put("query", "gor #dbsnp# | top 10123");
        stringMap.put("flags", "H");
        stringMap.put("securityContextKey", "5C5F5F9EC86B74AE1067C42AC0B01F44");
        stringMap.put("request-id", "a0f8acb5-afc3-4538-ba66-0b3a6f3e80e2");
        stringMap.put("userName", "system_admin");
        stringMap.put("querySource", "queryService");
        stringMap.put("fingerprint", "j77ipg5p14ia02nicl6eigf2f25");
        stringMap.put("originalQuery", "gor #dbsnp# | top 10123");
        stringMap.put("time", "1538481250");
        stringMap.put("project-id", "2");
        stringMap.put("project-id2", "");
        stringMap.put("project-id3", null);
        Set<String> keys = new HashSet<>();
        keys.add("query");
        keys.add("originalQuery");
        Map<String, String> encodedStringMap = StringUtil.base64Encode(stringMap, keys);
        Map<String, String> decodeStringMap = StringUtil.base64Decode(encodedStringMap, keys);
        Map<String, String> decodeUnencodedStringMap = StringUtil.base64Decode(stringMap, keys);
        Assert.assertEquals(stringMap, decodeStringMap);
        Assert.assertNotEquals(encodedStringMap, decodeStringMap);
        Assert.assertNotEquals(encodedStringMap, stringMap);
        Assert.assertEquals(stringMap, decodeUnencodedStringMap);
    }

}
