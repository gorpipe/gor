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

package org.gorpipe.gor.model;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.DefaultFileReader;
import org.gorpipe.gor.model.RacFile;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class UTestDefaultFileReader {
    DefaultFileReader reader;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private final String text = "This is a test\nof some text\nin a file.";


    @Before
    public void setUp() {
        reader = new DefaultFileReader("bingo");
    }

    @Test
    public void getSecurityContext() {
        assertEquals("bingo", reader.getSecurityContext());
    }

    @Test
    public void checkValidServerFileName() {
        reader.checkValidServerFileName("bongo");
        // All we care about is that this didn't throw an exception
    }

    @Test
    public void readAll() throws IOException {
        final String[] lines = reader.readAll(getFile().getAbsolutePath());
        assertEquals(text, String.join("\n", lines));
    }

    @Test
    public void readAll_throwsExceptionWhenInvalidFile() {
        thrown.expect(GorSystemException.class);
        reader.readAll("thisDoesNotExists.txt");
    }

    @Test
    public void readHeaderLine() throws IOException {
        final String header = reader.readHeaderLine(getFile().getAbsolutePath());
        assertEquals("This is a test", header);
    }

    @Test
    public void readHeaderLine_throwsExceptionWhenInvalidFile() {
        thrown.expect(GorSystemException.class);
        reader.readHeaderLine("thisDoesNotExists.txt");
    }

    @Test
    public void getReader() throws IOException {
        final BufferedReader bufferedReader = this.reader.getReader(getFile().getAbsolutePath());
        final String firstLine = bufferedReader.readLine();
        assertEquals("This is a test", firstLine);
    }

    @Test
    public void iterateFile() throws IOException {
        try (Stream<String> stream = reader.iterateFile(getFile().getAbsolutePath(), 0, false,false)) {
            final List<String> lines = stream.collect(Collectors.toList());
            assertEquals(3, lines.size());
        }
    }

    @Test
    public void iterateFileOnDirectory() throws IOException {
        try (Stream<String> stream = reader.iterateFile(workDir.getRoot().getAbsolutePath(), 0, false, false)) {
            final List<String> lines = stream.collect(Collectors.toList());
            assertTrue(lines.size() >= 1);
        }
    }

    @Test
    public void getDirectoryStream() throws IOException {
        final Stream<String> directoryStream = DefaultFileReader.getDirectoryStream(0, false, false, workDir.getRoot().toPath(), workDir.getRoot().toPath());
        final List<String> list = directoryStream.collect(Collectors.toList());
        assertTrue(list.size() >= 1);
    }

    @Test
    public void getDirectoryStreamWithModificationDate() throws IOException {
        final Stream<String> directoryStream = DefaultFileReader.getDirectoryStream(0, false, true, workDir.getRoot().toPath(), workDir.getRoot().toPath());
        final List<String> list = directoryStream.collect(Collectors.toList());
        assertTrue(list.size() >= 1);
    }

    @Test
    public void openFile() throws IOException {
        final RacFile racFile = reader.openFile(getFile().getAbsolutePath());
        final long length = racFile.length();
        racFile.close();
        assertEquals(text.length(), length);
    }

    @Test
    public void toPath() throws IOException {
        final Path path = reader.toPath(getFile().getAbsolutePath());
        assertEquals(getFile().toPath(), path);
    }

    @Test
    public void getReaderWithPath() throws IOException {
        final BufferedReader bufferedReader = this.reader.getReader(getFile().toPath());
        final String firstLine = bufferedReader.readLine();
        assertEquals("This is a test", firstLine);
    }

    @Test
    public void getDictionarySignature() throws IOException {
        final String signature = reader.getDictionarySignature("abc", null);
        assertFalse(signature.isEmpty());
    }

    @Test
    public void getFileSignature() throws IOException {
        final String fileSignature = reader.getFileSignature(getFile().getAbsolutePath());
        // File signature is based on time stamp (among other things), making it hard to check specifically here
        assertFalse(fileSignature.isEmpty());
    }

    private File getFile() throws IOException {
        return FileTestUtils.createTempFile(workDir.getRoot(), "test.txt", text);
    }
}