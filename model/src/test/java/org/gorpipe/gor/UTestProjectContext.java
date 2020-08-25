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

import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.session.ProjectContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class UTestProjectContext {
    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder symbolicTarget = new TemporaryFolder();

    private ProjectContext projectContext;

    @Before
    public void setUp() throws IOException {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("user_data");
        locations.add("studies");
        projectContext = new ProjectContext.Builder()
                .setRoot(projectDir.getRoot().getCanonicalPath())
                .setWriteLocations(locations)
                .build();
        createSymbolicLink();
    }

    private void createSymbolicLink() throws IOException {
        String targetLocation = symbolicTarget.getRoot().getCanonicalPath();
        String userDataFolder = projectContext.getRealProjectRoot() + "/user_data";
        File dir = new File(userDataFolder);
        dir.mkdir();
        Path link = Paths.get(userDataFolder + "/shared");
        Path target = Paths.get(targetLocation);
        Files.createSymbolicLink(link, target);
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedEmpty() {
        projectContext.validateWriteAllowed("");
    }

    @Test
    public void isWriteAllowedValidPath() {
        projectContext.validateWriteAllowed("user_data/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteNotAllowedValidPath() {
        projectContext.validateWriteAllowed("user_data1/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedAbsolutePath() {
        projectContext.validateWriteAllowed("/user_data/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedWithDots() {
        projectContext.validateWriteAllowed("user_data/../test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedButHasDots() {
        projectContext.validateWriteAllowed("user_data/folder/../test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedWhenPrefixMatches() {
        projectContext.validateWriteAllowed("user_data2/folder/../test.gor");
    }

    @Test
    public void isWriteAllowedSymbolicLink() {
        projectContext.validateWriteAllowed("user_data/shared/test.gor");
    }

    @Test(expected = GorResourceException.class)
    public void isWriteAllowedSymbolicLinkDots() {
        projectContext.validateWriteAllowed("user_data/shared/../test.gor");
    }

    @Test
    public void getWritePath() throws IOException {
        String writePath = projectContext.getWritePath("user_data/test.gor");
        File writeFile = new File(writePath);
        String data = "data";
        FileUtils.writeStringToFile(new File(projectContext.getRealProjectRoot() + "/user_data/test.gor"), data, Charset.defaultCharset());
        Assert.assertEquals(data, FileUtils.readFileToString(writeFile, Charset.defaultCharset()));
    }

    @Test
    public void getWritePathSymbolic() throws IOException {
        String writePath = projectContext.getWritePath("user_data/shared/test.gor");
        File writeFile = new File(writePath);
        String data = "data";
        FileUtils.writeStringToFile(new File(symbolicTarget.getRoot().toString() + "/test.gor"), data, Charset.defaultCharset());
        Assert.assertEquals(data, FileUtils.readFileToString(writeFile, Charset.defaultCharset()));
    }
}