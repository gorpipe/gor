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
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    private String resolve(Path linkFile, String relative) {
        return linkFile.getParent().resolve(relative).toAbsolutePath().normalize().toString();
    }
}
