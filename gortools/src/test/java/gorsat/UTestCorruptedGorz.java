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

import org.gorpipe.exceptions.GorResourceException;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by sigmar on 27/04/16.
 */
public class UTestCorruptedGorz {

    private static Path workDirPath;

    @BeforeClass
    public static void setUp() throws Exception {
        workDirPath = Files.createTempDirectory("uTestCorruptedGorz");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(workDirPath.toFile());
    }

    @Test
    public void testCorruptedGorz() {
        String query = "gor ../tests/data/external/samtools/corrupted.bam.gorz | top 10";
        boolean success = false;
        try {
            TestUtils.runGorPipeCount(query);
        } catch (Exception e) {
            success = true;
        }
        Assert.assertEquals("No error on corrupted gorz", true, success);
    }

    @Test
    public void testCorruptedGor() {
        String query = "gor ../tests/data/corrupted.bam.gor | top 10";
        boolean success = false;
        try {
            TestUtils.runGorPipeCount(query);
        } catch (Exception e) {
            success = true;
        }
        Assert.assertEquals("No error on corrupted gor", true, success);
    }

    @Test
    @Ignore("We are not validating Gorz files now but when we start doing that we should stop ignoring this test.")
    public void testUncompressedGorzFile() throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new FileReader("../tests/data/gor/dbsnp_test.gor"));
        final File notCompressedGorzFile = new File(workDirPath.toFile(), "notCompressedGorzFile.gorz");
        final FileWriter fileWriter = new FileWriter(notCompressedGorzFile);

        bufferedReader.lines().forEach(line -> {
            try {
                fileWriter.write(line + "\n");
            } catch (Exception e) {}
        });
        fileWriter.close();


        String[] args = {"gor " + notCompressedGorzFile.getAbsolutePath()};
        boolean success = false;
        try {
            TestUtils.runGorPipe(args);
        } catch (GorResourceException e) {
            success = true;
        }
        Assert.assertEquals(true, success);
    }

    @Test
    public void testOnlyHeader() throws Exception {
        final File headerOnly = new File(workDirPath.toFile(), "headerOnly.gorz");
        final FileWriter fileWriter = new FileWriter(headerOnly);
        fileWriter.write("chrom\tpos");
        fileWriter.close();
        String[] args = {"gor " + headerOnly.getAbsolutePath()};
        boolean success = true;
        try {
            TestUtils.runGorPipe(args);
        } catch (Exception e) {
            success = false;
        }
        Assert.assertEquals(true, success);
    }

    @Test
    public void testOnlyHeader2() throws Exception {
        final File headerOnly = new File(workDirPath.toFile(), "headerOnly2.gorz");
        final FileWriter fileWriter = new FileWriter(headerOnly);
        fileWriter.write("#chrom\tpos\n");
        fileWriter.close();
        String[] args = {"gor " + headerOnly.getAbsolutePath()};
        boolean success = true;
        try {
            TestUtils.runGorPipe(args);
        } catch (Exception e) {
            success = false;
        }
        Assert.assertEquals(true, success);
    }

    @Test
    public void testNormalFile() {
        String[] args = {"gor ../tests/data/gor/genes.gorz"};
        boolean success = true;
        try {
            TestUtils.runGorPipe(args);
        } catch (Exception e) {
            success = false;
        }
        Assert.assertEquals(true, success);
    }

    @Test
    public void testColumnCompressed() throws  Exception {
        final File headerOnlyColumnCompressed = new File(workDirPath.toFile(), "headerOnlyColumnCompress.gorz");
        final FileWriter fileWriter = new FileWriter(headerOnlyColumnCompressed);
        fileWriter.write("chrom\tpos\tid\tvalues\tbla\tblabla\tblablabla?Y/$'!!!#!%!");
        fileWriter.close();
        String[] args = {"gor " + headerOnlyColumnCompressed.getAbsolutePath()};
        boolean success = true;
        try {
            TestUtils.runGorPipe(args);
        } catch (Exception e) {
            success = false;
        }
        Assert.assertEquals(true, success);
    }

    @Test
    public void testColumnCompressedWithHashTag() throws Exception {
        final File headerOnlyColumnCompressedWithHashTag = new File(workDirPath.toFile(), "headerOnlyColumnCompressWithHashTag.gorz");
        final FileWriter fileWriter = new FileWriter(headerOnlyColumnCompressedWithHashTag);
        fileWriter.write("chrom\tpos\tid\tvalues\tbla\tblabla\tblablabla?Y/$'!!!#!%!");
        fileWriter.close();
        String[] args = {"gor " + headerOnlyColumnCompressedWithHashTag.getAbsolutePath()};
        boolean success = true;
        try {
            TestUtils.runGorPipe(args);
        } catch (Exception e) {
            success = false;
        }
        Assert.assertEquals(true, success);
    }
}
