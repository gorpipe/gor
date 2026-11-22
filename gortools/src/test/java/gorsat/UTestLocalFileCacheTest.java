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

import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.clients.LocalFileCacheClient;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
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
import java.util.Arrays;
import java.util.List;

public class UTestLocalFileCacheTest {
    DriverBackedFileReader fileReader;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void startup() {
        fileReader = new DriverBackedFileReader("");
    }

    @Test
    public void testEmptyLookupShouldBeNull() {
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String file = client.lookupFile("test123");
        Assert.assertNull(file);
    }

    @Test
    public void testTempFileLocation() {
        String fingerPrint = "test123";
        String extension = ".gor";
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String filePath = client.tempLocation(fingerPrint, extension);
        Assert.assertTrue(filePath.contains(fingerPrint));
    }

    @Test
    public void testStoreAndLookup() throws IOException {
        String fingerPrint = "test123";
        String extension = ".gor";
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String filePath = client.tempLocation(fingerPrint, extension);

        FileUtils.writeStringToFile(new File(filePath), "This is test", Charset.defaultCharset());
        String storedFile = client.store(Paths.get(filePath), fingerPrint, extension, 0, "");
        Assert.assertTrue(storedFile.contains(fingerPrint));

        String lookedupFile = client.lookupFile(fingerPrint);
        Assert.assertTrue(lookedupFile.contains(fingerPrint));
    }

    @Test
    public void testStoreAndLookupNotInCache() throws IOException {
        String fingerPrint = "test123";
        String extension = ".gor";
        LocalFileCacheClient client1 = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());

        String filePath = client1.tempLocation(fingerPrint, extension);
        Assert.assertTrue(filePath.contains(fingerPrint));

        FileUtils.writeStringToFile(new File(filePath), "This is test", Charset.defaultCharset());
        String storedFile = client1.store(Paths.get(filePath), fingerPrint, extension, 0, "");
        Assert.assertTrue(storedFile.contains(fingerPrint));

