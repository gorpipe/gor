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

package org.gorpipe.gor.table.util;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.table.util.TableLog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Unit tests for gor table logs.
 * <p>
 */
public class UTestTableLog {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private String workDirPath;
    private FileReader fileReader;

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().getPath();
        fileReader = ProjectContext.DEFAULT_READER;
    }

    @Test
    public void testTableLogAddLog() {
        TableLog tableLog = createSimpleTableLog();
        validateSimple(tableLog.unCommittedActions);
    }

    @Test
    public void testTableLogCommit() throws IOException {
        TableLog tableLog = createSimpleTableLog();
        tableLog.commit(fileReader);

        String tableLogFilePath = PathUtils.resolve(workDirPath, TableLog.LOG_FILE).toString();
        Assert.assertTrue("Log file was not created", fileReader.exists(PathUtils.formatUri(tableLogFilePath)));

        List<String> tableLogLines = Arrays.asList(fileReader.readAll(PathUtils.formatUri(tableLogFilePath)));
        validateSimple(tableLogLines.stream().map(l -> l + "\n").collect(Collectors.toList()));
    }

    @Test (expected = GorSystemException.class)
    public void testTableLogMissingLogDir() {
        TableLog tableLog = new TableLog("/non/existent/path");
        tableLog.commit(fileReader);
    }

    @Test (expected = GorSystemException.class)
    public void testTableCanNotSave() throws IOException {
        File tableLogFilePath = new File(PathUtils.resolve(workDirPath, TableLog.LOG_FILE));
        tableLogFilePath.createNewFile();
        tableLogFilePath.setWritable(false);

        TableLog tableLog = createSimpleTableLog();
        tableLog.commit(fileReader);
    }

    private TableLog createSimpleTableLog() {
        TableLog tableLog = new TableLog(workDirPath);

        tableLog.logAfter("1", TableLog.LogAction.INSERT, "ARG1",
                new DictionaryEntry.Builder("dummy1.gor", workDirPath).alias("A1").build().formatEntryNoNewLine());
        tableLog.logAfter("1", TableLog.LogAction.ADDTOBUCKET, "BUCKET1",
                new DictionaryEntry.Builder("dummy2.gor", workDirPath).bucket("BUCKET1").alias("A2").build().formatEntryNoNewLine());
        tableLog.logAfter("1", TableLog.LogAction.REMOVEFROMBUCKET, "BUCKET2",
                new DictionaryEntry.Builder("dummy3.gor", workDirPath).alias("A3").build().formatEntryNoNewLine());
        tableLog.logAfter("1", TableLog.LogAction.DELETE, "ARG2",
                new DictionaryEntry.Builder("dummy4.gor", workDirPath).alias("A4").build().formatEntryNoNewLine());
        return tableLog;
    }

    private void validateSimple(List<String> tableLogLines) {
        Assert.assertEquals(4, tableLogLines.size());
        Assert.assertEquals("1\t1\tINSERT\tARG1\tdummy1.gor\tA1\n", tableLogLines.get(0).split("\t", 2)[1]);
        Assert.assertEquals("1\t1\tADDTOBUCKET\tBUCKET1\tdummy2.gor|BUCKET1\tA2\n", tableLogLines.get(1).split("\t", 2)[1]);
        Assert.assertEquals("1\t1\tREMOVEFROMBUCKET\tBUCKET2\tdummy3.gor\tA3\n", tableLogLines.get(2).split("\t", 2)[1]);
        Assert.assertEquals("1\t1\tDELETE\tARG2\tdummy4.gor\tA4\n", tableLogLines.get(3).split("\t", 2)[1]);
    }
    
}
