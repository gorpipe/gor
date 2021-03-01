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

package gorsat.parser;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class UTestCols2List {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void singleColumn() {
        String query = "gorrow 1,1 | calc a 42 | calc r cols2list('a')";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\ta\tr\n" +
                "chr1\t1\t42\t42\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void singleColumnNoQuotes() {
        String query = "gorrow 1,1 | calc a 42 | calc r cols2list(a)";
        expectedException.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void twoColumns() {
        String query = "gorrow 1,1 | calc a 42 | calc b 3.14 | calc r cols2list('a,b')";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\ta\tb\tr\n" +
                "chr1\t1\t42\t3.14\t42,3.14\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void twoColumnsNor() {
        String query = "norrows 1 | calc a 42 | calc b 3.14 | calc r cols2list('a,b')";
        String res = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tRowNum\ta\tb\tr\n" +
                "chrN\t0\t0\t42\t3.14\t42,3.14\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void columnWildCard() {
        String query = "gorrow 1,1 | calc a1 42 | calc a2 3.14 | calc b1 'bingo' | calc r cols2list('a*')";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\ta1\ta2\tb1\tr\n" +
                "chr1\t1\t42\t3.14\tbingo\t42,3.14\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void columnWildCardWithCustomSeparator() {
        String query = "gorrow 1,1 | calc a1 42 | calc a2 3.14 | calc b1 'bingo' | calc r cols2list('a*', ':')";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\ta1\ta2\tb1\tr\n" +
                "chr1\t1\t42\t3.14\tbingo\t42:3.14\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void usingWithContains() {
        String query = "gorrow 1,1 | calc a1 'bingo' | calc a2 'bongo' | calc b1 'foo' | calc r if(contains(cols2list('a*'), 'bingo'), 1, 0)";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\ta1\ta2\tb1\tr\n" +
                "chr1\t1\tbingo\tbongo\tfoo\t1\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void singleColumnMap() {
        String query = "gorrow 1,1 | calc a 42 | calc r cols2listmap('a', 'int(x)*2')";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\ta\tr\n" +
                "chr1\t1\t42\t84\n";
        Assert.assertEquals(expected, res);
    }

    @Test
    public void twoColumnsMapCustomSep() {
        String query = "gorrow 1,1 | calc a 42 | calc b 3.14 | calc r cols2listmap('a,b', 'float(x)*2', ':')";
        String res = TestUtils.runGorPipe(query);
        String expected = "chrom\tpos\ta\tb\tr\n" +
                "chr1\t1\t42\t3.14\t84.0:6.28\n";
        Assert.assertEquals(expected, res);
    }

}
