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

import gorsat.process.NordFile;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.session.ProjectContext;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public void loadGenericNordFile() throws IOException {
        var path = UTestNorDictionary.createTestFiles(10, 10, true, false, true, false);
        var nordFile = new NordFile();
        nordFile.load(ProjectContext.DEFAULT_READER, Path.of(path, "test.nord"), false, new String[0], false);

        Assert.assertEquals(10, nordFile.entries().size());
        Assert.assertEquals(1, nordFile.properties().size());
    }

    @Test(expected = GorResourceException.class)
    public void loadWithMissingNordFile() throws IOException {
        var path = UTestNorDictionary.createTestFiles(10, 10, true, false, true, false);
        var nordFile = new NordFile();
        nordFile.load(ProjectContext.DEFAULT_READER, Path.of(path, "test_not_there.nord"), false, new String[0], false);
    }

    @Test(expected = GorParsingException.class)
    public void loadWithMissingFiles() throws IOException {
        var path = UTestNorDictionary.createTestFiles(1, 10, true, false, true, false);
        var nordFile = new NordFile();
        nordFile.load(ProjectContext.DEFAULT_READER, Path.of(path, "test.nord"), true, new String[] {"a", "b", "c"}, false);
    }

    @Test
    public void loadFromFileList() throws IOException {
        var path = UTestNorDictionary.createTestFiles(10, 1, true, false, true, false);
        var nordFile = NordFile.fromList(Files.list(Path.of(path, "files")).map(Path::toString).toArray(String[]::new));
        Assert.assertEquals(10, nordFile.entries().size());
        Assert.assertEquals(0, nordFile.properties().size());
    }
}
