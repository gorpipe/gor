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

package gorsat;

import org.apache.commons.lang3.StringUtils;
import org.gorpipe.test.SlowTests;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Created by Gunnar on 14/06/2017.
 */

public class UTestLog {

    private ByteArrayOutputStream baos;

    @Before
    public void setup() {
        baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));
    }

    @AfterClass
    public static void finish() {
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
    }

    @Category(SlowTests.class)
    @Test
    public void testLogByTime() {
        String result = TestUtils.runGorPipe("gorrows -p chr1:1-10000000 | log -t 1 | group genome -count | calc t time() | select 1,5");
        int time = Integer.parseInt(result.split("\n")[1].split("\\s")[1]);
        int expected = time / 1000;
        int logs = StringUtils.countMatches(baos.toString(), "Logger");
        Assert.assertTrue(logs >= expected / 2);
    }

    @Test
    public void testLogByTimeZero() {
        int expected = 10;
        int result = TestUtils.runGorPipeCount("gor 1.mem | select 1,2 | top 10 | log -t 0");
        Assert.assertEquals(expected, result);

        int logs = StringUtils.countMatches(baos.toString(), "Logger");
        Assert.assertEquals(expected, logs);
    }

    @Test
    public void testLogByLines() {
        int expected = 1000;
        int result = TestUtils.runGorPipeCount("gor 1.mem | select 1,2 | top 1000 | log 100");
        Assert.assertEquals(expected, result);

        expected = 10;
        int logs = StringUtils.countMatches(baos.toString(), "Logger");
        Assert.assertEquals(expected, logs);
    }

    @Test
    public void testLogByLinesNoParam() {
        int expected = 10;
        int result = TestUtils.runGorPipeCount("gor 1.mem | select 1,2 | top 10 | log");
        Assert.assertEquals(expected, result);

        int logs = StringUtils.countMatches(baos.toString(), "Logger");
        Assert.assertEquals(expected, logs);
    }

    @Test
    public void testLogWithLabel() {
        int expected = 100;
        int result = TestUtils.runGorPipeCount("gor 1.mem | select 1,2 | top 100 | log -l LABEL 10");
        Assert.assertEquals(expected, result);

        expected = 10;
        int logs = StringUtils.countMatches(baos.toString(), "LABEL");
        Assert.assertEquals(expected, logs);
    }

    @Test
    public void testLogWithLabelNoParam() {
        int expected = 10;
        int result = TestUtils.runGorPipeCount("gor 1.mem | select 1,2 | top 10 | log -l LABEL");
        Assert.assertEquals(expected, result);

        int logs = StringUtils.countMatches(baos.toString(), "LABEL");
        Assert.assertEquals(expected, logs);
    }

    @Category(SlowTests.class)
    @Test
    public void testLogWithLabelAndTime() {
        String result = TestUtils.runGorPipe("gorrows -p chr1:1-10000000 | log -l LABEL -t 1 | group genome -count | calc t time() | select 1,5");
        int time = Integer.parseInt(result.split("\n")[1].split("\\s")[1]);
        int expected = time / 1000;
        int logs = StringUtils.countMatches(baos.toString(), "LABEL");
        Assert.assertTrue(logs >= expected / 2);
    }
}
