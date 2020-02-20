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

import com.google.common.collect.Iterables;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


public class UTestAtMinMax {

    String testFileWithGroupsOfValuesPath;
    String testFileWithGroupsNonOrderedOfValuesPath;
    String testFilePath;

    @Before
    public void SetUp() {
        try {
            testFileWithGroupsOfValuesPath = CreateAtTestFileWithGroups();
            testFileWithGroupsNonOrderedOfValuesPath = CreateAtTestFileWithGroupsNonOrdered();
            testFilePath = CreateAtTestFile();
        }
        catch (Exception ex) {

        }
    }

    @Test
    public void testAtMax() {
        String[] res = GorRows(testFilePath, "atmax", "1000", "pos", "", 5);
        double[] correctValues = {521, 1902, 22500, 32106};
        CompareValues(res, 1, correctValues, "ATMAX with multiple entries per bin");
    }

    @Test
    public void testAtMin() {
        String[] res = GorRows(testFilePath, "atmin", "1000", "pos", "", 5);
        double[] correctValues = {500, 1300, 22500, 32100};
        CompareValues(res, 1, correctValues, "ATMIN with multiple entries per bin");
    }

    @Test
    public void testAtMaxPerChromosome() {
        String[] res = GorRows(testFilePath, "atmax", "chr", "pos", "", 3);
        double[] correctValues = {22500, 32106};

        CompareValues(res, 1, correctValues, "ATMAX with multiple entries per chromosome");
    }

    @Test
    public void testAtMinPerChromosome() {
        String[] res = GorRows(testFilePath, "atmin", "chr", "pos", "", 3);
        double[] correctValues = {500, 32100};
        CompareValues(res, 1, correctValues, "ATMIN with multiple entries per chromosome");
    }

    @Test
    public void testAtMaxFirstInGroup() {
        String[] res = GorRows(testFileWithGroupsOfValuesPath, "atmax", "1000", "pos", "", 5);
        double[] correctValues = {1.0, 1.0, 1.0, 1.0};
        CompareValues(res, 2, correctValues, "ATMAX with multiple rows with same position, picking first");
    }

    @Test
    public void testAtMaxLastInGroup() {
        String[] res = GorRows(testFileWithGroupsOfValuesPath, "atmax", "1000", "pos", "-last", 5);
        double[] correctValues = {2.0, 2.0, 1.0, 7.0};
        CompareValues(res, 2, correctValues, "ATMAX with multiple rows with same position, picking last");
    }

    @Test
    public void testAtMinFirstInGroup() {
        String[] res = GorRows(testFileWithGroupsOfValuesPath, "atmin", "1000", "pos", "", 5);
        double[] correctValues = {1.0, 1.0, 1.0, 1.0};
        CompareValues(res, 2, correctValues, "ATMIN with multiple rows with same position, picking first");
    }

    @Test
    public void testAtMinLastInGroup() {
        String[] res = GorRows(testFileWithGroupsOfValuesPath, "atmin", "1000", "pos", "-last", 5);
        double[] correctValues = {2.0, 2.0, 1.0, 7.0};
        CompareValues(res, 2, correctValues, "ATMIN with multiple rows with same position, picking last");
    }

    @Test
    public void testAtWithGrouping() {
        String[] atResults = GorRows(testFileWithGroupsOfValuesPath, "atmin", "chr", "value", "-gc #4", 4);
        // Compare with the rank statement
        String[] rankResults = TestUtils.runGorPipeLines("gor " + testFileWithGroupsOfValuesPath + " | rank chr value -gc #4 -o asc | where rank_value = 1 | hide rank_value | rownum | granno chrom -gc #4 -min -ic rownum | where rownum = min_rownum | hide min_rownum,rownum" );

        for (int i = 1; i < rankResults.length; i++ ) {
            Assert.assertEquals("Comparison of at vs rank", atResults[i], rankResults[i]);
        }
    }

    @Test
    public void testAtWithGroupingNonOrderedAndVerifyOrder() {
        // This test used to fail verify order step
        GorRows(testFileWithGroupsNonOrderedOfValuesPath, "atmax", "chr", "value", "-gc #4", 3);
    }

