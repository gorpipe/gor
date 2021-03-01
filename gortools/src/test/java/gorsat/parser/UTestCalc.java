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
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class UTestCalc {
    @Test
    public void addsNewColumn() {
        final String result = TestUtils.runGorPipe("gorrow 1,1 | calc data 42");
        final String expected = "chrom\tpos\tdata\n" +
                "chr1\t1\t42\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void addsNewColumnWithSuffixWhenItExists() {
        final String result = TestUtils.runGorPipe("gorrow 1,1 | calc data 42 | calc data 3.14");
        final String expected = "chrom\tpos\tdata\tdatax\n" +
                "chr1\t1\t42\t3.14\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testNumberParsing1() {
        String query = "gor ../tests/data/gor/genes.gorz | calc m +11 | top 1 | calc n m+1.0";
        String[] res = TestUtils.runGorPipeLines(query);
        Assert.assertTrue(res[1].contains("12.0"));
    }

    @Test
    public void testNumberParsing2() {
        String query = "gor ../tests/data/gor/genes.gorz | top 1 | calc m +11 | calc n m+1e+0";
        String[] res = TestUtils.runGorPipeLines(query);
        Assert.assertTrue(res[1].contains("12.0"));
    }

    @Test
    public void testNumberParsing3() {
        String query = "gor ../tests/data/gor/genes.gorz | top 1 | calc m +11 | calc n m+1e-1";
        String[] res = TestUtils.runGorPipeLines(query);
        Assert.assertTrue(res[1].contains("11.1"));

    }

    @Test
    public void testNumberParsing4() {
        String query = "gor ../tests/data/gor/genes.gorz | top 1 | calc m 1e1 | calc n m+1e-10";
        String[] res = TestUtils.runGorPipeLines(query);
        Assert.assertTrue(res[1].contains("10.0000000001"));

    }

    @Test
    public void testNumberParsing5() {
        String query = "gor ../tests/data/gor/genes.gorz | top 1 | calc m 1e+2 | calc n m+1";
        String[] res = TestUtils.runGorPipeLines(query);
        Assert.assertTrue(res[1].contains("101.0"));
    }

    @Test
    public void testNumberParsing6() {
        String query = "gor ../tests/data/gor/genes.gorz | top 1 | calc m 1e-2 | calc n m-1";
        String[] res = TestUtils.runGorPipeLines(query);
        Assert.assertTrue(res[1].contains("-0.99"));
    }

    @Test
    public void testOnceString() {
        String query = "gor ../tests/data/gor/genes.gorz | top 2 | rownum | calc a once(upper(rownum))";
        String[] lines = TestUtils.runGorPipeLines(query);
        String[] l1 = lines[1].split("\t");
        String[] l2 = lines[2].split("\t");
        Assert.assertEquals("Different value in column", l1[5], l2[5]);
    }

    @Test
    public void testOnceTwice() {
        String query = "gor ../tests/data/gor/genes.gorz | top 2 | rownum | calc a int(once(upper(rownum))) | calc b once(a+10)";
        String[] lines = TestUtils.runGorPipeLines(query);
        String[] l1 = lines[1].split("\t");
        String[] l2 = lines[2].split("\t");
        Assert.assertEquals("Different value in column", l1[5], l2[5]);
        Assert.assertEquals("Different value in column", l1[6], l2[6]);
    }

    @Test
    public void testOnceInt() {
        String query = "gor ../tests/data/gor/genes.gorz | top 2 | rownum | calc a once(int(rownum))";
        String[] lines = TestUtils.runGorPipeLines(query);
        String[] l1 = lines[1].split("\t");
        String[] l2 = lines[2].split("\t");
        Assert.assertEquals("Different value in column", l1[5], l2[5]);
    }

    @Test
    public void testOnceDouble() {
        String query = "gor ../tests/data/gor/genes.gorz | top 2 | rownum | calc a once(log(rownum+1))";
        String[] lines = TestUtils.runGorPipeLines(query);
        String[] l1 = lines[1].split("\t");
        String[] l2 = lines[2].split("\t");
        Assert.assertEquals("Different value in column", l1[5], l2[5]);
    }

    @Test
    public void testOnceEval() {
        String query = "gor ../tests/data/gor/genes.gorz | top 2 | rownum | calc a once(eval('nor ../tests/data/gor/genes.gorz | top 2 | calc r random()'))";
        String[] lines = TestUtils.runGorPipeLines(query);
        String[] l1 = lines[1].split("\t");
        String[] l2 = lines[2].split("\t");
        Assert.assertEquals("Different value in column", l1[5], l2[5]);
    }

    @Test
    public void testCalcConditionalLogicLong() {
        Long[] l = {111111234567000L, 111111234568000L};
        String actualConditions = validateAllLogicalOperators(l);
        Assert.assertEquals("Logical operator in the IF() condition is out of bounds for long: ", "[1, 1, 1, 1, 2, 2, 2]", actualConditions);
    }

    @Test
    public void testCalcConditionalLogicDouble() {
        Double[] d = {1.0D, 2.0D};
        String actualConditions = validateAllLogicalOperators(d);
        Assert.assertEquals("Logical operator in the IF() condition is out of bounds for double: ", "[1, 1, 1, 1, 2, 2, 2]", actualConditions);
    }

    @Test
    public void testCalcConditionalLogicInt() {
        Integer[] n = {2147483500, 2147483600};
        String actualConditions = validateAllLogicalOperators(n);
        Assert.assertEquals("Logical operator in the IF() condition is out of bounds for int: ", "[1, 1, 1, 1, 2, 2, 2]", actualConditions);
    }

    @Test
    public void testCalcConditionalLogicFloat() {
        Float[] f = {100.123F, 200.123F};
        String actualConditions = validateAllLogicalOperators(f);
        Assert.assertEquals("Logical operator in the IF() condition is out of bounds for float: ", "[1, 1, 1, 1, 2, 2, 2]", actualConditions);
    }

    /**
     * This method takes an array of two elements and checks every logical operation in the boolean condition of the calc IF() function.
     *
     * @param arr
     */
    private <T> String validateAllLogicalOperators(T[] arr) {
        String[] logicalOps = {">", ">=", "=", "==", "<", "<=", "!="};
        int[] checkArray = {};
        for (int i = 0; i < logicalOps.length; i++) {
            String query = "gor ../tests/data/gor/genes.gorz | calc xxx IF(" + arr[0] + logicalOps[i] + arr[1] + ",1,0) | where (xxx == 1) | top 1";
            String[] lines = TestUtils.runGorPipeLines(query);
            int[] newArray = new int[checkArray.length + 1];
            System.arraycopy(checkArray, 0, newArray, 0, checkArray.length);
            newArray[newArray.length - 1] = lines.length;
            checkArray = newArray;
        }
        return Arrays.toString(checkArray);
    }
}
