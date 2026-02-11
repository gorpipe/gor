package gorsat.Inputs;

import gorsat.Commands.CommandParseUtilities;
import gorsat.TestUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class UTestLink {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testLinkHelp() throws Exception {
        String res = TestUtils.runGorPipe("exec gor link --help ");

        assertTrue(res.split("\n")[1].split("\t")[2].startsWith("Link file management commands."));
    }

    @Test
    public void testUpdateCreatesLinkFileError() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("update_test.gor.link");

        try {
            String res = TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor -xxx ENTRIES_COUNT_MAX=5");
        } catch (GorParsingException e) {
            String res = e.getMessage();
            assertTrue(res.contains("Unknown options: '-xxx', 'ENTRIES_COUNT_MAX=5'"));
        }
        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(0, link.getEntriesCount());
    }

    @Test
    public void testUpdateCreatesLinkFileAndAppliesHeaders() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("update_test.gor.link");

        String res = TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor -h ENTRIES_COUNT_MAX=5");

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, "data/file1.gor"), link.getLatestEntryUrl());
        assertEquals(5, link.getEntriesCountMax());
    }

    @Test
    public void testUpdateCreatesLinkFileWithProjectRooot() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("update_test.gor.link");

        String res = TestUtils.runGorPipe("exec gor link update update_test.gor.link data/file1.gor -h ENTRIES_COUNT_MAX=5", temp.getRoot().toPath().toString() , true, "dummy");

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, "data/file1.gor"), link.getLatestEntryUrl());
        assertEquals(5, link.getEntriesCountMax());
    }

    @Test
    public void testUpdateWithMd5AndInfo() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("update_md5_info.gor.link");

        String res = TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor -m abc123 -i 'first entry'");

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        var latest = link.getLatestEntry();
        assertEquals("abc123", latest.md5());
        assertEquals("'first entry'", latest.info());
    }

    @Test
    public void testRollbackLatestEntry() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("rollback_latest.gor.link");

        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor");
        Thread.sleep(5);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file2.gor");

        String res = TestUtils.runGorPipe("exec gor link rollback " + linkFile.toString());

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, "data/file1.gor"), link.getLatestEntryUrl());
    }

    @Test
    public void testRollbackToDate() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("rollback_date.gor.link");

        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor");
        LinkFile first = LinkFile.load(new FileSource(linkFile));
        long firstTimestamp = first.getLatestEntry().timestamp();
        Thread.sleep(5);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file2.gor");

        String rollbackIso = Instant.ofEpochMilli(firstTimestamp).toString();
        String res = TestUtils.runGorPipe("exec gor link rollback " + linkFile.toString() + " -d " + rollbackIso);

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, "data/file1.gor"), link.getLatestEntryUrl());
    }

    @Test
    public void testResolveLatestEntry() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("resolve_latest.gor.link");

        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor");
        Thread.sleep(5);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file2.gor");

        String res = TestUtils.runGorPipe("exec gor link resolve " + linkFile);

        assertEquals(resolve(linkFile, "data/file2.gor"), res.split("\n")[1].split("\t")[2]);
    }

    @Test
    public void testResolveSpecificDate() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("resolve_date.gor.link");

        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor");
        LinkFile first = LinkFile.load(new FileSource(linkFile));
        long firstTimestamp = first.getLatestEntry().timestamp();
        Thread.sleep(5);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file2.gor");

        String res = TestUtils.runGorPipe("exec gor link resolve " + linkFile.toString() + " -d " + Instant.ofEpochMilli(firstTimestamp).toString());

        assertEquals(resolve(linkFile, "data/file1.gor"), res.split("\n")[1].split("\t")[2]);
    }

    @Test
    public void testResolveFullEntry() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("resolve_full.gor.link");

        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor");
        Thread.sleep(5);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file2.gor");

        String expectedEntry = LinkFile.load(new FileSource(linkFile)).getLatestEntry().format();

        String res = TestUtils.runGorPipe("exec gor link resolve " + linkFile.toString() + " -f");

        assertEquals(expectedEntry.replace('\t', ' '), CommandParseUtilities.quoteSafeSplit(res.split("\n")[1], '\t')[2]);
    }

    @Test
    public void testResolveEntryInfoOnly() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("resolve_full.gor.link");

        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " data/file1.gor -i 'info more'");
        String expectedEntry = LinkFile.load(new FileSource(linkFile)).getLatestEntry().format();
        String res = TestUtils.runGorPipe("exec gor link resolve " + linkFile.toString() + " -i");

        assertEquals("ChromNor\tPosNor\tcol1\nchrN\t0\tinfo more\n", res);
    }

    private String resolve(Path linkFile, String relative) {
        return linkFile.getParent().resolve(relative).toAbsolutePath().normalize().toString();
    }

    @Test
    public void testLinkLimitedByNumberEntries() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("limit_count.gor.link");
        Path file1 = temp.newFile("file1.gor").toPath();
        Path file2 = temp.newFile("file2.gor").toPath();
        Path file3 = temp.newFile("file3.gor").toPath();

        // Create link with max 2 entries
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file1.toString() + " ENTRIES_COUNT_MAX=2");
        Thread.sleep(10);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file2.toString());
        Thread.sleep(10);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file3.toString());

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(2, link.getEntriesCount());
        // The entries are sorted oldest first. So index 0 is file2, index 1 is file3.
        assertEquals(resolve(linkFile, file2.getFileName().toString()), link.getEntries().get(0).url());
        assertEquals(resolve(linkFile, file3.getFileName().toString()), link.getEntries().get(1).url());
    }

    @Test
    public void testLinkLimitedByTimestamp() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("limit_age.gor.link");
        Path file1 = temp.newFile("file1.gor").toPath();
        Path file2 = temp.newFile("file2.gor").toPath();

        // Max age 200ms
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file1.toString() + " ENTRIES_AGE_MAX=200");

        Thread.sleep(300); // Wait > 200ms

        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file2.toString());

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        // file1 should be expired.
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, file2.getFileName().toString()), link.getLatestEntryUrl());
    }

    @Test
    public void testLinkLimitedBySize() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("limit_size.gor.link");

        // Construct a large info string (~4KB)
        StringBuilder largeInfo = new StringBuilder();
        for(int i=0; i<4000; i++) largeInfo.append("a");

        Path file1 = temp.newFile("file1.gor").toPath();
        Path file2 = temp.newFile("file2.gor").toPath();
        Path file3 = temp.newFile("file3.gor").toPath();

        // Use LinkFile API directly to allow large inputs easily and control saving
        LinkFile link = LinkFile.create(new FileSource(linkFile), "");
        link.setEntriesCountMax(100);

        // Add entries. Total size will exceed default 10000 bytes.
        link.appendEntry(file1.toString(), "md5_1", largeInfo.toString());
        link.save(null);

        link.appendEntry(file2.toString(), "md5_2", largeInfo.toString());
        link.save(null);

        link.appendEntry(file3.toString(), "md5_3", largeInfo.toString());
        link.save(null);

        LinkFile reload = LinkFile.load(new FileSource(linkFile));
        // Should have dropped oldest entries to stay under size limit.
        // 3 entries * 4KB > 10KB. Expect < 3 entries.
        assertTrue("Link file should have less than 3 entries due to size limit", reload.getEntriesCount() < 3);
        assertEquals(file3.toString(), reload.getLatestEntryUrl());
    }

    @Test
    public void testGarbageCollectionManaged() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("gc_managed.gor.link");
        Path file1 = temp.newFile("file1.gor").toPath();
        Path file2 = temp.newFile("file2.gor").toPath();
        Path file3 = temp.newFile("file3.gor").toPath();

        Files.write(file1, "data1".getBytes());
        Files.write(file2, "data2".getBytes());
        Files.write(file3, "data3".getBytes());

        // ENTRIES_COUNT_MAX=2, DATA_LIFECYCLE_MANAGED=true
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file1.toString() + " ENTRIES_COUNT_MAX=2 DATA_LIFECYCLE_MANAGED=true");
        Thread.sleep(10);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file2.toString());

        assertTrue(file1.toFile().exists());

        Thread.sleep(10);
        // This update pushes file1 out.
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file3.toString());

        // Wait for async GC
        long start = System.currentTimeMillis();
        while(file1.toFile().exists() && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(50);
        }

        assertFalse("File1 should be deleted (Managed GC)", file1.toFile().exists());
        assertTrue("File2 should exist", file2.toFile().exists());
        assertTrue("File3 should exist", file3.toFile().exists());
    }

    @Test
    public void testGarbageCollectionUnmanaged() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("gc_unmanaged.gor.link");
        Path file1 = temp.newFile("file1.gor").toPath();
        Path file2 = temp.newFile("file2.gor").toPath();
        Path file3 = temp.newFile("file3.gor").toPath();

        // ENTRIES_COUNT_MAX=2, DATA_LIFECYCLE_MANAGED=false
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file1.toString() + " ENTRIES_COUNT_MAX=2 DATA_LIFECYCLE_MANAGED=false");
        Thread.sleep(10);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file2.toString());
        Thread.sleep(10);
        TestUtils.runGorPipe("exec gor link update " + linkFile.toString() + " " + file3.toString());

        // file1 pushed out. Should NOT be deleted.
        Thread.sleep(500);

        assertTrue("File1 should exist (Unmanaged GC)", file1.toFile().exists());
        assertTrue("File2 should exist", file2.toFile().exists());
        assertTrue("File3 should exist", file3.toFile().exists());
    }
}
