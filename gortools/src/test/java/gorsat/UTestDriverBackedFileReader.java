package gorsat;

import org.gorpipe.gor.model.DriverBackedFileReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestDriverBackedFileReader {
    DriverBackedFileReader driverBackedFileReader;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void init() {
        driverBackedFileReader = new DriverBackedFileReader("", temporaryFolder.getRoot().getAbsolutePath());
    }

    @Test
    public void testFileReader() throws IOException {
        var path = Path.of("../tests/data/bvl_min/bam/BVL_INDEX_SLC52A2.bam").toAbsolutePath().normalize();
        var header = driverBackedFileReader.readHeaderLine(path.toString());
        Assert.assertEquals("Chromo\tPos\tEnd\tQName\tFlag\tMapQ\tCigar\tMD\tMRNM\tMPOS\tISIZE\tSEQ\tQUAL\tTAG_VALUES", header);
    }

    @Test
    public void testCreateAndDeleteDirectories() throws IOException {
        var path = temporaryFolder.getRoot().toPath().resolve("one/two");
        driverBackedFileReader.createDirectories("one/two");
        Assert.assertTrue(driverBackedFileReader.exists("one/two") && Files.exists(path));
        driverBackedFileReader.deleteDirectory("one/two");
        Assert.assertFalse(driverBackedFileReader.exists("one/two") && Files.exists(path));
    }
}
