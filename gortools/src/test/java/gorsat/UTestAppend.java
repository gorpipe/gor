package gorsat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestAppend {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path workPath;

    @Before
    public void setUp() {
        workPath = workDir.getRoot().toPath();
    }

    @Test
    public void testWriteNew() throws IOException {
        Path file = workPath.resolve("file.tsv");
        TestUtils.runGorPipe("norrows 3 | tsvappend " + file);

        Assert.assertTrue(Files.exists(file));
        Assert.assertEquals("#RowNum\n0\n1\n2\n", Files.readString(file));
    }

    @Test
    public void testWriteNewNor() throws IOException {
        Path file = workPath.resolve("file.nor");
        TestUtils.runGorPipe("norrows 3 | tsvappend " + file);

        Assert.assertTrue(Files.exists(file));
        Assert.assertEquals("#RowNum\n0\n1\n2\n", Files.readString(file));
    }

    @Test
    public void testWriteAppend() throws IOException {
        Path file = workPath.resolve("file.tsv");
        Files.write(file, "#RowNum\n0\n1\n2\n".getBytes());

        TestUtils.runGorPipe("norrows 2 | tsvappend " + file);

        Assert.assertTrue(Files.exists(file));
        Assert.assertEquals("#RowNum\n0\n1\n2\n0\n1\n", Files.readString(file));
    }

    @Test
    public void testWriteAppendNor() throws IOException {
        Path file = workPath.resolve("file.nor");
        Files.write(file, "#RowNum\n0\n1\n2\n".getBytes());

        TestUtils.runGorPipe("norrows 2 | tsvappend " + file);

        Assert.assertTrue(Files.exists(file));
        Assert.assertEquals("#RowNum\n0\n1\n2\n0\n1\n", Files.readString(file));
    }

    @Test
    public void testWriteAppendGor() throws IOException {
        Path file = workPath.resolve("file.tsv");
        TestUtils.runGorPipe("gorrows -p chr1:1-2 | write " + file);

        try {
            TestUtils.runGorPipe("gorrows -p chr1:1-2 | tsvappend " + file);
            Assert.fail("Should not be able to append GOR data to a TSV file");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testWriteAppendWrongHeader() throws IOException {
        Path file = workPath.resolve("file.tsv");
        Files.write(file, "#Col1\n0\n1\n2\n".getBytes());

        try {
            TestUtils.runGorPipe("gorrows -p chr1:1-2 | tsvappend " + file);
            Assert.fail("Should not be able to append if headers are different");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testWriteNewNoHeader() throws IOException {
        Path file = workPath.resolve("file.tsv");

        TestUtils.runGorPipe("norrows 3 | tsvappend -noheader " + file);
        Assert.assertEquals("0\n1\n2\n", Files.readString(file));
    }

    @Test
    public void testWriteNewNoHeaderNor() throws IOException {
        Path file = workPath.resolve("file.nor");

        try {
            TestUtils.runGorPipe("norrows 3 | tsvappend -noheader " + file);
            Assert.fail("Should be able to create nor file without a header");
        } catch (Exception e) {
            // Expected
        }
    }


    @Test
    public void testWriteAppendNorz() {
        Path file = workPath.resolve("file.norz");
        TestUtils.runGorPipe("norrows 3 | write " + file);

        try {
            TestUtils.runGorPipe("gorrows -p chr1:1-2 | tsvappend " + file);
            Assert.fail("Should not be able to append to a NORZ file");
        } catch (Exception e) {
            // Expected
        }
    }
}
