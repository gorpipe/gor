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
import org.apache.commons.lang.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.net.InetAddress;

public class UTestDiagnosticFunctions {
    @Test
    public void testTime() {
        String result = TestUtils.runGorPipe("gor 1.mem | select 1,2 | top 1000 | calc x time()");
        int firstTime = Integer.parseInt(result.split("\n")[1].split("\t")[2]);
        int lastTime = Integer.parseInt(result.split("\n")[1000].split("\t")[2]);
        Assert.assertTrue(lastTime >= firstTime);
    }

    @Test
    public void testSleep() {
        TestUtils.assertCalculated("sleep(0)", "");
    }

    @Test
    public void testHostname() throws java.net.UnknownHostException {
        TestUtils.assertCalculated("hostname()", InetAddress.getLocalHost().getHostName());
    }

    @Test
    public void testIP()  throws java.net.UnknownHostException {
        TestUtils.assertCalculated("ip()", InetAddress.getLocalHost().getHostAddress());
    }

    @Test
    public void testArch() {
        String calculated = TestUtils.getCalculated("arch()");
        Assert.assertTrue(!calculated.isEmpty());
    }

    @Test
    public void testThreadId() {
        String calculated = TestUtils.getCalculated("threadid()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value > 0.0);
    }

    @Test
    public void testCpuLoad() {
        String calculated = TestUtils.getCalculated("cpuload()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testSysCpuLoad() {
        String calculated = TestUtils.getCalculated("syscpuload()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testFree() {
        String calculated = TestUtils.getCalculated("free()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testFreeMem() {
        String calculated = TestUtils.getCalculated("freemem()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testTotalMem() {
        String calculated = TestUtils.getCalculated("totalmem()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testMaxMem() {
        String calculated = TestUtils.getCalculated("maxmem()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testAvailCpu() {
        String calculated = TestUtils.getCalculated("availcpu()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testOpenFiles() {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String calculated = TestUtils.getCalculated("openfiles()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testMaxFiles() {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String calculated = TestUtils.getCalculated("maxfiles()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0);
    }

    @Test
    public void testSystem() {
        TestUtils.assertCalculated("system('xxx')", "Command xxx not allowed to run");
    }

    @Test
    public void testAvgSeekTimeMillis() {
        String calculated = TestUtils.getCalculated("AVGSEEKTIMEMILLIS()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0 || value == -1.0);
    }

    @Test
    public void testAvgRowsPerMillis() {
        String calculated = TestUtils.getCalculated("AVGROWSPERMILLIS()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0 || value == -1.0);
    }

    @Test
    public void testAvgBasesPerMillis() {
        String calculated = TestUtils.getCalculated("AVGBASESPERMILLIS()");
        double value = Double.parseDouble(calculated);
        Assert.assertTrue(value >= 0.0 || value == -1.0);
    }
}
