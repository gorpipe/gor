package org.gorpipe.gor.cli.files;

import gorsat.TestUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.Assert.*;

public class UTestFilesCommandExec {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    // ==================== CatCommand ====================

    @Test
    public void testCatBasic() throws Exception {
        Path f = createFile("cat_basic.txt", "line1\nline2\nline3");
        String res = runFiles("cat " + f);
        assertTrue(res.contains("line1"));
        assertTrue(res.contains("line2"));
        assertTrue(res.contains("line3"));
    }

    @Test
    public void testCatNumber() throws Exception {
        Path f = createFile("cat_num.txt", "alpha\nbeta\ngamma");
        String res = runFiles("cat -n " + f);
        String[] rows = res.split("\n");
        assertEquals("1", rows[1].split("\t")[2].trim());
        assertEquals("2", rows[2].split("\t")[2].trim());
        assertEquals("3", rows[3].split("\t")[2].trim());
    }

    @Test
    public void testCatNumberNonblank() throws Exception {
        Path f = createFile("cat_nb.txt", "alpha\n\nbeta");
        String res = runFiles("cat -b " + f);
        String[] rows = res.split("\n");
        // alpha → numbered 1
        assertEquals("1", rows[1].split("\t")[2].trim());
        // blank → no number (trailing empty fields dropped by split without -1 limit)
        String[] blankParts = rows[2].split("\t", -1);
        assertTrue(blankParts.length < 3 || blankParts[2].isEmpty());
        // beta → numbered 2
        assertEquals("2", rows[3].split("\t")[2].trim());
    }

    @Test
    public void testCatSqueezeBlank() throws Exception {
        Path f = createFile("cat_sq.txt", "a\n\n\nb");
        String res = runFiles("cat -s " + f);
        String[] rows = res.split("\n");
        // header + a + one blank + b = 4
        assertEquals(4, rows.length);
    }

    @Test
    public void testCatShowEnds() throws Exception {
        Path f = createFile("cat_ends.txt", "hello");
        String res = runFiles("cat -E " + f);
        assertTrue(res.contains("hello$"));
    }

    @Test
    public void testCatShowTabs() throws Exception {
        Path f = createFile("cat_tabs.txt", "col1\tcol2");
        String res = runFiles("cat -T " + f);
        assertTrue(res.contains("^I"));
    }

    @Test
    public void testCatShowAll() throws Exception {
        Path f = createFile("cat_all.txt", "a\tb");
        String res = runFiles("cat -A " + f);
        assertTrue(res.contains("^I"));
        assertTrue(res.contains("$"));
    }

    // ==================== LsCommand ====================

    @Test
    public void testLsBasic() throws Exception {
        Path dir = temp.newFolder("ls_basic").toPath();
        createFile(dir, "file1.txt", "x");
        createFile(dir, "file2.txt", "y");
        String res = runFiles("ls " + dir);
        assertTrue(res.contains("file1.txt"));
        assertTrue(res.contains("file2.txt"));
    }

    @Test
    public void testLsRecursive() throws Exception {
        Path dir = temp.newFolder("ls_rec").toPath();
        Path sub = dir.resolve("sub");
        Files.createDirectory(sub);
        createFile(sub, "deep.txt", "z");
        String res = runFiles("ls -R " + dir);
        assertTrue(res.contains("deep.txt"));
    }

    @Test
    public void testLsLong() throws Exception {
        Path dir = temp.newFolder("ls_long").toPath();
        createFile(dir, "file.txt", "hello");
        String res = runFiles("ls -l " + dir);
        String header = res.split("\n")[0];
        assertTrue(header.contains("Filename"));
        assertTrue(header.contains("Filesize"));
        assertTrue(header.contains("Modified"));
    }

    @Test
    public void testLsHumanReadable() throws Exception {
        Path dir = temp.newFolder("ls_hr").toPath();
        Files.write(dir.resolve("big.txt"), new byte[1024]);
        String withH = runFiles("ls -l -h " + dir);
        String withoutH = runFiles("ls -l " + dir);
        assertTrue(withH.contains("1.0K"));
        assertTrue(withoutH.contains("1024"));
    }

