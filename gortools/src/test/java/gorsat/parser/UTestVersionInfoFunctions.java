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
import gorsat.process.GorPipe;
import org.junit.Test;

public class UTestVersionInfoFunctions {
    @Test
    public void testGorVersion() {
        TestUtils.assertCalculated("gorversion()", GorPipe.version());
    }

    @Test
    public void testMajorVersion() {
        int major = -1;
        if(!GorPipe.version().equals("Unknown")) {
            major = Integer.parseInt(GorPipe.version().split("\\.")[1]);
        }
        TestUtils.assertCalculated("majorversion()", major);
    }

    @Test
    public void testMinorVersion() {
        int minor = -1;
        if(!GorPipe.version().equals("Unknown")) {
            minor = Integer.parseInt(GorPipe.version().split("\\.")[2]);
        }
        TestUtils.assertCalculated("minorversion()", minor);
    }

    @Test
    public void testJavaVersion() {
        TestUtils.assertCalculated("javaversion()", System.getProperty("java.version"));
    }
}
