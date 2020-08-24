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

import gorsat.Utilities.StringUtilities;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Gunnar on 13/06/2017.
 */
public class UTestLogDir {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testLogToFile() {
        String workDirPath = workDir.getRoot().toString();

        int expected = 10;
        String query = "gor 1.mem | select 1,2 | top 10 | log";
        int result = TestUtils.runGorPipeCount(query, "-aliases", "../server/src/main/dist/config/gor_aliases.txt",
                "-logdir", workDirPath);

        String logfileName = StringUtilities.createMD5(query) + ".log";
        Assert.assertEquals(expected, result);
        Assert.assertTrue(Files.exists(Paths.get(workDirPath).resolve(logfileName)));
    }

    @Test
    public void testLogToStdErr() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));

        int expected = 10;
        int result = TestUtils.runGorPipeCount("gor 1.mem | select 1,2 | top 10 | log");

        Assert.assertEquals(expected, result);
        Assert.assertTrue(baos.toString().contains("Logger"));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
    }
}
