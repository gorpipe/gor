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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Unit tests for gor table logs.
 * <p>
 */
public class UTestTableLog {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testTableLogAddLog() {
        TableLog tableLog = createSimpleTableLog();
        validateSimple(tableLog.unCommittedActions);
    }

    @Test
    public void testTableLogCommit() throws IOException {
        TableLog tableLog = createSimpleTableLog();
        tableLog.commit();

        Path tableLogFilePath = workDirPath.resolve(TableLog.LOG_FILE);
        Assert.assertTrue("Log file was not created", Files.exists(tableLogFilePath));

        List<String> tableLogLines = Files.readAllLines(tableLogFilePath);
        validateSimple(tableLogLines.stream().map(l -> l + "\n").collect(Collectors.toList()));
    }

    @Test (expected = GorSystemException.class)
    public void testTableLogMissingLogDir() {
        TableLog tableLog = new TableLog(Paths.get("/non/existent/path"));
        tableLog.commit();
    }

    @Test (expected = GorSystemException.class)
    public void testTableCanNotSave() throws IOException {
        File tableLogFilePath = workDirPath.resolve(TableLog.LOG_FILE).toFile();
        tableLogFilePath.createNewFile();
        tableLogFilePath.setWritable(false);

        TableLog tableLog = createSimpleTableLog();
        tableLog.commit();
    }

    private TableLog createSimpleTableLog() {
        TableLog tableLog = new TableLog(workDirPath);

        tableLog.logAfter(TableLog.LogAction.INSERT, "ARG1",
                new DictionaryEntry.Builder("dummy1.gor", workDirPath.toUri()).alias("A1").build());
        tableLog.logAfter(TableLog.LogAction.ADDTOBUCKET, "BUCKET1",
                new DictionaryEntry.Builder("dummy2.gor", workDirPath.toUri()).alias("A2").bucket("BUCKET1").build());
        tableLog.logAfter(TableLog.LogAction.REMOVEFROMBUCKET, "BUCKET2",
                new DictionaryEntry.Builder("dummy3.gor", workDirPath.toUri()).alias("A3").build());
        tableLog.logAfter(TableLog.LogAction.DELETE, "ARG2",
                new DictionaryEntry.Builder("dummy4.gor", workDirPath.toUri()).alias("A4").build());
        return tableLog;
    }

    private void validateSimple(List<String> tableLogLines) {
        Assert.assertEquals(4, tableLogLines.size());
        Assert.assertEquals("INSERT\tARG1\tdummy1.gor\tA1\n", tableLogLines.get(0).split("\t", 2)[1]);
        Assert.assertEquals("ADDTOBUCKET\tBUCKET1\tdummy2.gor|BUCKET1\tA2\n", tableLogLines.get(1).split("\t", 2)[1]);
        Assert.assertEquals("REMOVEFROMBUCKET\tBUCKET2\tdummy3.gor\tA3\n", tableLogLines.get(2).split("\t", 2)[1]);
        Assert.assertEquals("DELETE\tARG2\tdummy4.gor\tA4\n", tableLogLines.get(3).split("\t", 2)[1]);
    }
    
}
