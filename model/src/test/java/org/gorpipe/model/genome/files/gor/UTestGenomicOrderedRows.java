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
package org.gorpipe.model.genome.files.gor;

import org.gorpipe.model.gor.iterators.RowSource;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.test.SlowTests;
import gorsat.TestUtils;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.test.GorDictionarySetup;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.gorpipe.model.IteratorTestUtilities.countRemainingLines;

/**
 * Test GenomicRowSource
 *
 * @version $Id$
 */
public class UTestGenomicOrderedRows {

    private static final Logger log = LoggerFactory.getLogger(UTestGenomicOrderedRows.class);

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    /**
     * Test various queries that restrict columns
     */
    @Test
    public void testColumnRestrictions() {
        Assert.assertEquals("chr1\t0\n", TestUtils.runGorPipeNoHeader("gor 1.mem | select Chromo,Pos | top 1"));
        Assert.assertEquals("chr1\t0\n", TestUtils.runGorPipeNoHeader("gor -s foo 1.mem | select Chromo,Pos | top 1"));
    }

    @Ignore("Do we care about Hg output format?")
    @Test
    public void testHgOutputFormat() throws IOException {
        final String header = "Chromo\tPos\tCol3\tCol4\n";
        final String data = "1\t1\ttest1\ttest2\n2\t1\ttest1\ttest2\n3\t1\ttest1\ttest2\n4\t1\ttest1\ttest2\n5\t1\ttest1\ttest2\n6\t1\ttest1\ttest2\n7\t1\ttest1\ttest2\n8\t1\ttest1\ttest2\n9\t1\ttest1\ttest2\n10\t1\ttest1\ttest2\n11\t1\ttest1\ttest2\n12\t1\ttest1\ttest2\n13\t1\ttest1\ttest2\n14\t1\ttest1\ttest2\n15\t1\ttest1\ttest2\n16\t1\ttest1\ttest2\n17\t1\ttest1\ttest2\n18\t1\ttest1\ttest2\n19\t1\ttest1\ttest2\n20\t1\ttest1\ttest2\n21\t1\ttest1\ttest2\n22\t1\ttest1\ttest2\nX\t1\ttest1\ttest2\nY\t1\ttest1\ttest2\nMT\t1\ttest1\ttest2\nGLX\t1\ttest1\ttest2\n";
        final Path a = java.nio.file.Files.createTempFile("hgordered", ".gor");
        java.nio.file.Files.write(a, (header + data).getBytes());

        Assert.assertEquals(data, TestUtils.runGorPipeNoHeader("-X HG " + a.toFile().getAbsolutePath()));

        final String dataInLex = "chr1\t1\ttest1\ttest2\nchr2\t1\ttest1\ttest2\nchr3\t1\ttest1\ttest2\nchr4\t1\ttest1\ttest2\nchr5\t1\ttest1\ttest2\nchr6\t1\ttest1\ttest2\nchr7\t1\ttest1\ttest2\nchr8\t1\ttest1\ttest2\nchr9\t1\ttest1\ttest2\nchr10\t1\ttest1\ttest2\nchr11\t1\ttest1\ttest2\nchr12\t1\ttest1\ttest2\nchr13\t1\ttest1\ttest2\nchr14\t1\ttest1\ttest2\nchr15\t1\ttest1\ttest2\nchr16\t1\ttest1\ttest2\nchr17\t1\ttest1\ttest2\nchr18\t1\ttest1\ttest2\nchr19\t1\ttest1\ttest2\nchr20\t1\ttest1\ttest2\nchr21\t1\ttest1\ttest2\nchr22\t1\ttest1\ttest2\nchrX\t1\ttest1\ttest2\nchrY\t1\ttest1\ttest2\nchrM\t1\ttest1\ttest2\nGLX\t1\ttest1\ttest2\n";
        final String res = TestUtils.runGorPipeNoHeader(a.toFile().getAbsolutePath());
        Assert.assertEquals(dataInLex, res);
        Assert.assertEquals(dataInLex, TestUtils.runGorPipeNoHeader("-X LEX " + a.toFile().getAbsolutePath()));
    }

