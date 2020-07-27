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

import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.model.gor.iterators.RowSource;
import org.gorpipe.test.SlowTests;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sigmar on 11/05/16.
 */
public class UTestNorDictionary {
    private static File dictionaryFile;
    private static File pnFile;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        File gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
        dictionaryFile = FileTestUtils.createGenericDictionaryFile(workDir.getRoot(), gorFile.getName(),  "generic.gord");
        pnFile = FileTestUtils.createPNTxtFile(workDir.getRoot());
    }

    @Test
    public void testNorDictionaryHeader() throws IOException {
        String query = "nor -asdict " + dictionaryFile.getCanonicalPath();

        try (RowSource rs = TestUtils.runGorPipeIterator(query)) {
            String header = rs.getHeader();
            int count = 0;
            while (rs.hasNext()) {
                rs.next();
                count++;
            }
            Assert.assertEquals("Wrong nor dictionary header", "ChromNOR\tPosNOR\tcol1\tcol2", header);
            Assert.assertEquals("Wrong number of lines read", 2, count);
        }
    }

    @Test
    public void testNorDictionaryWithExclusionPattern() throws IOException {
        File tempFile = File.createTempFile("dict", ".nord");
        FileUtils.writeStringToFile(tempFile, "##Source=foo\n#data\tSource\nfoo_01\tbar\nfoo_02\tbar\n##foo_03\tbar\n", Charset.defaultCharset());
        String query = "nor -asdict " + tempFile.getAbsolutePath();

        String[] lines = TestUtils.runGorPipeLines(query);

        Assert.assertEquals(3, lines.length);
        Assert.assertEquals("ChromNOR\tPosNOR\tdata\tSource\n", lines[0]);
        Assert.assertEquals("chrN\t0\tfoo_01\tbar\n", lines[1]);
        Assert.assertEquals("chrN\t0\tfoo_02\tbar\n", lines[2]);
    }

    @Test
    public void testNorGorDictionaryWithFilter() throws IOException {
        String query = "nor " + dictionaryFile.getCanonicalPath() + " -f a";
        int count = TestUtils.runGorPipeCount(query);

        Assert.assertEquals("Nor should read dictionary file, not the files in the dictionary", 9, count);
    }

    @Test
    public void testNorGorDictionaryWithFFilter() throws IOException {
        String query = "nor " + dictionaryFile.getCanonicalPath() + " -ff <(nor " + pnFile.getCanonicalPath() + " | top 1)";
        String result = TestUtils.runGorPipe(query);

        String expected = "ChromNOR\tPosNOR\tChrom\tgene_start\tgene_end\tGene_Symbol\tSource\n" +
                "chrN\t0\tchr1\t11868\t14412\tDDX11L1\ta\n" +
                "chrN\t0\tchr1\t14362\t29806\tWASH7P\ta\n" +
                "chrN\t0\tchr1\t29553\t31109\tMIR1302-11\ta\n" +
                "chrN\t0\tchr1\t34553\t36081\tFAM138A\ta\n" +
                "chrN\t0\tchr1\t52472\t54936\tOR4G4P\ta\n" +
                "chrN\t0\tchr1\t62947\t63887\tOR4G11P\ta\n" +
                "chrN\t0\tchr1\t69090\t70008\tOR4F5\ta\n" +
                "chrN\t0\tchr1\t89294\t133566\tRP11-34P13.7\ta\n" +
                "chrN\t0\tchr1\t89550\t91105\tRP11-34P13.8\ta\n";

        Assert.assertEquals("Nor should read dictionary file, not the files in the dictionary", expected, result);
    }

    @Test
    public void testNorGorDictionaryWithEmptyFFilter() throws IOException {
        String query = "nor " + dictionaryFile.getCanonicalPath() + " -ff <(nor " + pnFile.getCanonicalPath() + " | top 0)";
        String result = TestUtils.runGorPipe(query);

        String expected = "ChromNOR\tPosNOR\tChrom\tgene_start\tgene_end\tGene_Symbol\tSource\n";

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testNorGorDictionaryWithEmptyFFilterInCreate() throws IOException {
        String query = "create pns = norrows 1 | calc pheno 'asdf' | where rownum > 1;" +
                "nor " + dictionaryFile.getCanonicalPath() + " -ff [pns]";
        String result = TestUtils.runGorPipe(query);

        String expected = "ChromNOR\tPosNOR\tChrom\tgene_start\tgene_end\tGene_Symbol\tSource\n";

        Assert.assertEquals(expected, result);
    }


    private static String createTestFiles(int numberOfDictionaryFiles,
                                         int numberOfLinesInDictionaryFile,
                                         boolean sourceFileHeader) throws IOException {

        final NorDictTestDataGenerator testDataGenerator = new NorDictTestDataGeneratorBuilder()
                .setNumberOfDictionaryFiles(numberOfDictionaryFiles)
                .setNumberOfLinesInDictionaryFile(numberOfLinesInDictionaryFile)
                .setSourceFileHeader(sourceFileHeader)
                .createNorDictTestDataGenerator();
        return testDataGenerator.invoke();
    }

    private static String createTestFiles(int numberOfDictionaryFiles,
                                         int numberOfLinesInDictionaryFile,
                                         boolean sourceFileHeader,
                                         boolean relativePaths,
                                         boolean addSourceHeader,
                                         boolean sourceColumn) throws IOException {

        final NorDictTestDataGenerator testDataGenerator = new NorDictTestDataGeneratorBuilder()
                .setNumberOfDictionaryFiles(numberOfDictionaryFiles)
                .setNumberOfLinesInDictionaryFile(numberOfLinesInDictionaryFile)
                .setSourceFileHeader(sourceFileHeader)
                .setRelativePaths(relativePaths)
                .setAddSourceHeader(addSourceHeader)
                .setSourceColumn(sourceColumn)
                .createNorDictTestDataGenerator();
        return testDataGenerator.invoke();
    }

    @Test
    @Category(SlowTests.class)
    public void testNordDictionaryWithNoFilter() throws IOException {
        int numDictFiles = 10000;
        int numDictFileLines = 100;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        int count = TestUtils.runGorPipeCount(String.format("nor %1$s/test.nord", path));

        Assert.assertEquals(numDictFiles * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryWithFirstFileEmptyNoHeader() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 100;

        final NorDictTestDataGenerator testDataGenerator = new NorDictTestDataGeneratorBuilder()
                .setNumberOfDictionaryFiles(numDictFiles)
                .setNumberOfLinesInDictionaryFile(numDictFileLines)
                .setSourceFileHeader(true)
                .setFileEmpty(0)
                .createNorDictTestDataGenerator();
        String path = testDataGenerator.invoke();

        expected.expect(GorDataException.class);
        final String query = String.format("nor %1$s/test.nord", path);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void testNordDictionaryWithMiddleFileEmptyNoHeader() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 100;

        final NorDictTestDataGenerator testDataGenerator = new NorDictTestDataGeneratorBuilder()
                .setNumberOfDictionaryFiles(numDictFiles)
                .setNumberOfLinesInDictionaryFile(numDictFileLines)
                .setSourceFileHeader(true)
                .setFileEmpty(5)
                .createNorDictTestDataGenerator();
        String path = testDataGenerator.invoke();

        expected.expect(GorDataException.class);
        final String query = String.format("nor %1$s/test.nord", path);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void testNordDictionaryWithFirstFileEmptyWithHeader() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 100;

        final NorDictTestDataGenerator testDataGenerator = new NorDictTestDataGeneratorBuilder()
                .setNumberOfDictionaryFiles(numDictFiles)
                .setNumberOfLinesInDictionaryFile(numDictFileLines)
                .setSourceFileHeader(true)
                .setFileHeaderOnly(0)
                .createNorDictTestDataGenerator();
        String path = testDataGenerator.invoke();

        int count = TestUtils.runGorPipeCount(String.format("nor %1$s/test.nord", path));

        Assert.assertEquals((numDictFiles-1) * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryWithMiddleFileEmptyWithHeader() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 100;

        final NorDictTestDataGenerator testDataGenerator = new NorDictTestDataGeneratorBuilder()
                .setNumberOfDictionaryFiles(numDictFiles)
                .setNumberOfLinesInDictionaryFile(numDictFileLines)
                .setSourceFileHeader(true)
                .setFileHeaderOnly(5)
                .createNorDictTestDataGenerator();
        String path = testDataGenerator.invoke();

        int count = TestUtils.runGorPipeCount(String.format("nor %1$s/test.nord", path));

        Assert.assertEquals((numDictFiles-1) * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryWithMiddleFileMissing() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 100;

        final NorDictTestDataGenerator testDataGenerator = new NorDictTestDataGeneratorBuilder()
                .setNumberOfDictionaryFiles(numDictFiles)
                .setNumberOfLinesInDictionaryFile(numDictFileLines)
                .setSourceFileHeader(true)
                .setFileMissing(5)
                .createNorDictTestDataGenerator();
        String path = testDataGenerator.invoke();

        expected.expect(GorResourceException.class);
        int count = TestUtils.runGorPipeCount(String.format("nor %1$s/test.nord", path));

        Assert.assertEquals((numDictFiles-1) * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryDataIntegrity() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 100;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        String[] lines = TestUtils.runGorPipeLinesNoHeader(String.format("nor %1$s/test.nord -f Patient_1,Patient_5 | validatecolumns -n 1 | where Index >= 90", path));

        Assert.assertEquals(20, lines.length);
    }

    @Test
    public void testNordDictionaryWithFilter() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 100;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        int count = TestUtils.runGorPipeCount(String.format("nor %1$s/test.nord -f Patient_99,Patient_11", path));

        Assert.assertEquals(2 * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryRelativeEntryPaths() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 10;
        String path = createTestFiles(numDictFiles, numDictFileLines, true, true, false, false);

        int count = TestUtils.runGorPipeCount(String.format("nor %1$s/test.nord", path));

        Assert.assertEquals(numDictFiles * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryRelativeNordFileRelativeEntryPaths() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 10;
        String path = createTestFiles(numDictFiles, numDictFileLines, true, true, false, false);

        int count = TestUtils.runGorPipeCount("nor test.nord", path);

        Assert.assertEquals(numDictFiles * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryRelativeNordFileRelativeEntryPathsOneLevelDown() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 10;
        String path = createTestFiles(numDictFiles, numDictFileLines, true, true, false, false);

        Path originalPath = Paths.get(path);
        Path parentPath = originalPath.getParent();

        int count = TestUtils.runGorPipeCount(String.format("nor ./%1$s/test.nord", originalPath.getFileName().toString()), parentPath.toString());

        Assert.assertEquals(numDictFiles * numDictFileLines, count);
    }

    @Test
    public void testNordDictionaryWithFilterAnsMissingEntry() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 100;
        final String path = createTestFiles(numDictFiles, numDictFileLines, true);
        final String query = String.format("nor %1$s/test.nord -f Patient_99,Patient_11,Patient_999,Pa778", path);

        try {
            TestUtils.runGorPipe(query);
            Assert.fail("We should get parsing error because of missing entries");
        } catch (GorParsingException gpe) {
            Assert.assertTrue(gpe.getMessage().contains("Patient_999"));
            Assert.assertTrue(gpe.getMessage().contains("Pa778"));
        }
    }

    @Test
    public void testNordDictionaryAsDict() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord -asdict -h", path));

        Assert.assertEquals(numDictFiles + 1, lines.length);
        Assert.assertEquals(4, lines[0].split("\t").length);
    }

    @Test
    public void testNordDictionaryCustomSourceColumn() throws IOException {
        int numDictFiles = 1;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord -s PN", path));

        Assert.assertEquals(numDictFiles + 1, lines.length);
        Assert.assertEquals(6, lines[0].split("\t").length);
        Assert.assertEquals("PN", lines[0].split("\t")[5].trim());
    }

    @Test
    public void testNordDictionarySourceColumnInHeader() throws IOException {
        int numDictFiles = 1;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true, false, true, false);

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord", path));

        Assert.assertEquals(numDictFiles + 1, lines.length);
        Assert.assertEquals(6, lines[0].split("\t").length);
        Assert.assertEquals("phenotype", lines[0].split("\t")[5].trim());
    }

    @Test
    public void testNordDictionaryNoHeaderOnFileEntries() throws IOException {
        int numDictFiles = 1;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, false);

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord -s Source", path));
        String[] headerEntries = lines[0].split("\t");

        Assert.assertEquals(numDictFiles + 1, lines.length);
        Assert.assertEquals(6, headerEntries.length);
        Assert.assertEquals("col1", headerEntries[2].trim());
        Assert.assertEquals("col2", headerEntries[3].trim());
    }

    @Test
    public void testNordDictionaryFileFilter() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        // Create filter file
        File filterFile = new File(path, "filter.txt");
        FileUtils.writeStringToFile(filterFile, "Patient_10\nPatient_21\nPatient_32\nPatient_43\n", Charset.defaultCharset());

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord -ff %2$s -s Source", path, filterFile.getAbsolutePath()));

        Assert.assertEquals(5, lines.length);
        Assert.assertTrue(lines[1].contains("Patient_10"));
        Assert.assertTrue(lines[2].contains("Patient_21"));
        Assert.assertTrue(lines[3].contains("Patient_32"));
        Assert.assertTrue(lines[4].contains("Patient_43"));
    }

    @Test
    public void testNordDictionaryEmptyFileFilter() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        // Create filter file
        File filterFile = new File(path, "filter.txt");
        FileUtils.writeStringToFile(filterFile, "#PN", Charset.defaultCharset());

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord -ff %2$s", path, filterFile.getAbsolutePath()));

        Assert.assertEquals(1, lines.length);
    }

    @Test
    public void testNordDictionaryEmptyFileFilterRelativePaths() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true, true, false, false);

        // Create filter file
        File filterFile = new File(path, "filter.txt");
        FileUtils.writeStringToFile(filterFile, "#PN", Charset.defaultCharset());

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord -ff %2$s", path, filterFile.getAbsolutePath()));

        Assert.assertEquals(1, lines.length);
    }

    @Test
    public void testNordDictionaryFileFilterNestedQuery() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        // Create filter file
        File filterFile = new File(path, "filter.txt");
        FileUtils.writeStringToFile(filterFile, "Patient_10\nPatient_21\nPatient_32\nPatient_43\n", Charset.defaultCharset());

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord -ff <(nor %2$s | skip 1 | top 2) -s Source", path, filterFile.getAbsolutePath()));

        Assert.assertEquals(3, lines.length);
        Assert.assertTrue(lines[1].contains("Patient_21"));
        Assert.assertTrue(lines[2].contains("Patient_32"));
    }

    @Test
    public void testNordDictionaryFileFilterNestedQueryReturnsNothing() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        // Create filter file
        File filterFile = new File(path, "filter.txt");
        FileUtils.writeStringToFile(filterFile, "Patient_10\nPatient_21\nPatient_32\nPatient_43\n", Charset.defaultCharset());

        String result = TestUtils.runGorPipe(String.format("nor %1$s/test.nord -ff <(nor %2$s | top 0) -s Source", path, filterFile.getAbsolutePath()));
        String expected = "ChromNOR\tPosNOR\tConstant\tCounter\tIndex\tSource\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testNordDictionaryFileFilterNestedQueryRelativePathsReturnsNothing() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true, true, false, false);

        // Create filter file
        File filterFile = new File(path, "filter.txt");
        FileUtils.writeStringToFile(filterFile, "Patient_10\nPatient_21\nPatient_32\nPatient_43\n", Charset.defaultCharset());

        String result = TestUtils.runGorPipe(String.format("nor %1$s/test.nord -ff <(nor %2$s | top 0) -s Source", path, filterFile.getAbsolutePath()));
        String expected = "ChromNOR\tPosNOR\tConstant\tCounter\tIndex\tSource\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testNordDictionaryFileFilterNestedQueryNorrow() throws IOException {
        int numDictFiles = 100;
        int numDictFileLines = 1;
        String path = createTestFiles(numDictFiles, numDictFileLines, true);

        // Create filter file
        File filterFile = new File(path, "filter.txt");
        FileUtils.writeStringToFile(filterFile, "Patient_10\nPatient_21\nPatient_32\nPatient_43\n", Charset.defaultCharset());

        final String query = String.format("nor %1$s/test.nord -ff <(norrows 10 | calc PN 'Patient_'+rownum | select #2)", path);
        String[] lines = TestUtils.runGorPipeLines(query);

        Assert.assertEquals(11, lines.length);
    }

    @Test
    public void testNordDictionaryWithCustomSourceColumn() throws IOException {
        int numDictFiles = 10;
        int numDictFileLines = 10;
        String path = createTestFiles(numDictFiles, numDictFileLines, true, false, false, true);

        String[] lines = TestUtils.runGorPipeLines(String.format("nor %1$s/test.nord", path));

        Assert.assertEquals(101, lines.length);

        // TEst header
        Assert.assertTrue(lines[0].contains("\tSource"));
        Assert.assertTrue(lines[1].contains("\tSource_0"));
        Assert.assertTrue(lines[10].contains("\tSource_0"));
        Assert.assertTrue(lines[11].contains("\tSource_1"));
        Assert.assertTrue(lines[20].contains("\tSource_1"));
        Assert.assertTrue(lines[91].contains("\tSource_9"));
        Assert.assertTrue(lines[100].contains("\tSource_9"));
    }

}
