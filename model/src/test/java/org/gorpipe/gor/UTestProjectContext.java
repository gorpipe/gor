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
import org.gorpipe.gor.model.DriverBackedGorServerFileReader;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class UTestProjectContext {
    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder symbolicTarget = new TemporaryFolder();

    private DriverBackedGorServerFileReader fileReader;

    @Before
    public void setUp() throws IOException {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("user_data");
        locations.add("studies");
        fileReader = new DriverBackedGorServerFileReader(projectDir.getRoot().getCanonicalPath(), null,false , "", locations);
        createSymbolicLink();
    }

    private void createSymbolicLink() throws IOException {
        String targetLocation = symbolicTarget.getRoot().getCanonicalPath();
        String userDataFolder = fileReader.getCommonRoot() + "/user_data";
        File dir = new File(userDataFolder);
        dir.mkdir();
        Path link = Paths.get(userDataFolder + "/shared");
        Path target = Paths.get(targetLocation);
        Files.createSymbolicLink(link, target);
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedEmpty() {
        fileReader.validateWriteAccess("");
    }

    @Test
    public void isWriteAllowedValidPath() {
        fileReader.validateWriteAccess("user_data/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteNotAllowedValidPath() {
        fileReader.validateWriteAccess("user_data1/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedAbsolutePath() {
        fileReader.validateWriteAccess("/user_data/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedWithDots() {
        fileReader.validateWriteAccess("user_data/../test.gor");
    }

    @Test(expected = GorResourceException.class)
    @Ignore("This test is no longer valid as up traversal is now allowed as long as it doesnt go out of project scope")
    public void isWriteAllowedButHasDots() {
        fileReader.validateWriteAccess("user_data/folder/../test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedWhenPrefixMatches() {
        fileReader.validateWriteAccess("user_data2/folder/../test.gor");
    }

    @Test
    public void isWriteAllowedSymbolicLink() {
        fileReader.validateWriteAccess("user_data/shared/test.gor");
    }

    @Test(expected = GorResourceException.class)
    @Ignore("Dots not allowed")
    public void isWriteAllowedSymbolicLinkDots() {
        fileReader.validateWriteAccess("user_data/shared/../test.gor");
    }
}