    @Test
    public void testLsSortByTime() throws Exception {
        Path dir = temp.newFolder("ls_time").toPath();
        Path older = createFile(dir, "older.txt", "old");
        Path newer = createFile(dir, "newer.txt", "new");
        Files.setLastModifiedTime(older, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
        Files.setLastModifiedTime(newer, FileTime.from(Instant.parse("2025-01-01T00:00:00Z")));
        String res = runFiles("ls -t " + dir);
        // newest first
        assertTrue(res.indexOf("newer.txt") < res.indexOf("older.txt"));
    }

    @Test
    public void testLsSortByTimeReverse() throws Exception {
        Path dir = temp.newFolder("ls_time_rev").toPath();
        Path older = createFile(dir, "older.txt", "old");
        Path newer = createFile(dir, "newer.txt", "new");
        Files.setLastModifiedTime(older, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
        Files.setLastModifiedTime(newer, FileTime.from(Instant.parse("2025-01-01T00:00:00Z")));
        String res = runFiles("ls -t -r " + dir);
        // oldest first after reverse
        assertTrue(res.indexOf("older.txt") < res.indexOf("newer.txt"));
    }

    @Test
    public void testLsDirectory() throws Exception {
        Path dir = temp.newFolder("ls_dir").toPath();
        createFile(dir, "file.txt", "x");
        String res = runFiles("ls -d " + dir);
        assertFalse(res.contains("file.txt"));
        assertTrue(res.contains(dir.toString()));
    }

    @Test
    public void testLsLongRecursive() throws Exception {
        Path dir = temp.newFolder("ls_lr").toPath();
        Path sub = dir.resolve("sub");
        Files.createDirectory(sub);
        createFile(sub, "deep.txt", "z");
        String res = runFiles("ls -l -R " + dir);
        assertTrue(res.contains("Filename"));
        assertTrue(res.contains("deep.txt"));
    }

    // ==================== CpCommand ====================

    @Test
    public void testCpBasic() throws Exception {
        Path src = createFile("cp_src.txt", "content");
        Path dst = temp.getRoot().toPath().resolve("cp_dst.txt");
        runFiles("cp " + src + " " + dst);
        assertTrue(Files.exists(dst));
        assertEquals("content", Files.readString(dst).strip());
    }

    @Test
    public void testCpVerbose() throws Exception {
        Path src = createFile("cpv_src.txt", "x");
        Path dst = temp.getRoot().toPath().resolve("cpv_dst.txt");
        String res = runFiles("cp -v " + src + " " + dst);
        assertTrue(res.contains("->"));
    }

    @Test
    public void testCpNoClobber() throws Exception {
        Path src = createFile("cpn_src.txt", "new");
        Path dst = createFile("cpn_dst.txt", "original");
        runFiles("cp -n " + src + " " + dst);
        assertEquals("original", Files.readString(dst).strip());
    }

    @Test
    public void testCpForceOverridesNoClobber() throws Exception {
        Path src = createFile("cpf_src.txt", "new");
        Path dst = createFile("cpf_dst.txt", "original");
        runFiles("cp -f -n " + src + " " + dst);
        assertEquals("new", Files.readString(dst).strip());
    }

    @Test
    public void testCpRecursive() throws Exception {
        Path srcDir = temp.newFolder("cpr_src").toPath();
        createFile(srcDir, "a.txt", "aaa");
        Path subDir = srcDir.resolve("sub");
        Files.createDirectory(subDir);
        createFile(subDir, "b.txt", "bbb");
        Path dstDir = temp.getRoot().toPath().resolve("cpr_dst");
        runFiles("cp -r " + srcDir + " " + dstDir);
        assertTrue(Files.exists(dstDir.resolve("a.txt")));
        assertTrue(Files.exists(dstDir.resolve("sub/b.txt")));
    }

    // ==================== MvCommand ====================

    @Test
    public void testMvBasic() throws Exception {
        Path src = createFile("mv_src.txt", "data");
        Path dst = temp.getRoot().toPath().resolve("mv_dst.txt");
        runFiles("mv " + src + " " + dst);
        assertFalse(Files.exists(src));
        assertTrue(Files.exists(dst));
    }

    @Test
    public void testMvVerbose() throws Exception {
        Path src = createFile("mvv_src.txt", "x");
        Path dst = temp.getRoot().toPath().resolve("mvv_dst.txt");
        String res = runFiles("mv -v " + src + " " + dst);
        assertTrue(res.contains("->"));
    }

    @Test
    public void testMvNoClobber() throws Exception {
        Path src = createFile("mvn_src.txt", "new");
        Path dst = createFile("mvn_dst.txt", "original");
        runFiles("mv -n " + src + " " + dst);
        assertEquals("original", Files.readString(dst).strip());
        assertTrue(Files.exists(src));
    }

    // ==================== RmCommand ====================

    @Ignore("rm disabled for now")
    @Test
    public void testRmBasic() throws Exception {
        Path f = createFile("rm_basic.txt", "x");
        runFiles("rm " + f);
        assertFalse(Files.exists(f));
    }

    @Ignore("rm disabled for now")
    @Test
    public void testRmRecursive() throws Exception {
        Path dir = temp.newFolder("rm_rec").toPath();
        createFile(dir, "child.txt", "x");
        runFiles("rm -r " + dir);
        assertFalse(Files.exists(dir));
    }

    @Ignore("rm disabled for now")
    @Test
    public void testRmForceNonExistent() throws Exception {
        Path nonExistent = temp.getRoot().toPath().resolve("does_not_exist.txt");
        runFiles("rm -f " + nonExistent);
        // no exception expected
    }

    @Ignore("rm disabled for now")
    @Test(expected = Exception.class)
    public void testRmNonExistentWithoutForce() throws Exception {
        Path nonExistent = temp.getRoot().toPath().resolve("ghost.txt");
        runFiles("rm " + nonExistent);
    }

    @Ignore("rm disabled for now")
    @Test
    public void testRmVerbose() throws Exception {
        Path f = createFile("rmv.txt", "x");
        String res = runFiles("rm -v " + f);
        assertTrue(res.contains("removed"));
    }

    @Ignore("rm disabled for now")
    @Test
    public void testRmDir() throws Exception {
        Path dir = temp.newFolder("rm_emptydir").toPath();
        runFiles("rm -d " + dir);
        assertFalse(Files.exists(dir));
    }

    @Ignore("rm disabled for now")
    @Test(expected = Exception.class)
    public void testRmDirNonEmpty() throws Exception {
        Path dir = temp.newFolder("rm_nonempty").toPath();
        createFile(dir, "child.txt", "x");
        runFiles("rm -d " + dir);
    }

    // ==================== Helpers ====================

    private String runFiles(String cmd) throws Exception {
        return TestUtils.runGorPipe("exec gor files " + cmd);
    }

    private Path createFile(String name, String content) throws IOException {
        Path f = temp.getRoot().toPath().resolve(name);
        Files.writeString(f, content);
        return f;
    }

    private Path createFile(Path dir, String name, String content) throws IOException {
        Path f = dir.resolve(name);
        Files.writeString(f, content);
        return f;
    }
}
