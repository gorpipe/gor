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

import org.gorpipe.exceptions.GorParsingException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import static gorsat.process.NorStreamIterator.HEADER_PREFIX;

@RunWith(Parameterized.class)
public class UTestNorif {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Parameterized.Parameter
    public String testName;
    @Parameterized.Parameter(1)
    public String query;
    @Parameterized.Parameter(2)
    public String expectedResult;
    @Parameterized.Parameter(3)
    public Class<? extends Exception> expectedException;
    public static String fileHeader = "#Grp\tCategory\tValue\n";
    public static String expectedHeader = HEADER_PREFIX + "Grp\tCategory\tValue\n";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws IOException {
        //create test files
        TemporaryFolder workDir = new TemporaryFolder();
        workDir.create();
        Path workDirPath = workDir.getRoot().toPath();
        Path testTsvFile1 = workDirPath.resolve("test_file_1.tsv");
        Files.writeString(testTsvFile1, fileHeader + "A\t1\tA\n");
        Path testTsvFile2 = workDirPath.resolve("test_file_2.tsv");
        Files.writeString(testTsvFile2, fileHeader + "B\t2\tB\nB\t2\t2\n");
        Path testEmptyFile = workDirPath.resolve("empty_file.tsv");

        return Arrays.asList(new Object[][]{
                //{testName, query, expectedResult, expectedException}
                {"No file path", "norif", null, GorParsingException.class},
                {"Empty file", "norif -dh col1,col2 " + testEmptyFile, HEADER_PREFIX + "col1\tcol2\n", null},
                {"One file path", "norif " + testTsvFile1,
                    expectedHeader + "chrN\t0\tA\t1\tA\n", null},
                {"One file path with -dh", "norif -dh col1,col2 " + testTsvFile1,
                        expectedHeader + "chrN\t0\tA\t1\tA\n", null},
                {"Multiple file paths with top", "norif " + testTsvFile1 + " " + testTsvFile2 + "| select 1-3 | top 2",
                    expectedHeader + "chrN\t0\tA\t1\tA\n" + "chrN\t0\tB\t2\tB\n", null},
                {"Nested query started with nor", "nor <(norif " + testTsvFile1 + "| top 1)",
                    expectedHeader + "chrN\t0\tA\t1\tA\n", null},
                {"Nested query started with norif", "norif <(nor " + testTsvFile1 + ")",
                    expectedHeader + "chrN\t0\tA\t1\tA\n", null},
                {"Invalid file path", "norif -dh col1,col2 not_exists.tsv", HEADER_PREFIX+ "col1\tcol2\n", null},
                {"Invalid file path missing -dh", "norif not_exists.tsv", null, GorParsingException.class},
                {"Empty file missing -dh", "norif " + testEmptyFile, null, GorParsingException.class},
                {"Invalid file path with invalid -dh value", "norif -dh col1 not_exists.tsv", null, GorParsingException.class},
                {"Both invalid and valid file path", "norif " + testTsvFile1 + " not_exists.tsv",
                    expectedHeader + "chrN\t0\tA\t1\tA\n", null},
        });
    }

    @Test
    public void testNorif() {
        if (expectedException != null) {
            try {
                TestUtils.runGorPipe(query);
                Assert.fail("Expected test to throw an exception");
            } catch (GorParsingException e) {
                // Expected
            }
        } else {
            String result = TestUtils.runGorPipe(query);
            Assert.assertEquals(expectedResult, result);
        }
    }
}

