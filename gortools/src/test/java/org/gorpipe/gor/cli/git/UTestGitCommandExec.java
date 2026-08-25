package org.gorpipe.gor.cli.git;

import gorsat.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class UTestGitCommandExec {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    // ==================== Help output ====================

    @Test
    public void testGitHelpListsSubcommands() throws Exception {
        String res = runGit("--help");
        assertTrue(res.contains("clone") || res.contains("Git command wrapper"));
    }

    @Test
    public void testStatusHelp() throws Exception {
        String res = runGit("status --help");
        assertTrue(res.contains("--short") || res.contains("working tree"));
    }

    @Test
    public void testLogHelp() throws Exception {
        String res = runGit("log --help");
        assertTrue(res.contains("--oneline") || res.contains("commit logs"));
    }

    @Test
    public void testFetchHelp() throws Exception {
        String res = runGit("fetch --help");
        assertTrue(res.contains("--prune") || res.contains("Download objects"));
    }

    @Test
    public void testCheckoutHelp() throws Exception {
        String res = runGit("checkout --help");
        assertTrue(res.contains("--track") || res.contains("Switch branches"));
    }

    @Test
    public void testAddHelp() throws Exception {
        String res = runGit("add --help");
        assertTrue(res.contains("--all") || res.contains("staging area"));
    }

    @Test
    public void testCommitHelp() throws Exception {
        String res = runGit("commit --help");
        assertTrue(res.contains("--message") || res.contains("Record changes"));
    }

    @Test
    public void testCloneHelp() throws Exception {
        String res = runGit("clone --help");
        assertTrue(res.contains("--depth") || res.contains("Clone a repository"));
    }

    @Test
    public void testDiffHelp() throws Exception {
        String res = runGit("diff --help");
        assertTrue(res.contains("--cached") || res.contains("Show changes"));
    }

    @Test
    public void testPullHelp() throws Exception {
        String res = runGit("pull --help");
        assertTrue(res.contains("--rebase") || res.contains("Fetch from"));
    }

    @Test
    public void testPushHelp() throws Exception {
        String res = runGit("push --help");
        assertTrue(res.contains("--force") || res.contains("Update remote"));
    }

    @Test
    public void testConfigHelp() throws Exception {
        String res = runGit("config --help");
        assertTrue(res.contains("--global") || res.contains("repository or global"));
    }

    // ==================== Functional tests (temp git repo) ====================

    @Test
    public void testStatusInEmptyRepo() throws Exception {
        Path repo = initRepo();
        runGitInDir("status", repo);
    }

    @Test
    public void testStatusShortInEmptyRepo() throws Exception {
        Path repo = initRepo();
        runGitInDir("status -s", repo);
    }

    @Test
    public void testLogInRepoWithCommit() throws Exception {
        Path repo = initRepoWithCommit();
        String res = runGitInDir("log --oneline", repo);
        assertFalse(res.isBlank());
    }

    @Test
    public void testLogMaxCount() throws Exception {
        Path repo = initRepoWithCommit();
        String res = runGitInDir("log -n 1 --oneline", repo);
        // exec output has one GOR header line; data rows follow
        String[] rows = res.strip().split("\n");
        assertEquals(2, rows.length);
    }

    @Test
    public void testAddAndStatus() throws Exception {
        Path repo = initRepo();
        Files.writeString(repo.resolve("hello.txt"), "hello");
        runGitInDir("add hello.txt", repo);
        String res = runGitInDir("status -s", repo);
        assertTrue(res.contains("hello.txt"));
    }

    // ==================== Helpers ====================

    private String runGit(String cmd) throws Exception {
        return TestUtils.runGorPipe("exec gor git " + cmd);
    }

    private String runGitInDir(String cmd, Path dir) throws Exception {
        return TestUtils.runGorPipe("exec gor git " + cmd + " -d " + dir.toAbsolutePath());
    }

    private Path initRepo() throws IOException, InterruptedException {
        Path dir = temp.newFolder().toPath();
        exec(dir, "git", "init");
        exec(dir, "git", "config", "user.email", "test@example.com");
        exec(dir, "git", "config", "user.name", "Test");
        return dir;
    }

    private Path initRepoWithCommit() throws IOException, InterruptedException {
        Path dir = initRepo();
        Files.writeString(dir.resolve("README.md"), "init");
        exec(dir, "git", "add", ".");
        exec(dir, "git", "commit", "-m", "init");
        return dir;
    }

    private void exec(Path dir, String... cmd) throws IOException, InterruptedException {
        new ProcessBuilder(cmd).directory(dir.toFile()).start().waitFor();
    }
}