        LocalFileCacheClient client2 = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String lookedupFile = client2.lookupFile(fingerPrint);
        Assert.assertTrue(lookedupFile.contains(fingerPrint));
    }

    @Test
    public void testStoreInSubfoldersSameFingerPrint() throws IOException {
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString(), true, 3);
        client.store(workDir.newFile("fingerprint1.gor").toPath(),"fingerprint1", ".gor", 0, "");
        client.store(workDir.newFile("fingerprint2.gor").toPath(),"fingerprint2", ".gor", 0, "");
        client.store(workDir.newFile("fingerprint3.gor").toPath(),"fingerprint3", ".gor", 0, "");
        client.store(workDir.newFile("fingerprint4.gor").toPath(),"fingerprint4", ".gor", 0, "");
        client.store(workDir.newFile("fingerprint4.gor").toPath(),"fingerprint4", ".gor", 0, "");

        // Should get 4 files, 1 directory
        String[] baseFilesList = workDir.getRoot().list();
        Assert.assertNotNull(baseFilesList);
        Assert.assertEquals("Only one directory",1, baseFilesList.length);
        Assert.assertTrue("Directory name should be fin", baseFilesList[0].endsWith("fin"));

        String[] subFolderFileList = (new File(workDir.getRoot(), "/fin")).list();
        Assert.assertNotNull(subFolderFileList);
        Assert.assertEquals("Four files in directory",4, subFolderFileList.length);
        List<String> subItemList = Arrays.asList(subFolderFileList);
        Assert.assertTrue(subItemList.contains("fingerprint1.gor"));
        Assert.assertTrue(subItemList.contains("fingerprint2.gor"));
        Assert.assertTrue(subItemList.contains("fingerprint3.gor"));
        Assert.assertTrue(subItemList.contains("fingerprint4.gor"));
    }

    @Test
    public void testStoreInSubfoldersDifferentFingerPrint() throws IOException {
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString(), true, 5);
        client.store(workDir.newFile("test1.gor").toPath(),"fin01gerprint1", ".gor", 0, "");
        client.store(workDir.newFile("test2.gor").toPath(),"fin02gerprint2", ".gor", 0, "");
        client.store(workDir.newFile("test3.gor").toPath(),"fin03gerprint3", ".gor", 0, "");
        client.store(workDir.newFile("test4.gor").toPath(),"fin04gerprint4", ".gor", 0, "");
        client.store(workDir.newFile("test4.gor").toPath(),"fin05gerprint4", ".gor", 0, "");

        // Should get 4 files, 1 directory
        String[] baseFilesList = workDir.getRoot().list();
        Assert.assertNotNull(baseFilesList);
        Assert.assertEquals("Only one directory",5, baseFilesList.length);
        List<String> itemList = Arrays.asList(baseFilesList);
        Assert.assertTrue(itemList.contains("fin01"));
        Assert.assertTrue(itemList.contains("fin02"));
        Assert.assertTrue(itemList.contains("fin03"));
        Assert.assertTrue(itemList.contains("fin04"));
        Assert.assertTrue(itemList.contains("fin05"));

        for(String subItem : itemList) {
            String[] subFolderFileList = (new File(workDir.getRoot(), subItem)).list();
            Assert.assertNotNull(subFolderFileList);
            Assert.assertEquals("One file in directory", 1, subFolderFileList.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStoreInSubfoldersWithTooShortFingerprint() throws IOException {
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString(), true, 5);
        client.store(workDir.newFile("test1.gor").toPath(),"fin01gerprint1", ".gor", 0, "");
        client.store(workDir.newFile("test2.gor").toPath(),"f01", ".gor", 0, "");
    }

    @Test
    public void testStoreSiblingAndLookup() throws IOException {
        String fingerPrint = "test123";
        String extension = ".gor";
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String file = client.lookupFile(fingerPrint);
        Assert.assertNull(file);

        String filePath = client.tempLocation(fingerPrint, extension);
        Assert.assertTrue(filePath.contains(fingerPrint));

        FileUtils.writeStringToFile(new File(filePath), "This is test", Charset.defaultCharset());
        String storedFile = client.store(Paths.get(filePath), fingerPrint, extension, 0, "");
        Assert.assertTrue(storedFile.contains(fingerPrint));

        File tmpFilePath = workDir.newFile("test.header.gor");
        client.storeSibling(tmpFilePath.toPath(), fingerPrint);

        String[] baseFilesList = workDir.getRoot().list();
        Assert.assertNotNull(baseFilesList);
        Assert.assertEquals("Only one directory",2, baseFilesList.length);
    }

    @Test
    public void testLookupOfTempTempFilebasedOnFingerPrint() throws IOException {
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString(), true, 5);
        client.store(workDir.newFile("test1.gor").toPath(),"fin01gerprint1", ".gor", 0, "");
        client.store(workDir.newFile("test2temptempfile.gor").toPath(),"fin02gerprint2", "", 0, "");
        client.store(workDir.newFile("test3.gor").toPath(),"fin03gerprint3", ".gor", 0, "");

        // Should get 0 files, 3 directory
        String[] baseFilesList = workDir.getRoot().list();
        Assert.assertNotNull(baseFilesList);
        Assert.assertEquals("Only one directory",3, baseFilesList.length);

        // Should get null when accessing fin02gerprint2
        var fin02 = client.lookupFile("fin02gerprint2");
        Assert.assertNull(fin02);

        // The temp file is left in place: it may be another writer's in-flight output (ENGKNOW-4296)
        var fin02File = new File(workDir.getRoot(), "test2/test2temptempfile.gor");
        Assert.assertTrue(fin02File.exists());
        Assert.assertNull(client.lookupFile("fin02gerprint2"));
    }

    /**
     * Two creates with the same query text share a fingerprint.  The first one writes its result to a temp file
     * next to the final cache file (<fingerprint>.<random>.temptempfile.gorz).  When the second one looks up the
     * fingerprint it must not find, nor delete, that in-flight temp file (or its .meta), otherwise the first
     * writer's temp -> final move fails with NoSuchFileException (ENGKNOW-4296).
     */
    @Test
    public void testLookupDoesNotDeleteInFlightTempFile() throws IOException {
        String fingerPrint = "test123";
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String finalPath = client.tempLocation(fingerPrint, ".gorz");
        File tempFile = new File(DataUtil.toTempTempFile(finalPath));
        File tempMetaFile = new File(DataUtil.toFile(tempFile.getPath(), DataType.META));
        FileUtils.writeStringToFile(tempFile, "in flight", Charset.defaultCharset());
        FileUtils.writeStringToFile(tempMetaFile, "## in flight", Charset.defaultCharset());

        Assert.assertNull("An in-flight temp file is not a cache hit", client.lookupFile(fingerPrint));
        Assert.assertTrue("In-flight temp file must not be deleted", tempFile.exists());
        Assert.assertTrue("In-flight temp meta file must not be deleted", tempMetaFile.exists());
    }

    /**
     * Files written in place (e.g. dictionary results) go through FileSource's atomic write, which writes to
     * <fingerprint>-temp-<random>.gord and renames on close.  That partial file must not be a cache hit
     * (ENGKNOW-4296).
     */
    @Test
    public void testLookupIgnoresInFlightAtomicWriteTempFile() throws IOException {
        String fingerPrint = "test123";
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        Path finalPath = Paths.get(client.tempLocation(fingerPrint, ".gord"));
        Path atomicTemp = PathUtils.getTempFilePath(finalPath);
        Assert.assertTrue(atomicTemp.getFileName().toString().startsWith(fingerPrint + "-temp-"));
        FileUtils.writeStringToFile(atomicTemp.toFile(), "partial", Charset.defaultCharset());

        Assert.assertNull("An in-flight atomic-write temp file is not a cache hit", client.lookupFile(fingerPrint));

        Files.move(atomicTemp, finalPath);
        Assert.assertEquals(finalPath.toString(), client.lookupFile(fingerPrint));
    }

    /**
     * A lookup that ran while another writer's temp file existed must not leave that temp file in the in-memory
     * cache: once the writer moves it into place, the next lookup finds the final file (ENGKNOW-4296).
     */
    @Test
    public void testLookupFindsFinalFileAfterInFlightTempFileIsMoved() throws IOException {
        String fingerPrint = "test123";
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String finalPath = client.tempLocation(fingerPrint, ".gorz");
        File tempFile = new File(DataUtil.toTempTempFile(finalPath));
        FileUtils.writeStringToFile(tempFile, "in flight", Charset.defaultCharset());

        Assert.assertNull(client.lookupFile(fingerPrint));

        Files.move(tempFile.toPath(), Paths.get(finalPath));

        Assert.assertEquals(finalPath, client.lookupFile(fingerPrint));
    }
}
