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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by sigmar on 25/06/15.
 */
public class UTestNor {

    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder symbolicTarget = new TemporaryFolder();

    private ProjectContext projectContext;

    @Before
    public void setUp() throws IOException {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("user_data");

        projectContext = new ProjectContext.Builder()
                .setRoot(projectDir.getRoot().getCanonicalPath())
                .setWriteLocations(locations)
                .build();
        createSymbolicLink();
    }

    private void createSymbolicLink() throws IOException {
        // Administrator privileges are required on Windows to create a symbolic link
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String userDataFolder = projectContext.getRealProjectRoot() + "/user_data";
        File dir = new File(userDataFolder);
        dir.mkdir();
        FileTestUtils.createTempFile(dir, "test.gor",
                "Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                        "chr1\t10179\tC\tCC\trs367896724\n" +
                        "chr1\t10250\tA\tC\trs199706086");
        Path link = Paths.get(projectContext.getRealProjectRoot() + "/shared");
        Path target = Paths.get(userDataFolder);
        Files.createSymbolicLink(link, target);
    }


    /**
     * Test noring a filesystem
     */
    @Test
    public void testNorFileSystem() {
        String curdir = new File(".").getAbsolutePath();
        String query = "nor " + curdir.substring(0, curdir.length() - 1) + "src/test/java/gorsat/";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (iterator.hasNext()) {
                String next = iterator.next().toString();
                if (next.contains("UTestNor.java")) count++;
            }
            Assert.assertTrue("UTestNor.java not listed in nor directory output", count > 0);
        }
    }

    @Test
    public void testNorWithEmptyLines() throws IOException {
        File tempFile = File.createTempFile("norrows", "txt");
        tempFile.deleteOnExit();
        FileUtils.writeStringToFile(tempFile, "#foo\tbar\n\nfoo1\tbar1\n\n\n\nfoo3\tbar3", (Charset) null);

        int count1 = TestUtils.runGorPipeCount(String.format("nor %s", tempFile));
        int count2 = TestUtils.runGorPipeCount(String.format("nor %s -i", tempFile));

        Assert.assertTrue(count1 != count2);
        Assert.assertEquals("We should receive all 6 lines", 6, count1);
        Assert.assertEquals("We should not get the empty line here, only 2 lines", 2, count2);
    }

    @Test
    public void testNorWithEmptyLinesInNestedQuery() throws IOException {
        File tempFile = File.createTempFile("norrows_nested", "txt");
        tempFile.deleteOnExit();
        FileUtils.writeStringToFile(tempFile, "#foo\tbar\n\nfoo1\tbar1\n\n\n\nfoo3\tbar3", (Charset) null);

        int count1 = TestUtils.runGorPipeCount(String.format("nor <(nor %s)", tempFile));
        int count2 = TestUtils.runGorPipeCount(String.format("nor <(nor %s -i)", tempFile));

        Assert.assertTrue(count1 != count2);
        Assert.assertEquals("We should receive all 6 lines", 6, count1);
        Assert.assertEquals("We should not get the empty line here, only 2 lines", 2, count2);
    }

    @Test
    public void testAutoNorOutput() {
        String query = "nor ../tests/data/external/samtools/noheader.vcf | top 1";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            while (iterator.hasNext()) {
                String next = iterator.next().toString();
                if (next.startsWith("chrN")) count++;
            }
            Assert.assertTrue("chrN not present in output", count > 0);
        }
    }

    @Test
    public void testDepthRangeNoringFolderTopLevel() {
        String query = "nor ../tests";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
            int depthRange = getDepthRangeFromIterator(iterator);
            Assert.assertEquals("Depth should be 1 when only scanning top level folder.", 1, depthRange);
        }
    }

    @Test
    public void testDepthRangeNoringFolderUnlimited() {
        String query = "nor ../tests -r";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
            int depthRange = getDepthRangeFromIterator(iterator);
            Assert.assertTrue("Depth should be greater than 3 when scanning with no limit.", depthRange > 3);
        }
    }

    @Test
    public void testDepthRangeNoringFolderLimit2() {
        String query = "nor ../tests -r -d 2";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
            int depthRange = getDepthRangeFromIterator(iterator);
            Assert.assertEquals("Depth should be 2 when scanning with -d 2 limit.", 2, depthRange);
        }
    }

    @Test
    public void testNoringFolderWithoutModificationDate() {
        String query = "nor ../tests -m | top 0";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(query)) {
            Assert.assertFalse("Header should not contain the modification date.", iterator.getHeader().contains("Modified"));
        }
    }

    @Test
    public void testNorWithGorRoot() throws IOException {
        String gorRoot = Paths.get("../").toFile().getCanonicalPath();
        String[] args = new String[]{"nor -r tests | top 10", "-gorroot", gorRoot};
        String results = TestUtils.runGorPipe(args);
        Assert.assertFalse(results.contains(gorRoot));
    }

    @Test
    public void testNorWithGorRootSymbolic() {
        String gorRoot = projectContext.getRealProjectRoot();
        String[] args1 = new String[]{"nor -r shared | top 10", "-gorroot", gorRoot};
        String[] args2 = new String[]{"nor -r user_data | top 10", "-gorroot", gorRoot};
        String results1 = TestUtils.runGorPipe(args1);
        String results2 = TestUtils.runGorPipe(args2);
        Assert.assertFalse(results1.contains(gorRoot));
        Assert.assertFalse(results2.contains(gorRoot));
    }

    private int getDepthRangeFromIterator(GenomicIterator iterator) {
        int minDepth = Integer.MAX_VALUE;
        int maxDepth = Integer.MIN_VALUE;
        while (iterator.hasNext()) {
            Row row = iterator.next();
            int depth = row.colAsInt(8);

            if (depth < minDepth)
                minDepth = depth;

            if (depth > maxDepth)
                maxDepth = depth;
        }

        if (minDepth == Integer.MAX_VALUE || minDepth == Integer.MIN_VALUE) return -1;

        return maxDepth - minDepth;
    }

    @Test
    public void testNorLongRowsFewColumns() throws IOException {
        int[] lineSizes = {1, 16, 64, 128};
        for (int sz : lineSizes) {
            int length = sz * 1000;
            String filePath = createTestFileLongLinesFewColumns(length);

            String[] result = TestUtils.runGorPipeLines("nor " + filePath + " | top 1 ");
            Assert.assertEquals(2, result.length);

            int lineLength = result[1].length();
            Assert.assertTrue(lineLength <= length + 7); // Account for chrN\t0\t
        }
    }

    @Test
    public void testNorLongRowsManyColumns() throws IOException {
        int[] lineSizes = {1, 16, 64, 128};
        for (int sz : lineSizes) {
            int length = sz * 1000;
            String filePath = createTestFileLongLinesManyColumns(length);

            String[] result = TestUtils.runGorPipeLines("nor " + filePath + " | top 1 ");
            Assert.assertEquals(2, result.length);

            int lineLength = result[1].length();
            Assert.assertTrue(lineLength <= length + 7); // Account for chrN\t0\t
        }
    }

    private String createTestFileLongLinesFewColumns(int length) throws IOException {
        File tempFile = File.createTempFile("testNorLongRowsFewColumns", ".tsv");
        tempFile.deleteOnExit();

        PrintWriter outputWriter = new PrintWriter(tempFile);
        outputWriter.println("#Col1\tCol2\tCol3");

        int columnLength = (length - 3) / 3;
        String col1 = StringUtils.repeat("a", columnLength);
        String col2 = StringUtils.repeat("b", columnLength);
        String col3 = StringUtils.repeat("c", columnLength);

        for (int i = 0; i < 10; i++) {
            outputWriter.print(col1);
            outputWriter.print("\t");
            outputWriter.print(col2);
            outputWriter.print("\t");
            outputWriter.println(col3);
        }

        outputWriter.close();

        return tempFile.getAbsolutePath();
    }

    private String createTestFileLongLinesManyColumns(int length) throws IOException {
        File tempFile = File.createTempFile("testNorLongRowsManyColumns", ".tsv");
        tempFile.deleteOnExit();

        PrintWriter outputWriter = new PrintWriter(tempFile);

        outputWriter.print("#");

        int numColumns = length / 3;
        for (int column = 0; column < numColumns; column++) {
            outputWriter.print(String.format("Col%d", column));
            if (column < numColumns - 1) {
                outputWriter.print("\t");
            }
        }
        outputWriter.println();


        for (int i = 0; i < 10; i++) {
            for (int column = 0; column < numColumns - 1; column++) {
                outputWriter.print("xx");
                outputWriter.print("\t");
            }
            outputWriter.println("xx");
        }

        outputWriter.close();

        return tempFile.getAbsolutePath();
    }
}
