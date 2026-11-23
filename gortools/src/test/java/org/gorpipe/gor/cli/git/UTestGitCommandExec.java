package org.gorpipe.gor.cli.git;

import gorsat.TestUtils;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UTestGitCommandExec {

    // Variables that make git use another repository than the one in the working directory. Set when the tests run
    // under a git hook or "git rebase --exec", for example.
    private static final List<String> REPO_ENV_VARS = List.of(
            "GIT_DIR", "GIT_WORK_TREE", "GIT_INDEX_FILE", "GIT_COMMON_DIR", "GIT_OBJECT_DIRECTORY");

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

    @Test
    public void testRepoSetupIgnoresInheritedGitDir() throws Exception {
        // A GIT_DIR inherited from the parent process once made the setup write user.name=Test into, and commit to,
        // the checkout the tests ran in.
        Path other = temp.newFolder().toPath();
        exec(other, "git", "init");
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("GIT_DIR", other.resolve(".git").toString());

        Path repo = initRepo(env);

        assertEquals("", gitConfig(other, "user.name"));
        assertEquals("Test", gitConfig(repo, "user.name"));
    }

    @Test
    public void testRepoSetupKeepsInheritedGitConfigGlobal() throws Exception {
        // Runners and sandboxes set GIT_CONFIG_GLOBAL and friends to keep git away from the host's config (signing,
        // hooks). Setup must keep them, like the gor git command under test does.
        Path global = temp.newFile("gitconfig").toPath();
        Files.writeString(global, "[init]\n\tdefaultBranch = from-global-config\n");
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("GIT_CONFIG_GLOBAL", global.toString());

        Path repo = initRepo(env);

        assertEquals("ref: refs/heads/from-global-config", Files.readString(repo.resolve(".git/HEAD")).trim());
    }

    @Test
    public void testRepoSetupIgnoresInheritedGitConfig() throws Exception {
        // git config writes to the file GIT_CONFIG names instead of the repository's config.
        Path other = temp.newFile("other-config").toPath();
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("GIT_CONFIG", other.toString());

        Path repo = initRepo(env);

        assertEquals("", Files.readString(other));
        assertEquals("Test", gitConfig(repo, "user.name"));
    }

    private static String gitConfig(Path repo, String key) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "config", "--file", repo.resolve(".git/config").toString(), key)
                .redirectErrorStream(true).start();
        String value = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        process.waitFor();
        return value;
    }

    // ==================== Helpers ====================

    private String runGit(String cmd) throws Exception {
        return TestUtils.runGorPipe("exec gor git " + cmd);
    }

    private String runGitInDir(String cmd, Path dir) throws Exception {
        return TestUtils.runGorPipe("exec gor git " + cmd + " -d " + dir.toAbsolutePath());
    }

    private Path initRepo() throws IOException, InterruptedException {
        // The gor git command under test runs git with this process's environment, so with an inherited GIT_DIR it
        // would add and commit to that repository instead of the temp one.
        Assume.assumeTrue("Inherited " + REPO_ENV_VARS + " would point the gor git command at another repository",
                REPO_ENV_VARS.stream().noneMatch(v -> System.getenv(v) != null));
        return initRepo(System.getenv());
    }

    private Path initRepo(Map<String, String> inheritedEnv) throws IOException, InterruptedException {
        Path dir = temp.newFolder().toPath();
        exec(dir, inheritedEnv, "git", "init");
        exec(dir, inheritedEnv, "git", "config", "user.email", "test@example.com");
        exec(dir, inheritedEnv, "git", "config", "user.name", "Test");
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
        exec(dir, System.getenv(), cmd);
    }

    /**
     * Run a git setup command in dir only: inherited variables that point git at another repository or config file
     * are dropped, so it can't write there, and a failure fails the test instead of being ignored. Other GIT_*
     * variables (GIT_CONFIG_GLOBAL, GIT_CONFIG_NOSYSTEM, ...) are kept, as the gor git command under test keeps them.
     */
    private static void exec(Path dir, Map<String, String> inheritedEnv, String... cmd)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.clear();
        env.putAll(inheritedEnv);
        REPO_ENV_VARS.forEach(env::remove);
        env.remove("GIT_CONFIG");
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(String.join(" ", cmd) + " failed with exit code " + exitCode + ": " + output);
        }
    }
}
