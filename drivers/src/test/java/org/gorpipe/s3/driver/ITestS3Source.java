package org.gorpipe.s3.driver;

import com.amazonaws.auth.BasicAWSCredentials;
import org.gorpipe.gor.driver.providers.stream.sources.CommonStreamTests;
import org.gorpipe.utils.DriverUtils;
import gorsat.TestUtils;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.ExtendedRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryWrapper;
import org.junit.*;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.SystemErrRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

//@Category(IntegrationTests.class)
public class ITestS3Source extends CommonStreamTests {

    private static String S3_KEY;
    private static String S3_SECRET;

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue
            = new ProvideSystemProperty("aws.accessKeyId", S3_KEY);

    @Rule
    public final ProvideSystemProperty otherPropertyIsMissing
            = new ProvideSystemProperty("aws.secretKey", S3_SECRET);

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();


    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");
    }


    @Override
    protected String getDataName(String name) {
        return "s3://nextcode-unittest/csa_test_data/data_sets/gor_driver_testfiles/" + name;
    }

    @Override
    protected StreamSource createSource(String name) throws IOException {
        return new S3Source(newClient(),
                new SourceReference(name));
    }

    private S3Client newClient() {
        return new S3Client(new BasicAWSCredentials(S3_KEY, S3_SECRET));
    }

    @Override
    protected String expectCanonical(StreamSource source, String name) {
        return name;
    }

    @Override
    protected void verifyDriverDataSource(String name, DataSource fs) {
        Assert.assertEquals(ExtendedRangeWrapper.class, fs.getClass());
        fs = ((ExtendedRangeWrapper) fs).getWrapped();
        Assert.assertEquals(RetryWrapper.class, fs.getClass());
        fs = ((RetryWrapper) fs).getWrapped();
        Assert.assertEquals(S3Source.class, fs.getClass());
    }

    @Override
    protected SourceType expectedSourcetype(StreamSource fs) {
        return S3SourceType.S3;
    }

    @Ignore("Fails on linux")
    @Test
    public void testS3SecurityContext() throws IOException {
        Path p = Paths.get("genes.gord");
        try {
            Files.write(p, (getDataName("dummy.gor")+"\tstuff").getBytes());
            String securityContext = DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET);
            String res = TestUtils.runGorPipe("create xxx = gor -f 'stuff' genes.gord | top 10; gor [xxx]", true, securityContext);
            Assert.assertEquals("Wrong result from s3 dictionary", "chrom\tpos\ta\tSource\n" +
                    "chr1\t0\tb\tstuff\n", res);
        } finally {
            if(Files.exists(p)) Files.delete(p);
        }
    }

    @Test
    public void testS3Write() {
        TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write s3://nextcode-unittest/s3write/genes.gor");
    }

    @Test
    public void testS3WriteServerMode() throws IOException {
        String securityContext = DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET);
        TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write s3://nextcode-unittest/s3write/genes.gor", true, securityContext, new String[] {"s3://"});
    }

    @Test
    public void testS3NotAllBytesEx() {
        TestUtils.runGorPipe("gor s3://nextcode-unittest/csa_test_data/data_sets/gor_driver_testfiles/ensgenes_transcripts.gor");
        Assert.assertFalse(systemErrRule.getLog().contains("Not all bytes were read from the S3ObjectInputStream"));
    }

    @Test
    public void testS3Meta() throws IOException {
        String securityContext = DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET);
        var result = TestUtils.runGorPipe("meta s3://nextcode-unittest/s3write/genes.gor", true, securityContext, new String[] {"s3://"});

        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.contains("source.type\tS3"));
    }

    @Test
    public void testS3MetaWithMetafile() throws IOException {
        String securityContext = DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET);
        TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write s3://nextcode-unittest/s3write/genes.gorz", true, securityContext, new String[] {"s3://"});
        var result = TestUtils.runGorPipe("meta s3://nextcode-unittest/s3write/genes.gorz", true, securityContext, new String[] {"s3://"});

        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.contains("source.type\tS3"));
        Assert.assertTrue(result.contains("data.md5"));
        Assert.assertTrue(result.contains("data.line_count"));
    }

    @Override
    protected SourceReference mkSourceReference(String name) throws IOException {
        return new SourceReferenceBuilder(name).securityContext(DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET)).build();
    }

    @Override
    protected long expectedTimeStamp(String s) {
        return newClient().getObjectMetadata("nextcode-unittest", "csa_test_data/data_sets/gor_driver_testfiles/" + s).getLastModified().getTime();
    }
}
