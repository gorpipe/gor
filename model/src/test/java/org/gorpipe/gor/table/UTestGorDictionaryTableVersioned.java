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

package org.gorpipe.gor.table;

import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.gorpipe.gor.table.dictionary.gor.GorDictionaryTableMeta.HEADER_BUCKETIZE_KEY;

/**
 * Unit tests for gor table.
 * <p>
 * Created by gisli on 03/01/16.
 */
public class UTestGorDictionaryTableVersioned {

    private static Path tableWorkDir;
    private static String gort1;

    @Before
    public void setUp() throws Exception {
        tableWorkDir = Files.createTempDirectory("UnitTestGorTableWorkDir");

        for (int i = 1; i < 25; i++) {
            Files.createFile(tableWorkDir.resolve(String.format("filepath%d.gor", i)));
        }

        gort1 = "filepath1.gor\n" +
                "filepath2.gor\ttagA\n" +
                "filepath3.gor\ttagB\n" +
                "filepath4.gor\t\tchr1\t10000\tchr1\t30000\ttagD,tagE\n" +
                "filepath5.gor\ttagF\tchr1\t10000\tchr1\t20000\t\n" +
                "filepath6.gor\ttagF\tchr1\t30000\tchr2\t10000\t\n" +
                "filepath7.gor\t\tchr3\t10000\tchr4\t10000\ttagF1,tagF2\n" +
                "filepath8.gor\ttagA\n" +
                "filepath9.gor|bucket1\ttagG\n" +
                "filepath10.gor|bucket1\ttagH\n" +
                "filepath11.gor|bucket2\ttagI\n" +
                "filepath12.gor|bucket2\t\tchr1\t1\tchr2\t20000\ttagJ,tagK\n" +
                "filepath13.gor|bucket2\n" +
                "filepath14.gor|D|bucket2\ttagL\n" +
                "filepath15.gor|D|bucket2\n" +
                "filepath16.gor\ttagD\n" +
                "filepath17.gor\ttagB\n" +
                "filepath18.gor\t\t\t\t\t\ttagJ,tagM\n" +
                "filepath19.gor\ttagK\n";
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tableWorkDir.toFile());
    }

    @Test
    public void testTableCreation() {
        String tableName = "gortable_table_creation";
        Path gordFile = tableWorkDir.resolve(tableName + ".gord");

        GorDictionaryTable dict = new GorDictionaryTable.Builder<>(PathUtils.markAsFolder(gordFile.toString())).build();
        dict.save();

        Assert.assertEquals("Path check failed", gordFile.toAbsolutePath(), Path.of(dict.getFolderPath()));

        dict = new GorDictionaryTable.Builder<>(gordFile).build();
        Assert.assertEquals("Path check failed", gordFile.toAbsolutePath(), Path.of(dict.getFolderPath()));

        Assert.assertEquals(null, dict.getBooleanConfigTableProperty(HEADER_BUCKETIZE_KEY, null));
    }


    @Test
    public void testTableSaveLoad() throws IOException {
        String tableName = "gortable_table_load";
        Path gordFile = tableWorkDir.resolve(tableName + ".gord");
        Files.createDirectory(gordFile);
        Files.write(gordFile.resolve("version.v1.gord"), gort1.getBytes());
        Files.write(gordFile.resolve("thedict.gord.link"), "## VERSION = 1\nversion.v1.gord".getBytes());

        GorDictionaryTable dict = new GorDictionaryTable.Builder<>(gordFile).build();
        dict.save();

        Assert.assertNotEquals(gordFile.resolve("thedict.v1.gord"), dict.getPath());
        String savedContent = Files.readString(Path.of(dict.getPath()));
        Assert.assertEquals("Content not loaded or saved correctly", gort1, savedContent);
    }

    @Test
    public void testRepeatedSaves() {
        String tableName = "gortable_repeated_saves";
        Path gordFile = new File(tableWorkDir.toFile(), tableName + ".gord").toPath();

        GorDictionaryTable dict = new GorDictionaryTable.Builder<>(gordFile).embeddedHeader(false).build();
        dict.save();
        dict.save();

        GorDictionaryTable dict2 = new GorDictionaryTable.Builder<>(gordFile).embeddedHeader(false).build();
        dict2.reload();
        Assert.assertEquals("Dicts are different", dict.getEntries(), dict2.getEntries());
    }

    @Test
    public void testCreateSimple() {
        // Add one file.
        String tableName = "gortable_create_simple";
        String dataFileName = Paths.get("../tests/data/gor/genes.gor").toAbsolutePath().toString();
        GorDictionaryTable dict = new GorDictionaryTable.Builder<>(
                PathUtils.markAsFolder(tableWorkDir.resolve(tableName + ".gord").toString())).build();

        dict.insert(dataFileName);
        dict.save();

        Assert.assertTrue("BaseTable file was not created", Files.exists(Path.of(dict.getPath())));
        Assert.assertTrue("Link file was not created", Files.exists(Path.of(dict.getLinkPath())));

        Assert.assertFalse("Logging dir should not be created", new File(tableWorkDir.toFile(), tableName + ".log").exists());
        Assert.assertArrayEquals("Columns def not correct", new String[]{"Chrom", "gene_start", "gene_end", "Gene_Symbol"}, dict.getColumns());
    }
}
