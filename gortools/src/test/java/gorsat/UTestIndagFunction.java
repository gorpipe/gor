/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import gorsat.parser.ParseArith;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import scala.util.parsing.combinator.Parsers;

import java.io.File;
import java.io.IOException;


/**
 * INDAG function unit testing
 *
 * @author Brett Archuleta
 */

public class UTestIndagFunction {

    private static File goDag;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        goDag = FileTestUtils.createTempFile(workDir.getRoot(), "go_dag.txt",
                "GO:0000002\tGO:0032042\n" +
                        "GO:0000002\tGO:0033955\n" +
                        "GO:0000003\tGO:0003006\n" +
                        "GO:0000003\tGO:0019098\n" +
                        "GO:0000003\tGO:0019953\n" +
                        "GO:0000003\tGO:0019954\n" +
                        "GO:0000003\tGO:0022414\n" +
                        "GO:0000003\tGO:0032504\n" +
                        "GO:0000003\tGO:0032505\n" +
                        "GO:0000003\tGO:0051321\n" +
                        "GO:0000009\tGO:0033164\n" +
                        "GO:0000009\tGO:0052824\n" +
                        "GO:0000009\tGO:0052917\n" +
                        "GO:0000012\tGO:1903516\n" +
                        "GO:0000012\tGO:1903517\n" +
                        "GO:0000012\tGO:1903518\n" +
                        "GO:0000012\tGO:1903824\n"
        );

    }

    /**
     * This method takes a string of commands as the input and returns a string reference to the parsed object from
     * spredicomp() in ParseUtilities.scala:1952.
     *
     * @param indagCommandString the string that is parsed by the specific parser spredicom() for the indag function within a gor query.
     */
    private String parseSpredicompCommands(String indagCommandString) {
        ParseArith instanceOfParseParseArithClass = new ParseArith(null);
        Parsers.ParseResult parseResult = instanceOfParseParseArithClass.parseAll(instanceOfParseParseArithClass.spredicomp(), indagCommandString);
        return parseResult.toString();
    }

    /**
     * Generic Test -- quoted dag file
     */
    @Test
    public void genericIndagTestQuotedDAGFile() throws IOException {
        String result = TestUtils.runGorPipe("gor 1.mem | top 10 | where '1.0' indag('" + goDag.getCanonicalPath() + "','GO:0000008')");
        Assert.assertEquals("Should return the file header", "Chromo\tPos\tCol3\tCol4\tCol5\n", result);
    }

    /**
     * Generic Test -- unquoted dag file
     */
    @Test
    public void genericIndagTestUnquotedDAGFile() throws IOException {
        String result = TestUtils.runGorPipe("gor 1.mem | top 10 | where '1.0' indag(" + goDag.getCanonicalPath() + ",'GO:0000008')");
        Assert.assertEquals("Should return the file header", "Chromo\tPos\tCol3\tCol4\tCol5\n", result);
    }

    /**
     * Generic Test -- alias for dag file
     */
    @Test
    public void genericIndagTestGorAlias() throws IOException {
        String result = TestUtils.runGorPipe("create #yyy# = gor 1.mem | signature -timeres 1; create #indagAliasTest# = nor " + goDag.getCanonicalPath() + " | signature -timeres 1 | top 100; gor [#yyy#] | top 100 | where '1.0' indag([#indagAliasTest#],'GO:0000008');");
        Assert.assertEquals("Should return the file header", "Chromo\tPos\tCol3\tCol4\tCol5\n", result);
    }

    /**
     * Quoted Dag File Unit Test - this tests that the parser successfully parses quoted filenames passed to the Indag(filename, str-cont) function
     * by comparing expected and actual RuntimeExceptions.
     *
     * @throws RuntimeException
     */
    @Test
    public void testIndagQuotedFilename() throws RuntimeException {
        try {
            String quotedFileTestString = "1.0 INDAG('xxx','yyy')";
            parseSpredicompCommands(quotedFileTestString);
        } catch (GorResourceException exception) {
            File file = new File(exception.getUri());
            Assert.assertEquals("xxx", file.getName());
        }
    }

    /**
     * Unquoted Dag File Unit Test - this tests that the parser successfully parses unquoted filenames passed to the Indag(filename, str-cont)
     * function by comparing expected and actual RuntimeExceptions.
     *
     * @throws RuntimeException
     */
    @Test
    public void testIndagUnquotedFilename() throws RuntimeException {
        try {
            String unquotedFileTestString = "1.0 INDAG([#temp#],'yyy')";
            parseSpredicompCommands(unquotedFileTestString);
        } catch (GorResourceException exception) {
            File file = new File(exception.getUri());
            Assert.assertEquals("[#temp#]", file.getName());
        }
    }

    @Test
    public void indagShouldBeCaseInsensitive() {
        String query = "create xxx = norrows 10 | calc parent 1+rownum | calc child 2+rownum | select parent,child | replace 1- 'x'+#rc;\n" +
                "nor [xxx] | where child indag([xxx],'X5')";

        String expected = "ChromNOR\tPosNOR\tparent\tchild\n" +
                "chrN\t0\tx4\tx5\n" +
                "chrN\t0\tx5\tx6\n" +
                "chrN\t0\tx6\tx7\n" +
                "chrN\t0\tx7\tx8\n" +
                "chrN\t0\tx8\tx9\n" +
                "chrN\t0\tx9\tx10\n" +
                "chrN\t0\tx10\tx11\n";

        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }
}
