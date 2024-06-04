package org.gorpipe.gor.driver.providers.stream.sources;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.utils.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static gorsat.TestUtils.assertTwoGorpipeResults;
import static org.gorpipe.gor.driver.utils.TestUtils.md5;

/**
 * Created by villi on 25/08/15.
 */
public abstract class CommonStreamTests {

    private static final Logger log = LoggerFactory.getLogger(CommonStreamTests.class);

    protected abstract String getDataName(String name);

    protected abstract StreamSource createSource(String name) throws IOException;

    protected abstract String expectCanonical(StreamSource source, String name) throws IOException;

    protected String securityContext() {
        return null;
    }

    protected String DUMMY_FILE_STRING = "chrom\tpos\ta\nchr1\t0\tb";

    @Test
    public void testMetadataOnEmpty() throws IOException {
        String name = getDataName("empty_file.txt");
        StreamSource fs = createSource(name);
        StreamSourceMetadata meta = fs.getSourceMetadata();
        Assert.assertEquals(expectCanonical(fs, name), meta.getCanonicalName());
        Assert.assertEquals(0, (long) meta.getLength());
    }

    @Test
    public void testMoreSizes() throws Exception {
        StreamSource fs = createSource(getDataName("lines_1000.txt"));
        Assert.assertEquals(4000, (long) fs.getSourceMetadata().getLength());

        fs = createSource(getDataName("lines_10000.txt"));
        Assert.assertEquals(50000, (long) fs.getSourceMetadata().getLength());

    }

