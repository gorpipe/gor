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
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

public class UTestGroup {

    String testFileWithFloatValuesPath;
    String testFileWithIntegerValuesPath;
    String[] functions = new String[]{"min", "med", "max", "avg", "std", "sum"};
    File gorFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        try {
            testFileWithFloatValuesPath = createAtTestFileWithFloatingNumbers();
            testFileWithIntegerValuesPath = createAtTestFileWithIntegerNumbers();

            gorFile = FileTestUtils.createTempFile(workDir.getRoot(), "go_dag.txt",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tdistance\tPOS\tReference\tCall\tlis_CallCopies\tnumMarkers\n" +
                            "chr1\t14362\t29806\tWASH7P\t0\t16856\tA\tG\tNA,NA,NA,NA\t4337\n" +
                            "chr1\t69090\t70008\tOR4F5\t0\t69849\tG\tA\t0,NA,0,0,NA\t4337\n" +
                            "chr1\t134900\t139379\tAL627309.1\t0\t135804\tG\tA\t0,1,1,1,0\t4337\n"
            );
        } catch (Exception ex) {
            // Ignored
        }
    }

    @Test
    public void testGroupCount() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | group chrom -count | top 3");
        int[] numberOfGenes = {4747, 2011, 2982};
        int count = 0;

        for (String line : Iterables.skip(Arrays.asList(lines), 1)) {
            String[] columns = line.split("\t", -1);

            Assert.assertEquals("Number og rows from group count", 4, columns.length);
            Assert.assertEquals("Number of genes in group", numberOfGenes[count++], Integer.parseInt(columns[3].trim()));
        }
    }

    @Test
    @Ignore("Need correct build for this to work")
    public void testGroupNestedQueryCreate() {
        String query = "create xxx = pgor <(gor ../tests/data/gor/genes.gor | group chrom -count) | signature -timeres 1;" +
                "gor [xxx] | group genome -sum -ic allCount | rename sum_allCount allCount | merge <(gor ../tests/data/gor/genes.gor | group genome -count) | group 1 -gc 3- -count | throwif allCount = 1";
        String[] lines = TestUtils.runGorPipeLines(query);
        Assert.assertEquals(2, lines.length);
    }

    @Test
    public void testGroupNestedQuery() {
        String[] lines = TestUtils.runGorPipeLines("gor -p chr22:10000000-20000000 <(gor ../tests/data/gor/genes.gor | group chrom -count)");
        Assert.assertEquals(2, lines.length);
    }

    @Test
    public void testGroupSteps() {
        String query = "gor <(norrows 1000000 | calc chrom 'chr1' | calc pos #1+1 | select chrom,pos) | select chrom,pos | group 100 -steps 5 -count | where #3-#2 < 100";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong result from group with steps","Chrom\tbpStart\tbpStop\tallCount\n",res);
    }

    @Test
    public void testGroupCDist() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/genes.gor | group chrom -cdist | top 3");
        int[] numberOfGenes = {4747, 2011, 2982};
        int count = 0;

        for (String line : Iterables.skip(Arrays.asList(lines), 1)) {
            String[] columns = line.split("\t", -1);

            Assert.assertEquals("Number of rows from group count", 4, columns.length);
            Assert.assertEquals("Number of genes in group", numberOfGenes[count++], Integer.parseInt(columns[3].trim()));
        }
    }

    @Test
    public void testGroupColumnGroups() {
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | group genome -gc allele -count");

        HashMap<String, Integer> alleleCount = new HashMap<>();
        alleleCount.put("A", 12);
        alleleCount.put("C", 9);
        alleleCount.put("CAA", 1);
        alleleCount.put("CC", 2);
        alleleCount.put("G", 14);
        alleleCount.put("T", 10);

        for (String line : Iterables.skip(Arrays.asList(lines), 1)) {
            String[] columns = line.split("\t", -1);

            Assert.assertEquals("Number og rows from group count", 5, columns.length);
            Assert.assertEquals("Number of genes in group", (int) alleleCount.get(columns[3]), Integer.parseInt(columns[4].trim()));
        }
    }

    @Test
    public void testGroupStringColumnGroups() {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | group genome -sc allele -min -max -std -avg -sum -med");

        Assert.assertEquals("Number of lines from the string column query", 2, lines.length);

        String column = lines[0].toLowerCase();
        assertContainedInColumn(column, "min", true);
        assertContainedInColumn(column, "max", true);
        assertContainedInColumn(column, "std", false);
        assertContainedInColumn(column, "avg", false);
        assertContainedInColumn(column, "sum", false);
        assertContainedInColumn(column, "med", true);
    }

    @Test
    public void testGroupIntegerColumnGroups() {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | ROWNUM | calc a mod(rownum,10) | group genome -gc allele -ic a -min -max -std -avg -sum -med");
        testNumberColumns(lines);
    }

    @Test
    public void testGroupFloatingPointColumnGroups() {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | ROWNUM | calc a mod(rownum,10) | group genome -gc allele -fc a,rownum -min -max -std -avg -sum -med");
        testNumberColumns(lines);
    }

    @Test
    public void testGroupSetAttribute() {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | where len(reference) = 1 | group genome -sc reference -set");

        Assert.assertEquals("Number of lines from the string column query", 2, lines.length);
        Assert.assertTrue("Group -set result contain A,C,G,T", lines[1].contains("A,C,G,T"));
    }

    @Test
    public void testGroupSetAttributeWithCustomSeparator() {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        String[] lines = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gor | where len(reference) = 1 | group genome -sc reference -set -s ';'");

        Assert.assertEquals("Number of lines from the string column query", 2, lines.length);
        Assert.assertTrue("Group -set result contain A;C;G;T", lines[1].contains("A;C;G;T"));
    }

    @Test(expected = GorParsingException.class)
    public void testGroupWithOverlappingGroupingAndStringColumnAggregationResultingInError() throws IOException {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        TestUtils.runGorPipeLines("gor " + gorFile.getCanonicalPath() + " | top 3 | where len(reference) = 1 | group genome -gc reference -sc 5-7,10 -sum");
        Assert.fail("Grouping cannot contain columns for aggregation.");
    }

    @Test(expected = GorParsingException.class)
    public void testGroupWithOverlappingGroupingAndIntegerColumnAggregationResultingInError() throws IOException {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        TestUtils.runGorPipeLines("gor " + gorFile.getCanonicalPath() + " | top 3 | where len(reference) = 1 | group genome -gc reference -ic 5-7,10 -sum");
        Assert.fail("Grouping cannot contain columns for aggregation.");
    }

    @Test(expected = GorParsingException.class)
    public void testGroupWithOverlappingGroupingAndFloatingColumnAggregationResultingInError() throws IOException {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        TestUtils.runGorPipeLines("gor " + gorFile.getCanonicalPath() + " | top 3 | where len(reference) = 1 | group genome -gc reference -fc 5-7,10 -sum");
        Assert.fail("Grouping cannot contain columns for aggregation.");
    }

    @Test
    public void testGroupSetAttributeWithStringColumnRange() throws IOException {
        // Calculates min and max for the alleles but not the std as this is defines as s string column
        String[] lines = TestUtils.runGorPipeLines("gor " + gorFile.getCanonicalPath() + " | top 3 | where len(reference) = 1 | group genome -gc reference -sc 5-6,10 -set");

        String column = lines[0].toLowerCase();
        assertContainedInColumn(column, "set_distance", true);
        assertContainedInColumn(column, "set_pos", true);
        assertContainedInColumn(column, "set_reference", false); // This column is remove as it is a part of the -gc option
        assertContainedInColumn(column, "set_call", false);
        assertContainedInColumn(column, "set_lis_callcopies", false);
        assertContainedInColumn(column, "set_nummarkers", true);
    }

    @Test
    public void testGroupFloatCalculations() {
        String[] lines = TestUtils.runGorPipeLines("gor " + testFileWithFloatValuesPath + " | group chrom -gc chr -fc value -max -min -avg -med -sum -std");
        assertValueArray(lines[1], 4, new double[]{1.0, 5.5, 10.0, 5.5, 3.2, 22.0}, functions);
        assertValueArray(lines[2], 4, new double[]{3.0, 5.0, 7.0, 5.0, 1.63, 15.0}, functions);
    }

    @Test
    public void testGroupIntegerCalculations() {
        String[] lines = TestUtils.runGorPipeLines("gor " + testFileWithIntegerValuesPath + " | group chrom -gc chr -fc value -max -min -avg -med -sum -std");
        assertValueArray(lines[1], 4, new double[]{1.0, 5.5, 10.0, 5.5, 3.2, 22.0}, functions);
        assertValueArray(lines[2], 4, new double[]{3.0, 5.0, 7.0, 5.0, 1.6, 15.0}, functions);
    }

    @Test
    public void testGroupInNorContext() {
        String[] lines = TestUtils.runGorPipeLines("nor -h " + testFileWithIntegerValuesPath + " | group -fc value -max -min");
        assertValueArray(lines[1], 2, new double[]{1.0, 10.0}, functions);
    }

    @Test
    public void testGroupInNorContextWithError() {
        try {
            TestUtils.runGorPipeLines("nor -h " + testFileWithIntegerValuesPath + " | group 1000 -fc value -max -min");
        } catch (GorParsingException ex) {
            Assert.assertTrue("Should get parsing exception", ex.getMessage().contains("Cannot have binSize"));
        }
    }

    @Test
    public void set() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbongo2\n" +
                "chr1\t1\tbongo3\n" +
                "chr1\t1\tbongo4\n" +
                "chr1\t1\tbongo5\n" +
                "chr1\t1\tbongo6\n" +
                "chr1\t1\tbingo2\n" +
                "chr1\t1\tbingo3\n" +
                "chr1\t1\tbingo4\n" +
                "chr1\t1\tbingo5\n" +
                "chr1\t1\tbingo6\n";

        String expected = "Chrom\tbpStart\tbpStop\tset_Data\n" +
                "chr1\t0\t250000000\tbingo1,bingo2,bingo3,bingo4,bingo5,bingo6,bongo1,bongo2,bongo3,bongo4,bongo5,bongo6\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | group chrom -sc Data -set", file.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void setWithInts() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\t12341\n" +
                "chr1\t1\t43211\n" +
                "chr1\t1\t12341\n" +
                "chr1\t1\t43211\n" +
                "chr1\t1\t43212\n" +
                "chr1\t1\t43213\n" +
                "chr1\t1\t43214\n" +
                "chr1\t1\t43215\n" +
                "chr1\t1\t43216\n" +
                "chr1\t1\t12342\n" +
                "chr1\t1\t12343\n" +
                "chr1\t1\t12344\n" +
                "chr1\t1\t12345\n" +
                "chr1\t1\t12346\n";

        String expected = "Chrom\tbpStart\tbpStop\tset_Data\n" +
                "chr1\t0\t250000000\t12341,12342,12343,12344,12345,12346,43211,43212,43213,43214,43215,43216\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | group chrom -ic Data -set", file.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void setIsTruncated() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbongo2\n" +
                "chr1\t1\tbongo3\n" +
                "chr1\t1\tbongo4\n" +
                "chr1\t1\tbongo5\n" +
                "chr1\t1\tbongo6\n" +
                "chr1\t1\tbingo2\n" +
                "chr1\t1\tbingo3\n" +
                "chr1\t1\tbingo4\n" +
                "chr1\t1\tbingo5\n" +
                "chr1\t1\tbingo6\n";

        String expected = "Chrom\tbpStart\tbpStop\tset_Data\n" +
                "chr1\t0\t250000000\tbingo1,bingo2,bingo3,bing...\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | group chrom -sc Data -set -len 25", file.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void setWithIntsTruncated() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\t12341\n" +
                "chr1\t1\t43211\n" +
                "chr1\t1\t12341\n" +
                "chr1\t1\t43211\n" +
                "chr1\t1\t43212\n" +
                "chr1\t1\t43213\n" +
                "chr1\t1\t43214\n" +
                "chr1\t1\t43215\n" +
                "chr1\t1\t43216\n" +
                "chr1\t1\t12342\n" +
                "chr1\t1\t12343\n" +
                "chr1\t1\t12344\n" +
                "chr1\t1\t12345\n" +
                "chr1\t1\t12346\n";

        String expected = "Chrom\tbpStart\tbpStop\tset_Data\n" +
                "chr1\t0\t250000000\t12341,12342,12343,12344,1...\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | group chrom -ic Data -set -len 25", file.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void list() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbongo2\n" +
                "chr1\t1\tbongo3\n" +
                "chr1\t1\tbongo4\n" +
                "chr1\t1\tbongo5\n" +
                "chr1\t1\tbongo6\n" +
                "chr1\t1\tbingo2\n" +
                "chr1\t1\tbingo3\n" +
                "chr1\t1\tbingo4\n" +
                "chr1\t1\tbingo5\n" +
                "chr1\t1\tbingo6\n";

        String expected = "Chrom\tbpStart\tbpStop\tlis_Data\n" +
                "chr1\t0\t250000000\tbingo1,bongo1,bingo1,bongo1,bongo2,bongo3,bongo4,bongo5,bongo6,bingo2,bingo3,bingo4,bingo5,bingo6\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | group chrom -sc Data -lis", file.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void listTooLongThrowsError() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbongo2\n" +
                "chr1\t1\tbongo3\n" +
                "chr1\t1\tbongo4\n" +
                "chr1\t1\tbongo5\n" +
                "chr1\t1\tbongo6\n" +
                "chr1\t1\tbingo2\n" +
                "chr1\t1\tbingo3\n" +
                "chr1\t1\tbingo4\n" +
                "chr1\t1\tbingo5\n" +
                "chr1\t1\tbingo6\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | group chrom -sc Data -lis -len 25 -notruncate", file.getAbsolutePath());
        thrown.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void listTooLongTruncated() throws IOException {
        String contents = "Chrom\tPos\tData\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbingo1\n" +
                "chr1\t1\tbongo1\n" +
                "chr1\t1\tbongo2\n" +
                "chr1\t1\tbongo3\n" +
                "chr1\t1\tbongo4\n" +
                "chr1\t1\tbongo5\n" +
                "chr1\t1\tbongo6\n" +
                "chr1\t1\tbingo2\n" +
                "chr1\t1\tbingo3\n" +
                "chr1\t1\tbingo4\n" +
                "chr1\t1\tbingo5\n" +
                "chr1\t1\tbingo6\n";

        String expected = "Chrom\tbpStart\tbpStop\tlis_Data\n" +
                "chr1\t0\t250000000\tbingo1,bongo1,bingo1,bong...\n";

        final File file = FileTestUtils.createTempFile(workDir.getRoot(), "test.gor", contents);
        final String query = String.format("gor %s | group chrom -sc Data -lis -len 25", file.getAbsolutePath());
        final String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void orderedGroupWithNorReturnsProperChrom() {
        final String query = "norrows 100 | calc x random() | sort -c rownum | group -gc rownum -count -ordered | group -count";
        final String result = TestUtils.runGorPipe(query);
        final String expected = "Chrom\tPosNOR\tallCount\n" +
                "chrN\t0\t100\n";
        Assert.assertEquals(expected, result);
    }

    private void assertValueArray(String line, int offset, double[] expectedValues, String[] functions) {
        String[] values = line.split("\t", -1);
        int count = 0;

        for (String value : Iterables.skip(Arrays.asList(values), offset)) {
            double calculatedValue = Double.parseDouble(value);

            Assert.assertEquals("Expected values using " + functions[count], expectedValues[count], calculatedValue, 0.1);
            count++;
        }
    }

    private void testNumberColumns(String[] lines) {
        Assert.assertEquals("Number of lines from the group query", 7, lines.length);

        String column = lines[0].toLowerCase();
        assertContainedInColumn(column, "min", true);
        assertContainedInColumn(column, "max", true);
        assertContainedInColumn(column, "std", true);
        assertContainedInColumn(column, "avg", true);
        assertContainedInColumn(column, "sum", true);
        assertContainedInColumn(column, "med", true);
    }

    private static void assertContainedInColumn(String columnHeader, String value, boolean exists) {
        if (exists) {
            Assert.assertTrue(String.format("Column should contain %s column", value), columnHeader.contains(value));
        } else {
            Assert.assertFalse(String.format("Column should not contain %s column", value), columnHeader.contains(value));
        }
    }

    private static String createAtTestFileWithFloatingNumbers() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("valueswithfloatvalues", ".tsv");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tpos\tvalue");
        outputWriter.println("chr1\t500\t1.0");
        outputWriter.println("chr1\t500\t5.0");
        outputWriter.println("chr1\t1300\t10.0");
        outputWriter.println("chr1\t1300\t6.0");
        outputWriter.println("chr2\t22500\t3.0");
        outputWriter.println("chr2\t32100\t7.0");
        outputWriter.println("chr2\t32200\t5.0");
        outputWriter.close();

        return patientsPath.toString();
    }

    private static String createAtTestFileWithIntegerNumbers() throws IOException {
        // Create parent relation file .tsv
        Path patientsPath = Files.createTempFile("valueswithintegervalues", ".tsv");
        File outputFile = patientsPath.toFile();
        outputFile.deleteOnExit();
        PrintWriter outputWriter = new PrintWriter(outputFile);
        outputWriter.println("chr\tpos\tvalue");
        outputWriter.println("chr1\t500\t1");
        outputWriter.println("chr1\t500\t5");
        outputWriter.println("chr1\t1300\t10");
        outputWriter.println("chr1\t1300\t6");
        outputWriter.println("chr2\t22500\t3");
        outputWriter.println("chr2\t32100\t7");
        outputWriter.println("chr2\t32200\t5");
        outputWriter.close();

        return patientsPath.toString();
    }
}
