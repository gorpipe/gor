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
import org.junit.Assert;
import org.junit.Test;

public class UTestBooleanFunctions {
    @Test
    public void testIf() {
        TestUtils.assertCalculated("if(2>1,'A','B')", "A");
        TestUtils.assertCalculated("if(2>10,2,10)", "10");
        TestUtils.assertCalculated("if(1==1,'test','123')", "test");
    }

    @Test
    public void testNot() {
        TestUtils.assertCalculated("if(not(2>1),'A','B')", "B");
    }

    @Test
    public void testStringLe() {
        TestUtils.assertCalculated("if('a' <= 'b', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('b' <= 'a', 'true', 'false')", "false");
        TestUtils.assertCalculated("if('a' <= 'a', 'true', 'false')", "true");
    }

    @Test
    public void testStringGe() {
        TestUtils.assertCalculated("if('a' >= 'b', 'true', 'false')", "false");
        TestUtils.assertCalculated("if('b' >= 'a', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('a' >= 'a', 'true', 'false')", "true");
    }

    @Test
    public void testStringNe() {
        TestUtils.assertCalculated("if('a' <> 'b', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('b' <> 'a', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('a' <> 'a', 'true', 'false')", "false");
        TestUtils.assertCalculated("if('a' != 'b', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('b' != 'a', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('a' != 'a', 'true', 'false')", "false");
    }

    @Test
    public void testStringEq() {
        TestUtils.assertCalculated("if('a' = 'b', 'true', 'false')", "false");
        TestUtils.assertCalculated("if('b' = 'a', 'true', 'false')", "false");
        TestUtils.assertCalculated("if('a' = 'a', 'true', 'false')", "true");
    }

    @Test
    public void testStringLt() {
        TestUtils.assertCalculated("if('a' < 'b', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('b' < 'a', 'true', 'false')", "false");
        TestUtils.assertCalculated("if('a' < 'a', 'true', 'false')", "false");
    }

    @Test
    public void testStringGt() {
        TestUtils.assertCalculated("if('a' > 'b', 'true', 'false')", "false");
        TestUtils.assertCalculated("if('b' > 'a', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('a' > 'a', 'true', 'false')", "false");
    }

    @Test
    public void testStringIn() {
        TestUtils.assertCalculated("if('a' in ('b'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if('b' in ('a','b','c','d'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if('a' in ('b','c','d','e'), 'true', 'false')", "false");
    }

    @Test
    public void testIntLe() {
        TestUtils.assertCalculated("if(1 <= 2, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2 <= 1, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1 <= 1, 'true', 'false')", "true");
    }

    @Test
    public void testIntGe() {
        TestUtils.assertCalculated("if(1 >= 2, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(2 >= 1, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1 >= 1, 'true', 'false')", "true");
    }

    @Test
    public void testIntNe() {
        TestUtils.assertCalculated("if(1 <> 2, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2 <> 1, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1 <> 1, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1 != 2, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2 != 1, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1 != 1, 'true', 'false')", "false");
    }

    @Test
    public void testIntEq() {
        TestUtils.assertCalculated("if(1 = 2, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(2 = 1, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1 = 1, 'true', 'false')", "true");
    }

    @Test
    public void testIntLt() {
        TestUtils.assertCalculated("if(1 < 2, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2 < 1, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1 < 1, 'true', 'false')", "false");
    }

    @Test
    public void testIntGt() {
        TestUtils.assertCalculated("if(1 > 2, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(2 > 1, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1 > 1, 'true', 'false')", "false");
    }

    @Test
    public void testDoubleLe() {
        TestUtils.assertCalculated("if(1.0 <= 2.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2.0 <= 1.0, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1.0 <= 1.0, 'true', 'false')", "true");
    }

    @Test
    public void testDoubleGe() {
        TestUtils.assertCalculated("if(1.0 >= 2.0, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(2.0 >= 1.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1.0 >= 1.0, 'true', 'false')", "true");
    }

    @Test
    public void testDoubleNe() {
        TestUtils.assertCalculated("if(1.0 <> 2.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2.0 <> 1.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1.0 <> 1.0, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1.0 != 2.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2.0 != 1.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1.0 != 1.0, 'true', 'false')", "false");
    }

    @Test
    public void testDoubleEq() {
        TestUtils.assertCalculated("if(1.0 = 2.0, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(2.0 = 1.0, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1.0 = 1.0, 'true', 'false')", "true");
    }

