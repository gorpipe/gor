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

package gorsat.analysis;

import gorsat.Analysis.BucketSplitAnalysis;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Test;

import java.util.ArrayList;

public class UTestBucketSplit {

    private static final String[] INPUT_DATA_BASE = {
            "chr1\t1\t${test}\tfoo\tbar",
            "chr1\t10\t${test}\tfoo\tbar",
            "chr2\t100\t${test}\tfoo\tbar"};

    @Test
    public void testDefaultSplitWithComma() {
        performTest("a,b,c", "a,b,c", ",", 1, -1, "", true);
    }

    @Test(expected = GorDataException.class)
    public void testDefaultSplitWithCommaEmptyInputShouldBeAnError() {
        performTest("", "a,b,c", ",", 1, -1, "", true);
    }

    @Test
    public void testDefaultSplitWithCommaEmptyIngnoreError() {
        performTest("", "", ",", 1, -1, "", false);
    }

    @Test
    public void testDefaultSplitWithSemiColon() {
        performTest("a;b;c", "a,b,c", ";", 1, -1, "", true);
    }

    @Test
    public void testDefaultSplitWithCommaBucketSize2() {
        performTest("a,b,c", "a,b,c", ",", 2, -1, "", true);
    }

    @Test
    public void testFixedSplitSizeOf1ANDBucketSize2() {
        performTest("a,b,c,d,e", "a,b,c,d,e", "", 2, 1,"", true);
    }

    @Test
    public void testFixedSplitSizeOf2ANDBucketSize2() {
        performTest("a,b,c,d,e,f", "ab,cd,ef", "", 2, 2,"", true);
    }

    @Test
    public void testFixedSplitSizeOf2ANDBucketSize100() {
        performTest("a,b,c,d,e,f", "ab,cd,ef", "", 100, 2,"", true);
    }

    @Test(expected = GorDataException.class)
    public void testFixedSplitSizeOf2WithUnmatchedInputWhichCausesAnError() {
        performTest("a,b,c,d,e,", "ab,cd,ef", "", 100, 2,"", true);
    }

    @Test(expected = GorDataException.class)
    public void testFixedSplitSizeOf2WithUnmatchedInputIgnoreError() {
        performTest("a,b,c,d,e,", "ab,cd,ef", "", 100, 2,"", false);
    }

    @Test
    public void testDefaultSplitWithCommaAndPrefix() {
        performTest("a,b,c", "a,b,c", ",", 1, -1, "A", true);
    }

    @Test
    public void testFixedSplitWithCommaAndPrefix() {
        performTest("a,b,c,d,e,f,g,h,i", "abc,def,ghi", "", 1, 3, "Bucket", true);
    }

    private void performTest(String inputList, String testList, String seperator, int bucketSize, int valueSize, String prefix, boolean doValidation) {
        BucketSplitAnalysis analysis = new BucketSplitAnalysis(2, bucketSize, seperator, valueSize, prefix, doValidation);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, generateTestData(inputList.replace(",", seperator)), generateResult(testList, bucketSize, seperator, prefix));
    }

    private static String[] generateTestData(String values) {
        ArrayList<String> results = new ArrayList<>(INPUT_DATA_BASE.length);

        for(String dataLine : INPUT_DATA_BASE) {
            results.add(dataLine.replace("${test}", values));
        }

        return results.toArray(new String[0]);
    }

    private static String[] generateResult(String values, int bucketSize, String seperator, String bucketPrefix) {
        ArrayList<String> results = new ArrayList<>(INPUT_DATA_BASE.length);
        String[] entries = values.split(",");

        for(String pre : INPUT_DATA_BASE) {
            int bucketNumber = 1;
            int counter = 0;
            StringBuilder result = new StringBuilder();
            for (String entry : entries) {
                counter++;
                result.append(entry).append(seperator);

                if (counter == bucketSize) {
                    results.add(getResultString(seperator, bucketPrefix, pre, bucketNumber, result));
                    result = new StringBuilder();
                    counter = 0;
                    bucketNumber++;
                }
            }

            if (counter > 0) {
                results.add(getResultString(seperator, bucketPrefix, pre, bucketNumber, result));
            }
        }

        return results.toArray(new String[0]);
    }

    private static String getResultString(String seperator, String bucketPrefix, String pre, int bucketNumber, StringBuilder result) {
        return pre.replace("${test}", result.substring(0, result.length() - seperator.length())) + "\t" + bucketPrefix + bucketNumber;
    }
}
