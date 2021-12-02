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

package org.gorpipe.gor;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.PluggableGorDriver;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.model.DriverBackedGorServerFileReader;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class UTestProjectContext {
    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder sharedDir = new TemporaryFolder();

    private DriverBackedGorServerFileReader fileReader;

    @Before
    public void setUp() throws IOException {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("user_data");
        locations.add("studies");
        fileReader = new DriverBackedGorServerFileReader(projectDir.getRoot().getCanonicalPath(), null,false , "", locations);

        File userDataDir = new File(fileReader.getCommonRoot() + "/user_data");
        userDataDir.mkdir();
        File otherDir = new File(fileReader.getCommonRoot() + "/user_data/folder");
        otherDir.mkdir();

        Path sharedDirPath = sharedDir.getRoot().toPath();
        Path projectDirPath = projectDir.getRoot().toPath();

        Files.write(sharedDirPath.resolve("shared1.gor"), "#chrom\tpos\tref\nchr1\t1\tA\n".getBytes(StandardCharsets.UTF_8));
        Files.write(projectDirPath.resolve("user_data/shared2.gor"), "#chrom\tpos\tref\nchr1\t1\tB\n".getBytes(StandardCharsets.UTF_8));
        Files.write(projectDirPath.resolve("test.gor"), "#chrom\tpos\tref\nchr1\t1\tC\n".getBytes(StandardCharsets.UTF_8));
        Files.write(projectDirPath.resolve("user_data/test.gor"), "#chrom\tpos\tref\nchr1\t1\tD\n".getBytes(StandardCharsets.UTF_8));


        createSymbolicLink("shared1_symboliclink.gor", "shared1.gor");
        createGorLink("shared1_gorlink.gor.link", "shared1.gor");

        createSymbolicLink("user_data/shared1_symboliclink.gor", "shared1.gor");
        createGorLink("user_data/shared1_gorlink.gor.link", "shared1.gor");
    }

    private void createSymbolicLink(String link, String target) throws IOException {
        Path sharedDirPath = sharedDir.getRoot().toPath();
        Path projectDirPath = projectDir.getRoot().toPath();
        Files.createSymbolicLink(projectDirPath.resolve(link), sharedDirPath.resolve(target));
    }

    private void createGorLink(String link, String target) throws IOException {
        Path targetLocation = sharedDir.getRoot().toPath();
        Path projectDirPath = projectDir.getRoot().toPath();
        Files.write(projectDirPath.resolve(link), targetLocation.resolve(target).toString().getBytes(StandardCharsets.UTF_8));
    }


    // Read

    private void validateAccess(String url) {
        fileReader.validateAccess(PluggableGorDriver.instance().getDataSource(new SourceReference(url)));
        try {
            fileReader.getReader(url).readLine();
        } catch (Exception e) {
            throw new GorSystemException("Failed", e);
        }
    }

    @Test
    public void isReadAllowedValidPath() {
        validateAccess("test.gor");
    }


    @Test(expected = GorResourceException.class)
    public void isReadNotAllowedValidPath() {
        validateAccess("../test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isReadAllowedAbsolutePath() {
        validateAccess("/test.gor");
    }

    @Test
    public void isReadAllowedWithDots() {
        validateAccess("user_data/../test.gor");
    }

    @Test
    public void isReadAllowedSharedSymbolicLink() {
        validateAccess("shared1_symboliclink.gor");
    }

    @Test
    public void isReadAllowedFromUserDataSharedSymbolicLink() {
        validateAccess("user_data/shared1_symboliclink.gor");
    }
    

    @Test
    public void isReadAllowedSharedGorLink() {
        validateAccess("shared1_gorlink.gor");
    }

    @Test
    public void isReadAllowedFromUserDataSharedGorLink() {
        validateAccess("user_data/shared1_gorlink.gor");
    }

    // Write

    private void validateWriteAccess(String url) {
        fileReader.validateWriteAccess(url);
        try {
            fileReader.getOutputStream(url, false).write("#chrom\tpos\tref\nchr2\t2\tX\n".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new GorSystemException("Failed", e);
        }

    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedEmpty() {
        validateWriteAccess("");
    }

    @Test
    public void isWriteAllowedValidPath() {
        validateWriteAccess("user_data/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteNotAllowedValidPath() {
        validateWriteAccess("user_data1/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedAbsolutePath() {
        validateWriteAccess("/user_data/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedWithDots() {
        validateWriteAccess("user_data/../test.gor");
    }

    @Test
    public void isWriteAllowedButHasDots() {
        validateWriteAccess("user_data/folder/../test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedWhenPrefixMatches() {
        validateWriteAccess("user_data2/folder/../test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedSharedSymbolicLink() {
        validateWriteAccess("shared1_symboliclink.gor");
    }

    @Test
    public void isWriteAllowedFromUserDataSharedSymbolicLink() {
        validateWriteAccess("user_data/shared1_symboliclink.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedSharedGorLink() {
        validateWriteAccess("shared1_gorlink.gor");
    }

    @Test
    public void isWriteAllowedFromUserDataSharedGorLink() {
        validateWriteAccess("user_data/shared1_gorlink.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteCreateGorLink() {
        validateWriteAccess("user_data/custom.gor.link");
    }


}