package org.gorpipe.gor.cli.link;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;

import org.gorpipe.gor.cli.GorCLI;
import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import picocli.CommandLine;

public class LinkCommandTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testUpdateCreatesLinkFileAndAppliesHeaders() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("update_test.gor.link");
        CommandLine cmd = new CommandLine(new GorCLI());

        int exitCode = cmd.execute("link", "update", linkFile.toString(), "data/file1.gor", "-h", "ENTRIES_COUNT_MAX=5");
        assertEquals(0, exitCode);

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, "data/file1.gor"), link.getLatestEntryUrl());
        assertEquals(5, link.getEntriesCountMax());
    }

    @Test
    public void testUpdateWithMd5AndInfo() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("update_md5_info.gor.link");
        CommandLine cmd = new CommandLine(new GorCLI());

        int exitCode = cmd.execute("link", "update", linkFile.toString(), "data/file1.gor",
                "-m", "abc123", "-i", "first entry");
        assertEquals(0, exitCode);

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        var latest = link.getLatestEntry();
        assertEquals("abc123", latest.md5());
        assertEquals("first entry", latest.info());
    }

    @Test
    public void testRollbackLatestEntry() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("rollback_latest.gor.link");
        CommandLine cmd = new CommandLine(new GorCLI());

        cmd.execute("link", "update", linkFile.toString(), "data/file1.gor");
        Thread.sleep(5);
        cmd.execute("link", "update", linkFile.toString(), "data/file2.gor");

        int exitCode = cmd.execute("link", "rollback", linkFile.toString());
        assertEquals(0, exitCode);

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, "data/file1.gor"), link.getLatestEntryUrl());
    }

    @Test
    public void testRollbackToDate() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("rollback_date.gor.link");
        CommandLine cmd = new CommandLine(new GorCLI());

        cmd.execute("link", "update", linkFile.toString(), "data/file1.gor");
        LinkFile first = LinkFile.load(new FileSource(linkFile));
        long firstTimestamp = first.getLatestEntry().timestamp();

        Thread.sleep(5);
        cmd.execute("link", "update", linkFile.toString(), "data/file2.gor");

        String rollbackIso = Instant.ofEpochMilli(firstTimestamp).toString();
        int exitCode = cmd.execute("link", "rollback", linkFile.toString(), "-d", rollbackIso);
        assertEquals(0, exitCode);

        LinkFile link = LinkFile.load(new FileSource(linkFile));
        assertEquals(1, link.getEntriesCount());
        assertEquals(resolve(linkFile, "data/file1.gor"), link.getLatestEntryUrl());
    }

    @Test
    public void testResolveLatestEntry() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("resolve_latest.gor.link");
        CommandLine cmd = new CommandLine(new GorCLI());

        cmd.execute("link", "update", linkFile.toString(), "data/file1.gor");
        Thread.sleep(5);
        cmd.execute("link", "update", linkFile.toString(), "data/file2.gor");

        String resolved = executeAndCapture(cmd, "link", "resolve", linkFile.toString());
        assertEquals(resolve(linkFile, "data/file2.gor"), resolved);
    }

    @Test
    public void testResolveSpecificDate() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("resolve_date.gor.link");
        CommandLine cmd = new CommandLine(new GorCLI());

        cmd.execute("link", "update", linkFile.toString(), "data/file1.gor");
        long firstTimestamp = LinkFile.load(new FileSource(linkFile)).getLatestEntry().timestamp();
        Thread.sleep(5);
        cmd.execute("link", "update", linkFile.toString(), "data/file2.gor");

        String resolved = executeAndCapture(cmd, "link", "resolve", linkFile.toString(),
                "-d", Instant.ofEpochMilli(firstTimestamp).toString());
        assertEquals(resolve(linkFile, "data/file1.gor"), resolved);
    }

    @Test
    public void testResolveFullEntry() throws Exception {
        Path linkFile = temp.getRoot().toPath().resolve("resolve_full.gor.link");
        CommandLine cmd = new CommandLine(new GorCLI());

        cmd.execute("link", "update", linkFile.toString(), "data/file1.gor");
        Thread.sleep(5);
        cmd.execute("link", "update", linkFile.toString(), "data/file2.gor");

        String expectedEntry = LinkFile.load(new FileSource(linkFile)).getLatestEntry().format();
        String resolved = executeAndCapture(cmd, "link", "resolve", linkFile.toString(), "-f");
        assertEquals(expectedEntry, resolved);
    }

    private String resolve(Path linkFile, String relative) {
        return linkFile.getParent().resolve(relative).toAbsolutePath().normalize().toString();
    }

    private String executeAndCapture(CommandLine cmd, String... args) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos, true));
        try {
            int exitCode = cmd.execute(args);
            assertEquals(0, exitCode);
        } finally {
            System.setOut(originalOut);
        }
        String output = baos.toString();
        if (output.endsWith("\r\n")) {
            output = output.substring(0, output.length() - 2);
        } else if (output.endsWith("\n") || output.endsWith("\r")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }
}

