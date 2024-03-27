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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.gor.table.NorDictionaryTable;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by sigmar on 11/05/16.
 */
public class UTestNorDictFile {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        File gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
    }

    @Test
    public void loadGenericNordDict() throws IOException {
        var path = UTestNorDictionary.createTestFiles(10, 10, true, false, true, false);
        var nordDict = new NorDictionaryTable( Path.of(path, "test.nord").toString(), ProjectContext.DEFAULT_READER);
        Assert.assertEquals(10, nordDict.getEntries().size());
        Assert.assertEquals("phenotype", nordDict.getSourceColumn());
    }

    @Test()
    public void loadWithMissingNordDict() throws IOException {
        var path = UTestNorDictionary.createTestFiles(10, 10, true, false, true, false);
        var nordDict = new NorDictionaryTable(Path.of(path, "test_not_there.nord").toString(), ProjectContext.DEFAULT_READER);
        Assert.assertEquals(0, nordDict.getEntries().size());
    }

    @Test(expected = GorDataException.class)
    public void loadWithMissingFiles() throws IOException {
        var path = UTestNorDictionary.createTestFiles(1, 10, true, false, true, false);
        var nordDict = new NorDictionaryTable(Path.of(path, "test.nord").toString(), ProjectContext.DEFAULT_READER);
        nordDict.getOptimizedLines(new HashSet<>(Arrays.asList("a", "b", "c")), false, false);
    }
}
