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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

public class UTestGorrows {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testPositionOption() {
        String[] result = TestUtils.runGorPipeLines("gorrows -p chr1:0-10");

        Assert.assertEquals(11, result.length);
        Assert.assertEquals(Arrays.asList("chrom", "pos"), getRowValues(result[0]));
        Assert.assertEquals(Arrays.asList("chr1", "0"), getRowValues(result[1]));
        Assert.assertEquals(Arrays.asList("chr1", "9"), getRowValues(result[10]));
    }

    @Test
    public void testCreateVirtualSource() {
        String[] results = TestUtils.runGorPipeLinesNoHeader("create xxx = gorrows -p chr1:0-10 | signature -timeres 1; gor [xxx]");

        Assert.assertEquals(10, results.length);
    }

    @Test
    public void testPositionOptionStartAndStopValues() {
        thrown.expectMessage("Error stop_position is required"); /* start_position defaults to 0 in CommandParseUtilities */
        thrown.expect(GorParsingException.class);

        TestUtils.runGorPipeLines("gorrows -p chr1");
    }

    @Test
    public void testPositionStopValueMissing() {
        thrown.expectMessage("Error stop_position is required");
        thrown.expect(GorParsingException.class);

        TestUtils.runGorPipeLines("gorrows -p chr1:1-");
    }

    @Test()
    public void testPositionOption_whenMissingPositionOption() {
        thrown.expectMessage("Position option");
        thrown.expect(GorParsingException.class);

        TestUtils.runGorPipeLines("gorrows");
    }

    @Test(expected = GorParsingException.class)
    public void testPositionOptionWithValuesMissing() {
        TestUtils.runGorPipeLines("gorrows -p");
    }

    @Test
    public void testPositionOptionWithTooManyValues_whenParsingValuesAreSplitOnColonAndHyphen() {
        thrown.expect(GorParsingException.class);
        thrown.expectMessage("Invalid range value 'chr1:-1-10'");

        TestUtils.runGorPipeLines("gorrows -p chr1:-1-10");
    }

    @Test
    public void testPositionOptionWithStopValueLessThanStartValue() {
        thrown.expect(GorParsingException.class);
        thrown.expectMessage("stop cannot be lower than 100");

        TestUtils.runGorPipeLines("gorrows -p chr1:100-10");
    }

    @Test
    public void testPositionOptionWithStopValueEqualToStartValue() {
        thrown.expect(GorParsingException.class);
        thrown.expectMessage("stop_position cannot be lower than 101");

        TestUtils.runGorPipeLines("gorrows -p chr1:100-100");
    }

    @Test
    public void testSegmentOption() {
        String[] result = TestUtils.runGorPipeLines("gorrows -p chr1:0-10 -segment 100");

        Assert.assertEquals(11, result.length);
        Assert.assertEquals(Arrays.asList("chrom", "bpStart", "bpStop"), getRowValues(result[0]));
        Assert.assertEquals(Arrays.asList("chr1", "0", "100"), getRowValues(result[1]));
        Assert.assertEquals(Arrays.asList("chr1", "9", "109"), getRowValues(result[10]));
    }

    @Test
    public void testSegmentOptionWithTooLowValue() {
        thrown.expect(GorParsingException.class);
        thrown.expectMessage("-segment cannot be lower than 1");

        TestUtils.runGorPipeLines("gorrows -p chr1:0-10 -segment 0");
    }

    @Test(expected = GorParsingException.class)
    public void testSegmentOptionWithValueMissing() {
        TestUtils.runGorPipeLines("gorrows -p chr1:0-10 -segment");
    }

    @Test
    public void testStepOption() {
        String[] result = TestUtils.runGorPipeLines("gorrows -p chr1:0-100 -step 10");

        Assert.assertEquals(11, result.length);
        Assert.assertEquals(Arrays.asList("chrom", "pos"), getRowValues(result[0]));
        Assert.assertEquals(Arrays.asList("chr1", "0"), getRowValues(result[1]));
        Assert.assertEquals(Arrays.asList("chr1", "10"), getRowValues(result[2]));
        Assert.assertEquals(Arrays.asList("chr1", "90"), getRowValues(result[10]));
    }

    @Test
    public void testStepOptionWithTooLowValue() {
        thrown.expect(GorParsingException.class);
        thrown.expectMessage("-step cannot be lower than 1");

        TestUtils.runGorPipeLines("gorrows -p chr1:0-10 -step 0");
    }

    @Test
    public void testWithPositionSegmentAndStepOptions() {
        String[] result = TestUtils.runGorPipeLines("gorrows -p chr1:0-100 -segment 100 -step 10");

        Assert.assertEquals(11, result.length);
        Assert.assertEquals(Arrays.asList("chrom", "bpStart", "bpStop"), getRowValues(result[0]));
        Assert.assertEquals(Arrays.asList("chr1", "0", "100"), getRowValues(result[1]));
        Assert.assertEquals(Arrays.asList("chr1", "90", "190"), getRowValues(result[10]));
    }

    private List<String> getRowValues(String row) {
        return Arrays.asList(row.trim().split("\t"));
    }
}
