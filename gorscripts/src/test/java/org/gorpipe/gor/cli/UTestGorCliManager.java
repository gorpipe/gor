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

package org.gorpipe.gor.cli;

import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.gorpipe.gor.table.dictionary.BucketableTableEntry;
import org.gorpipe.test.GorDictionarySetup;
import org.gorpipe.test.SlowTests;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by gisli on 18/08/16.
 */
@Category(SlowTests.class)
// TODO:  This should really be marked as slow tests, but until we create that category.
@Ignore("Running all integration tests locally works, running only these tests inside container works, these tests fail when running all integration tests within a container")
public class UTestGorCliManager {

    private final GorCliManagerUtils testTableManagerUtil = new GorCliManagerUtils();

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;
    private String[] testFiles;

    @BeforeClass
    public static void setUp() throws Exception {

    }

    @Before
    public void setupTest() throws Exception {

        workDirPath = workDir.getRoot().toPath();

        int fileCount = 4;
        String[] sources = new String[]{"A", "B", "C", "D"};
        Map<String, List<String>> dataFiles = GorDictionarySetup.createDataFilesMap(
                "tableManagerCLI", workDirPath, fileCount, new int[]{1, 2, 3}, 10, "PN", true, sources);

        testFiles = new String[fileCount];

        testFiles[0] = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("A").get(0))).normalize().toString();
        testFiles[1] = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("B").get(0))).normalize().toString();
        testFiles[2] = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("C").get(0))).normalize().toString();
        testFiles[3] = workDirPath.toAbsolutePath().relativize(Paths.get(dataFiles.get("D").get(0))).normalize().toString();
    }

    @Test
    public void testErrorHandlingCLI() throws Exception {
        String name = "testErrorHandlingCLI";

        Path dictFile = workDirPath.resolve(name + ".gord");

        // Setup
        testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "A", dictFile.toString(), testFiles[0]}, workDirPath.toString(), true);


        // Commmand Error.
        try {
            testTableManagerUtil.executeGorManagerCommand( "isert", new String[]{"--alias", "B", dictFile.toString(), testFiles[0]}, workDirPath.toString(), true);
            Assert.fail("Should fail on bad command");
        } catch (Exception ee) {
            // Expected
            Assert.assertTrue(ee.getMessage().startsWith("BaseTable manager command failed with exit code"));
        }

        // Run Error
        try {
            testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "C", "nonexistend", dictFile.toString()}, workDirPath.toString(), true);
            Assert.fail("Should fail on bad file");
        } catch (ExecutionException ee) {
            // Expected
            Assert.assertTrue(ee.getMessage().startsWith("BaseTable manager command failed with exit code"));
        }

    }

    @Test
    public void testBasicCLI() throws Exception {
        String name = "testBasicCLI";

        Path dictFile = workDirPath.resolve(name + ".gord");

        testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "A", dictFile.toString(), testFiles[0]}, workDirPath.toString(), true);
        testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "B", dictFile.toString(), testFiles[1]}, workDirPath.toString(), true);
        testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "D", dictFile.toString(), testFiles[3]}, workDirPath.toString(), true);

        TableManager man = new TableManager();
        BaseDictionaryTable<BucketableTableEntry> table = man.initTable(dictFile);

        String result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[0] + "\tA\n" + testFiles[1] + "\tB\n" + testFiles[3] + "\tD\n", result);

        testTableManagerUtil.executeGorManagerCommand("delete", new String[]{"--tags", "B", dictFile.toString()}, workDirPath.toString(), true);
        table.reload();
        result = table.selectUninon(table.filter().tags("B")).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Delete failed", "", result);

        testTableManagerUtil.executeGorManagerCommand( "bucketize", new String[]{"-w", "1", "--min_bucket_size", "1", table.getPath().toString()}, ".", true);
        table.reload();
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());

        List<String> buckets = table.filter().get().stream().map(l -> l.getBucket()).distinct().collect(Collectors.toList());
        ArrayList<String> inputs = new ArrayList<>();
        inputs.add(table.getPath().toString());
        inputs.addAll(buckets);
        testTableManagerUtil.executeGorManagerCommand("delete_bucket", inputs.toArray(new String[0]), ".", true);
        table.reload();
        Assert.assertEquals("Delete buckets failed", 2, table.needsBucketizing().size());

        testTableManagerUtil.executeGorManagerCommand("delete", new String[]{"--tags", "A", table.getPath().toString()}, workDirPath.toString(), true);
        table.reload();
        result = table.selectUninon(table.filter().tags("A")).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Delete failed", "", result);
        Assert.assertEquals("Delete failed", 1, table.selectAll().size());

        testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "C", dictFile.toString(), testFiles[2]}, workDirPath.toString(), true);
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[2] + "\tC\n" + testFiles[3] + "\tD\n", result);
        Assert.assertEquals("Insert failed", 2, table.selectAll().size());

        Files.createDirectories(table.getRootPath().resolve("X"));
        Files.createDirectories(table.getRootPath().resolve("Y"));
        testTableManagerUtil.executeGorManagerCommand( "bucketize", new String[]{"-w", "1", "--min_bucket_size", "1", "--bucket_dirs", "X,Y", table.getPath().toString()}, ".", true);
        table.reload();
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
        List<Path> bucketFolders = table.filter().get().stream().map(l -> Paths.get(l.getBucket()).getParent()).distinct().collect(Collectors.toList());
        for (Path f : bucketFolders) {
            Assert.assertTrue("Invalid buckets", f.toString().matches("[XY]"));
        }
    }

    @Test
    public void testMultiInsertCLI() throws Exception {
        String name = "testMultiInsertCLI";

        Path dictFile = workDirPath.resolve(name + ".gord");
        TableManager man = new TableManager();
        BaseDictionaryTable<BucketableTableEntry> table = man.initTable(dictFile);
        String result;

        // Single file insert.
        testTableManagerUtil.executeGorManagerCommand("multiinsert", new String[]{"--aliases", "A", table.getPath().toString(), testFiles[0]}, ".", true);
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Incorrect multiinsert",
                testFiles[0] + "\tA\n", result);

        // Multi file insert with just files.
        table.delete(table.selectAll());
        testTableManagerUtil.executeGorManagerCommand( "multiinsert", new String[]{table.getPath().toString(), testFiles[0], testFiles[1], testFiles[2]}, ".", true);
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Incorrect multiinsert",
                testFiles[0] + "\n" +
                        testFiles[1] + "\n" +
                        testFiles[2] + "\n", result);

        // Multi file insert with just aliases options.
        table.delete(table.selectAll());
        testTableManagerUtil.executeGorManagerCommand( "multiinsert", new String[]{"--aliases", "A,B,C", table.getPath().toString(), testFiles[0], testFiles[1], testFiles[2]}, ".", true);
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Incorrect multiinsert",
                testFiles[0] + "\tA\n" +
                        testFiles[1] + "\tB\n" +
                        testFiles[2] + "\tC\n", result);

        // Multi file insert with all options.
        table.delete(table.selectAll());
        testTableManagerUtil.executeGorManagerCommand( "multiinsert", new String[]{"--tags", "A,B,C", "--ranges", "chr1,,chr3", "--aliases", ",2,3", table.getPath().toString(), testFiles[0], testFiles[1], testFiles[2]}, ".", true);
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Incorrect multiinsert",
                testFiles[0] + "\tA\tchr1\t-1\tchr1\t-1\n" +
                        testFiles[1] + "\t\t\t\t\t\t2,B\n" +
                        testFiles[2] + "\t\tchr3\t-1\tchr3\t-1\t3,C\n", result);

        // Multi file insert with incorrect number of files.
        table.delete(table.selectAll());
        try {
            testTableManagerUtil.executeGorManagerCommand( "multiinsert", new String[]{"--aliases", "A", table.getPath().toString(), testFiles[0], testFiles[1]}, ".", true);
            Assert.assertTrue("Should get exception if args differ", false);
        } catch (Exception e) {
            // Should go here.

        }

    }

    @Test
    public void testHistoryOptionCLI() throws Exception {
        String name = "testHistoryOptionCLI";

        // Test History option.
        String noHistDict = "noHistDict";
        testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "A", workDirPath.resolve(noHistDict + ".gord").toString(), testFiles[0]}, workDirPath.toString(), true);
        Assert.assertTrue(Files.exists(workDirPath.resolve("." + noHistDict).resolve(BaseDictionaryTable.HISTORY_DIR_NAME)));

        String noHistDict2 = "noHistDict2";
        testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "A", "--nohistory", workDirPath.resolve(noHistDict2 + ".gord").toString(), testFiles[0]}, workDirPath.toString(), true);
        Assert.assertTrue(!Files.exists(workDirPath.resolve("." + noHistDict2).resolve(BaseDictionaryTable.HISTORY_DIR_NAME)));

        String histDict2 = "histDict2";
        testTableManagerUtil.executeGorManagerCommand( "insert", new String[]{"--alias", "A", workDirPath.resolve(histDict2 + ".gord").toString(), testFiles[0]}, workDirPath.toString(), true);
        testTableManagerUtil.executeGorManagerCommand( "insert", new String[]{"--alias", "B", workDirPath.resolve(histDict2 + ".gord").toString(), testFiles[1]}, workDirPath.toString(), true);
        Assert.assertTrue(Files.exists(workDirPath.resolve("." + histDict2).resolve(BaseDictionaryTable.HISTORY_DIR_NAME)));
        Assert.assertTrue(Files.list(workDirPath.resolve("." + histDict2).resolve(BaseDictionaryTable.HISTORY_DIR_NAME)).count() == 2); // 1 header 1 gord file.

    }

    @Test
    public void testFlagTagskeyValueCLI() throws Exception {
        String name = "testFlagTagskeyValueCLI";
        Path dictFile = workDirPath.resolve(name + ".gord");

        //Insert 4 files with tags
        GorCLI.main(new String[]{"manager", "insert", "--alias", "A", dictFile.toString(), testFiles[0]});
        GorCLI.main(new String[]{"manager", "insert", "--alias", "B", dictFile.toString(), testFiles[1]});
        GorCLI.main(new String[]{"manager", "insert", "--alias", "C", dictFile.toString(), testFiles[2]});
        GorCLI.main(new String[]{"manager", "insert", "--alias", "D", dictFile.toString(), testFiles[3]});

        TableManager man = new TableManager();
        BaseDictionaryTable<BucketableTableEntry> table = man.initTable(dictFile);

        //Verify insert of files
        String result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[0] + "\tA\n" + testFiles[1] + "\tB\n" + testFiles[2] + "\tC\n" + testFiles[3] + "\tD\n", result);

        //Insert file 1 again but tag it the same as file 4 using the letter "D".
        //Using the --tagskey flag prevents the same tag to be used twice but instead the new line overwrites the old one.
        GorCLI.main(new String[]{"manager", "insert", "--alias", "D", "--tagskey", dictFile.toString(), testFiles[0]});
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[0] + "\tA\n" + testFiles[0] + "\tD\n" + testFiles[1] + "\tB\n" + testFiles[2] + "\tC\n", result);

        //Insert file 1 once again and tag it the same as file 2 using the letter "B"
        //This should be successful since the --tagskey option is not being used. There should now be two files tagged with "B".
        GorCLI.main(new String[]{"manager", "insert", "--alias", "B", dictFile.toString(), testFiles[0]});
        table.reload();
        //Total count of tags
        Assert.assertEquals(5, table.filter().get().stream().map(l -> l.getFilterTags()).distinct().count());
        //Count the tag "B"
        Assert.assertEquals(2, table.selectUninon(table.filter().tags("B")).stream().map(l -> l.formatEntry()).count());
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[0] + "\tA\n" + testFiles[0] + "\tB\n" + testFiles[0] + "\tD\n" + testFiles[1] + "\tB\n" + testFiles[2] + "\tC\n", result);
        //Try to insert tag that already exists in 2 or more lines in the dictfile. An exception should be thrown.
        try {
            testTableManagerUtil.executeGorManagerCommand("insert", new String[]{"--alias", "B", "--tagskey", dictFile.toString(), testFiles[3]}, workDirPath.toString(), true);
            Assert.fail("This should not fail");
        } catch (Exception re) {
            /* this is normal */
        }

        //Delete "B" so now there is only one remaining line with the same tag and it should be possible again to update the single existing line.
        GorCLI.main(new String[]{"manager", "delete", "--aliases", "B", dictFile.toString(), testFiles[1]});
        //Insert tag "B" again which should update the current "B" tagged line with this entry.
        GorCLI.main(new String[]{"manager", "insert", "--alias", "B", "--tagskey", dictFile.toString(), testFiles[3]});
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[0] + "\tA\n" + testFiles[0] + "\tD\n" + testFiles[2] + "\tC\n" + testFiles[3] + "\tB\n", result);
    }

    @Test
    public void testFlagTagskeyListCLI() throws Exception {
        String name = "testFlagTagskeyListCLI";
        Path dictFile = workDirPath.resolve(name + ".gord");

        TableManager man = new TableManager();
        BaseDictionaryTable<BucketableTableEntry> table = man.initTable(dictFile);

        //Check matching list of tags. If the same set of tags can be found it is replaced by the new line of tags.
        GorCLI.main(new String[]{"manager", "insert", "--alias", "A", "--tags", "GO,RC,OR", "--tagskey", dictFile.toString(), testFiles[0]});
        GorCLI.main(new String[]{"manager", "insert", "--alias", "A", "--tags", "GO,RC,OR", "--tagskey", dictFile.toString(), testFiles[1]});
        table.reload();
        String result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[1] + "\t\t\t\t\t\tGO,RC,OR,A\n", result);

        //Check a single tag against a list of tags to confirm that it is not enough to match a single tag in the list but the whole list must match.
        GorCLI.main(new String[]{"manager", "insert", "--tags", "GO", "--tagskey", dictFile.toString(), testFiles[1]});
        table.reload();
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[1] + "\t\t\t\t\t\tGO,RC,OR,A\n" + testFiles[1] + "\tGO\n", result);

        //Confirm the other flags such as range are ignored with the --tagskey flag. The second line "updates" the first line with range being ignored and tags only being used as an identifier.
        GorCLI.main(new String[]{"manager", "insert", "--alias", "B", "--tags", "GO,RC,OR", "--tagskey", "--range","chr1-chr3", dictFile.toString(), testFiles[2]});
        GorCLI.main(new String[]{"manager", "insert", "--alias", "B", "--tags", "GO,RC,OR", "--tagskey", dictFile.toString(), testFiles[3]});
        result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        table.reload();
        Assert.assertEquals("Insert failed", testFiles[1] + "\t\t\t\t\t\tGO,RC,OR,A\n" + testFiles[1] + "\tGO\n" + testFiles[3] + "\t\t\t\t\t\tGO,RC,OR,B\n", result);
    }

    @Test
    public void testDirectCLI() throws Exception {
        String name = "testDirectCLI";
        Path dictFile = workDirPath.resolve(name + ".gord");

        GorCLI.main(new String[]{"manager", "insert", "--alias", "A", dictFile.toString(), testFiles[0]});
        GorCLI.main(new String[]{"manager", "insert", "--alias", "B", dictFile.toString(), testFiles[1]});
        GorCLI.main(new String[]{"manager", "insert", "--alias", "D", dictFile.toString(), testFiles[3]});

        TableManager man = new TableManager();
        BaseDictionaryTable<BucketableTableEntry> table = man.initTable(dictFile);

        String result = table.selectUninon(table.filter()).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Insert failed", testFiles[0] + "\tA\n" + testFiles[1] + "\tB\n" + testFiles[3] + "\tD\n", result);

        GorCLI.main(new String[]{"manager", "delete", "--tags", "B", dictFile.toString()});
        table.reload();
        result = table.selectUninon(table.filter().tags("B")).stream().map(l -> l.formatEntry()).sorted().collect(Collectors.joining());
        Assert.assertEquals("Delete failed", "", result);

        GorCLI.main(new String[]{"manager", "bucketize", "-w", "1", "--min_bucket_size", "1", dictFile.toString()});
        table.reload();
        Assert.assertEquals("Not all lines bucketized", 0, table.needsBucketizing().size());
    }
}
    