    @Test
    public void testDoubleLt() {
        TestUtils.assertCalculated("if(1.0 < 2.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(2.0 < 1.0, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(1.0 < 1.0, 'true', 'false')", "false");
    }

    @Test
    public void testDoubleGt() {
        TestUtils.assertCalculated("if(1.0 > 2.0, 'true', 'false')", "false");
        TestUtils.assertCalculated("if(2.0 > 1.0, 'true', 'false')", "true");
        TestUtils.assertCalculated("if(1.0 > 1.0, 'true', 'false')", "false");
    }

    @Test
    public void testLike() {
        TestUtils.assertCalculated("if('' like '', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abc' like '*', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abc' like '???', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abcdef' like '???', 'true', 'false')", "false");
    }

    @Test
    public void testRLike() {
        TestUtils.assertCalculated("if('' rlike '', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abc' rlike '.*', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abc' rlike '...', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abcdef' rlike '...', 'true', 'false')", "false");
    }

    @Test
    public void testTilde() {
        TestUtils.assertCalculated("if('' ~ '', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abc' ~ '.*', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abc' ~ '...', 'true', 'false')", "true");
        TestUtils.assertCalculated("if('abcdef' ~ '...', 'true', 'false')", "false");
    }

    @Test
    public void testIsInt() {
        TestUtils.assertCalculated("if(isint(Chromo), 'A', 'B')", "B");
        TestUtils.assertCalculated("if(isint(pos), 'A', 'B')", "A");
    }

    @Test
    public void testIsLong() {
        TestUtils.assertCalculated("if(islong(Chromo), 'A', 'B')", "B");
        TestUtils.assertCalculated("if(islong(pos), 'A', 'B')", "A");
    }

    @Test
    public void testIsFloat() {
        TestUtils.assertCalculated("if(isfloat(Chromo), 'A', 'B')", "B");
    }

    @Test
    public void testListHasAny() {
        // Need to work with rows here so we can do listhasany without using string literals

        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a' | where listhasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\ta,b,c,d,e,f\ta\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 't' | where listhasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 't,d' | where listhasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\ta,b,c,d,e,f\tt,d\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 't,u' | where listhasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'A,B,C,D,E,F' | calc y 't,d' | where listhasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\tA,B,C,D,E,F\tt,d\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'A,B,C,D,E,F' | calc y 't,u' | where listhasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);
    }

    @Test
    public void testListHasAnyLiteralList() {
        TestUtils.assertCalculated("if(listhasany('a,b,c,d,e,f', 'a'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(listhasany('a,b,c,d,e,f', 'x'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(listhasany('a,b,c,d,e,f', 't', 'h', 'e', 's'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(listhasany('A,B,C,D,E,F', 't', 'h', 'e', 's'), 'true', 'false')", "true");
    }

    @Test
    public void testListHasAnyUsedInWhere() {
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 1", "gor ../tests/data/gor/genes.gorz | top 1 | where listhasany('hjalti,thor','thor')");
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 0", "gor ../tests/data/gor/genes.gorz | where listhasany('hjalti,thor,isleifsson','jon')");
        int count = TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | top 5 | calc SOME_GENE if(LISTHASANY(gene_symbol,'WASH7P'),'Tier1',if(LISTHASANY(gene_symbol,'FAM138A'),'Tier2',if(LISTHASANY(gene_symbol,'OR4G4P'),'Tier3',gene_symbol)))");
        Assert.assertEquals("Wrong number of lines using LISTHASANY", count, 5);
    }

    @Test
    public void testCsListHasAny() {
        // Need to work with rows here so we can do listhasany without using string literals

        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a' | where cslisthasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\ta,b,c,d,e,f\ta\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 't' | where cslisthasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 't,d' | where cslisthasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\ta,b,c,d,e,f\tt,d\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 't,u' | where cslisthasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'A,B,C,D,E,F' | calc y 't,d' | where cslisthasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'A,B,C,D,E,F' | calc y 't,u' | where cslisthasany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);
    }

    @Test
    public void testCsListHasAnyUsingStringLiterals() {
        TestUtils.assertCalculated("if(cslisthasany('a,b,c,d,e,f', 'a'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(cslisthasany('a,b,c,d,e,f', 'x'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(cslisthasany('a,b,c,d,e,f', 't', 'h', 'e', 's'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(cslisthasany('A,B,C,D,E,F', 't', 'h', 'e', 's'), 'true', 'false')", "false");
    }

