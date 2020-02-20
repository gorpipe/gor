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

package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.model.genome.files.gor.GenomicIterator;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class UTestGorzSeekableIterator extends UTestSeekableGenomicIterator {

    public static File workDir;

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(workDir);
    }

    @Parameterized.Parameters(name = "Test file: {0}")
    public static Collection<Object[]> data() throws IOException {
        workDir = Files.createTempDirectory("uTestGorzSeekableIterator").toFile();
        final TestFileGenerator[] testFileGenerators = new TestFileGenerator[]{
                new TestFileGenerator("BASIC_GOR_FILE", workDir,10,1, NOT_SO_BIG_NUMBER, false),
                new TestFileGenerator("GOR_FILE_WITH_LONG_LINES", workDir,10, 1, BIG_NUMBER, false),
                new TestFileGenerator("GOR_FILE_WITH_MANY_LINES", workDir,5, 100, NOT_SO_BIG_NUMBER, false),
                new TestFileGenerator("PATHOLOGICAL_GOR_FILE", workDir,5, 10, BIG_NUMBER,true)
        };
        for (TestFileGenerator testFileGenerator : testFileGenerators) {
            testFileGenerator.writeFile(true);
        }
        return Arrays.stream(testFileGenerators).map(testFile -> new Object[]{testFile}).collect(Collectors.toList());
    }

    @Override
    public GenomicIterator getIterator(String filePath) {
        final StreamSourceSeekableFile file = new StreamSourceSeekableFile(new FileSource(new SourceReference(filePath)));
        return new GorzSeekableIterator(file);
    }
}
