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

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * Created by sigmar on 09/05/2017.
 */
public class UTestSystemEval {
    @Test
    public void testRowSourcePerf() {
        int expected = 51776;
        String query = "gor ../tests/data/gor/genes.gor | calc avg avgrowspermillis()";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testSystem() {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        int expected = 10;
        String query = "gor ../tests/data/gor/genes.gor | top 10 | calc date system('date') | calc host system('hostname')";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testSystemWithLongStdout() {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        int expected = 10;
        String query = "gor ../tests/data/gor/genes.gor | top 10 | calc date system('cat ../tests/data/gor/genes.gor') | calc host system('hostname')";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testSystemWithStderr() {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        int expected = 10;
        String query = "gor ../tests/data/gor/genes.gor | top 10 | calc date system('logger -s test') | calc host system('hostname')";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testEval() {
        int expected = 3;
        String query = "gor ../tests/data/gor/genes.gor | top 3 | calc ip eval('nor ../tests/data/gor/genes.gor | select gene_start | top 1')";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testHostname() {
        int expected = 3;
        String query = "gor ../tests/data/gor/genes.gor | top 3 | calc hostname hostname() | calc threadid threadid() | calc free free()";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testMajorMinorVersion() {
        int expected = 3;
        String query = "gor ../tests/data/gor/genes.gor | top 3 | calc ma,mi majorversion(),minorversion()";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }
}
