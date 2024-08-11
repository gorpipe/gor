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

import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorSecurityException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.util.DataUtil;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static org.gorpipe.gor.driver.meta.DataType.GORI;

/**
 * Tests behavior of template query execution invoked in form `gor file.yml()`
 */
public class UTestGorTemplate {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

//    @Rule
//    public TemporaryFolder tempRoot = new TemporaryFolder();
//    private Path tempRootPath;

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
        Files.createDirectories(workDirPath.resolve("result_cache"));
    }

    String writeQueryAsTemplate(String filestem, String query, String tmpName) throws IOException {
        String templateYml = new StringBuilder()
                .append(tmpName).append(":\n")
                .append("  ").append("query: ").append(query).append("\n")
                .toString();

        String filename = filestem + ".yml";
        Files.writeString(workDirPath.resolve(filename), templateYml);
        return filename;
    }

    String writeQueryAsTemplate(String filestem, String query) throws IOException {
        return writeQueryAsTemplate(filestem, query, filestem);
    }

    String[] firstRow(String result) {
        return result.split(TestUtils.LINE_SPLIT_PATTERN)[1].split("\\s+");
    }
    /**
     * A create statement containing a template query invocation should not be serviced from cache,
     * because we do not have the analysis to demonstrate the results will be unchanged
     * @throws IOException
     */
    @Test
    public void testTemplateInvocationIsCacheMiss() throws IOException {

        String eachTimeDifferent = "eachTimeDifferent";
        String etdFn = writeQueryAsTemplate(eachTimeDifferent, "norrows 1 | calc inner_ts epoch()");
        String callerQ = "create xx = gor " + etdFn + "() | calc outer_ts epoch() ; " +
                "gor [xx]";
        String[] try1 = firstRow(TestUtils.runGorPipe(callerQ, workDirPath.toString(), false));
        String[] try2 = firstRow(TestUtils.runGorPipe(callerQ, workDirPath.toString(), false));

        // values for inner_ts are different because template was re-evaluated
        Assert.assertNotEquals("time stamps should be different", Long.parseLong(try1[3]), Long.parseLong(try2[3]));
        // values for outer_ts are different because create xx was re-evaluated
        Assert.assertNotEquals("time stamps should be different", Long.parseLong(try1[4]), Long.parseLong(try2[4]));
    }
}