    @Category(SlowTests.class)
    @Test
    public void testBucketsInDictionary() throws Exception {
        // Generate test files, both individual, dictionary and merged file
        final String[] files = new String[5];
        for (int i = 0; i < files.length; i++) {
            files[i] = createTempGorFile("bucktest" + i, " gor 1.mem | where Col4=" + i);
        }
        final String mergeFile = File.createTempFile("bucktestbuckets", ".gorz").getAbsolutePath();

        // Create dictionary files
        final String dictionary = File.createTempFile("buckdict", ".gord").getAbsolutePath();
        final String dictionaryWithDeletedFiles = File.createTempFile("buckdictdel", ".gord").getAbsolutePath();

        try {
            final StringBuilder sb = new StringBuilder();
            final StringBuilder sbdel = new StringBuilder();
            for (int i = 0; i < files.length; i++) {
                sb.append(files[i]);
                sbdel.append(files[i]);
                if (i % 2 == 0) {
                    // Mark every other file as deleted in the deleted buffer
                    // A nice side effect is that the first and last files will be marked as deleted
                    // which is a good edge case to test for.
                    sbdel.append("|D");
                }
                sb.append('|').append(mergeFile).append('\t');
                sbdel.append('|').append(mergeFile).append('\t');
                sb.append("tag").append(i).append("\n");
                sbdel.append("tag").append(i).append("\n");
            }
            java.nio.file.Files.write(Paths.get(dictionary), sb.toString().getBytes());
            java.nio.file.Files.write(Paths.get(dictionaryWithDeletedFiles), sbdel.toString().getBytes());

            // Merge file as a query from the dictionary
            TestUtils.runGorPipe(String.format("gor %s -Y -s TagName | write %s", dictionary, mergeFile));

            // Check that source name is as expected
            RowSource iterator = TestUtils.runGorPipeIterator(mergeFile);
            Assert.assertEquals("TagName", iterator.getHeader().split("\t")[5]);
            iterator.close();
            iterator = TestUtils.runGorPipeIterator(dictionary + " -f tag0,tag1,tag2,tag3 -s TagName");
            Assert.assertEquals("TagName", iterator.getHeader().split("\t")[5]);
            iterator.close();
            try {
                TestUtils.runGorPipe(dictionary + " -f tag0,tag1,tag2,tag3,tagN");
                Assert.fail("Expected query with invalid -f paramter to Assert.fail");
            } catch (GorException ex) {
                log.debug(ex.getMessage());
                if (!ex.getMessage().contains("Invalid Source Filter")) {
                    Assert.fail("Unexpected exception" + ex.getMessage());
                }
            }
            try {
                TestUtils.runGorPipe(dictionaryWithDeletedFiles + " -f tag0,tag1,tag2,tag3,tag4");
                Assert.fail("Expected query with invalid -f paramter to Assert.fail");
            } catch (GorException ex) {
                log.debug(ex.getMessage());
                if (!ex.getMessage().contains("Invalid Source Filter")) {
                    Assert.fail("Unexpected exception: " + ex.getMessage());
                }

                String message = ex.getMessage();
                if (!(message.contains("tag0") && message.contains("tag2") && message.contains("tag4"))) {
                    Assert.fail("Exception should complain about all even number tags: " + ex.getMessage());
                }
            }
            iterator = TestUtils.runGorPipeIterator(dictionary + " -f tag0,tag1,tag2 -s TagName");
            Assert.assertEquals("TagName", iterator.getHeader().split("\t")[5]);
            iterator.close();

            // Compare direct query from individual files with assumed merge file access from the dictionary
            final String allfiles = String.join(" ", files);
            Assert.assertEquals(TestUtils.runGorPipe(allfiles), TestUtils.runGorPipe(String.format("gor %s -f tag0,tag1,tag2,tag3,tag4 | select 1-5", dictionary)));

            // Read bucket file, check if mixing tags and columns works.
            //String[] result = GOR.string(mergeFile + " -f tag1 -c 1-5 -m 1 -y").split("\t");
            // TODO: Skip for now as not sure if this should fail or not.
            // Assert.assertEquals("Incorrect source", "tag1", result[result.length - 1]);

            // Compare output from the deleted files dictionary with a filtered query on the non-deleted dictionary
            Assert.assertEquals(TestUtils.runGorPipe(String.format("gor %s -f tag1,tag3 | select 1-5", dictionary)),
                    TestUtils.runGorPipe(String.format("gor %s | select 1-5", dictionaryWithDeletedFiles)));

            // Compare by forcing use of individual files with dictionary with one that assumes merge read path
            Assert.assertEquals(TestUtils.runGorPipe(String.format("gor %s -Y -f tag0,tag1,tag2,tag3 ", dictionary)),
                    TestUtils.runGorPipe(String.format("gor %s -f tag0,tag1,tag2,tag3", dictionary)));

            // Create a new dictionary file with multiple large merge files (but swindle a bit by reusing same data)
            final String largedict = File.createTempFile("buckdict", ".gord").getAbsolutePath();
            final String mergeFileA = File.createTempFile("bucktestbuckets", ".gorz").getAbsolutePath();
            final String mergeFileB = File.createTempFile("bucktestbuckets", ".gorz").getAbsolutePath();
            final String mergeFileC = File.createTempFile("bucktestbuckets", ".gorz").getAbsolutePath();
            try {
                sb.setLength(0);
                final StringBuilder sbTagListA = new StringBuilder();
                final StringBuilder sbTagListB = new StringBuilder();
                final StringBuilder sbTagListC = new StringBuilder();
                for (int i = 0; i < 300; i++) {
                    appendDictionaryData(sb, mergeFileA, sbTagListA, i);
                }
                for (int i = 300; i < 600; i++) {
                    appendDictionaryData(sb, mergeFileB, sbTagListB, i);
                }
                for (int i = 600; i < 900; i++) {
                    appendDictionaryData(sb, mergeFileC, sbTagListC, i);

                }
                java.nio.file.Files.write(Paths.get(largedict), sb.toString().getBytes());

                // Merge files as a query from the dictionary, selecting a few lines from each source
                TestUtils.runGorPipe(String.format("gor %s -Y -s TagName -f %s | where pos<10 | write %s", largedict, sbTagListA.toString(), mergeFileA));
                TestUtils.runGorPipe(String.format("gor %s -Y -s TagName -f %s | where pos<10 | write %s", largedict, sbTagListB.toString(), mergeFileB));
                TestUtils.runGorPipe(String.format("gor %s -Y -s TagName -f %s | where pos<10 | write %s", largedict, sbTagListC.toString(), mergeFileC));

                Assert.assertEquals(TestUtils.runGorPipe(String.format("gor %s -Y -f tag0,tag1,tag301,tag702,tag802,tag809", largedict)),
                        TestUtils.runGorPipe(String.format("gor %s -f tag0,tag1,tag301,tag702,tag802,tag809", largedict)));

                Assert.assertEquals(TestUtils.runGorPipe("-Y -f tag0,tag1,tag301,tag702,tag802,tag809 " + largedict), TestUtils.runGorPipe("-f tag0,tag1,tag301,tag702,tag802,tag809 " + largedict));

                sb.setLength(0);
                sb.append("-f tag0");
                for (int i = 1; i < 150; i++) {
                    sb.append(",tag").append(i);
                }
                for (int i = 300; i < 450; i++) {
                    sb.append(",tag").append(i);
                }
                for (int i = 600; i < 750; i++) {
                    sb.append(",tag").append(i);
                }

                String[] result1 = TestUtils.runGorPipeLines(String.format("gor %s -Y %s | where pos<10| top 500000", largedict, sb.toString()));
                String[] result2 = TestUtils.runGorPipeLines(String.format("gor %s %s | where pos<10| top 500000", largedict, sb.toString()));

                Assert.assertTrue(testResultArrays(result1, result2));

            } finally {
                new File(largedict).delete();
                new File(mergeFileA).delete();
                new File(mergeFileB).delete();
                new File(mergeFileC).delete();
            }
        } finally {
            new File(mergeFile).delete();
            new File(dictionary).delete();
            new File(dictionaryWithDeletedFiles).delete();
            for (String file : files) {
                new File(file).delete();
            }
        }
    }

