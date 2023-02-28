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

import org.gorpipe.exceptions.GorSystemException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Created by sigmar on 29/09/15.
 */
public class UTestMissingGorTemp {
    /**
     * Test if missing gortemp when not set does not result in error
     *
     */
    @Test
    public void testMissingGorTemp() {
        String query = "create xxx = gor ../tests/data/external/samtools/serialization_test.bam  | top 10 | signature -timeres 1; gor [xxx]";

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(bout));

        TestUtils.runGorPipeIteratorOnMain(query);

        System.out.close();
        System.setOut(old);
    }

    /**
     * Test for exception when missing a gortemp and -cachedir is set
     *
     */
    @Test
    public void testExceptionOnMissingGorTemp() {
        String[] args = new String[]{"create xxx = gor ../tests/data/external/samtools/serialization_test.bam | top 10 | signature -timeres 1; gor [xxx]", "-cachedir", "gortemptest"};
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(bout));
        boolean exc = false;
        try {
            TestUtils.runGorPipeIteratorOnMain(args);
        } catch (GorSystemException e) {
            if (e.getMessage().contains("Cache directory given by -cachedir")) exc = true;
        }
        System.out.close();
        System.setOut(old);

        Assert.assertTrue("No exception with missing directory on set -cachedir", exc);
    }

    /**
     * Test if missing gortemp when not set does not result in error
     *
     */
    @Test
    public void testMissingGorTempTwoCreates() {
        String query = "create xxx = gor ../tests/data/external/samtools/serialization_test.bam | top 10 | signature -timeres 1; create yyy = gor ../tests/data/external/samtools/serialization_test.bam | top 10; gor [xxx] [yyy]";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(bout));

        TestUtils.runGorPipeIteratorOnMain(query);

        System.out.close();
        System.setOut(old);
    }

}
