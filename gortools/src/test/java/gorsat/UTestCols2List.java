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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class UTestCols2List {

    @Test
    public void shouldCollapseColumnsWhenNoExpression() {
        String query = "gor ../tests/data/external/samtools/test.vcf | cols2list id,ref,alt values| top 1";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("CHROM\tPOS\tvalues\nchr20\t14370\trs6054257,G,A\n", res);
    }

    @Test
    public void shouldCollapseColumnsWhenNoExpressionInNor() {
        String query = "norrows 10 | calc x rownum*2 | calc y rownum*3 | cols2list x,y values -gc 1,2";
        String res = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tRowNum\tx\tvalues\n" +
                "chrN\t0\t0\t0\t0,0\n" +
                "chrN\t0\t1\t2\t2,3\n" +
                "chrN\t0\t2\t4\t4,6\n" +
                "chrN\t0\t3\t6\t6,9\n" +
                "chrN\t0\t4\t8\t8,12\n" +
                "chrN\t0\t5\t10\t10,15\n" +
                "chrN\t0\t6\t12\t12,18\n" +
                "chrN\t0\t7\t14\t14,21\n" +
                "chrN\t0\t8\t16\t16,24\n" +
                "chrN\t0\t9\t18\t18,27\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void shouldCollapseColumnsWithExpression() {
        String query = "gor ../tests/data/external/samtools/test.vcf | cols2list id,ref,alt values -map lower(x) | top 1";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("CHROM\tPOS\tvalues\nchr20\t14370\trs6054257,g,a\n", res);
    }

    @Test
    public void shouldCollapseColumnsWithExpressionReferencingOtherColumns() {
        String query = "gor ../tests/data/external/samtools/test.vcf | cols2list 10 values -map vcfformattag(format,x,'GT') | top 1";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("CHROM\tPOS\tvalues\nchr20\t14370\t0|0\n", res);
    }

    @Test
    public void shouldAllowExpressionReturningNumber() {
        String [] contents = {
                "Chrom\tPos\tCol1\tCol2\tCol3",
                "chr1\t1\t3\t4\t5"
        };
        String expected = "Chrom\tPos\tvalues\n" +
                "chr1\t1\t4,5,6\n";

        assertQuery(contents, expected, "gor %s | cols2list col1-col3 values -map int(x)+1");
    }

    @Test
    public void shouldCollapseColumnsWithDoubleQoutedExpression() {
        String [] contents = {
                "Chrom\tPos\tCol1\tCol2\tCol3",
                "chr1\t1\ta\tb\tc"
        };
        String expected = "Chrom\tPos\tvalues\n" +
                "chr1\t1\tAx,Bx,Cx\n";

        assertQuery(contents, expected, "gor %s | cols2list col1-col3 values -map \"upper(x) + 'x'\"");
    }

    @Test
    public void shouldCollapseColumnsWithSingleQoutedExpression() {
        String [] contents = {
                "Chrom\tPos\tCol1\tCol2\tCol3",
                "chr1\t1\ta\tb\tc"
        };
        String expected = "Chrom\tPos\tvalues\n" +
                "chr1\t1\tAx,Bx,Cx\n";

        assertQuery(contents, expected, "gor %s | cols2list col1-col3 values -map 'upper(x) + \"x\"'");
    }

    private void assertQuery(String[] lines, String expected, String formatString) {
        final File gorFile = TestUtils.createGorFile("UTestCols2List", lines);
        String query = String.format(formatString, gorFile);
        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }
}
