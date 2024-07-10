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

import gorsat.Commands.CommandParseUtilities;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

/**
 * Created by sigmar on 21/05/16.
 */
public class UTestGorGorScriptComments {

    private File gorScript;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        gorScript = FileTestUtils.createTempFile(workDir.getRoot(), "gor.script",
                "create xxx = gor ../tests/data/gor/dbsnp_test.gorz | top 10 /* more description */;\n" +
                        "gor [xxx]\n"
        );
    }

    @Test
    public void testQuoteWithinComments() {
        String query = "/* ' */\ngorrows chr1,1,1";
        String result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("\ngorrows chr1,1,1", result);

        String query2 = "/* ' */'ABC'/* ' */\ngorrows chr1,1,1";
        String result2 = CommandParseUtilities.removeComments(query2);
        Assert.assertEquals("'ABC'\ngorrows chr1,1,1", result2);
    }

    @Test
    public void testBlockComments() {
        String query = "/* This is a query comment*/\ngorrows chr1,1,1";
        String result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("\ngorrows chr1,1,1", result);

        query = "/*This is a line comment*/";
        result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("", result);

        query = "gorrows chr1,1,1\n/*This is a multiline comment\nFooBar*/";
        result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("gorrows chr1,1,1\n", result);

        query = "gorrows chr1,1,1\nThis is /*a line comment*/\nFooBar";
        result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("gorrows chr1,1,1\nThis is \nFooBar", result);

        query = "gorrows chr1,1,1\nThis is /*a line comment*/\n/*FooBar*/";
        result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("gorrows chr1,1,1\nThis is \n", result);
    }

    @Test
    public void testNoCharRescan() {
        // Once a * or / is consumed for a comment delimiter, it is not paired into another one
        // These comments with no content are technically valid:
        String query = "gorrows chr1,1,1\nThis is /**//*/**/*/\n/*FooBar*/";
        String result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("gorrows chr1,1,1\nThis is \n", result);

        // '/*/' should not be parsed as both comment start and end, so this is malformed
        query = "gorrows chr1,1,1\nThis is /*/\n/*FooBar*/";
        String text = exceptionTextFromRemoveComments(query);
        String exp = ("Malformed comment: unterminated comment starting at position " + 25);
        Assert.assertEquals(exp, text);
    }

    @Test
    public void testNestedComments() {
        String query = "/* This is a /* query */ comment*/\ngorrows chr1,1,1";
        String result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("\ngorrows chr1,1,1", result);

        query = "/*This is a /* line */ comment*/";
        result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("", result);

        query = "gorrows chr1,1,1\n/*This is /*a multiline comment\nFoo*/Bar*/";
        result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("gorrows chr1,1,1\n", result);

        query = "gorrows chr1,1,1\n/*This is /*/*/*a pathologically packed comment\nFoo*/*/*/Bar*/";
        result = CommandParseUtilities.removeComments(query);
        Assert.assertEquals("gorrows chr1,1,1\n", result);
    }

    @Test
    public void testGorScriptComments() throws IOException {
        String[] args = new String[]{"-script", gorScript.getCanonicalPath(), ""};
        int count = TestUtils.runGorPipeCount(args);

        Assert.assertEquals("Wrong number of lines read from gor.script execution", count, 10);
    }

    @Test
    public void testCommentInQuotes() {
        String query = "norrows 1 /* 'hello/*' */";
        // Now 'true' is default, and commented quotes are ignored, so original expectation no longer applies
        // String result = CommandParseUtilities.removeComments(query, true);
        // Assert.assertEquals("norrows 1 ", result);
        String text = exceptionTextFromRemoveComments(query);
        String exp = ("Malformed comment: unterminated comment starting at position " + 10);
        Assert.assertEquals(exp, text);
    }

    @Test
    public void testSqlHintComment() {
        {
            // Outer comment with /*+ is preserved, and regular comments are removed
            String query = "gor /*+ BROADCAST(a) */ /* this is /*inner*/first comment /* Nested */ */c:/data/test./* comment*/gor";
            String result = CommandParseUtilities.removeComments(query);
            String exp = "gor /*+ BROADCAST(a) */ c:/data/test.gor";
            Assert.assertEquals(exp, result);
        }
        {
            // Nested comment is removed, with or without '+'
            // and independent of type of surrounding comment
            // maybe these are not useful constructs but this documents behavior
            String query = "gor /*+ BROADCAST(a) /*+removed*/*//* also \n removed */ /* this is /*inner*/first /*+removed*/ comment /* Nested */ */c:/data/test./* comment*/gor";
            String result = CommandParseUtilities.removeComments(query);
            String exp = "gor /*+ BROADCAST(a) */ c:/data/test.gor";
            Assert.assertEquals(exp, result);
        }
        {
            // string inside SQL hint is parsed as such, so delimiter it contains is ignored
            // maybe never useful, but documents behavior
            String query = "norrows 1 /*+ BROADCAST() 'hello/*' 'YES /* string with contained comment */' */";
            String result = CommandParseUtilities.removeComments(query);
            String exp = "gor /*+ BROADCAST(a) */ c:/data/test.gor";
            Assert.assertEquals(query, result);
        }

    }


    String exceptionTextFromRemoveComments(String query) {
        try {
            String result = CommandParseUtilities.removeComments(query);
            Assert.fail("Should have thrown an exception");
            return null;
        } catch (GorParsingException e) {
            return e.getMessage();
        }
    }

    @Test
    public void testMalformedComments() {
        {
            String query = "/* ' */\ngorrows chr1,1,1/*";
            String text = exceptionTextFromRemoveComments(query);
            String exp = ("Malformed comment: unterminated comment starting at position " + (query.length() - 2));
            Assert.assertEquals(exp, text);
        }
        {
            String query = "/* ' */\ngorrows chr1,1,1*/";
            String text = exceptionTextFromRemoveComments(query);
            String exp = ("Malformed comment: unpaired comment terminator \"*/\" at position " + (query.length() - 2));
            Assert.assertEquals(exp, text);
        }
        {
            String query = "/* /*' */\ngorrows chr1,1,1/*";
            String text = exceptionTextFromRemoveComments(query);
            String exp = ("Malformed comment: unterminated comment starting at position " + 0);
            Assert.assertEquals(exp, text);
        }
        {
            String query = "/* ' */  /*\ngorrows chr1,1,1/*";
            String text = exceptionTextFromRemoveComments(query);
            String exp = ("Malformed comment: unterminated comment starting at position " + 9);
            Assert.assertEquals(exp, text);
        }
        {
            String query = "norrows 1 /* 'hello/*' */";
            String text = exceptionTextFromRemoveComments(query);
            String exp = ("Malformed comment: unterminated comment starting at position " + 10);
            Assert.assertEquals(exp, text);
        }
        {
            String query = "norrows 1 /*+ BROADCAST() /* hello */";
            String text = exceptionTextFromRemoveComments(query);
            String exp = ("Malformed comment: unterminated SQL hint comment starting at position " + 10);
            Assert.assertEquals(exp, text);
        }
    }


    @Test
    public void testAdjoiningCommentBlocks() {
        String query = "create #dummy# = nor <(norrows 100  | sort -c rownum:n);\n" +
                "\n" +
                "\n" +
                "create #buckets# = nor [#dummy#] | rename #1 PN | calc bucket 'b_'+str(div(PN,10)+1);\n" +
                "\n" +
                "create #loci# = gorrow chr1,1,2 | multimap -cartesian -h <(norrows 2) | calc Npos #2+RowNum | select 1,Npos | sort genome | rename #2 Pos | calc ref 'G' |  calc alt 'C';\n" +
                "\n" +
                "create #gt# = gor [#loci#] | multimap -cartesian -h [#buckets#] | distloc 2 | hide bucket | calc gt mod(PN,4) ;\n" +
                "\n" +
                "create #cov# = gorrow chr1,0,3 | multimap -cartesian -h [#buckets#] | where not(bucket='b_2');\n" +
                "\n" +
                "/*\n" +
                "create #t0# = gor [#gt#] | select 1-4 | distinct\n" +
                "| multimap -cartesian -h <(nor [#buckets#])\n" +
                "| varjoin -r -l -xl pn -xr pn [#gt#]\n" +
                "| sort 1 -c ref,alt,PN\n" +
                "| group 1 -gc ref,alt,bucket -lis -sc gt -s '' | rename lis_gt values;\n" +
                "*/\n" +
                "\n" +
                "/*\n" +
                "gor [#gt#] | gtgen -gc ref,alt [#buckets#] [#cov#] \n" +
                "| csvsel -gc ref,alt -u 4 -vs 1 -tag PN [#buckets#] <(nor [#buckets#] | select #1) | rename value GT\n" +
                "| merge [#gt#] | group 1 -gc 3- -count | throwif allcount != 2\n" +
                "*/\n" +
                "\n" +
                "gor [#gt#] | rownum | where mod(rownum,2)=1 | hide rownum | merge <(gorrow chr1,1,2 | calc ref 'g' | calc alt 't' | hide #3 | rename #2 Pos) " +
                "/* | merge [#t0#] \n" +
                "*/";

        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals("Wrong number of lines read from gor.script execution", 101, count);
    }
}
