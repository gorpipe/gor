/*
 * Copyright (c) 2013 deCODE Genetics Inc.
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * deCODE Genetics Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with deCODE.
 */

package org.gorpipe.gor.model;

import com.nextcode.gor.driver.utils.DatabaseHelper;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.utils.TestUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.util.collection.extract.Extract;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * UTestDriverBackedSecureFileReader is testing the DriverBackedSecureFileReader
 *
 * @version $Id$
 */
public class UTestDriverBackedSecureFileReader {

    private static final Logger log = LoggerFactory.getLogger(UTestDriverBackedSecureFileReader.class);

    private static String[] paths;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Before
    public void setUp() throws Exception {
        workDirPath = workDir.getRoot().toPath();
    }

    @BeforeClass
    public static void setup() throws IOException, ClassNotFoundException, SQLException {
        paths = DatabaseHelper.createRdaDatabase();
        System.setProperty("gor.db.credentials", paths[2]);
        DbConnection.initInConsoleApp();
    }

    /**
     * Test file signature methods
     *
     * @throws Exception
     */
    @Test
    public void testFileSignature() throws Exception {
        File f1 = File.createTempFile("test", DataType.TXT.suffix);
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader("", null, null,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());
        final String f1SignatureA = reader.getFileSignature(f1.getAbsolutePath());
        Assert.assertEquals(f1SignatureA, reader.getFileSignature(f1.getAbsolutePath()));

        Files.write(f1.toPath(), "somedata".getBytes());
        f1.setLastModified(System.currentTimeMillis() + 10000);
        final String f1SignatureB = reader.getFileSignature(f1.getAbsolutePath());
        Assert.assertFalse(f1SignatureA.equals(f1SignatureB));

        DbConnection.install(new DbConnection("rda", "jdbc:derby:" + paths[1], "rda", "beta3"));
        for (int i = 0; i < 10; i++) {
            final long start = System.currentTimeMillis();
            String fileSignature = reader.getFileSignature("db://rda:rda.v_variant_annotations");
            log.info("FileSignature: " + fileSignature + " in " + Extract.durationStringSince(start));
        }
    }

    /**
     * Test working with links files
     *
     * @throws Exception
     */
    @Test
    public void testLinkFiles() throws Exception {

        final Object[] constants = {};


        // Setup temporary file structure to test withh
        final Path root = Files.createTempDirectory("symlinktest");
        final Path d1 = Files.createDirectory(Paths.get(root.toString(), "d1"));
        final String fileName = DataUtil.toFile("testfile", DataType.GOR);
        final Path file = Paths.get(d1.toString(), fileName);
        final String link1name = DataUtil.toLinkFile("testfile1", DataType.GOR);
        final Path link1 = Paths.get(d1.toString(), link1name);
        final String link2name = DataUtil.toLinkFile("testfile2", DataType.GOR);
        final Path link2 = Paths.get(root.toString(), link2name);
        final String link3name = DataUtil.toLinkFile("testfile3", DataType.GOR);
        final Path link3 = Paths.get(d1.toString(), link3name);
        final String link4name = DataUtil.toLinkFile("testfile4", DataType.GOR);
        final Path link4 = Paths.get(d1.toString(), link4name);
        final String link5name = DataUtil.toLinkFile("testfile5", DataType.GOR);
        final Path link5 = Paths.get(d1.toString(), link5name);


        // Ensure link files can be read
        Files.write(file, "chr1\t10".getBytes());
        Files.write(link1, ("file://" + file + "\n").getBytes());
        Files.write(link2, ("file://" + link1).getBytes());
        Files.write(link3, fileName.getBytes());
        Files.write(link4, fileName.getBytes());
        Files.write(link5, (file + "\n").getBytes());

        //
        // Test with allow absolute links.
        //

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(d1.toString() + "/", constants,
                null, AccessControlContext.builder().withAllowAbsolutePath(true).build());

        final String[] fileContent = reader.readAll(file.toString());

        // Test standard link file (absolute path).
        final String[] linke1Content = reader.readAll(link1.toString());
        Assert.assertEquals(Arrays.toString(fileContent), Arrays.toString(linke1Content));

        // Test link to link
        final String[] linke2Content = reader.readAll(link2.toString());
        Assert.assertEquals(Arrays.toString(fileContent), Arrays.toString(linke2Content));

        // Test standard link file (relative path).
        final String[] linke3Content = reader.readAll(link3.toString());
        Assert.assertEquals(Arrays.toString(fileContent), Arrays.toString(linke3Content));

        // Read form link file.
        try (Stream<String> r = reader.readFile(link2.toString().replace(DataType.LINK.suffix, ""))) {
            r.limit(1).collect(Collectors.toList()).get(0);
        }

        // Test fallback links, i.e. check files that do not exists, but link file exists, will be found.
        Assert.assertEquals(
                reader.getFileSignature(link1.toString()),
                reader.getFileSignature(link1.toString().replace(DataType.LINK.suffix, "")));

        //
        // Test with not allow absolute links.
        //

        reader = new DriverBackedSecureFileReader(d1 + "/", constants, null, null);

        // Test standard link file (absolute path in link).
        final String[] linke1Content2 = reader.readAll(link1name);
        Assert.assertEquals(Arrays.toString(fileContent), Arrays.toString(linke1Content2));

        // Test fail for absolute paths to links.
        try {
            reader.readAll(link1.toString());
        } catch (GorResourceException e) {
            Assert.fail("Should be able to read absolute files within project scope");
        }

        // Test standard link file (relative path).
        final String[] linke3Content2 = reader.readAll(link3name);
        Assert.assertEquals(Arrays.toString(fileContent), Arrays.toString(linke3Content2));

        // Test fallback link file if absolute paths not allowed.
        Assert.assertEquals(
                reader.getFileSignature(link5name),
                reader.getFileSignature(link5name.replace(DataType.LINK.suffix, "")));

        // Test recursive fallback (uses link4).
        reader.readAll(link1name.replace(DataType.LINK.suffix, ""));
    }

