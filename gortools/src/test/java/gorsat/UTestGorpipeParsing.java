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

import org.gorpipe.test.GorDictionarySetup;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;


public class UTestGorpipeParsing {

    @Test
    public void testSingleQuotedLiteral() {
        TestUtils.assertCalculated("'abcd1234'", "abcd1234");
    }

    @Test
    public void testSingleQuotedLiteralWithEscapedQuote() {
        TestUtils.assertCalculated("'abc\\'d1234'", "abc'd1234");
    }

    @Test
    public void testSingleQuotedLiteralWithBackslash() {
        TestUtils.assertCalculated("'abc\\xd1234'", "abcxd1234");
    }

    @Test
    public void testSingleQuotedLiteralWithEscapedBackslash() {
        TestUtils.assertCalculated("'abc\\\\d1234'", "abc\\d1234");
    }

    @Test
    public void testDoubleQuotedLiteral() {
        TestUtils.assertCalculated("\"abcd1234\"", "abcd1234");
    }

    @Test
    public void testDoubleQuotedLiteralWithEscapedQuote() {
        TestUtils.assertCalculated("\"abcd\\\"1234\"", "abcd\"1234");
    }

    @Test
    public void testLongLiteral() {
        String data = "TO2_FB11440MO,TO2_FB11440P1,TO2_FB11440S1,TO2_FB11441FA,TO2_FB11441MO,TO2_FB11441P1,TO2_FB11441S1,TO2_FB11445FA,TO2_FB11445MO,TO2_FB11445P1,TO2_FB11445S1,TO2_FB11446FA,TO2_FB11446P1,TO2_FB11959MO,TO2_FB11959S1,TO2_FB11963FA,TO2_FB11963P1,TO2_FB11963S1,TO2_FB11964FA,TO2_FB11964MO,TO2_FB11964P1,TO2_FB11964S1,TO2_FB11966FA,TO2_FB11966MO,TO2_FB11966P1,TO2_FB11966S1,TO2_FB12483MO,TO2_FB12483P1,TO2_FB12483S1,TO2_FB12484FA,TO2_FB12484MO,TO2_FB12484P1,TO2_FB12484S3,TO2_FB12485FA,TO2_FB12485MO,TO2_FB12485P1,TO2_FB12493FA,TO2_FB12493S1,TO2_FB12962FA,TO2_FB12962MO,TO2_FB12962P1,TO2_FB12962S1,TO2_FB12964FA,TO2_FB12964MO,TO2_FB12964P1,TO2_FB12964S1,TO2_FB12967S1,TO2_FB12969FA,TO2_FB12969MO,TO2_FB12969P1,TO2_FB12969S1,TO2_FB13446FA,TO2_FB13446P1,TO2_FB13451FA,TO2_FB13451MO,TO2_FB13451P1,TO2_FB13454FA,TO2_FB13454MO,TO2_FB13454P1,TO2_FB13456FA,TO2_FB13456MO,TO2_FB13456P1,TO2_FB13456S1,TO2_FB13806FA,TO2_FB13808FA,TO2_FB13808MO,TO2_FB13808P1,TO2_FB13808S1,TO2_FB13809FA,TO2_FB13809MO,TO2_FB13809P1,TO2_FB13809S1,TO2_FB13810P1,TO2_FB13812FA,TO2_FB13812MO,TO2_FB13812P1,TO2_FB14106P1,TO2_FB14106S1,TO2_FB14108FA,TO2_FB14108MO,TO2_FB14108S1,TO2_FB14109FA,TO2_FB14109MO,TO2_FB14109P1,TO2_FB14110FA,TO2_FB14110MO,TO2_FB14110P1,TO2_FB14110S1,TO2_FB14407FA,TO2_FB14407MO,TO2_FB14407P1,TO2_FB14407S1,TO2_FB14409FA,TO2_FB14409MO,TO2_FB14409P1,TO2_FB14410FA,TO2_FB14410MO,TO2_FB14410P1,TO2_FB14410S1,TO2_FB14411MO";
        TestUtils.assertCalculated("'" + data + "'", data);
    }

    @Test
    public void testDoubleQuotedLiteralWithBackSlash() {
        TestUtils.assertCalculated("\"abcd\\x1234\"", "abcdx1234");
    }

    @Test
    public void testDoubleQuotedLiteralWithEscapedBackSlash() {
        TestUtils.assertCalculated("\"abcd\\\\1234\"", "abcd\\1234");
    }

    @Test
    public void testNestedFiltering() throws Exception {
        final int linesPerTag = 10;
        final int numFiles = 10;

        // Create test data;
        GorDictionarySetup dict = new GorDictionarySetup("testGorpipeParsingDictA", numFiles, 5, new int[]{1}, linesPerTag);

        String filtertags = "PN1,PN2";
        String query = "gor -parts 2 -dict " + dict.dictionary + " -f " + filtertags + " <(gor -fs " + dict.dictionary + " -f #{tags},BOGUS_TAG | top 1 | select 1,2,3)";
        TestUtils.assertGorpipeResults("Chr\tPos\tPN\n" + "chr1\t1\tPN1\n" + "chr1\t1\tPN2\n", query);
    }


    @Test
    public void testPredicateFilter() {
        //second filter exceeds int character limit, triggering long colType in ParseArith.
        String query = "gor ../tests/data/gor/genes.gor | where (Chrom = 'chr21' and gene_start >= 9683175 and gene_start <= 2147483648)";
        String[] lines = TestUtils.runGorPipeLines(query);
        Assert.assertEquals(23439,Arrays.toString(lines).length());
    }
}
