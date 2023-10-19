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

import org.gorpipe.exceptions.GorSecurityException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.AccessControlContext;
import org.gorpipe.gor.model.DriverBackedSecureFileReader;
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

    private DriverBackedSecureFileReader fileReader;

    @Before
    public void setUp() throws IOException {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("user_data");
        locations.add("studies");
        fileReader = new DriverBackedSecureFileReader(projectDir.getRoot().getCanonicalPath(), "",
                AccessControlContext.builder().withWriteLocations(locations).build());

        File userDataDir = new File(fileReader.getCommonRoot() + "/user_data");
        userDataDir.mkdir();
        File otherDir = new File(fileReader.getCommonRoot() + "/user_data/folder");
        otherDir.mkdir();

        Path sharedDirPath = sharedDir.getRoot().toPath();
        Path projectDirPath = projectDir.getRoot().toPath();

        Path sharedFilePath =  sharedDirPath.resolve("shared1.gor");

        Files.writeString(sharedFilePath, "#chrom\tpos\tref\nchr1\t1\tA\n");

        Files.writeString(projectDirPath.resolve("user_data/shared2.gor"), "#chrom\tpos\tref\nchr1\t1\tB\n");
        Files.writeString(projectDirPath.resolve("test.gor"), "#chrom\tpos\tref\nchr1\t1\tC\n");
        Files.writeString(projectDirPath.resolve("user_data/test.gor"), "#chrom\tpos\tref\nchr1\t1\tD\n");

        createProjectToSharedSymbolicLink("shared1_symboliclink.gor", "shared1.gor");
        createProjectToSharedGorLink("shared1_gorlink.gor.link", "shared1.gor");

        createProjectToSharedSymbolicLink("user_data/shared1_symboliclink.gor", "shared1.gor");
        createProjectToSharedGorLink("user_data/shared1_gorlink.gor.link", "shared1.gor");

        Files.writeString(projectDirPath.resolve("absolutelink.gor.link"), "/some/absolute/gorfile.gorz");
        Files.writeString(projectDirPath.resolve("s3link.gor.link"), "s3://gdb-unit-test-data/csa_test_data/data_sets/gor_driver_testfiles/dummy.gor");
        Files.writeString(projectDirPath.resolve("dblink.rep.link"), "//db:select * from rda.v_all_rep all_rep where all_rep.project_id = #{project-id}");

        Files.writeString(sharedDirPath.resolve("unaccessiable.gor.link"), "/some/absolute/gorfile.gorz");
        Files.writeString(sharedDirPath.resolve("unaccessiable.rep.link"), "//db:select * from rda.v_all_rep all_rep where all_rep.project_id = #{project-id}");

    }

    private void createProjectToSharedSymbolicLink(String link, String target) throws IOException {
        Path sharedDirPath = sharedDir.getRoot().toPath();
        Path projectDirPath = projectDir.getRoot().toPath();
        Files.createSymbolicLink(projectDirPath.resolve(link), sharedDirPath.resolve(target));
    }

    private void createProjectToSharedGorLink(String link, String target) throws IOException {
        Path targetLocation = sharedDir.getRoot().toPath();
        Path projectDirPath = projectDir.getRoot().toPath();
        Files.write(projectDirPath.resolve(link), targetLocation.resolve(target).toString().getBytes(StandardCharsets.UTF_8));
    }


    // Read

    private void validateAccessAndRead(String url) {
        fileReader.resolveUrl(url, false);
        try {
            fileReader.getReader(url).readLine();
        } catch (Exception e) {
            throw new GorSystemException("Failed", e);
        }
    }

    @Test
    public void isReadAllowedValidPath() {
        validateAccessAndRead("test.gor");
    }

    @Test
    public void isReadNotAllowedValidPath() {
        Assert.assertThrows(GorSecurityException.class, () -> validateAccessAndRead("../test.gor"));
    }

    @Test
    public void isReadAllowedAbsolutePath() {
        Assert.assertThrows(GorSecurityException.class, () -> validateAccessAndRead("/test.gor"));
    }

    @Test
    public void isReadAllowedWithDots() {
        validateAccessAndRead("user_data/../test.gor");
    }

    @Test
    public void isReadAllowedSharedSymbolicLink() {
        validateAccessAndRead("shared1_symboliclink.gor");
    }

    @Test
    public void isReadAllowedFromUserDataSharedSymbolicLink() {
        validateAccessAndRead("user_data/shared1_symboliclink.gor");
    }
    

    @Test
    public void isReadAllowedSharedGorLink() {
        validateAccessAndRead("shared1_gorlink.gor");
    }

    @Test
    public void isReadAllowedSharedGorLinkWithLink() {
        validateAccessAndRead("shared1_gorlink.gor.link");
    }

    @Test
    public void isReadAllowedFromUserDataSharedGorLink() {
        validateAccessAndRead("user_data/shared1_gorlink.gor");
    }

    @Test
    public void isReadAllowedFromLinkToAbsolute() {
        fileReader.resolveUrl("absolutelink.gor", false);
    }

    @Ignore("Fails as the linked file does not exists.  Did never work, the s3 driver was just nota active")
    @Test
    public void isReadAllowedFromLinkToS3() {
        fileReader.resolveUrl("s3link.gor", false);
    }

    @Test
    public void isReadAllowedFromLinkToDb() {
        fileReader.resolveUrl("dblink.rep", false);
    }

    @Test
    public void isReadAllowedFromUnaccessibleGorLink() {
        // Should not be able to access, links with absolute paths (what ever they point to)
        Assert.assertThrows(GorSecurityException.class, () -> fileReader.resolveUrl(sharedDir.getRoot().toPath().resolve("unaccessiable.gor.link").toString(), false));
    }

    @Test
    public void isReadAllowedFromUnaccessibleDbLink() {
        // Should not be able to access, links with absolute paths (what ever they point to)
        Assert.assertThrows(GorSecurityException.class, () -> fileReader.resolveUrl(sharedDir.getRoot().toPath().resolve("unaccessiable.rep.link").toString(), false));
    }

    @Test
    public void isReadAllowedFromUnaccessibleGorLinkRelativePath() {
        // Should not be able to access, links with relative paths outside project root (what ever they point to)
        Path releativePath = projectDir.getRoot().toPath().relativize(sharedDir.getRoot().toPath());
        Assert.assertThrows(GorSecurityException.class, () -> fileReader.resolveUrl(releativePath.resolve("unaccessiable.gor.link").toString(), false));
    }

    // Write

    private void validateWriteAccess(String url) {
        fileReader.resolveUrl(url, true);
        try {
            fileReader.getOutputStream(url, false).write("#chrom\tpos\tref\nchr2\t2\tX\n".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new GorSystemException("Failed", e);
        }
    }

    @Test
    public void isWriteAllowedEmpty() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess(""));
    }

    @Test
    public void isWriteAllowedValidPath() {
        validateWriteAccess("user_data/test.gor");
    }

    @Test
    public void isWriteNotAllowedValidPath() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess("user_data1/test.gor"));
    }

    @Test
    public void isWriteAllowedAbsolutePath() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess("/user_data/test.gor"));
    }

    @Test
    public void isWriteAllowedWithDots() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess("user_data/../test.gor"));
    }

    @Test
    public void isWriteAllowedButHasDots() {
        validateWriteAccess("user_data/folder/../test.gor");
    }

    @Test
    public void isWriteAllowedWhenPrefixMatches() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess("user_data2/folder/../test.gor"));
    }

    @Test
    public void isWriteAllowedSharedSymbolicLink() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess("shared1_symboliclink.gor"));
    }

    @Test
    public void isWriteAllowedFromUserDataSharedSymbolicLink() {
        validateWriteAccess("user_data/shared1_symboliclink.gor");
    }

    @Test
    public void isWriteAllowedSharedGorLink() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess("shared1_gorlink.gor"));
    }

    @Test
    public void isWriteAllowedFromUserDataSharedGorLink() {
        validateWriteAccess("user_data/shared1_gorlink.gor");
    }

    @Test
    public void isWriteCreateGorLink() {
        Assert.assertThrows(GorSecurityException.class, () -> validateWriteAccess("user_data/custom.gor.link"));
    }


}