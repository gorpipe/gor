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

import gorsat.Commands.CommandParseUtilities;
import gorsat.Macros.PartGor;
import gorsat.Script.ExecutionBlock;
import gorsat.Script.MacroInfo;
import gorsat.Script.MacroParsingResult;
import gorsat.process.GenericSessionFactory;
import gorsat.process.GorPipeMacros;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.manager.BucketManager;
import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.test.GorDictionarySetup;
import org.gorpipe.test.SlowTests;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Category(SlowTests.class)
public class UTestPartGor {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @BeforeClass
    static public void setupTests() {
        GorPipeMacros.register();

        //ScriptExecutionEngine engine = new ScriptExecutionEngine(null, null, null, null);
    }

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void partGorFullTestFixedPN() throws IOException {
        String name = "partGorFullTestFixedPN";
        final int NUMBER_OF_FILES = 1000;

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, NUMBER_OF_FILES + 1).mapToObj(i -> String.format("PN%d", i)).toArray(String[]::new);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, NUMBER_OF_FILES, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        man.setMinBucketSize(10);
        man.setBucketSize(50);
        GorDictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        Path dictionaryPath = Path.of(table.getPath());

        // Create the partgor
        String query = "partgor -dict " + dictionaryPath.toAbsolutePath().toString() + " -f " + String.join(",", sources) +
                " <(gor " + dictionaryPath.toAbsolutePath().toString() + " -f #{tags})";

        MacroInfo info = GorPipeMacros.getInfo("partgor").get();
        String[] dependencies = {};
        String[] args = CommandParseUtilities.quoteSafeSplit(query, ' ');
        String[] finalArgs = Arrays.copyOfRange(args, 1, args.length);

        MacroParsingResult result =  info.init("[test]", new ExecutionBlock("[group1]", query, null, dependencies, "group1", null,true, false),
                new GenericSessionFactory().create().getGorContext(), false, finalArgs, false);

        Assert.assertEquals(11, result.createCommands().size());

        var count =  result.createCommands().keySet().stream().filter(x -> x.contains("test_")).count();
        Assert.assertEquals(10, count);

        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(30000, lines.length);

