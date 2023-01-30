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

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;

public class UTestEmptyGorFile {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test(expected = GorResourceException.class)
    public void nonExistingFileShouldThrowAnError() throws IOException {
        TestUtils.runGorPipe("gor tmp.gor | top 1");

    }

    @Test(expected = GorResourceException.class)
    public void emptyGorFileShouldThrowAnError() throws IOException {
        var emptyFile = workDir.newFile("test.gor");
        TestUtils.runGorPipe(String.format("gor %s | top 1", emptyFile));
    }

    @Test(expected = GorResourceException.class)
    public void emptyGorzFileShouldThrowAnError() throws IOException {
        var emptyFile = workDir.newFile("test.gorz");
        TestUtils.runGorPipe(String.format("gor %s | top 1", emptyFile));
    }

    @Test(expected = GorResourceException.class)
    public void emptyNorFileShouldThrowAnError() throws IOException {
        var emptyFile = workDir.newFile("test.nor");
        TestUtils.runGorPipe(String.format("nor %s | top 1", emptyFile));
    }

    @Test(expected = GorResourceException.class)
    public void emptyNorFileWithHeaderOptionShouldThrowAnError() throws IOException {
        var emptyFile = workDir.newFile("test.nor");
        TestUtils.runGorPipe(String.format("nor -h %s | top 1", emptyFile));
    }
}
