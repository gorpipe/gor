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
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.dictionary.TableFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;


/**
 * Unit tests for gor table filter.
 * <p>
 */
public class UTestTableFilter {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testTestFilesDifferentPath() throws IOException {
        String testName = "testTestSameBucketDifferentPath";
        File gordFile = createDictOne(workDirPath, testName);

        DictionaryTable dict = new DictionaryTable(gordFile.toPath());

        // Files
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(0),
                selectStringFilter(dict, dict.filter().files(testName + "_pn1.gor")).trim());
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(0),
                selectStringFilter(dict, dict.filter().files(workDirPath.resolve(testName + "_pn1.gor").toString())).trim());
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(3),
                selectStringFilter(dict, dict.filter().files("/abs/path/" + testName + "_pn4.gor")).trim());
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(3),
                selectStringFilter(dict, dict.filter().files("/abs/path/" + testName + "_pn4.gor")).trim());

        // Buckets
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(1) + "\n" + FileUtils.readLines(gordFile, "UTF-8").get(0),
                selectStringFilter(dict, dict.filter().buckets(testName + "_bucket1.gor")).trim());
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(1) + "\n" + FileUtils.readLines(gordFile, "UTF-8").get(0),
                selectStringFilter(dict, dict.filter().buckets( testName + "_bucket1.gor")).trim());
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(3) + "\n" + FileUtils.readLines(gordFile, "UTF-8").get(2),
                selectStringFilter(dict, dict.filter().buckets("/abs/path/" + testName + "_bucket2.gor")).trim());
        Assert.assertEquals(FileUtils.readLines(gordFile, "UTF-8").get(3) + "\n" + FileUtils.readLines(gordFile, "UTF-8").get(2),
                selectStringFilter(dict, dict.filter().buckets("/abs/path/" + testName + "_bucket2.gor")).trim());

    }

    // Setup data.  4 files two buckets.
    private File createDictOne(Path workDirPath, String testName) throws IOException {

        File gordFile = new File(workDirPath.toFile(), testName + ".gord");
        FileUtils.write(gordFile,
                testName + "_pn1.gor|" + testName + "_bucket1.gor\tpn1\n"
                        + "/abs/path/"  + testName + "_pn2.gor" + "|" + testName + "_bucket1.gor\tpn2\n"
                        + testName + "_pn3.gor|" + "/abs/path/" + testName + "_bucket2.gor\tpn3\n"
                        + "/abs/path/"  + testName + "_pn4.gor" + "|" + "/abs/path/"  + testName + "_bucket2.gor" + "\tpn4\n"
                , "UTF-8");

        return gordFile;
    }

    @SafeVarargs
    private final String selectStringFilter(DictionaryTable table, TableFilter<DictionaryEntry>... filters) {
        return table.selectUninon(filters).stream().map(DictionaryEntry::formatEntry).sorted().collect(Collectors.joining());
    }
}
