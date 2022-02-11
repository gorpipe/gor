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
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class UTestLocalFileCacheTest {
    DriverBackedFileReader fileReader;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    void startup() {
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
        String storedFile = client.store(Paths.get(filePath), fingerPrint, extension, 0);
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
        String storedFile = client1.store(Paths.get(filePath), fingerPrint, extension, 0);
        Assert.assertTrue(storedFile.contains(fingerPrint));

        LocalFileCacheClient client2 = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString());
        String lookedupFile = client2.lookupFile(fingerPrint);
        Assert.assertTrue(lookedupFile.contains(fingerPrint));
    }

    @Test
    public void testStoreInSubfoldersSameFingerPrint() throws IOException {
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString(), true, 3);
        client.store(workDir.newFile("fingerprint1.gor").toPath(),"fingerprint1", ".gor", 0);
        client.store(workDir.newFile("fingerprint2.gor").toPath(),"fingerprint2", ".gor", 0);
        client.store(workDir.newFile("fingerprint3.gor").toPath(),"fingerprint3", ".gor", 0);
        client.store(workDir.newFile("fingerprint4.gor").toPath(),"fingerprint4", ".gor", 0);
        client.store(workDir.newFile("fingerprint4.gor").toPath(),"fingerprint4", ".gor", 0);

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
        client.store(workDir.newFile("test1.gor").toPath(),"fin01gerprint1", ".gor", 0);
        client.store(workDir.newFile("test2.gor").toPath(),"fin02gerprint2", ".gor", 0);
        client.store(workDir.newFile("test3.gor").toPath(),"fin03gerprint3", ".gor", 0);
        client.store(workDir.newFile("test4.gor").toPath(),"fin04gerprint4", ".gor", 0);
        client.store(workDir.newFile("test4.gor").toPath(),"fin05gerprint4", ".gor", 0);

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
            Assert.assertEquals("One file in directory", 2, subFolderFileList.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStoreInSubfoldersWithTooShortFingerprint() throws IOException {
        LocalFileCacheClient client = new LocalFileCacheClient(fileReader, workDir.getRoot().toPath().toString(), true, 5);
        client.store(workDir.newFile("test1.gor").toPath(),"fin01gerprint1", ".gor", 0);
        client.store(workDir.newFile("test2.gor").toPath(),"fi01", ".gor", 0);
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
        String storedFile = client.store(Paths.get(filePath), fingerPrint, extension, 0);
        Assert.assertTrue(storedFile.contains(fingerPrint));

        File tmpFilePath = workDir.newFile("test.header.gor");
        client.storeSibling(tmpFilePath.toPath(), fingerPrint);

        String[] baseFilesList = workDir.getRoot().list();
        Assert.assertNotNull(baseFilesList);
        Assert.assertEquals("Only one directory",2, baseFilesList.length);
    }
}
