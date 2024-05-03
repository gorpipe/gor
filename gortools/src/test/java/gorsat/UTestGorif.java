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

@RunWith(Parameterized.class)
public class UTestGorif {

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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws IOException {
        //create test files
        TemporaryFolder workDir = new TemporaryFolder();
        workDir.create();
        Path workDirPath = workDir.getRoot().toPath();
        Path testFileGor1 = workDirPath.resolve("test_file_1.gor");
        Files.writeString(testFileGor1, "#chrom\tpos\tcol3\nchr1\t0\ta\n");
        Path testFileGor2 = workDirPath.resolve("test_file_2.gor");
        Files.writeString(testFileGor2, "#chrom\tpos\tcol3\nchr2\t2\tb\nchr3\t1\tc\n");
        Path testEmptyFile = workDirPath.resolve("empty_file.gor");

        return Arrays.asList(new Object[][]{
                //{testName, query, expectedResult, expectedException}
                {"No file path", "gorif", null, GorParsingException.class},
                {"Empty file", "gorif -dh col1,col2 " + testEmptyFile, "col1\tcol2\n", null},
                {"One file path", "gorif " + testFileGor1,
                    """
                    chrom\tpos\tcol3
                    chr1\t0\ta
                    """, null},
                {"Multiple file paths", "gorif " + testFileGor1 + " " + testFileGor2,
                    """
                    chrom\tpos\tcol3
                    chr1\t0\ta
                    chr2\t2\tb
                    chr3\t1\tc
                    """, null},
                {"Multiple file paths with top", "gorif " + testFileGor1 + " " + testFileGor2 + "| top 2",
                    """
                    chrom\tpos\tcol3
                    chr1\t0\ta
                    chr2\t2\tb
                    """, null},
                {"Multiple file paths with -p filter", "gorif  -p chr2:1-5 " + testFileGor1 + " " + testFileGor2,
                    """
                    chrom\tpos\tcol3
                    chr2\t2\tb
                    """, null},
                {"Nested query started with gor", "gor <(gorif " + testFileGor1 + "| top 1)",
                """
                chrom\tpos\tcol3
                chr1\t0\ta
                """, null},
                {"Nested query started with gor with -p", "gor -p chr2:1-5 <(gorif " + testFileGor1  + " " + testFileGor2 + ")",
                        """
                chrom\tpos\tcol3
                chr2\t2\tb
                """, null},
                {"Nested query started with gor with -p and -dh", "gor -p chr2:1-5 <(gorif -dh col1,col2 not_exists.gor)",
                "col1\tcol2\n", null},
                {"Nested query started with nor", "nor <(gorif " + testFileGor1 + "| top 1)",
                        """
                ChromNOR\tPosNOR\tchrom\tpos\tcol3
                chr1\t0\tchr1\t0\ta
                """, null},
                {"Nested query started with gorif", "gorif <(gor " + testFileGor1 + ")",
                """
                chrom\tpos\tcol3
                chr1\t0\ta
                """, null},
                {"Invalid file path missing -dh", "gorif not_exists.gor", null, GorParsingException.class},
                {"Empty file missing -dh", "gorif " + testEmptyFile, null, GorParsingException.class},
                {"Invalid file path with -dh", "gorif -dh col1,col2 not_exists.gor", "col1\tcol2\n", null},
                {"Invalid file path with invalid -dh value", "gorif -dh col1 not_exists.gor", null, GorParsingException.class},
                {"Both invalid and valid file path", "gorif " + testFileGor1 + " not_exists.gor",
                """
                chrom\tpos\tcol3
                chr1\t0\ta
                """, null}
        });
    }

    @Test
    public void testGorif() {
        if (expectedException != null) {
            try {
                TestUtils.runGorPipe(query);
                Assert.fail("Expected test to throw an exception");
            } catch (GorParsingException e) {
                // Expected
            }
        } else {
            Assert.assertEquals(expectedResult, TestUtils.runGorPipe(query));
        }
    }
}
