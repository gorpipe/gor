package org.gorpipe.gor.driver.providers.stream.sources;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceRetryHandler;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceType;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryStreamSourceWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by villi on 25/08/15.
 */
public class UTestFileSource extends CommonStreamTests {

    private static final Logger log = LoggerFactory.getLogger(UTestFileSource.class);

    @Test
    public void testSymlink() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        File symlink = new File(tempDir.toFile(), "link.txt");
        Files.createSymbolicLink(symlink.toPath(), lines1000File.toPath());

        String linkPath = symlink.getPath();
        // Ensure that we are working with unexpanded path to symlink
        Assert.assertTrue(linkPath.endsWith("/link.txt"));

        StreamSourceMetadata sourceMetadata;
        try (StreamSource fs = createSource(linkPath)) {
            sourceMetadata = fs.getSourceMetadata();

            log.info("Metadata attributes: {}", sourceMetadata.attributes());
            Assert.assertEquals(lines1000File.lastModified(), sourceMetadata.getLastModified().longValue());
            Assert.assertEquals(4000, sourceMetadata.getLength().longValue());
            Assert.assertEquals("file://" + lines1000File.getCanonicalPath(), sourceMetadata.attributes().get("CanonicalName"));
        }
    }

    @Test
    public void testAtomicWrite() throws IOException {
        File outfile = new File(workDir.getRoot(), "text.txt");
        outfile.deleteOnExit();

        try (StreamSource source = createSource(getDataName(outfile)); OutputStream os = source.getOutputStream()) {
            os.write("Hello".getBytes());
            os.flush();

            Assert.assertFalse(Files.exists(Path.of(outfile.getPath())));
            Assert.assertTrue(Files.list(Path.of(workDir.getRoot().getPath()))
                    .anyMatch(p -> p.getFileName().toString().matches("text-temp-.*\\.txt")));

            os.write(" World".getBytes());
        }
        Assert.assertTrue(Files.exists(Path.of(outfile.getPath())));
        Assert.assertTrue(Files.list(Path.of(workDir.getRoot().getPath()))
                .allMatch(p -> !p.getFileName().toString().matches("text-temp-.*\\.txt")));
    }

    @Test
    public void testStackOverflowInRetry() throws IOException {
        File f = TestUtils.createGorFile( "test", new String[]{"chrom\tpos\tvar", "chr1\t1\t10"});

        final int[] readCounter = {0};
        StreamSource ss = new RetryStreamSourceWrapper(
                new FileSourceRetryHandler(1, 10),
                new FileSource(new SourceReference(f.getAbsolutePath())) {
                    @Override
                    public InputStream open(long start)  {
                        ensureOpenForRead();
                        try {
                            raf.seek(start);
                        } catch (IOException e) {
                            throw GorResourceException.fromIOException(e, getPath().toString()).retry();
                        }
                        return new FileSourceStream() {
                            @Override
                            public int read(byte[] b, int off, int len) throws IOException {
                                readCounter[0]++;
                                if (readCounter[0] > 20) {
                                    throw new RuntimeException("Test failed");
                                }
                                if (off > 0) {
                                    throw new IOException("Testing failed read");
                                }
                                return super.read(b, off, len);
                            }
                        };
                    }
                });

        try (InputStream in = ss.open()) {
            //f.delete();
            var b = new byte[1];
            in.read(b, 0, 1);
            in.read(b, 1, 1);
            Assert.assertEquals('h', b[0]);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Giving up after"));
        }
    }

    @Override
    protected String getDataName(File file) throws IOException {
        return file.getCanonicalPath();
    }

    @Override
    protected StreamSource createSource(String name) {
        return new FileSource(name);
    }

    @Override
    protected String expectCanonical(StreamSource source, String name) {
        return "file://" + name;
    }

    @Override
    protected void verifyDriverDataSource(String name, DataSource fs) {
        Assert.assertEquals(RetryStreamSourceWrapper.class, fs.getClass());
        fs = ((RetryStreamSourceWrapper) fs).getWrapped();
        Assert.assertEquals(FileSource.class, fs.getClass());
    }

    @Override
    protected SourceType expectedSourcetype(StreamSource fs) {
        return FileSourceType.FILE;
    }
}