    private Boolean testResultArrays(String[] result1, String[] result2) {
        if (result1.length != result2.length) return false;

        List<String> resultList2 = Arrays.asList(result2);
        for (String line : result1) {
            if (!resultList2.contains(line)) {
                return false;
            }
        }
        return true;
    }

    private void appendDictionaryData(StringBuilder sb, String mergeFileA, StringBuilder sbTagListA, int i) {
        sb.append(i).append(".mem");
        sb.append('|').append(mergeFileA).append('\t');
        final String tag = "tag" + i;
        sb.append(tag).append("\n");
        if (sbTagListA.length() != 0) {
            sbTagListA.append(',');
        }
        sbTagListA.append(tag);
    }

    private String createTempGorFile(String prefix, String query) throws Exception {
        final String name = File.createTempFile(prefix, ".gorz").getAbsolutePath();
        TestUtils.runGorPipe(query + " | write " + name);
        return name;
    }

    /**
     * Test that -s parameter produces an extra column at the end of the row, as per change on 2012-09-02 by gfg and hakon
     */
    @Test
    public void testSourceAsFirstExtraColumn() {
        for (String rowString : TestUtils.runGorPipeLinesNoHeader("gor -s foo 1.mem | top 10")) {
            String[] row = rowString.split("\t");
            Assert.assertEquals(6, row.length);
            Assert.assertEquals("1.mem", row[5].trim());
        }
    }

    /**
     * Test that source name as 3 column works correctly, i.e. #3 column is source column and can correctly be used in expressions
     */
    @Test
    public void testSourceColumn() {

        // When inserting source name, all column indexes must be increesed by one, so third column in source is #4 (since #3 is now the source name)
        try (RowSource iterator = TestUtils.runGorPipeIterator("1.mem -s MyStuff | top 10")) {
            int cnt = 0;
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Assert.assertEquals("chr1", row.chr);
                Assert.assertEquals(cnt, row.pos);
                Assert.assertEquals("1.mem", row.colAsString(5).toString());
                Assert.assertEquals("data1", row.colAsString(2).toString());
                Assert.assertEquals(cnt % 5, row.colAsInt(3));
                cnt++;
            }
            Assert.assertEquals(10, cnt);
        }

