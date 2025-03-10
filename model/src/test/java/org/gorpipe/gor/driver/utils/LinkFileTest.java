package org.gorpipe.gor.driver.utils;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LinkFileTest {

    private StreamSource mockSource;
    private String linkFileContent;

    @Before
    public void setUp() {
        mockSource = mock(StreamSource.class);
        linkFileContent = "## VERSION=1\n" +
                "## ENTRIES_COUNT_MAX=100\n" +
                "## ENTRIES_AGE_MAX=31536000000\n" +
                "# FILE\tTIMESTAMP\tMD5\tSERIAL\n" +
                "source/var/var1.gorz\t1734304890790\tABCDEAF13422\t1\n" +
                "source/var/var2.gorz\t1734305124533\t334DEAF13422\t2\n";
    }

    @Test
    public void testCreateLinkFile() {
        LinkFile linkFile = LinkFile.create(mockSource, linkFileContent);
        assertNotNull(linkFile);
        assertEquals(2, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
        assertEquals(31536000000L, linkFile.getEntriesAgeMax());
    }

    @Test
    public void testLoadLinkFile() throws IOException {
        when(mockSource.open()).thenReturn(new ByteArrayInputStream(linkFileContent.getBytes()));
        LinkFile linkFile = LinkFile.load(mockSource);
        assertNotNull(linkFile);
        assertEquals(2, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
        assertEquals(31536000000L, linkFile.getEntriesAgeMax());
    }

    @Test
    public void testAppendEntry() {
        LinkFile linkFile = LinkFile.create(mockSource, linkFileContent);
        linkFile.appendEntry("source/var/var3.gorz", "NEWMD5SUM");
        assertEquals(3, linkFile.getEntries().size());
    }

    @Test
    public void testGetLatestPath() {
        when(mockSource.getFullPath()).thenReturn("/mnt/csa/projects/test/x.link");
        LinkFile linkFile = LinkFile.create(mockSource, linkFileContent);
        assertEquals("/mnt/csa/projects/test/source/var/var2.gorz", linkFile.getLatestEntryUrl());
        linkFile.appendEntry("source/var/var3.gorz", "NEWMD5SUM");
        assertEquals("/mnt/csa/projects/test/source/var/var3.gorz", linkFile.getLatestEntryUrl());
    }

    @Test
    public void testGetTimedPath() {
        when(mockSource.getFullPath()).thenReturn("/mnt/csa/projects/test/x.link");
        LinkFile linkFile = LinkFile.create(mockSource, linkFileContent);
        linkFile.appendEntry("source/var/var3.gorz", "NEWMD5SUM");

        assertEquals(null, linkFile.getEntryUrl(1734304890790L - 100));
        assertEquals("/mnt/csa/projects/test/source/var/var1.gorz", linkFile.getEntryUrl(1734304890790L + 100));
        assertEquals("/mnt/csa/projects/test/source/var/var2.gorz", linkFile.getEntryUrl(1734305124533L));
        assertEquals("/mnt/csa/projects/test/source/var/var2.gorz", linkFile.getEntryUrl(1734305124533L + 100));
        assertEquals("/mnt/csa/projects/test/source/var/var3.gorz", linkFile.getEntryUrl(System.currentTimeMillis()));
    }

    @Test
    public void testSaveLinkFile() throws IOException {
        LinkFile linkFile = LinkFile.create(mockSource, linkFileContent);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        linkFile.save(outputStream);
        String savedContent = outputStream.toString();
        assertTrue(savedContent.contains("## VERSION = 1"));
        assertTrue(savedContent.contains("source/var/var2.gorz\t1734305124533\t334DEAF13422\t2"));
    }
}
