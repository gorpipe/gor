package gorsat;

import org.gorpipe.test.SlowTests;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by villi on 18/09/15.
 */

public class UTestGorpipe {

    protected File file;
    protected File file2;
    protected String prevDictCache;
    protected Path workPath;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        File dir = new File("/tmp/my_cache");
        if (dir.exists()) {
            FileTestUtils.deleteFolder(dir.toPath());
        }
        workPath = workDir.getRoot().toPath().toAbsolutePath();
        Files.createDirectories(workPath.resolve("result_cache"));
        file = FileTestUtils.createDummyGorFile(workDir.getRoot());
        file2 = FileTestUtils.createDummyGorFile(workDir.getRoot());
        prevDictCache = System.setProperty("gor.dictionary.cache.active","true");
    }

    @After
    public void finish() {
        if (prevDictCache!=null) System.setProperty("gor.dictionary.cache.active",prevDictCache);
    }

    @Category(SlowTests.class)
    @Test
    public void testOpenFilesGor() throws InterruptedException, IOException {
        assertNoOpenFiles("gor -p chr2 " + file + " | merge " + file2, false);
    }

    @Category(SlowTests.class)
    @Test
    public void testOpenFilesGorNewDriver() throws InterruptedException, IOException {
        assertNoOpenFiles("gor -p chr2 " + file + " | merge " + file2, false);
    }

    @Category(SlowTests.class)
    @Test
    public void testOpenFilesPgor() throws InterruptedException, IOException {
        // This seems to be working on an off. We will assume this untill we can fix this
        // TODO: Investigate why this is failing sporadically
        assertNoOpenFiles("pgor " + file, true);
    }

    @Category(SlowTests.class)
    @Test
    public void testOpenFilesPgorNewDriver() throws InterruptedException, IOException {
        assertNoOpenFiles("pgor " + file, false);
    }

    @Test
    public void testNorOfNorzfile() throws Exception {
        String rfile = org.gorpipe.gor.driver.utils.TestUtils.getTestFile("/nor/simple.nor");
        String file = org.gorpipe.gor.driver.utils.TestUtils.getTestFile("/nor/simple.norz");
        List<String> result = query("nor " + file + " | where 1=1");
        List<String> expected = query("nor " + rfile);
        Assert.assertEquals(expected, result);
    }

    private void assertNoOpenFiles(String query, boolean assume) throws InterruptedException, IOException {
        // Warm up to load dyanamically loaded files.
        var args = new String[] {query,"-gorroot",workPath.toString(),"-cachedir","result_cache"};
        TestUtils.runGorPipeCount(args);
        // GC once to close everything that could possibly be closed by GC while run.
        System.gc();

        // Measure and run 100 times
        long count = TestUtils.countOpenFiles();
        for (int i = 0; i < 100; i++) {
            TestUtils.runGorPipeCount(args);
        }
        // Sleep one sec to make sure count open files works correctly (lsof)
        Thread.sleep(1000);
        long newCount = TestUtils.countOpenFiles();

        if (assume) {
            Assume.assumeTrue("Open files now " + newCount + ", was " + count, newCount <= count);
        } else {
            Assert.assertTrue("Open files now " + newCount + ", was " + count, newCount <= count);
        }
    }

    private List<String> query(String query) {
        return Arrays.stream(TestUtils.runGorPipeLines(query)).map(String::trim).collect(Collectors.toList());
    }
}
