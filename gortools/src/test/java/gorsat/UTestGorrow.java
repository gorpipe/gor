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
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UTestGorrow {

    private final String gorrowCommand = "gorrow";

    @Test
    public void testWithTwoParams() {
        String[] result = TestUtils.runGorPipeLines(gorrowCommand + " chr1,100");
        Assert.assertEquals(2, result.length);

        Assert.assertEquals(Arrays.asList("chrom", "pos"), getRowValues(result[0]));
        Assert.assertEquals(Arrays.asList("chr1", "100"), getRowValues(result[1]));
    }

    @Test
    public void testFirstParamDoesNotStartWithChr() {
        String[] result = TestUtils.runGorPipeLinesNoHeader(gorrowCommand + " test,100");

        Assert.assertEquals(1, result.length);
        Assert.assertEquals(Arrays.asList("chrtest", "100"), getRowValues(result[0]));
    }

    @Test(expected = GorParsingException.class)
    public void whenSecondParameterOfTwoIsNotInteger_thenThrowParsingException() {
        TestUtils.runGorPipeLines(gorrowCommand + " chr1,x");
    }

    @Test
    public void testWithThreeParams() {
        String[] result = TestUtils.runGorPipeLines(gorrowCommand + " chr1,1,100");
        Assert.assertEquals(2, result.length);

        Assert.assertEquals(Arrays.asList("chrom", "bpStart", "bpStop"), getRowValues(result[0]));
        Assert.assertEquals(Arrays.asList("chr1", "1", "100"), getRowValues(result[1]));
    }

    @Test(expected = GorParsingException.class)
    public void whenSecondParameterOfThreeIsNotInteger_thenThrowParsingException() {
        TestUtils.runGorPipeLines(gorrowCommand + " chr1,1,x");
    }

    @Test(expected = GorParsingException.class)
    public void whenThirdParameterOfThreeIsNotInteger_thenThrowParsingException() {
        TestUtils.runGorPipeLines(gorrowCommand + " chr1,x,1");
    }

    private List<String> getRowValues(String row) {
        return Arrays.asList(row.trim().split("\t"));
    }
}