        try (RowSource iterator = TestUtils.runGorPipeIterator("1.mem -s MyStuff | top 10")) {
            int cnt = 0;
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Assert.assertEquals("chr1", row.chr);
                Assert.assertEquals(cnt, row.pos);
                Assert.assertEquals("1.mem", row.colAsString(5).toString());
                Assert.assertEquals("data1", row.colAsString(2).toString());
                cnt++;
            }
            Assert.assertEquals(10, cnt);
        }
    }

    /**
     * Test single pos filtering
     */
    @Test
    public void testSinglePosFiltering() {
        try (RowSource iterator = TestUtils.runGorPipeIterator("1.mem -p chr1:3")) {
            int count = 0;
            while (iterator.hasNext()) {
                Row row = iterator.next();
                count++;
                Assert.assertEquals(3, row.pos);
            }
            Assert.assertEquals(1, count);
        }

        try (RowSource iterator = TestUtils.runGorPipeIterator("1.mem -p chr1:3-3")) {
            int count = 0;
            while (iterator.hasNext()) {
                Row row = iterator.next();
                count++;
                Assert.assertEquals(3, row.pos);
            }
            Assert.assertEquals(1, count);
        }
    }

    /**
     * Test query of data with any kind of chromosome data
     *
     * @throws Exception
     */
    @Test
    public void testQueryWithAnyChromosomeData() throws Exception {
        final File file = File.createTempFile("anychromotest", ".gor");
        try {
            final StringBuilder sb = new StringBuilder();
            for (int chr = 100; chr < 1000; chr++) {
                for (int pos = 1; pos < 10; pos++) {
                    sb.append(String.format("chr%d\t%d\ta\n", chr, pos));
                }
            }
            final String content = sb.toString(); //"chrMyMy\t10\ta\nchrMyMy\t20\tb\nchrNuts\t101\tc\nchrNutButs\t102";
            writeTestFileWith(file, content);
            try (RowSource iterator = TestUtils.runGorPipeIterator(file.getAbsolutePath())) {

                Set<String> chromosomes = new HashSet<>();

                while (iterator.hasNext()) {
                    Row row = iterator.next();

                    if (!chromosomes.contains(row.chr))
                        chromosomes.add(row.chr);
                }
                for (int chr = 100; chr < 1000; chr++) {
                    Assert.assertTrue(chromosomes.contains("chr" + (chr)));
                }
            }
        } finally {
            file.delete();
        }
    }

    private void writeTestFileWith(File file, String content) throws IOException {
        file.mkdirs();
        final PrintWriter out = new PrintWriter(file);
        try {
            out.print(content);
            out.flush();
        } finally {
            out.close();
        }
    }


    /**
     * When source name is inserted, ensure that the name of the column is unique, i.e. in case the same file on difference paths
     * is used, include enough of the part (from right) to ensure the name is unique.
     *
     * @throws Exception
     */
    @Test
    public void testFileNameUniqueInSourceNameInsert() throws Exception {
        final String[] parts = {"moretemp/x/yetamoretemp", "moretemp2/x/yetamoretemp"};
        final String tmpdir = System.getProperty("java.io.tmpdir").replace('\\', '/');
        final String tmpFolder = tmpdir + (tmpdir.endsWith("/") ? "" : "/");
        final String[] folders = {tmpFolder + parts[0], tmpFolder + parts[1], tmpFolder};
        for (String folder : folders) {
            writeTestFile(folder, "temp.gor");
        }

        final GorOptions opt = GorOptions.createGorOptions("-s foo", folders[0] + "/temp.gor", folders[1] + "/temp.gor", folders[2] + "temp.gor");
        try (final GenomicIterator source = opt.getIterator()) {
            Assert.assertTrue(source.hasNext());
            Assert.assertEquals(parts[0] + "/temp.gor", source.next().colAsString(5).toString());
            Assert.assertTrue(source.hasNext());
            Assert.assertEquals(parts[1] + "/temp.gor", source.next().colAsString(5).toString());
            Assert.assertTrue(source.hasNext());
            Assert.assertTrue((folders[2] + "temp.gor").endsWith(source.next().colAsString(5).toString().replace("//", "/")));
            Assert.assertFalse(source.hasNext());
        }
    }

    private void writeTestFile(String path, String filename) throws IOException {
        final File filepath = new File(path);
        filepath.mkdirs();
        final PrintWriter out = new PrintWriter(path + '/' + filename);
        try {
            out.println("chr\tpos\tcol1\tcol2\tcol3");
            out.println("chr1\t1\ta\tb\tc");
            out.flush();
        } finally {
            out.close();
        }
    }

    /**
     * Read bytes from provided input stream, stop at the first of a) stream end, or b) the specified max byte count
     */
    private static byte[] read(InputStream source, int maxByteCount) throws IOException {
        final byte[] buf = new byte[maxByteCount];
        int n, used = 0;
        while (used < maxByteCount && (n = source.read(buf, used, maxByteCount - used)) > 0) {
            used += n;
        }
        return (maxByteCount == used) ? buf : Arrays.copyOf(buf, used);
    }

    /**
     * Tests position seek cache implementation
     */
    @Category(IntegrationTests.class)
    @Test
    public void testFilePositionCacheSeek() {
        Assert.assertEquals(1, 1);
        System.setProperty("gor.files.position.cache", "true");
        String fileName = "../tests/data/gor/example.wgs.goodcov.gorz";
        TestUtils.runGorPipe(fileName + " -p chr1:560000- | top 5");
    }

    /**
     * Test invoking gor with simple .gord dictionary file
     *
     * @throws Exception
     */
    @Test
    public void testSimpleGordFile() throws Exception {
        final File file = File.createTempFile("gortest1", ".gord");
        final File file2 = File.createTempFile("gortest2", ".gord");
        final File file3 = File.createTempFile("gortest3", ".gord");

        try {
            java.nio.file.Files.write(file.toPath(), "1.mem".getBytes());
            final GorOptions options = GorOptions.createGorOptions(file.getAbsolutePath());
            try (final GenomicIterator source = options.getIterator()) {
                Assert.assertEquals(4000, countRemainingLines(source));
            }

            // Create a gord file, referencing two other gord files that include the data
            java.nio.file.Files.write(file2.toPath(), "2.mem".getBytes());

            java.nio.file.Files.write(file3.toPath(), (file.getAbsolutePath() + "\n" + file2.getAbsolutePath()).getBytes());

            // Ensure that the data from both data files is found
            final GorOptions optionsB = GorOptions.createGorOptions(file3.getAbsolutePath());
            try (final GenomicIterator sourceB = optionsB.getIterator()) {
                Assert.assertEquals(8000, countRemainingLines(sourceB));
            }
        } finally {
            FileUtils.forceDelete(file);
            FileUtils.forceDelete(file2);
            FileUtils.forceDelete(file3);
        }
    }

    /**
     * Test invoking gor with a dictionary file, i.e. defining source ranges an external file.
     *
     * @throws Exception
     */
    @Test
    public void testRangeByDictionaryFile() throws Exception {
        // Check simple containing range
        final ArrayList<SourceRef> refs = new ArrayList<>();
        refs.add(new SourceRef("1.mem", "1", null, null, "chr1", 0, "chr1", Integer.MAX_VALUE, new HashSet<>(), false, null, null));


        try (final GenomicIterator bit = new BoundedIterator(new GorOptions(refs).getIterator(), "chr1", 0, Integer.MAX_VALUE)) {
            Assert.assertTrue(bit.hasNext());
            final Row line = bit.next();
            Assert.assertEquals("chr1", line.chr);
            Assert.assertEquals(0, line.pos);

            // Check no range
            refs.clear();
            refs.add(new SourceRef("1.mem", "1", null, null));
        }
        try (final GenomicIterator bit = new BoundedIterator(new GorOptions(refs).getIterator(),"chr1", 100, Integer.MAX_VALUE)) {
            Assert.assertTrue(bit.hasNext());
            final Row line = bit.next();
            Assert.assertEquals("chr1", line.chr);
            Assert.assertEquals(100, line.pos);

            // Check only start of range
            refs.clear();
            refs.add(new SourceRef("1.mem", "1", null, null, "chr2", 10, null, -1, new HashSet<>(), false, null, null));
        }

        try (final GenomicIterator bit = new BoundedIterator(new GorOptions(refs).getIterator(), "chr2", 1, Integer.MAX_VALUE)) {
            Assert.assertTrue(bit.hasNext());
            final Row line = bit.next();
            Assert.assertEquals("chr2", line.chr);
            Assert.assertEquals(1, line.pos);

            // Check only stop of range
            refs.clear();
            refs.add(new SourceRef("1.mem", "1", null, null, "", -1, "chr2", Integer.MAX_VALUE, new HashSet<>(), false, null, null));
        }

        try (final GenomicIterator bit = new BoundedIterator(new GorOptions(refs).getIterator(),"chr2", 1, Integer.MAX_VALUE)) {
            Assert.assertTrue(bit.hasNext());
            final Row line = bit.next();
            Assert.assertEquals("chr2", line.chr);
            Assert.assertEquals(1, line.pos);
        }
    }

    /**
     * Test using link files
     *
     * @throws Exception
     */
    @Test
    public void testLinkFile() throws Exception {
        // Setup data file in a subfolder and a link to it that assumes said subfolder is a common root
        final Path commonRoot = java.nio.file.Files.createTempDirectory("testdir");
        final File data1 = File.createTempFile("test", ".gor", commonRoot.toFile());
        final String content = "CHROM\tPOS\tCOL\nchr1\t1\tabc\nchr1\t2\tdef\nchr1\t3\thig";
        java.nio.file.Files.write(data1.toPath(), (content + "\n").getBytes());

        final File linkFolder = new File(commonRoot.toFile(), "linkfolder");
        linkFolder.mkdirs();
        final File link1 = File.createTempFile("test", ".gor.link", linkFolder);
        java.nio.file.Files.write(link1.toPath(), ("file://" + data1.getName()).getBytes());

        log.info(data1.toString());
        log.info(link1.toString());
        log.info(linkFolder.getName() + "/" + link1.getName());
        final String results = TestUtils.runGorPipe(linkFolder.getName() + "/" + link1.getName() + " -r " + commonRoot.toString()).trim();
        Assert.assertEquals(content, results);
    }

    /**
     * Test invoking gor with a dictionary file, i.e. defining source tags in an external file.
     *
     * @throws Exception
     */
    @Test
    public void testTagsByDictionaryFile() throws Exception {
        // Check usage of query tags
        final File file1 = File.createTempFile("test", ".gor");
        final File file2 = File.createTempFile("test", ".gor");
        try {
            PrintWriter out = new PrintWriter(file1);
            try {
                out.println("CHROM\tPOS\tCOL");
                out.println("chr1\t1\tabc");
                out.println("chr1\t2\tdef");
                out.println("chr1\t3\thig");
                out.flush();
            } finally {
                out.close();
            }
            out = new PrintWriter(file2);
            try {
                out.println("CHROM\tPOS\tCOL");
                out.println("chr2\t10\tgcd");
                out.println("chr2\t20\tefd");
                out.println("chr2\t30\thig");
                out.flush();
            } finally {
                out.close();
            }

            // Check that sources with only alias (and no tags) and use of option insert source with treat the alias as tag
            final ArrayList<SourceRef> refsNoTags = new ArrayList<>();
            refsNoTags.add(new SourceRef(file1.getAbsolutePath(), "pn1", null, null));
            refsNoTags.add(new SourceRef(file2.getAbsolutePath(), "pn2", null, null));
            GorOptions opt = new GorOptions(-1, 0, Integer.MAX_VALUE, true, 0, asQueryTags("pn1"), false, false, null, refsNoTags, null,  null);
            GenomicIterator source = opt.getIterator();
            Assert.assertEquals(3, countRemainingLines(source));
            source.close();

            opt = new GorOptions(-1, 0, Integer.MAX_VALUE, true, 0, asQueryTags("pn3"), false, false, null, refsNoTags, null,  null);
            source = opt.getIterator();
            Assert.assertFalse(source.hasNext());
            source.close();

            opt = new GorOptions(-1, 0, Integer.MAX_VALUE, true, 0, asQueryTags("pn1", "pn2"), false, false, null, refsNoTags, null,  null);
            source = opt.getIterator();
            Assert.assertEquals(6, countRemainingLines(source));
            source.close();
        } finally {
            file1.delete();
            file2.delete();
        }
    }

    private static Set<String> asQueryTags(String... values) {
        return new LinkedHashSet(Arrays.asList(values));
    }


    @Test
    public void testWriteNextForBam() throws IOException {
        final GorOptions opt = GorOptions.createGorOptions("../tests/data/external/samtools/serialization_test.bam -p chr1:0-");
        final GenomicIterator source = opt.getIterator();

        Assert.assertTrue(source.hasNext());
        final Row line = source.next();
        Assert.assertEquals("chr1", line.chr);
        Assert.assertEquals(200, line.pos);
        Assert.assertEquals("ACCCTAACCCTAACCCTAACCCTAACCATAACCCTAAGACTAACCCTAAACCTAACCCTCATAATCGAAATACAAC", line.colAsString(11).toString());
        Assert.assertEquals("RG=0 RB=", line.colAsString(13).toString());
        Assert.assertFalse(source.hasNext());
    }

    /**
     * Test merging of multiple files
     *
     * @throws Exception
     */
    @Test
    public void testMultiFileMerge() throws Exception {
        checkFileMergeResultCnt(5, 7);
        checkFileMergeResultCnt(3, 5);
        checkFileMergeResultCnt(6, 11);
        checkFileMergeResultCnt(31, 33);
        checkFileMergeResultCnt(101, 101);
        checkFileMergeResultCnt(331, 19);
        checkFileMergeResultCnt(61, 131);
        checkFileMergeResultCnt(127, 333);
    }

    // Check that the exact number of lines are found in the merged output file
    private void checkFileMergeResultCnt(final int range, final int fileCnt) throws Exception {
        final File[] files = new File[fileCnt];
        final String[] args = new String[files.length];
        final String output = File.createTempFile("gortestoutput", ".out").getAbsolutePath();
        try {
            for (int i = 0; i < files.length; i++) {
                files[i] = File.createTempFile("gortest", ".mfj" + i + ".gor");
                args[i] = files[i].getAbsolutePath();
                final PrintWriter out = new PrintWriter(files[i]);
                out.println("chr\tpos\tdata");
                for (int j = 1; j <= range; j++) {
                    out.print("chr16");
                    out.print('\t');
                    out.print(i * range + j);
                    out.println("\txyz");
                }
                for (int j = 1; j <= range; j++) {
                    out.print("chr17");
                    out.print('\t');
                    out.print(i * range + j);
                    out.println("\txyz");
                }

                out.flush();
                out.close();
            }

            TestUtils.runGorPipe(String.format("gor %s | write %s", String.join(" ", args), output));

            final BufferedReader reader = new BufferedReader(new java.io.FileReader(output));
            int cnt = -1; // ignore the header
            while (reader.readLine() != null) {
                cnt++;
            }
            reader.close();

            Assert.assertEquals("Expect all source lines to show up in output", range * fileCnt * 2, cnt);

        } finally {
            new File(output).delete();
            for (File file : files) {
                if (file != null) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Test that sorting of equals positions is stable in regards of source files
     *
     * @throws IOException
     */
    @Test
    public void testStableSorting() throws IOException {
        final File file1 = File.createTempFile("stable", ".gor");
        final File file2 = File.createTempFile("stable", ".gor");
        final File file3 = File.createTempFile("stable", ".gor");
        try {
            PrintStream out = new PrintStream(file1);
            out.println("Chromo\tPos\tData1\tData");
            out.println("chr1\t10\ta\t1");
            out.println("chr1\t100\tb\t2");
            out.println("chr1\t102\tc\t3");
            out.println("chr1\t104\td\t4");
            out.flush();
            out.close();

            out = new PrintStream(file2);
            out.println("Chromo\tPos\tData1\tData");
            out.println("chr1\t11\ta\tr1");
            out.println("chr1\t100\tb\tr2");
            out.println("chr1\t101\tc\tr3");
            out.println("chr1\t102\td\tr4");
            out.flush();
            out.close();

            out = new PrintStream(file3);
            out.println("Chromo\tPos\tData1\tData");
            out.println("chr1\t12\ta\ts1");
            out.println("chr1\t100\tb\ts2");
            out.println("chr1\t101\tc\ts3");
            out.println("chr1\t104\td\ts4");
            out.flush();
            out.close();

            List<SourceRef> list = new ArrayList<>();
            list.add(new SourceRef(file1.getAbsolutePath(), "First", null, null));
            list.add(new SourceRef(file2.getAbsolutePath(), "Second", null, null));
            list.add(new SourceRef(file3.getAbsolutePath(), "Third", null, null));

            final GenomicIterator source = new GorOptions(-1, 0, Integer.MAX_VALUE, true, 0, null, false, false, null, list, null,  null).getIterator();
            final String[] expected = {"chr1\t10\ta\t1\tFirst", "chr1\t11\ta\tr1\tSecond", "chr1\t12\ta\ts1\tThird", "chr1\t100\tb\t2\tFirst", "chr1\t100\tb\tr2\tSecond", "chr1\t100\tb\ts2\tThird", "chr1\t101\tc\tr3\tSecond", "chr1\t101\tc\ts3\tThird", "chr1\t102\tc\t3\tFirst", "chr1\t102\td\tr4\tSecond", "chr1\t104\td\t4\tFirst", "chr1\t104\td\ts4\tThird"};

            for (String line : expected) {
                Assert.assertTrue(source.hasNext());
                Assert.assertEquals(line, source.next().toString());
            }
            Assert.assertFalse(source.hasNext());
        } finally {
            file1.delete();
            file2.delete();
            file3.delete();
        }
    }

    /**
     * Test tags filter (-f, -ff -fs and -nf options) with extra filter items (items not in data).
     *
     * @throws Exception
     */
    @Test
    public void testTagsFilter() throws Exception {
        final int linesPerTag = 10;
        final int numTags = 10;
        final int numFiles = 100;

        // Create test data;
        GorDictionarySetup dict = new GorDictionarySetup("testTagsFilter", numFiles, 5, new int[]{1}, linesPerTag, true);

        ArrayList<String> tags = new ArrayList<>();
        for (int i = 1; i <= numTags; i++) {
            tags.add("PN" + i * numFiles / numTags);
        }

        // Single tag read.
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dict.dictionary, "PN1")).getIterator()) {
            Assert.assertEquals("Error in filtering single tags", linesPerTag, countRemainingLines(source));
        }

        // All tags.
        String filterTags = tags.stream().collect(Collectors.joining(","));
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dict.dictionary, filterTags)).getIterator()) {
            Assert.assertEquals("Error in filtering all tags", numTags * linesPerTag, countRemainingLines(source));
        }

        // Unknown tags

        tags.add("PN" + numFiles + 1);
        tags.add("SOME_BOGUS_TAG");
        filterTags = tags.stream().collect(Collectors.joining(","));

        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dict.dictionary, filterTags)).getIterator()) {
            while (source.next() != null) {
                // Don't care about the line
            }
            Assert.fail("Should throw error as we additional tags not in data.");
        } catch (GorException ge) {
            Assert.assertTrue(ge.getMessage().contains("Following are not in dictionary") && ge.getMessage().contains("SOME_BOGUS_TAG") && ge.getMessage().contains("PN" + numFiles + 1));
        }

        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s -fs", dict.dictionary, filterTags)).getIterator()) {
            Assert.assertEquals("Error in filtering", numTags * linesPerTag, countRemainingLines(source));
        }

        // Test no files selected.

        filterTags = "SOME_BOGUS_TAG";
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("%s -s PN -f %s -fs", dict.dictionary, filterTags)).getIterator()) {
            Assert.assertFalse(source.hasNext());
        }

        filterTags = "PNMA3";
        final String res = TestUtils.runGorPipe(String.format("gor %s -s PN -f %s -nf | where PN = '%s'", dict.dictionary, filterTags, filterTags));
        final String wantedRes = "Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tPNx\n" +
                "chr1\t2\tPNMA3\tLineData for the chromosome and position line 1 2\tThis line should be long enough for this test purpose\t280385\tMany\n";
        Assert.assertEquals(wantedRes, res);

        filterTags = "PNMB1";
        final String res2 = TestUtils.runGorPipe(String.format("%s -s PN -nf -f %s | where PN = '%s'", dict.dictionary, filterTags, filterTags));
        final String wantedRes2 = "Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tSource\tPNx\n" +
                "chr1\t10\tPNMB1\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t940393\tPNMB1\tMany\n";
        Assert.assertEquals(wantedRes2, res2);
    }

    /**
     * Test tags filter file (-ff -fs and -nf options) with extra filter items (items not in data).
     *
     * @throws Exception
     */
    @Category(SlowTests.class)
    @Test
    public void testTagsFilterFile() throws Exception {
        final int linesPerTag = 10;
        final int numTags = 10;
        final int numFiles = 100;

        // Create test data;
        GorDictionarySetup dict = new GorDictionarySetup("testTagsFilterFile", numFiles, 5, new int[]{1}, linesPerTag, true);

        ArrayList<String> tags = new ArrayList<>();
        for (int i = 1; i <= numTags; i++) {
            tags.add("PN" + i * numFiles / numTags);
        }

        final File tagfile = File.createTempFile("tagfile", ".tags");

        FileUtils.writeLines(tagfile, tags.subList(0, 1));

        // Single tag read.
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -ff %s", dict.dictionary, tagfile.toString())).getIterator()) {
            Assert.assertEquals("Error in filtering single tags", 1 * linesPerTag, countRemainingLines(source));
        }

        // All tags.
        FileUtils.writeLines(tagfile, tags);
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -ff %s", dict.dictionary, tagfile.toString())).getIterator()) {
            Assert.assertEquals("Error in filtering all tags", numTags * linesPerTag, countRemainingLines(source));
        }

        // Unknown tags
        tags.add("PN" + numFiles + 1);
        tags.add("SOME_BOGUS_TAG");
        FileUtils.writeLines(tagfile, tags);
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -ff %s", dict.dictionary, tagfile.toString())).getIterator()) {
            while (source.next() != null) {
                // Don't care about the line
            }
            Assert.fail("Should throw error as we additional tags not in data.");
        } catch (GorException ge) {
            Assert.assertTrue(ge.getMessage().contains("Following are not in dictionary") && ge.getMessage().contains("SOME_BOGUS_TAG") && ge.getMessage().contains("PN" + numFiles + 1));
        }

        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -ff %s -fs", dict.dictionary, tagfile.toString())).getIterator()) {
            Assert.assertEquals("Error in filtering", numTags * linesPerTag, countRemainingLines(source));
        }

        // Test no files selected
        tags = new ArrayList<>();
        tags.add("SOME_BOGUS_TAG");
        FileUtils.writeLines(tagfile, tags);
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("%s -s PN -ff %s -fs", dict.dictionary, tagfile.toString())).getIterator()) {
            Assert.assertFalse("Error in filtering", source.hasNext());
        }

        String pn = "PNMA3";
        tags = new ArrayList<>();
        tags.add(pn);
        FileUtils.writeLines(tagfile, tags);

        final String res = TestUtils.runGorPipe(String.format("gor %s -s PN -nf -ff %s | where PN = '%s'", dict.dictionary, tagfile.toString(), pn));
        final String wantedRes = "Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tPNx\n" +
                "chr1\t2\tPNMA3\tLineData for the chromosome and position line 1 2\tThis line should be long enough for this test purpose\t280385\tMany\n";
        Assert.assertEquals(wantedRes, res);

        pn = "PNMB1";
        tags = new ArrayList<>();
        tags.add(pn);
        FileUtils.writeLines(tagfile, tags);

        final String res2 = TestUtils.runGorPipe(String.format("gor %s -s PN -nf -ff %s | where PN = '%s'", dict.dictionary, tagfile.toString(), pn));
        final String wantedRes2 = "Chr\tPos\tPN\tChromoInfo\tConstData\tRandomData\tSource\tPNx\n" +
                "chr1\t10\tPNMB1\tLineData for the chromosome and position line 1 10\tThis line should be long enough for this test purpose\t940393\tPNMB1\tMany\n";
        Assert.assertEquals(wantedRes2, res2);
    }


    /**
     * Test tags filter (-f and -ff options) if mix of files and buckets.
     * See GOR-82.
     *
     * @throws Exception
     */
    @Test
    public void testTagsFilterIfMixOfFilesAndBuckets() throws Exception {
        /*
        The following query should return 3000 lines (but did beore fix GOR-82 return 2720)

        gorpipe "create #pns# = nor BCH_Nik_list.rep | top 300 ;
        gor -p chr1 wgs_varcalls.gord -s PN -ff [#pns#]"

        To the equivalent of this query using the GenomicOrderedRows object directly.

        */

        final int linesPerTag = 10;
        final int numTags = 300;     // Need minimum 300 to trigger the mixing.

        // Create test data;
        GorDictionarySetup dict = new GorDictionarySetup("testTagsFilterIfMixOfFilesAndBuckets", 700, 100, new int[]{1}, linesPerTag);

        // Get the tags for filtering. Pick the tags so that the first lines from the gord will be not picked from bucket (
        // plus no bucket should have more than 75% hit).
        ArrayList<String> tags = new ArrayList<>();
        for (int i = 0; i < numTags; i++) {
            tags.add("PN" + (100 + i * 2));
        }

        String filterTags = tags.stream().collect(Collectors.joining(","));

        // Read all the lines and compare with the expected count.
        // Note:  When reading the parameters GorOptions will transform the gord file into filelist with the underlying
        //        files from the gord file, and as we have 300 file we will go over the file limit and GorOptions will
        //        replace some of the files with the corresponding bucket file, hence creating a mixture of files and buckets.
        //        The GOR-82 error was that if the bucket was not the first file the filtering got screwed up (the field index
        //        for the filter field was wrong).
        try (final GenomicIterator source = GorOptions.createGorOptions(String.format("-p chr1 %s -s PN -f %s", dict.dictionary, filterTags)).getIterator()) {
            Assert.assertEquals("Check if result has the correct number of lines: ", numTags * linesPerTag, countRemainingLines(source));
        }
    }

    private ArrayList<SourceRef> tolist(String... items) {
        ArrayList<SourceRef> list = new ArrayList<SourceRef>();
        for (String s : items) {
            list.add(new SourceRef(s, s, null));
        }
        return list;
    }
}