    @Test
    public void testContains() {
        TestUtils.assertCalculated("if(contains('the test string', 't', 'h', 'e', 's'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(contains('the test string', 't', 'h', 'e', 's', ','), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(contains('the test, string', 't', 'h', 'e', 's', ','), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(contains('the test string', 'the', 'test'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(contains('the test string', 'T'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(contains('the test string', 'x'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(contains('the, test, string', ','), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(contains('the test string', ','), 'true', 'false')", "false");
    }

    @Test
    public void testContainsUsedInWhere() {
        // Note that in the tests below the data read in is irrelevant - the 'where contains' condition only uses
        // the parameters passed in.
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 1", "gor ../tests/data/gor/genes.gorz | top 1 | where contains('hjalti','a','j')");
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 1", "gor ../tests/data/gor/genes.gorz | top 1 | where contains('hjalti','a')");
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 0", "gor ../tests/data/gor/genes.gorz | where contains('hjalti','a','j','k')");
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 0", "gor ../tests/data/gor/genes.gorz | where contains('hjalti','k')");
    }

    @Test
    public void testCsContains() {
        TestUtils.assertCalculated("if(cscontains('the test string', 't'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(cscontains('the test string', 'the', 'test'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(cscontains('the test string', 'T'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(cscontains('the test string', 'x'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(cscontains('the test string', ','), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(cscontains('the test, string', ','), 'true', 'false')", "true");
    }

    @Test
    public void testCsContainsUsedInWhere() {
        // Note that in the tests below the data read in is irrelevant - the 'where cscontains' condition only uses
        // the parameters passed in.
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 1", "gor ../tests/data/gor/genes.gorz | top 1 | where cscontains('hjalti','a','j')");
        TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/genes.gorz | top 0", "gor ../tests/data/gor/genes.gorz | where cscontains('hjalti','A')");
    }

    @Test
    public void testContainsAll() {
        TestUtils.assertCalculated("if(containsall('the TEST string', 't'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(containsall('the TEST string', 'x'), 'true', 'false')", "false");

        TestUtils.assertCalculated("if(containsall('the TEST string', 't', 'h', 's'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(containsall('the TEST string', 't', 'h', 's', 'x'), 'true', 'false')", "false");
    }

    @Test
    public void testCsContainsAll() {
        TestUtils.assertCalculated("if(cscontainsall('the SPECIAL string', 'c'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(cscontainsall('the SPECIAL string', 'C'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(cscontainsall('the SPECIAL string', 'x'), 'true', 'false')", "false");

        TestUtils.assertCalculated("if(cscontainsall('the SPECIAL string', 't', 'h', 's'), 'true', 'false')", "true");
        TestUtils.assertCalculated("if(cscontainsall('the SPECIAL string', 'T', 'h', 's'), 'true', 'false')", "false");
        TestUtils.assertCalculated("if(cscontainsall('the SPECIAL string', 't', 'h', 's', 'x'), 'true', 'false')", "false");
    }

    @Test
    public void testContainsAny() {
        TestUtils.assertCalculated("if(containsany('abcdefg', 'x'), 'true', 'false')", "false" );
        TestUtils.assertCalculated("if(containsany('abcdefg', 'a'), 'true', 'false')", "true" );

        TestUtils.assertCalculated("if(containsany('abcdefg', 'a', 'x', 'y'), 'true', 'false')", "true" );
        TestUtils.assertCalculated("if(containsany('abcdefg', 'A', 'X', 'Y'), 'true', 'false')", "true" );
    }

    @Test
    public void testCsContainsAny() {
        // Need to work with rows here so we can do containscount without using string literals

        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a' | where cscontainsany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\ta,b,c,d,e,f\ta\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'x' | where cscontainsany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,b,c' | where cscontainsany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\ta,b,c,d,e,f\ta,b,c\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,B,c' | where cscontainsany(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\nchr1\t1\t1\ta,b,c,d,e,f\ta,B,c\n", result);
    }

    @Test
    public void testCsContainsAnyWithStringLiterals() {
        TestUtils.assertCalculated("if(cscontainsany('abcdefg', 'x'), 'true', 'false')", "false" );
        TestUtils.assertCalculated("if(cscontainsany('abcdefg', 'a'), 'true', 'false')", "true" );

        TestUtils.assertCalculated("if(cscontainsany('abcdefg', 'a', 'x', 'y'), 'true', 'false')", "true" );
        TestUtils.assertCalculated("if(cscontainsany('abcdefg', 'A', 'X', 'Y'), 'true', 'false')", "false" );
    }

