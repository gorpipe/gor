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

import gorsat.Utilities.AnalysisUtilities;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.test.SlowTests;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class UTestSignature {

    @Test
    public void testEmptySignature() {
        try {
            TestUtils.runGorPipeLines("create xxx = gor ../tests/data/gor/genes.gor | signature | top 3 | wait 1000; gor [xxx]");
            Assert.fail();
        } catch (GorParsingException gpe) {
            Assert.assertEquals("SIGNATURE", gpe.getCommandName());
            // This should happen
        }

        try {
            TestUtils.runGorPipeLines("create xxx = gor ../tests/data/gor/genes.gor | signature -timeres | top 3 | wait 1000; gor [xxx]");
            Assert.fail();
        } catch (GorParsingException gpe) {
            Assert.assertEquals("-timeres", gpe.getOption());
            // This should happen
        }
    }

    @Category(SlowTests.class)
    @Test
    public void testSignatureZeroTime() {

        // Run a simple query to reduce initialization timelags
        TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | top 0");

        long start = System.currentTimeMillis();
        String[] lines = TestUtils.runGorPipeLines("create xxx = gor ../tests/data/gor/genes.gor | signature -timeres 0 | top 10 | wait 100; gor [xxx]");
        long duration = System.currentTimeMillis() - start;

        Assert.assertEquals("Expect 11 lines", 11, lines.length);
        Assume.assumeTrue("Duration should be long due to wait command", duration > 1000);

        start = System.currentTimeMillis();
        TestUtils.runGorPipeLines("create xxx = gor ../tests/data/gor/genes.gor | signature -timeres 0 | top 10 | wait 100; gor [xxx]");
        duration = System.currentTimeMillis() - start;

        Assert.assertEquals("Expect 11 lines", 11, lines.length);
        Assume.assumeTrue("Duration should be long due to wait command", duration > 1000);
    }

    @Test
    public void testSignature10Seconds() {

        // Run a simple query to reduce initialization timelags
        TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | top 0");

        long start = System.currentTimeMillis();
        String[] lines = TestUtils.runGorPipeLines("create xxx = gor ../tests/data/gor/genes.gor | signature -timeres 10 | top 10 | wait 100; gor [xxx]");
        long duration = System.currentTimeMillis() - start;

        Assert.assertEquals("Expect 11 lines", 11, lines.length);
        Assume.assumeTrue("Duration should be long due to wait command", duration > 1000);

        start = System.currentTimeMillis();
        TestUtils.runGorPipeLines("create xxx = gor ../tests/data/gor/genes.gor | signature -timeres 10 | top 10 | wait 100; gor [xxx]");
        duration = System.currentTimeMillis() - start;

        Assert.assertEquals("Expect 11 lines", 11, lines.length);
        Assume.assumeTrue("Duration should be short with access to the cache", duration < 900);
    }

    @Test
    public void testSignatureParsing() {

        String signature = AnalysisUtilities.getSignature("gor ../tests/data/gor/genes.gor | signature -timeres 1000");
        long timeValue = Long.parseLong(signature.trim());
        Assert.assertEquals(0, timeValue % 1000);

        signature = AnalysisUtilities.getSignature("pgor ../tests/data/gor/genes.gor | signature -timeres 100");
        timeValue = Long.parseLong(signature.trim());
        Assert.assertEquals(0, timeValue % 100);

        signature = AnalysisUtilities.getSignature("gor ../tests/data/gor/genes.gor | calc signature 1.0 | signature -timeres 1000");
        timeValue = Long.parseLong(signature.trim());
        Assert.assertEquals(0, timeValue % 1000);

        signature = AnalysisUtilities.getSignature("gor ../tests/data/gor/genes.gor | calc signature 1.0 ");
        Assert.assertEquals("", signature);

    }
}