    @Test
    public void testFullStream() throws Exception {
        byte[] buf = new byte[60000];
        InputStream stream;
        StreamSource fs;

        fs = createSource(getDataName("empty_file.txt"));
        stream = fs.open();
        Assert.assertEquals(-1, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        stream.close();

        fs = createSource(getDataName("lines_1000.txt"));
        stream = fs.open();
        Assert.assertEquals(4000, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        Assert.assertEquals("c7f1529561a520a21b672b85e6accb18", md5(buf, 0, 4000));
        stream.close();

        fs = createSource(getDataName("lines_10000.txt"));
        stream = fs.open();
        Assert.assertEquals(50000, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        Assert.assertEquals("b56372ebf4f50ae9c4cbf67a9f8326e4", md5(buf, 0, 50000));
        stream.close();

    }

    @Test
    public void testPartialStream() throws Exception {
        byte[] buf = new byte[60000];
        InputStream stream;
        StreamSource fs;

        fs = createSource(getDataName("empty_file.txt"));
        stream = fs.open(0);
        Assert.assertEquals(-1, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        stream.close();

        fs = createSource(getDataName("lines_1000.txt"));
        stream = fs.open(0);
        Assert.assertEquals(4000, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        Assert.assertEquals("c7f1529561a520a21b672b85e6accb18", md5(buf, 0, 4000));
        stream.close();

        fs = createSource(getDataName("lines_10000.txt"));
        stream = fs.open(0);
        Assert.assertEquals(50000, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        Assert.assertEquals("b56372ebf4f50ae9c4cbf67a9f8326e4", md5(buf, 0, 50000));
        stream.close();

    }

    @Test
    public void testRangeStream() throws Exception {
        byte[] buf = new byte[60000];
        InputStream stream;
        StreamSource fs;

        fs = createSource(getDataName("empty_file.txt"));

        stream = fs.open(0, 100);
        Assert.assertEquals(-1, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        stream.close();

        // Full stream with ranged request
        fs = createSource(getDataName("lines_1000.txt"));
        stream = fs.open(0, 60000);
        Assert.assertEquals(4000, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        Assert.assertEquals("c7f1529561a520a21b672b85e6accb18", md5(buf, 0, 4000));
        stream.close();

        // Start of file
        fs = createSource(getDataName("lines_1000.txt"));
        stream = fs.open(0, 8);
        Assert.assertEquals(8, StreamUtils.readToBuffer(stream, buf, 0, 8));
        Assert.assertEquals("000\n001\n", new String(buf, 0, 8));
        stream.close();

        // Inside file
        fs = createSource(getDataName("lines_1000.txt"));
        stream = fs.open(2000, 8);
        Assert.assertEquals(8, StreamUtils.readToBuffer(stream, buf, 0, 8));
        Assert.assertEquals("500\n501\n", new String(buf, 0, 8));
        stream.close();

        // End of file
        fs = createSource(getDataName("lines_1000.txt"));
        stream = fs.open(3992, 8);
        Assert.assertEquals(8, StreamUtils.readToBuffer(stream, buf, 0, 100));
        Assert.assertEquals("998\n999\n", new String(buf, 0, 8));
        stream.close();

        // Full stream with ranged request

        fs = createSource(getDataName("lines_10000.txt"));
        stream = fs.open(0, 60000);
        Assert.assertEquals(50000, StreamUtils.readToBuffer(stream, buf, 0, 60000));
        Assert.assertEquals("b56372ebf4f50ae9c4cbf67a9f8326e4", md5(buf, 0, 50000));
        stream.close();

        // Start of file
        fs = createSource(getDataName("lines_10000.txt"));
        stream = fs.open(0, 10);
        Assert.assertEquals(10, StreamUtils.readToBuffer(stream, buf, 0, 10));
        Assert.assertEquals("0000\n0001\n", new String(buf, 0, 10));
        stream.close();

        // Inside file
        fs = createSource(getDataName("lines_10000.txt"));
        stream = fs.open(25000, 10);
        Assert.assertEquals(10, StreamUtils.readToBuffer(stream, buf, 0, 10));
        Assert.assertEquals("5000\n5001\n", new String(buf, 0, 10));
        stream.close();

        // End of file
        fs = createSource(getDataName("lines_10000.txt"));
        stream = fs.open(49990, 10);
        Assert.assertEquals(10, StreamUtils.readToBuffer(stream, buf, 0, 10));
        Assert.assertEquals("9998\n9999\n", new String(buf, 0, 10));
        stream.close();

    }

    @Test
    public void testContinousSeeks() throws IOException {
        // End of file
        StreamSource fs = createSource(getDataName("lines_1000.txt"));
        InputStream stream = null;
        byte[] buf = new byte[200];

        for (int i = 0; i < 20; i++) {
            stream = fs.open(i * 200, 200);
            Assert.assertEquals(200, StreamUtils.readToBuffer(stream, buf, 0, 200));
            Assert.assertEquals(rangedLines(i * 50, 50, "%03d\n"), new String(buf));
            fs.close();
        }
    }

    @Test
    public void testBackwardSeeks() throws IOException {
        // End of file
        StreamSource fs = createSource(getDataName("lines_1000.txt"));
        InputStream stream = null;
        byte[] buf = new byte[200];

        for (int j = 0; j < 20; j++) {
            int i = 20 - j - 1;
            stream = fs.open(i * 200, 200);
            Assert.assertEquals(200, StreamUtils.readToBuffer(stream, buf, 0, 200));
            Assert.assertEquals(rangedLines(i * 50, 50, "%03d\n"), new String(buf));
            fs.close();
        }
    }

    @Test
    public void testRandomSeeks() throws IOException {
        // End of file
        StreamSource fs = createSource(getDataName("lines_1000.txt"));
        InputStream stream = null;
        byte[] buf = new byte[200];
        for (long seed : getSeeds()) {
            List<Integer> indexes = getRandomIndexes(20, seed);
            for (int j = 0; j < 20; j++) {
                int i = indexes.get(j);
                stream = fs.open(i * 200, 200);
                Assert.assertEquals(200, StreamUtils.readToBuffer(stream, buf, 0, 200));
                Assert.assertEquals(rangedLines(i * 50, 50, "%03d\n"), new String(buf));
                fs.close();
            }
        }
    }

    @Test
    public void testGetName() throws IOException {
        String name = getDataName("empty_file.txt");
        StreamSource fs = createSource(name);
        Assert.assertEquals(name, fs.getName());
    }

    @Test
    public void testGetSourceType() throws IOException {
        String name = getDataName("empty_file.txt");
        StreamSource fs = createSource(name);
        Assert.assertEquals(expectedSourcetype(fs), fs.getSourceType());
    }

    @Test
    public void testGetTimestamp() throws IOException {
        String name = getDataName("empty_file.txt");
        StreamSource fs = createSource(name);

        Assert.assertEquals(expectedTimeStamp("empty_file.txt"), fs.getSourceMetadata().getLastModified().longValue());
    }

    protected long expectedTimeStamp(String s) throws IOException {
        String file = TestUtils.getTestFile(s);
        return Files.getLastModifiedTime(Paths.get(file)).toMillis();
    }

    @Test
    public void testGetAttributes() throws IOException {
        String name = getDataName("dummy.gor");
        StreamSource fs = createSource(name);
        StreamSourceMetadata md = fs.getSourceMetadata();
        Map<String, String> attr = md.attributes();
        Assert.assertEquals("GOR", attr.get("DataType"));
        Assert.assertEquals(expectedAttributeSourcetype(fs).getName(), attr.get("SourceType"));
        Assert.assertEquals(DUMMY_FILE_STRING.length(), Long.valueOf(attr.get("ByteLength")).longValue());
    }

    @Test
    public void testExists() throws IOException {
        String name = getDataName("empty_file.txt");
        StreamSource fs = createSource(name);
        Assert.assertTrue(fs.exists());
        fs.close();

        try {
            fs = createSource(getDataName("no_such_file"));
        } catch (FileNotFoundException fnfe) {
            // Get this for http, ignore.
        }
        boolean existsAfterClosing = fs.exists();
        Assert.assertTrue(!existsAfterClosing);
    }

    @Test
    public void testGetSourceMetadata() throws IOException {
        String name = getDataName("empty_file.txt");
        StreamSource fs = createSource(name);
        SourceMetadata sourceMetadata = fs.getSourceMetadata();
        Assert.assertNotNull(sourceMetadata);
        fs.close();

        try {
            fs = createSource(getDataName("no_such_file"));
        } catch (FileNotFoundException fnfe) {
            // Get this for http, ignore.
        }

        try {
            SourceMetadata sourceMetadataAfterClosing = fs.getSourceMetadata();
        } catch (NullPointerException npe) {
            // A null pointer exception could be thrown due to underlying data source being null.
            log.info(npe.getMessage());
        } catch (GorResourceException ioe) {
            // An exception could be thrown due to retries failing.
            log.info(ioe.getMessage());
        } catch (Exception e) {
            // An Amazon Exception could be thrown. If it contains status code 404 it means that the file was not found which is ok.
            log.info(e.getMessage());
            if (!e.getMessage().contains("Status Code: 404")) {
                throw e;
            }
        }
    }

    @Test
    public void testDriver() throws IOException {
        String name = getDataName("empty_file.txt");
        DataSource fs = GorDriverFactory.fromConfig().getDataSource(mkSourceReference(name));
        verifyDriverDataSource(name, fs);
    }

    protected SourceReference mkSourceReference(String name) throws IOException {
        return new SourceReference(name);
    }

    @Test
    public void testGetNamedUrl() throws IOException {
        // This just tests that the method returns something
        String name = getDataName("dummy.gor");
        StreamSource fs = createSource(name);
        Assert.assertTrue(fs.getSourceMetadata().getNamedUrl().contains("dummy.gor"));
    }

    @Test
    public void testNor() throws IOException {
        String testFile = "nor/simple_result.nor";
        String source = TestUtils.getTestFile(testFile);

        TestUtils.assertFullGor(securityContext(),
                source,
                TestUtils.readFile(new File(TestUtils.getTestFile(testFile))));
    }

    @Test
    public void testNorz() throws IOException {
        String source = TestUtils.getTestFile("nor/simple.norz");
        String testFile = TestUtils.getTestFile("nor/simple_result.nor");

        assertTwoGorpipeResults("nor " + testFile + " | select 3-", "nor " + source);
    }

    protected abstract void verifyDriverDataSource(String name, DataSource fs);

    protected abstract SourceType expectedSourcetype(StreamSource fs);

    protected SourceType expectedAttributeSourcetype(StreamSource fs) {
        return expectedSourcetype(fs);
    }

    @Test
    public void testGetDataType() throws IOException {
        String name = getDataName("dummy.gor");
        StreamSource fs = createSource(name);
        Assert.assertEquals(DataType.GOR, fs.getDataType());

        name = getDataName("file.unknown_type");
        fs = createSource(name);
        Assert.assertEquals(null, fs.getDataType());
    }

    public String rangedLines(int start, int length, String format) {
        StringBuilder b = new StringBuilder();
        for (int i = start; i < start + length; i++) {
            b.append(String.format(format, i));
        }

        return b.toString();
    }

    public long[] getSeeds() {
        long[] seeds = {System.nanoTime()};
        return seeds;
    }

    public List<Integer> getRandomIndexes(int count, long seed) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        // Record in test log - for reproducability
        log.info("Shuffle seed: " + seed);
        Collections.shuffle(list, new Random(seed));
        return list;
    }

}