    /**
     * Test working with symbolic links
     *
     * @throws Exception
     */
    @Test
    public void testSymbolicLinks() throws Exception {
        final Object[] constants = {};
        final DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader("", constants, null,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        // Setup temporary file structure to test with
        final Path root = Files.createTempDirectory("symlinktest");
        final Path d1 = Files.createDirectory(Paths.get(root.toString(), "d1"));
        final Path d2 = Files.createDirectory(Paths.get(root.toString(), "d2"));
        final Path file = Paths.get(d1.toString(), "testfile");
        final Path d1l1 = Paths.get(d1.toString(), "d1l1");
        final Path d2l2 = Paths.get(d2.toString(), "d2l2");
        final Path l = Paths.get(root.toString(), "l");

        Files.write(file, "chr1\t10".getBytes());
        Thread.sleep(1000);
        Files.createSymbolicLink(d1l1, file);
        Thread.sleep(1000);
        Files.createSymbolicLink(d2l2, d1l1);
        Thread.sleep(1000);
        Files.createSymbolicLink(l, d2l2);
        Thread.sleep(1000);

        final FileTime changedFileTime = FileTime.from((System.currentTimeMillis() / 1000) * 1000, TimeUnit.MILLISECONDS); // use ms precision

        log.info("{}", root);
        log.info("Begin f={}, d1l1={}, d2l2={}, l={}", Files.getLastModifiedTime(file),
                Files.getLastModifiedTime(d1l1, LinkOption.NOFOLLOW_LINKS),
                Files.getLastModifiedTime(d2l2, LinkOption.NOFOLLOW_LINKS),
                Files.getLastModifiedTime(l, LinkOption.NOFOLLOW_LINKS));

        // Ensure signatures are correctly reported
        final String md5File = reader.getFileSignature(file.toString());
        final String md5D1L1 = reader.getFileSignature(d1l1.toString());
        final String md5D2L2 = reader.getFileSignature(d2l2.toString());
        final String md5L = reader.getFileSignature(l.toString());
        Set<String> md5 = new HashSet<>();
        Collections.addAll(md5, md5File, md5D1L1, md5D2L2, md5L);
        Assert.assertEquals(1, md5.size());

        // Ensure file changes are correctly reported in signatures
        Files.setLastModifiedTime(file, changedFileTime);

        final String md5FileChanged = reader.getFileSignature(file.toString());
        Assert.assertFalse(md5.contains(md5FileChanged));
        Assert.assertEquals(md5FileChanged, reader.getFileSignature(d1l1.toString()));
        Assert.assertEquals(md5FileChanged, reader.getFileSignature(d2l2.toString()));
        Assert.assertEquals(md5FileChanged, reader.getFileSignature(l.toString()));

        log.info("End f={}, d1l1={}, d2l2={}, l={}", Files.getLastModifiedTime(file),
                Files.getLastModifiedTime(d1l1, LinkOption.NOFOLLOW_LINKS),
                Files.getLastModifiedTime(d2l2, LinkOption.NOFOLLOW_LINKS),
                Files.getLastModifiedTime(l, LinkOption.NOFOLLOW_LINKS));
    }


    /**
     * Test readAll method
     *
     * @throws Exception
     */
    @Test
    public void testReadMethods() throws Exception {
        // Simple test of reading a file
        final File f1 = File.createTempFile("test", DataType.TXT.suffix);
        Files.write(f1.toPath(), "somedata".getBytes());


        Object[] constants = {};

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader("", constants, null,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        // Test read standard file

        // Test readAll.
        String[] lines = reader.readAll(f1.getAbsolutePath());
        Assert.assertEquals(1, lines.length);
        Assert.assertEquals("somedata", lines[0]);
        Assert.assertTrue("File was not properly closed", TestUtils.isFileClosed(f1));

        // Test readFile
        try (Stream<String> r = reader.readFile(f1.getAbsolutePath())) {
            r.allMatch(s -> {
                if (s != null) {
                    assert "somedata".equals(s);
                    return true;
                }
                return false;
            });
        }
        Assert.assertTrue("File was not properly closed", TestUtils.isFileClosed(f1));

        // Test readHeader
        String header = reader.readHeaderLine(f1.getAbsolutePath());
        Assert.assertEquals("somedata", header);
        Assert.assertTrue("File was not properly closed", TestUtils.isFileClosed(f1));
    }

    /**
     * Test testReadingDbData method
     *
     * @throws Exception
     */
    @Test
    public void testReadingDbData() throws Exception {
        String dbUrl = "jdbc:derby:" + paths[1];
        String header;
        String[] lines;

        Object[] constants = {};

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader("", constants, null,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        // Test reading gor.db.credentials

        try {
            DbConnection.initializeDbSources("nonexisting/path/to/nowhere");
            Assert.fail("Should get exception for non existent gor.db.credentials");
        } catch (Exception e) {
            // Success
        }

        DbConnection.install(new DbConnection("rda", dbUrl, "rda", "beta3"));

        // Test reading db link file.

        final Path dblinkfile = Files.createTempFile("test.db", ".rep.link");
        final String sqltest = "select pos from rda.v_variant_annotations s";
        Files.write(dblinkfile, ("//db:" + sqltest).getBytes());

        // Test the results
        String linkfile = dblinkfile.toString();
        String[] flines = reader.readAll(dblinkfile.toString());
        header = reader.readHeaderLine(dblinkfile.toString());
        Assert.assertEquals(flines[0], header);
        Assert.assertEquals("Too many open connections", 0, TestUtils.getActivePoolConnections(dbUrl, "rda"));

        // Test readFile vs readall
        int[] i = {0};
        try (Stream<String> r = reader.readFile(linkfile)) {
            r.allMatch(s -> {
                if (s != null) {
                    Assert.assertEquals("Lines at " + i[0] + " must match", flines[i[0]++], s);
                    return true;
                }
                return false;
            });
        }
        Assert.assertEquals("Read all lines", i[0], flines.length);
        Assert.assertEquals("Too many open connections", 0, TestUtils.getActivePoolConnections(dbUrl, "rda"));

        // Test file signature
        final String signature = reader.getFileSignature(linkfile);
        Assert.assertNotNull(signature);

        // Test fallback for //db:
        final String fallbackfile = linkfile;
        String[] fallbacklines = reader.readAll(fallbackfile);
        Assert.assertTrue("Fallback must match link", Arrays.equals(flines, fallbacklines));

    }

    @Test
    public void testReadingSqlData() throws Exception {
        String dbUrl = "jdbc:derby:" + paths[1];
        String header;
        String[] lines;

        Object[] constants = {};

        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader("", constants, null,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        // Test reading gor.db.credentials

        try {
            DbConnection.initializeDbSources("nonexisting/path/to/nowhere");
            Assert.fail("Should get exception for non existent gor.db.credentials");
        } catch (Exception e) {
            // Success
        }

        DbConnection.install(new DbConnection("rda", dbUrl, "rda", "beta3"));

        // Test reading db link file.

        final Path dblinkfile = Files.createTempFile("test.db", ".rep.link");
        final String sqltest = "select pos from rda.v_variant_annotations s";
        Files.write(dblinkfile, ("sql://" + sqltest).getBytes());

        // Test the results
        String linkfile = dblinkfile.toString();
        String[] flines = reader.readAll(dblinkfile.toString());
        header = reader.readHeaderLine(dblinkfile.toString());
        Assert.assertEquals(flines[0], header);
        Assert.assertEquals("Too many open connections", 0, TestUtils.getActivePoolConnections(dbUrl, "rda"));

        // Test readFile vs readall
        int[] i = {0};
        try (Stream<String> r = reader.readFile(linkfile)) {
            r.allMatch(s -> {
                if (s != null) {
                    Assert.assertEquals("Lines at " + i[0] + " must match", flines[i[0]++], s);
                    return true;
                }
                return false;
            });
        }
        Assert.assertEquals("Read all lines", i[0], flines.length);
        Assert.assertEquals("Too many open connections", 0, TestUtils.getActivePoolConnections(dbUrl, "rda"));

        // Test file signature
        final String signature = reader.getFileSignature(linkfile);
        Assert.assertNotNull(signature);

        // Test fallback for //db:
        final String fallbackfile = linkfile;
        String[] fallbacklines = reader.readAll(fallbackfile);
        Assert.assertTrue("Fallback must match link", Arrays.equals(flines, fallbacklines));

    }

    @Test
    public void testCopy() throws IOException {
        Path f1 = workDirPath.resolve(DataUtil.toFile("test", DataType.TXT));
        Files.write(f1, "somedata".getBytes());

        Object[] constants = {};
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader("", constants, null,
                AccessControlContext.builder()
                        .withAllowAbsolutePath(true)
                        .withWriteLocations(Arrays.asList(new String[]{workDirPath.toString()})).build());

        Path c1 = workDirPath.resolve(DataUtil.toFile("copy", DataType.TXT));
        reader.copy(f1.toString(), c1.toString());

        Assert.assertTrue(Files.exists(f1));
        Assert.assertTrue(Files.exists(c1));
        Assert.assertEquals("somedata", new String(Files.readAllBytes(c1)));
    }

    @Test
    public void testMove() throws IOException {
        Path f1 = workDirPath.resolve(DataUtil.toFile("test", DataType.TXT));
        Files.write(f1, "somedata".getBytes());

        Object[] constants = {};
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader("", constants, null,
                AccessControlContext.builder()
                        .withAllowAbsolutePath(true)
                        .withWriteLocations(Arrays.asList(new String[]{workDirPath.toString()})).build());

        Path c1 = workDirPath.resolve(DataUtil.toFile("copy", DataType.TXT));
        reader.move(f1.toString(), c1.toString());

        Assert.assertFalse(Files.exists(f1));
        Assert.assertTrue(Files.exists(c1));
        Assert.assertEquals("somedata", new String(Files.readAllBytes(c1)));
    }

    @Test
    public void testReadingDbDataDirect() throws Exception {

        String dbviewquery = "db://rda:rda.v_variant_annotations";
        String dbsqlquery = "//db:select pos from rda.v_variant_annotations s";
        String sqlquery = "sql://select * from rda.v_variant_annotations s";
        String sqlqueryWithScope = "sql://select * from rda.v_variant_annotations where project_id = #{project-id}";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";
        String securityContext2 = "dbscope=project_id#int#10005,organization_id#int#1|||extrastuff=other";

        Object[] constants = {};
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(".", constants, securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        DriverBackedSecureFileReader reader2 = new DriverBackedSecureFileReader(".", constants, securityContext2,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        DbConnection.install(new DbConnection("rda", "jdbc:derby:" + paths[1], "rda", "beta3"));

        String[] content;
        // 1.  Fails, i.e. it succeeds when it should not.
        var data = reader.readAll(dbsqlquery);
        Assert.assertEquals(11, data.length);
        var data2 = reader.readAll(sqlquery);
        Assert.assertEquals(11, data2.length);
        Assert.assertTrue(data2[1].contains("10004"));
        Assert.assertTrue(data2[10].contains("10005"));
        var data3 = reader.readAll(sqlqueryWithScope);
        Assert.assertEquals(6, data3.length);
        Assert.assertTrue(data3[1].contains("10004"));
        Assert.assertTrue(data3[5].contains("10004"));

        // 2. Fails, should succeed but fails on check that should be removed in DriverBackedSecureFileReader.directDbUrl.
        data = reader.readAll(dbviewquery);


        String result;

        // Server

        // 3. OK, fails as should.  Fails on 'SimpleSource does not support gor iterator' rather than access check.
        //    Note when running similar query in SM, if fails but on access check as it takes //db:... as absolute path.)
        try {
            result = gorsat.TestUtils.runGorPipeServer("gor {" + dbsqlquery + "}", ".", securityContext);
            Assert.fail("Should get exception for db sql query");
        } catch (Exception e) {
            // Success
            e.printStackTrace();
        }

        // 4. Ok, succeeds as should.
        result = gorsat.TestUtils.runGorPipeServer("gor " + dbviewquery, ".", securityContext);

        // 5. Ok/Fails, fails as should, but the exception is wrong, so it is not failing for the right reasons.
        //    Fails on setting up db connection (no info in exception) instead of access check.
        // TODO: Find out why this doesn't fail.
        try {
            result = gorsat.TestUtils.runGorPipeServer("nor {" + dbsqlquery + "}", ".", securityContext);
            //Assert.fail("Should get exception for db sql query");
        } catch (Exception e) {
            // Success
            e.printStackTrace();
        }

        // 6. Fails, should succeed but fails on check that should be removed in DriverBackedSecureFileReader.directDbUrl.
        result = gorsat.TestUtils.runGorPipeServer("nor " + dbviewquery, ".", securityContext);

        // Gorpipe

        // 7. OK, Fails on 'SimpleSource does not support gor iterator'.
        try {
            result = gorsat.TestUtils.runGorPipeCLI("gor " + dbsqlquery, ".", securityContext);
            Assert.fail("Should get exception for db sql query");
        } catch (Exception e) {
            // Success
            e.printStackTrace();
        }

        // 8.Ok, succeeds as should.
        result = gorsat.TestUtils.runGorPipeCLI("gor " + dbviewquery, ".", securityContext);

        // 9. Fails, should succeed but fails on setting up db connection (no info in exception).
        // this should fail, only allowed through a link file???
        result = gorsat.TestUtils.runGorPipeCLI("nor {" + dbsqlquery + "}", ".", securityContext);

        // 10. Ok, succeeds as should.
        result = gorsat.TestUtils.runGorPipeCLI("nor " + dbviewquery, ".", securityContext);
    }

    @Test
    public void testReadingDbDataDirectMultipleSources() throws Exception {
        String sqlqueryRda = "sql://select * from rda.v_variant_annotations";
        String sqlqueryAvas = "sql://avas:select * from avas.v_variant_annotations";
        String securityContext = "dbscope=project_id#int#10004|||extrastuff=other";

        Object[] constants = {};
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(".", constants, securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        DbConnection.install(new DbConnection("rda", "jdbc:derby:" + paths[1], "rda", "beta3"));

        // Create avas database
        var avasPaths = DatabaseHelper.createAvasDatabase();
        DbConnection.install(new DbConnection("avas", "jdbc:derby:" + avasPaths[1], "avas", "beta3"));

        // 1.  Fails, i.e. it succeeds when it should not.
        var dataRda = reader.readAll(sqlqueryRda);
        var dataAvas = reader.readAll(sqlqueryAvas);

        Assert.assertTrue(dataRda[1].contains("rda1"));
        Assert.assertTrue(dataAvas[1].contains("avas1"));
    }

    @Test
    public void testReadingDbDataWithProjectAndOrgScope() throws Exception {
        String sqlqueryWithScopeAndOrg = "sql://select * from rda.v_variant_annotations where project_id = #{project-id} and organization_id = #{organization-id}";
        String securityContext = "dbscope=project_id#int#10005,organization_id#int#1|||extrastuff=other";

        Object[] constants = {};
        DriverBackedSecureFileReader reader = new DriverBackedSecureFileReader(".", constants, securityContext,
                AccessControlContext.builder().withAllowAbsolutePath(true).build());

        var paths = DatabaseHelper.createRdaDatabaseWithOrg();
        DbConnection.install(new DbConnection("rda", "jdbc:derby:" + paths[1], "rda", "beta3"));


        var data = reader.readAll(sqlqueryWithScopeAndOrg);

        Assert.assertTrue(data.length == 4);
    }
}