    @Test
    public void testContainsCount() {
        // Need to work with rows here so we can do containscount without using string literals

        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a' | calc result containscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta\t1\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'x' | calc result containscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\tx\t0\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,b,c' | calc result containscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,b,c\t3\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,B,c' | calc result containscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,B,c\t3\n", result);
    }

    @Test
    public void testContainsCountWithStringLiterals() {
        TestUtils.assertCalculated("containscount('abcdef', 'a')", 1);
        TestUtils.assertCalculated("containscount('abcdef', 'x')", 0);
        TestUtils.assertCalculated("containscount('abcdefabcab', 'a','b','c')", 3);
        TestUtils.assertCalculated("containscount('ABCDEFABCAB', 'a','b','c')", 3);
    }

    @Test
    public void testCsContainsCount() {
        // Need to work with rows here so we can do containscount without using string literals

        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a' | calc result cscontainscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta\t1\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'x' | calc result cscontainscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\tx\t0\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,b,c' | calc result cscontainscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,b,c\t3\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,B,c' | calc result cscontainscount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,B,c\t2\n", result);
    }

    @Test
    public void testCsContainsCountWithStringLiterals() {
        TestUtils.assertCalculated("cscontainscount('abcdef', 'a')", 1);
        TestUtils.assertCalculated("cscontainscount('abcdef', 'x')", 0);
        TestUtils.assertCalculated("cscontainscount('abcdefabcab', 'a','b','c')", 3);
        TestUtils.assertCalculated("cscontainscount('ABCDEFABCAB', 'a','b','c')", 0);
    }

    @Test
    public void testListHasCount() {
        // Need to work with rows here so we can do listhascount without using string literals

        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a' | calc result listhascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta\t1\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'x' | calc result listhascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\tx\t0\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,b,c' | calc result listhascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,b,c\t3\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,B,c' | calc result listhascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,B,c\t3\n", result);
    }

    @Test
    public void testListHasCountWithStringLiterals() {
        TestUtils.assertCalculated("listhascount('a,b,c,d,e,f', 'a')", 1);
        TestUtils.assertCalculated("listhascount('a,b,c,d,e,f', 'x')", 0);
        TestUtils.assertCalculated("listhascount('a,b,c,d,e,f,a,b,c,a,b', 'a','b','c')", 3);
        TestUtils.assertCalculated("listhascount('A,B,C,D,E,F,A,B,C,A,B', 'a','b','c')", 3);
    }

    @Test
    public void testMatchLenWithStringLiterals() {
        TestUtils.assertCalculated("matchlen('ACCTTG', 'A')", 1);
        TestUtils.assertCalculated("matchlen('ACCTTG', 'ACCTA')", 4);
        TestUtils.assertCalculated("matchlen('ACCTTG', 'ACCCTA')", 3);
        TestUtils.assertCalculated("matchlen('ACCTTG', 'CCTTG')", 0);
        TestUtils.assertCalculated("matchlen('A', 'ACCTTG')", 1);
        TestUtils.assertCalculated("matchlen('ACCTA', 'ACCTTG')", 4);
        TestUtils.assertCalculated("matchlen('ACCCTA', 'ACCTTG')", 3);
        TestUtils.assertCalculated("matchlen('ACCCTA', 'ACCCTA')", 6);
        TestUtils.assertCalculated("matchlen('xACCCTA', 'yACCCTA')", 0);
        TestUtils.assertCalculated("matchlen('CCTTG', 'ACCTTG')", 0);
        TestUtils.assertCalculated("matchlen('ACCTTG', '')", 0);
        TestUtils.assertCalculated("matchlen('', 'A')", 0);
        TestUtils.assertCalculated("matchlen('', '')", 0);
    }

    @Test
    public void testCsListHasCount() {
        // Need to work with rows here so we can do listhascount without using string literals

        String result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a' | calc result cslisthascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta\t1\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'x' | calc result cslisthascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\tx\t0\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,b,c' | calc result cslisthascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,b,c\t3\n", result);

        result = TestUtils.runGorPipe("gorrow chr1,1,1 | calc x 'a,b,c,d,e,f' | calc y 'a,B,c' | calc result cslisthascount(x, y)");
        Assert.assertEquals("chrom\tbpStart\tbpStop\tx\ty\tresult\nchr1\t1\t1\ta,b,c,d,e,f\ta,B,c\t2\n", result);
    }
}
