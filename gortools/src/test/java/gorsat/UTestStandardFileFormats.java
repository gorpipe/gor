package gorsat;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

/**
 * Tests for NOR-ing the following data types: .tsv, .csv
 */
public class UTestStandardFileFormats {
    private static File tmpDir;

    private static final String wanted_csv = "ChromNOR\tPosNOR\tcol1\tcol2\tcol3\tcol4\n" +
            "chrN\t0\tthis\tis\tcsv\t1\n" +
            "chrN\t0\tthis\tis\tcsv\t2\n" +
            "chrN\t0\tthis\tis\tcsv\t3\n";

    private static final String wanted_tsv = "ChromNOR\tPosNOR\tcol1\tcol2\tcol3\tcol4\n" +
            "chrN\t0\tthis\tis\ttsv\t1\n" +
            "chrN\t0\tthis\tis\ttsv\t2\n" +
            "chrN\t0\tthis\tis\ttsv\t3\n";

    @BeforeClass
    public static void setup() throws IOException {
        tmpDir = Files.createTempDirectory("uteststandardfileformats").toFile();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(tmpDir);
    }

    private static String writeLinesToFile(String[] lines, String path, boolean zip) throws IOException {
        final File file = new File(tmpDir, zip ? path + ".gz" : path);
        final BufferedWriter bw = new BufferedWriter(zip ? new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file))) : new FileWriter(file));
        for (String line : lines) {
            bw.write(line + "\n");
        }
        bw.close();
        return file.getAbsolutePath();
    }

    @Test
    public void test_nor_tsv() throws IOException {
        final String[] lines = {"#col1\tcol2\tcol3\tcol4",
                "this\tis\ttsv\t1",
                "this\tis\ttsv\t2",
                "this\tis\ttsv\t3"};
        final String path = writeLinesToFile(lines, "tsvfile.tsv", false);
        final String results = TestUtils.runGorPipe(String.format("nor %s", path));
        Assert.assertEquals(wanted_tsv, results);
    }

    @Test
    public void test_nor_tsv_zipped() throws IOException {
        final String[] lines = {"#col1\tcol2\tcol3\tcol4",
                "this\tis\ttsv\t1",
                "this\tis\ttsv\t2",
                "this\tis\ttsv\t3"};
        final String path = writeLinesToFile(lines, "tsvfile.tsv", true);
        final String results = TestUtils.runGorPipe(String.format("nor %s", path));
        Assert.assertEquals(wanted_tsv, results);
    }

    @Test
    public void test_nor_csv() throws IOException {
        final String[] lines = {"#col1,col2,col3,col4",
                "this,is,csv,1",
                "this,is,csv,2",
                "this,is,csv,3"};
        final String path = writeLinesToFile(lines, "csvfile.csv", false);
        final String results = TestUtils.runGorPipe(String.format("nor %s", path));
        Assert.assertEquals(wanted_csv, results);
    }

    @Test
    public void test_nor_csv_zipped() throws IOException {
        final String[] lines = {"#col1,col2,col3,col4",
                "this,is,csv,1",
                "this,is,csv,2",
                "this,is,csv,3"};
        final String path = writeLinesToFile(lines, "csvfile.csv", true);
        final String results = TestUtils.runGorPipe(String.format("nor %s", path));
        Assert.assertEquals(wanted_csv, results);
    }
}
