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

package gorsat.parser;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class UTestTagFunctions {

    @Test
    public void testGenTag() {
        String queryResult = TestUtils.runGorPipe(new String[]{"gorrow chr1,2,3 |" +
                        "calc test 'END=1;BEND=2;SEND=3;GO=2;GOAL=3' |" +
                        "calc tag TAG(test,'END',';')"});
        Assert.assertTrue("Incorrect value for tag: ", queryResult.trim().endsWith("1"));
    }

    @Test
    public void testGenTagCase() {
        String queryResult = TestUtils.runGorPipe(new String[]{"gorrow chr1,2,3 |" +
                        "calc test 'end=bEnD;send=lEnD;' |" +
                        "calc tag TAG(test,'end',';')"});
        Assert.assertTrue("Incorrect value for tag: ", queryResult.trim().endsWith("bEnD"));
    }

    @Test
    public void testGenTagNotFound() {
        String queryResult = TestUtils.runGorPipe(new String[]{"gorrow chr1,2,3 |" +
                        "calc test 'CIEND=2;END=1' |" +
                        "calc tag TAG(test,'xyz',';')"});
        Assert.assertTrue("Located an unspecified tag: ", queryResult.trim().endsWith("NOT_FOUND"));
    }

    @Test
    public void testGenTagParsing() {
        String queryResult = TestUtils.runGorPipe(new String[]{"gorrow chr1,2,3 " +
                "| calc INFO 'Alt=AC,GT=0/1,AC=1,AF=0.472,AN=2' " +
                "| calc GT tag(INFO,'GT',',') " +
                "| calc AF tag(INFO,'AF',',')"});

        Assert.assertFalse("Located an unspecified tag: ", queryResult.contains("NOT_FOUND"));
        Assert.assertEquals("0/1", queryResult.split("\n")[1].split("\t")[4]);
        Assert.assertEquals("0.472", queryResult.split("\n")[1].split("\t")[5]);
    }

    @Test
    public void testGenIncorrectSeparatorTagParsing() {
        String queryResult = TestUtils.runGorPipe(new String[]{"gorrow chr1,2,3 " +
                "| calc INFO 'Alt=AC:GT=0/1,AC=1,AF=0.472,AN=2' " +
                "| calc GT tag(INFO,'GT',',') " +
                "| calc AF tag(INFO,'AF',',')"});

        Assert.assertEquals("NOT_FOUND", queryResult.split("\n")[1].split("\t")[4]);
        Assert.assertEquals("0.472", queryResult.split("\n")[1].split("\t")[5]);
    }
}
