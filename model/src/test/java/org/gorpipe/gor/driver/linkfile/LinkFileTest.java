package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LinkFileTest {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

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
        assertEquals(2, linkFile.getEntries().size());
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
        LinkFile linkFile = new LinkFileV1(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.contains("## VERSION = 1"));
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveNewV0LinkFile() throws IOException {
        var linkPath = workPath.resolve("test.link");
        LinkFile linkFile = new LinkFileV0(new FileSource(linkPath.toString()));
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
        LinkFile linkFile = new LinkFileV0(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }

    @Test
    public void testSaveLinkFileV0ToV1() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, "a/b/c.gorz");
        LinkFile linkFile = new LinkFileV1(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.contains("## VERSION = 1"));
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveLinkFileV1ToV0() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, v1LinkFileContent);
        LinkFile linkFile = new LinkFileV0(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save();
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }
}
