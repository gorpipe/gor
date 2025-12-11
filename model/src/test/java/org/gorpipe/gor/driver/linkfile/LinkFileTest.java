package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LinkFileTest {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();


    private StreamSource mockSource;
    private final String v1LinkFileContent = """
            ## SERIAL = 1
            ## VERSION = 1
            #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
            source/v1/ver1.gorz\t2024-12-15T11:21:30.790Z\tABCDEAF13422\t1\t
            source/v1/ver2.gorz\t2024-12-15T23:25:24.533Z\t334DEAF13422\t2\t
            """;
    // 2024-12-15T11:21:30.790Z  = 1734261690790
    // 2024-12-15T23:25:24.533Z  = 1734305124533L
    private final String v0LinkFileContent = "source/v0/verx.gorz\n";
    private final String simpleFile = "source/y.gorz";
    protected Path workPath;

    @Before
    public void setUp() {
        workPath = workDir.getRoot().toPath().toAbsolutePath();
        mockSource = mock(StreamSource.class);

    }

    @Test
    public void testCreateLinkFile() {
        LinkFile linkFile = LinkFile.create(mockSource, v1LinkFileContent);
        assertNotNull(linkFile);
        assertTrue(linkFile instanceof LinkFileV1);
        assertEquals("1", linkFile.getMeta().getVersion());
        assertEquals(2, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Test
    public void testCreateLinkFileSimple() {
        LinkFile linkFile = LinkFile.create(mockSource, "test.gorz");
        assertNotNull(linkFile);
        assertTrue(linkFile instanceof LinkFileV1);
        assertEquals("1", linkFile.getMeta().getVersion());
        assertEquals(1, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Ignore("Fiddly test depending on system properties, ignore for now.  Can run in isolation to verify.")
    @Test
    public void testCreateLinkFileSimpleWithDefault0() {
        System.setProperty("gor.driver.link.default.version", "0");
        LinkFile linkFile = LinkFile.create(mockSource, "test.gorz");
        assertNotNull(linkFile);
        assertTrue(linkFile instanceof LinkFileV0);
        assertEquals("0", linkFile.getMeta().getVersion());
        assertEquals(1, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Test
    public void testLoadLinkFile() throws IOException {
        when(mockSource.exists()).thenReturn(true);
        when(mockSource.open()).thenReturn(new ByteArrayInputStream(v1LinkFileContent.getBytes()));
        LinkFile linkFile = LinkFile.load(mockSource);
        assertNotNull(linkFile);
        assertEquals(2, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Test
    public void testAppendEntry() {
        LinkFile linkFile = LinkFile.create(mockSource, v1LinkFileContent);
        linkFile.appendEntry(simpleFile, "NEWMD5SUM", "Test1");
        assertEquals(3, linkFile.getEntries().size());
    }

    @Test
    public void testGetLatestPath() {
        when(mockSource.getFullPath()).thenReturn("/mnt/csa/projects/test/x.link");
        LinkFile linkFile = LinkFile.create(mockSource, v1LinkFileContent);
        assertEquals("/mnt/csa/projects/test/source/v1/ver2.gorz", linkFile.getLatestEntryUrl());
        linkFile.appendEntry(simpleFile, "NEWMD5SUM", "");
        assertEquals("/mnt/csa/projects/test/" + simpleFile, linkFile.getLatestEntryUrl());
    }

    @Test
    public void testGetTimedPath() {
        when(mockSource.getFullPath()).thenReturn("/mnt/csa/projects/test/x.link");
        LinkFile linkFile = LinkFile.create(mockSource, v1LinkFileContent);
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");

        assertEquals(null, linkFile.getEntryUrl(1734261690790L - 1000));
        assertEquals("/mnt/csa/projects/test/source/v1/ver1.gorz", linkFile.getEntryUrl(1734261690790L + 1000));
        assertEquals("/mnt/csa/projects/test/source/v1/ver2.gorz", linkFile.getEntryUrl(1734305124533L));
        assertEquals("/mnt/csa/projects/test/source/v1/ver2.gorz", linkFile.getEntryUrl(1734305124533L + 1000));
        assertEquals("/mnt/csa/projects/test/" + simpleFile, linkFile.getEntryUrl(System.currentTimeMillis()));
    }

    @Test
    public void testSaveNewV1LinkFile() throws IOException {
        var linkPath = workPath.resolve("test.link");
        LinkFile linkFile = LinkFile.createV1(new FileSource(linkPath.toString()), "");
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.contains("## VERSION = 1"));
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveNewV0LinkFile() throws IOException {
        var linkPath = workPath.resolve("test.link");
        LinkFile linkFile = LinkFile.createV0(new FileSource(linkPath.toString()), "");
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }

    @Test
    public void testSaveLinkFileV1ToV1() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, v1LinkFileContent);
        LinkFile linkFile = LinkFile.load(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.startsWith("## SERIAL = 2"));
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveLinkFileV0ToV0() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, "a/b/c.gorz");
        LinkFile linkFile = LinkFile.load(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }

    @Test
    public void testSaveLinkFileV0ToV1() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, "a/b/c.gorz");
        LinkFile linkFile = LinkFile.loadV1(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.contains("## VERSION = 1"));
        assertEquals(2, linkFile.getEntries().size());
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveLinkFileV1ToV0() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, v1LinkFileContent);
        LinkFile linkFile = LinkFile.loadV0(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInferDataFileNameFromLinkFile_NullOrEmptyPath() throws Exception {
        LinkFile.inferDataFileNameFromLinkFile(new FileSource(""), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInferDataFileNameFromLinkFile_AbsolutePath() throws Exception {
        LinkFile.inferDataFileNameFromLinkFile(new FileSource("/abs/path/x.link"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInferDataFileNameFromLinkFile_NoRootConfigured() throws Exception {
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_FILES_URL, null);
        LinkFile.inferDataFileNameFromLinkFile(new FileSource("x.link"), null);
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromEnvVariable_WithProject() throws Exception {
        String root = "/managed/root";
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_FILES_URL, root);

        String result = LinkFile.inferDataFileNameFromLinkFile(new FileSource(new SourceReference("x.gor.link", null, "/projects/test", -1, null,  null, false, false)), null);
        assertNotNull(result);
        assertTrue(result.matches((root + "/test/x\\..*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromEnvVariable_WithOutProject() throws Exception {
        String root = "/managed/root";
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_FILES_URL, root);

        String result = LinkFile.inferDataFileNameFromLinkFile(new FileSource("x.gor.link"), null);
        assertTrue(result.matches((root + "/x\\..*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromExiting_File() throws Exception {
        String root = "/managed/fromfile";
        String linkFilePath =  "x.gor.link";
        Files.createDirectory(workPath.resolve("test"));
        Files.writeString(workPath.resolve("test").resolve(linkFilePath), "## " + LinkFileMeta.HEADER_CONTENT_LOCATION_MANAGED_KEY + " = " + root + "\nsource/y.gorz\n");

        String result = LinkFile.inferDataFileNameFromLinkFile(new FileSource(new SourceReference(linkFilePath, null, workPath.resolve("test").toString(), -1, null,  null, false, false)), null);
        assertNotNull(result);
        assertTrue(result.matches((root + "/test/x\\..*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromMetaParam() throws Exception {
        String root = "/managed/fromparam";
        String linkFilePath = "x.gor.link";
        String linkFileMeta = "## " + LinkFileMeta.HEADER_CONTENT_LOCATION_MANAGED_KEY + " = " + root;

        String result = LinkFile.inferDataFileNameFromLinkFile(new FileSource(new SourceReference(linkFilePath)), linkFileMeta);
        assertNotNull(result);
        assertTrue(result.matches((root + "/x\\..*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromMetaParam_ExistingFile() throws Exception {
        String fileroot = "/managed/fromfile";
        String linkFilePath = "x.gor.link";
        Files.writeString(workPath.resolve(linkFilePath), "## " + LinkFileMeta.HEADER_CONTENT_LOCATION_MANAGED_KEY + " = " + fileroot + "\nsource/y.gorz\n");

        String paramroot = "/managed/fromparam";
        String linkFileMeta = "## " + LinkFileMeta.HEADER_CONTENT_LOCATION_MANAGED_KEY + " = " + paramroot;

        String result = LinkFile.inferDataFileNameFromLinkFile(new FileSource(new SourceReference(linkFilePath)), linkFileMeta);
        assertNotNull(result);
        assertTrue(result.matches((paramroot + "/x\\..*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_PathReplace() throws Exception {
        String root = "/managed/root";
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_FILES_URL, root);
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_INFER_REPLACE, "wont;will");

        String result = LinkFile.inferDataFileNameFromLinkFile(new FileSource("wont/x.gor.link"), null);

        assertNotNull(result);
        assertTrue(result.matches((root + "/will/x\\..*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_AbsolutePathReplace() throws Exception {
        String root = "/managed/root";
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_FILES_URL, root);
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_INFER_REPLACE, "\\/abs\\/");

        String result = LinkFile.inferDataFileNameFromLinkFile(new FileSource("/abs/path/x.gor.link"), null);

        assertNotNull(result);
        assertTrue(result.matches((root + "/path/x\\..*\\.gor").replace("/", "\\/")));
    }
}