        query = query + " | top 10";
        lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(100, lines.length);
    }

    @Test
    public void partGorWithDictWithHeader() throws IOException {
        String contents = "#col1\tcol2\tcol3\tcol4\tcol5\tcol6\tlis_PN\n" +
                "data/bucket_1.gorz\tbucket_1\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000001\n" +
                "data/bucket_10.gorz\tbucket_10\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000010\n" +
                "data/bucket_11.gorz\tbucket_11\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000011\n" +
                "data/bucket_12.gorz\tbucket_12\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000012\n" +
                "data/bucket_13.gorz\tbucket_13\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000013\n" +
                "data/bucket_14.gorz\tbucket_14\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000014\n" +
                "data/bucket_15.gorz\tbucket_15\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000015\n" +
                "data/bucket_16.gorz\tbucket_16\tchr1\t0\tchrZ\t1000000000\tSAMPLE_SIM842_000016\n";

        String expected = "chrom\tbpStart\tbpStop\tx\n" +
                "chr1\t1\t1\t'SAMPLE_SIM842_000001','SAMPLE_SIM842_000010','SAMPLE_SIM842_000011','SAMPLE_SIM842_000012','SAMPLE_SIM842_000013','SAMPLE_SIM842_000014','SAMPLE_SIM842_000015','SAMPLE_SIM842_000016'\n";

        File dictFile = FileTestUtils.createTempFile(workDir.getRoot(), "variants.gord", contents);

        String query = "create #x = partgor -dict " + dictFile.getAbsolutePath() + " <(gorrow chr1,1,1| calc x \"#{tags:q}\");gor [#x]";
        String results = TestUtils.runGorPipe(query);

        Assert.assertEquals(expected, results);
    }

    @Test
    public void partGorFullTestVirtualFilePnSinglePartition() throws IOException {
        String name = "partGorFullTestVirtualFilePnSinglePartition";
        final int NUMBER_OF_FILES = 1000;

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, NUMBER_OF_FILES +1 ).mapToObj(i -> String.format("PN%d", i)).toArray(String[]::new);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, NUMBER_OF_FILES, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        man.setMinBucketSize(10);
        man.setBucketSize(50);
        GorDictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        Path dictionaryPath = Path.of(table.getPath());

        // Create the partgor
        String query = "create pns = norrows 100 -offset 1 | calc Source 'PN'+str(#1) | select #2 | signature -timeres 1; partgor -dict " + dictionaryPath.toAbsolutePath().toString() + " -ff <(nor [pns]) " +
                " <(gor " + dictionaryPath.toAbsolutePath().toString() + " -f #{tags})";

        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(3000, lines.length);

        query = query + " | top 10";
        lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(10, lines.length);
    }

    @Test
    public void partGorFullTestVirtualFilePnMultiplePartitions() throws IOException {
        String name = "partGorFullTestVirtualFilePnMultiplePartitions";
        final int NUMBER_OF_FILES = 1000;

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, NUMBER_OF_FILES + 1).mapToObj(i -> String.format("PN%d", i)).toArray(String[]::new);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, NUMBER_OF_FILES, new int[]{1, 2, 3}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        man.setMinBucketSize(10);
        man.setBucketSize(50);
        GorDictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        Path dictionaryPath = Path.of(table.getPath());

        // Create the partgor
        String query = "create pns = norrows 500 -offset 1 | calc Source 'PN'+str(#1) | select #2 | signature -timeres 1; partgor -dict " + dictionaryPath.toAbsolutePath().toString() + " -ff <(nor [pns]) " +
                " <(gor " + dictionaryPath.toAbsolutePath().toString() + " -f #{tags})";

        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(15000, lines.length);

        query = query + " | top 10";
        lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(50, lines.length);
    }

    @Test
    public void partGorFullTestVirtualFileDirectAccess() throws IOException {
        String name = "partGorFullTestVirtualFileDirectAccess";
        final int NUMBER_OF_FILES = 10000;

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, NUMBER_OF_FILES + 1).mapToObj(i -> String.format("PN%d", i)).toArray(String[]::new);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, NUMBER_OF_FILES, new int[]{1}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        man.setMinBucketSize(10);
        man.setBucketSize(50);
        GorDictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        Path dictionaryPath = Path.of(table.getPath());

        // Create the partgor
        String query = "create pns = norrows 2000 -offset 4000 | calc Source 'PN'+str(#1) | select #2 | signature -timeres 1; partgor -dict " + dictionaryPath.toAbsolutePath().toString() + " -ff [pns] " +
                " <(pgor " + dictionaryPath.toAbsolutePath().toString() + " -f #{tags})";

        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(20000, lines.length);

        query = query + " | top 10";
        lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(200, lines.length);
    }

    @Test
    public void partGorFullTestVirtualWithBuckets() throws IOException {
        String name = "partGorFullTestVirtualWithBuckets";
        final int NUMBER_OF_FILES = 10000;

        Path dataDir = workDirPath.resolve("data");
        Files.createDirectory(dataDir);
        String[] sources = IntStream.range(1, NUMBER_OF_FILES + 1).mapToObj(i -> String.format("PN%d", i)).toArray(String[]::new);
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                name, dataDir, NUMBER_OF_FILES, new int[]{1}, 10, "PN", true, sources);

        TableManager man = new TableManager();
        man.setMinBucketSize(10);
        man.setBucketSize(50);
        GorDictionaryTable table = TestUtils.createDictionaryWithData(name, workDirPath, dataFiles);
        String dictionaryPath = table.getPath();

        man.bucketize(dictionaryPath, BucketManager.BucketPackLevel.NO_PACKING, 8, 10000, null);

        // Create the partgor
        String query = "create pns = norrows 1000 -offset 1000 | calc Source 'PN'+str(#1) | select #2 | signature -timeres 1; partgor -dict "
                + Path.of(dictionaryPath).toAbsolutePath().toString() + " -ff [pns] "
                + " <(gor " + Path.of(dictionaryPath).toAbsolutePath().toString() + " -f #{tags})";

        String[] lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(10000, lines.length);

        query = query + " | top 10";
        lines = TestUtils.runGorPipeLinesNoHeader(query);
        Assert.assertEquals(220, lines.length);
    }

    @Test
    public void testPartGorSingleQuotedReplacement() {
        String[] tags = {"a", "b", "c"};
        String result = PartGor.replaceTags("partgor -dict multicolumns.gor -f a,b,c <(pgor -f #{tags:q} bar.gord)", tags);
        Assert.assertEquals("partgor -dict multicolumns.gor -f a,b,c <(pgor -f 'a','b','c' bar.gord)", result);

    }

    @Test
    public void testPartGorDoubleQuotedReplacement() {
        String[] tags = {"a", "b", "c"};
        String result = PartGor.replaceTags("partgor -dict multicolumns.gor -f a,b,c <(pgor -f #{tags:dq} bar.gord)", tags);
        Assert.assertEquals("partgor -dict multicolumns.gor -f a,b,c <(pgor -f \"a\",\"b\",\"c\" bar.gord)", result);

    }

    @Test(expected = GorParsingException.class)
    public void testPartGorIllegalQuotedReplacement() {
        String[] tags = {"a", "b", "c"};
        PartGor.replaceTags("partgor -dict multicolumns.gor -f a,b,c <(pgor -f #{tags:d} bar.gord)", tags);
    }

    @Test(expected = GorParsingException.class)
    public void testPartGorNoTagsReplacement() {
        String[] tags = {"a", "b", "c"};
        PartGor.replaceTags("some nested query no tags", tags);
    }

    @Test
    public void testPartGorEmptyTagsReplacement() {
        String[] tags = {};
        String result = PartGor.replaceTags("#{tags} | #{tags:q} | #{tags:dq}", tags);
        Assert.assertEquals(" |  | ", result);
    }

    @Test
    @Ignore("We don't support mixed case yet.")
    public void testPartGorMixedCaseTagsReplacement() {
        String[] tags = {"a", "b", "c"};
        String result = PartGor.replaceTags("#{tags} | #{TAGS} | #{TagS}", tags);
        Assert.assertEquals("a,b,c | a,b,c | a,b,c", result);
    }

    @Test
    public void testPartGorRepeatedTagsReplacement() {
        String[] tags = {"a", "b", "c"};
        String result = PartGor.replaceTags("#{tags} | #{tags}", tags);
        Assert.assertEquals("a,b,c | a,b,c", result);
    }

    @Test
    public void testPartGorMixedTagsReplacement() {
        String[] tags = {"a", "b", "c"};
        String result = PartGor.replaceTags("#{tags} | #{tags:q} | #{tags:dq}", tags);
        Assert.assertEquals("a,b,c | 'a','b','c' | \"a\",\"b\",\"c\"", result);
    }



}
