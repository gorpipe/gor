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
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class UTestGorHeaderRowMismatch {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void extraColumnInFirstRow() throws IOException {
        String contents = "Chrom\tPos\tdata\nchr1\t1\tbingo\tbongo";
        File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "extraColumnInFirst.gor", contents);
        String query = String.format("gor %s | where data='bingo'", tempFile);

        expected.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void extraColumnInSecondRow() throws IOException {
        String contents = "Chrom\tPos\tdata\nchr1\t1\t3\nchr1\t2\t4\t5";
        File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "extraColumnInSecondRow.gor", contents);
        String query = String.format("gor %s | where data='bingo'", tempFile);

        expected.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void missingColumnFromFirstRow() throws IOException {
        String contents = "Chrom\tPos\tdata1\tdata2\nchr1\t1\tbingo";
        File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "missingColumnFromFirstRow.gor", contents);
        String query = String.format("gor %s | where data1='bingo'", tempFile);

        expected.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }

    @Test
    public void missingColumnFromSecondRow() throws IOException {
        String contents = "Chrom\tPos\tdata1\tdata2\nchr1\t1\t3\t4\nchr1\t2\t5";
        File tempFile = FileTestUtils.createTempFile(workDir.getRoot(), "missingColumnFromFirstRow.gor", contents);
        String query = String.format("gor %s | where data1='bingo'", tempFile);

        expected.expect(GorDataException.class);
        TestUtils.runGorPipe(query);
    }
}
