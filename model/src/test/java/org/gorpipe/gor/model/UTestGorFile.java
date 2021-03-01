/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.gor.model;

import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GorOptions;
import org.gorpipe.gor.model.Row;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class UTestGorFile {
    @Test
    public void icelandicChars() throws IOException {
        String line = "chr1\t1\tÞetta er próf - áéíóúýö";
        assertLineContentsAfterReadingFromFile(line);
    }

    @Test
    public void allKindsOfChars() throws IOException {
        String line = "chr1\t1\tÞetta er próf 이것은 시험이다. これはテストです 这是一个测试 Это тест";
        assertLineContentsAfterReadingFromFile(line);
    }

    private void assertLineContentsAfterReadingFromFile(String line) throws IOException {
        String contents = "Chrom\tPos\tText\n" + line + "\n";
        File directory = FileTestUtils.createTempDirectory("UTestGorFile");
        File tempFile = FileTestUtils.createTempFile(directory, "test.gor", contents);

        GorOptions options = GorOptions.createGorOptions(tempFile.getAbsolutePath());
        GenomicIterator iterator = options.getIterator();
        iterator.init(null);

        // todo: some iterators don't return a row on next unless hasNext is called
        iterator.hasNext();

        Row r = iterator.next();

        Assert.assertEquals(line, r.getAllCols().toString());
    }
}