    @Test
    public void testAtWithNorContext() {
        String query = "nor -h " + testFilePath + " | atmin value";
        String[] res = TestUtils.runGorPipeLines(query);
        double[] correctValues = {1.0};
        CompareValues(res, 4, correctValues, "ATMIN testing nor context");
    }

    @Test
    public void testAtWithNorContextWithFailure() {
        String query = "nor -h " + testFilePath + " | atmin 1000 value";
        try {
            String[] res = TestUtils.runGorPipeLines(query);
        } catch (GorParsingException ex) {
            Assert.assertTrue("Should get parsing exception", ex.getMessage().contains("Cannot have binSize"));
        }
    }

    private String[] GorRows(String testFile, String command, String binSize, String columnName, String options, int expectedNumberOfRows) {
        String query = String.format("gor %s | %s %s %s %s | verifyorder", testFile, command, binSize, columnName, options);
        String[] res = TestUtils.runGorPipeLines(query);
        Assert.assertEquals("Number of rows from At command", expectedNumberOfRows, res.length);

        return res;
    }

    private void CompareValues(String[] res, int columnNumber, double[] correctValues, String message) {
        int count = 0;

        for (String line : Iterables.skip(Arrays.asList(res), 1)) {
            String[] columns = line.split("\t");
            double value = Double.parseDouble(columns[columnNumber]);
            Assert.assertEquals(message, correctValues[count], value, 0.01);
            count++;
        }
    }

    private static String CreateAtTestFileWithGroups() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("valueswithgroups", ".gor");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tpos\tvalue\tdate");
        outputWriter.println("chr1\t500\t1.0\t2010-10-3");
        outputWriter.println("chr1\t500\t2.0\t2010-10-3");
        outputWriter.println("chr1\t1300\t1.0\t2010-10-3");
        outputWriter.println("chr1\t1300\t2.0\t2010-10-3");
        outputWriter.println("chr1\t22500\t1.0\t2010-10-4");
        outputWriter.println("chr2\t32100\t1.0\t2010-10-4");
        outputWriter.println("chr2\t32100\t2.0\t2010-10-4");
        outputWriter.println("chr2\t32100\t3.0\t2010-10-4");
        outputWriter.println("chr2\t32100\t4.0\t2010-10-4");
        outputWriter.println("chr2\t32100\t5.0\t2010-10-4");
        outputWriter.println("chr2\t32100\t6.0\t2010-10-4");
        outputWriter.println("chr2\t32100\t7.0\t2010-10-4");
        outputWriter.close();

        return patientsPath.toString();

    }

    private static String CreateAtTestFileWithGroupsNonOrdered() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("valueswithgroupsnonordered", ".gor");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tpos\tvalue\tdate");
        outputWriter.println("chr1\t500\t1.0\t2010-10-3");
        outputWriter.println("chr1\t500\t2.0\t2010-10-4");
        outputWriter.println("chr1\t1300\t1.0\t2010-10-4");
        outputWriter.println("chr1\t1300\t2.0\t2010-10-4");
        outputWriter.println("chr1\t22500\t4.0\t2010-10-3");

        outputWriter.close();

        return patientsPath.toString();

    }

    private static String CreateAtTestFile() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("valuesnogroups", ".gor");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tpos\tvalue\tdate");
        outputWriter.println("chr1\t500\t1.0\t2010-10-3");
        outputWriter.println("chr1\t521\t2.0\t2010-10-4");
        outputWriter.println("chr1\t1300\t1.0\t2010-10-5");
        outputWriter.println("chr1\t1902\t2.0\t2010-10-6");
        outputWriter.println("chr1\t22500\t1.0\t2010-10-7");
        outputWriter.println("chr2\t32100\t1.0\t2010-10-8");
        outputWriter.println("chr2\t32101\t2.0\t2010-10-9");
        outputWriter.println("chr2\t32102\t3.0\t2010-10-10");
        outputWriter.println("chr2\t32103\t4.0\t2010-10-11");
        outputWriter.println("chr2\t32104\t5.0\t2010-10-12");
        outputWriter.println("chr2\t32105\t6.0\t2010-10-13");
        outputWriter.println("chr2\t32106\t7.0\t2010-10-14");
        outputWriter.close();

        return patientsPath.toString();

    }

}
