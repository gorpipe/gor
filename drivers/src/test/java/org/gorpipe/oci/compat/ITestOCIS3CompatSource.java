package org.gorpipe.oci.compat;

import gorsat.TestUtils;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.sources.CommonStreamTests;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.ExtendedRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryStreamSourceWrapper;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.s3.driver.S3Source;
import org.gorpipe.s3.driver.S3SourceType;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

@Category(IntegrationTests.class)
public class ITestOCIS3CompatSource extends CommonStreamTests {

    private static String S3_KEY;
    private static String S3_SECRET;
    private static String S3_ENDPOINT;
    private static String S3_REGION = "us-ashburn-1";
    private static final String S3_BUCKET = "gdb-gor-test-data-dev";

    private String securityContext;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

//    @Rule
//    public final ProvideSystemProperty awsAccessKeyId
//            = new ProvideSystemProperty("aws.accessKeyId", S3_KEY);
//
//    @Rule
//    public final ProvideSystemProperty awsSecretKey
//            = new ProvideSystemProperty("aws.secretKey", S3_SECRET);
//
//    @Rule
//    public final ProvideSystemProperty awsSecretAccessKey
//            = new ProvideSystemProperty("aws.secretAccessKey", S3_SECRET);
//
//    @Rule
//    public final ProvideSystemProperty awsRegion
//            = new ProvideSystemProperty("aws.region", S3_REGION);
//
//    @Rule
//    public final ProvideSystemProperty awsEndPoint
//            = new ProvideSystemProperty("aws.endpointUrl", S3_ENDPOINT);

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();


    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_COMPAT_KEY");
        S3_SECRET = props.getProperty("S3_COMPAT_SECRET");
        S3_ENDPOINT = props.getProperty("S3_COMPAT_ENDPOINT");
    }

    @Before
    public void setUp() {
        securityContext = DriverUtils.createSecurityContext("s3", S3_BUCKET,  Credentials.OwnerType.System, "", S3_KEY, S3_SECRET, S3_ENDPOINT, "");
    }


    @Override
    protected String getDataName(String name) {
        return "s3://%s/csa_test_data/data_sets/gor_driver_testfiles/%s".formatted(S3_BUCKET, name);
    }

    @Override
    protected StreamSource createSource(String name) throws IOException {
        return new S3Source(newClient(), new SourceReference(name));
    }

    private S3Client newClient() {
        var credProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.builder()
                        .accessKeyId(S3_KEY)
                        .secretAccessKey(S3_SECRET).build());
        var builder = S3Client.builder()
                .region(Region.of(S3_REGION))
                .endpointOverride(URI.create(S3_ENDPOINT)) // Needed for OCI
                .credentialsProvider(credProvider)
                .forcePathStyle(true); // Needed for OCI
        return builder.build();
    }

    @Override
    protected String expectCanonical(StreamSource source, String name) {
        return name;
    }

    @Override
    protected void verifyDriverDataSource(String name, DataSource fs) {
        Assert.assertEquals(ExtendedRangeWrapper.class, fs.getClass());
        fs = ((ExtendedRangeWrapper) fs).getWrapped();
        Assert.assertEquals(RetryStreamSourceWrapper.class, fs.getClass());
        fs = ((RetryStreamSourceWrapper) fs).getWrapped();
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
            String res = TestUtils.runGorPipe("create xxx = gor -f 'stuff' genes.gord | top 10; gor [xxx]", true, securityContext);
            Assert.assertEquals("Wrong result from s3 dictionary", """
                    chrom\tpos\ta\tSource
                    chr1\t0\tb\tstuff
                    """, res);
        } finally {
            if(Files.exists(p)) Files.delete(p);
        }
    }

    @Test
    public void testS3Write() {
        TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write s3://%s/gor_unittests/s3write/genes.gor".formatted(S3_BUCKET),
                false, securityContext );
    }

    @Ignore("Local file, also too large and slow to use always, no clean up")
    @Test
    public void testS3WriteLargeFile() {
        long startTime = System.currentTimeMillis();
        TestUtils.runGorPipe(String.format("gorrows -p chr1:1-1000000000 | calc data 'Some dummy data to fatten the lines, boooooooooo' | write s3://%s/gor_unittests/s3write/large.gor".formatted(S3_BUCKET)), false, securityContext);
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Time: " + duration/(1000) + " s");
    }

    @Ignore("Local file, also too large and slow to use always, no clean up")
    @Test
    public void testS3WriteMoreThanMaxChunks() {
        System.setProperty("gor.s3.write.chunksize", String.valueOf(1 << 21));
        try {
            TestUtils.runGorPipe(String.format("gorrows -p chr1:1-1000000000 | calc data 'Some dummy data to fatten the lines, boooooooooo' | write s3://%s/gor_unittests/s3write/large.gor".formatted(S3_BUCKET)));
            Assert.fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Output stream limit exceeded: 20971651072 > 20971520000", e.getMessage());
        }
    }

    @Ignore("Too slow to always run")
    @Test
    public void testS3WritePgorGord() throws IOException {
        String randomId = UUID.randomUUID().toString();
        String dict = String.format("s3://%s/gor_unittests/s3write/%s-genes.gord/", S3_BUCKET, randomId);
        TestUtils.runGorPipe("pgor -split 2 ../tests/data/gor/genes.gor | top 2 | write " + dict, false, securityContext);
        String expected = TestUtils.runGorPipe("create x = pgor -split 2 ../tests/data/gor/genes.gor | top 2; gor [x] | select 1-4", false, securityContext);
        String result = TestUtils.runGorPipe("gor " + dict + " | select 1-4", false, securityContext);
        Assert.assertEquals(expected, result);
        DriverBackedFileReader fileReader = new DriverBackedFileReader(securityContext);
        fileReader.deleteDirectory(dict);
    }

    @Test
    public void testS3WriteServerMode() {
        TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write s3://%s/gor_unittests/s3write/genes.gor".formatted(S3_BUCKET),
                true, securityContext, new String[] {"s3://"});
    }

    @Test
    public void testS3NotAllBytesEx() throws IOException {
        TestUtils.runGorPipe("gor  s3://%s/csa_test_data/data_sets/ref/versions/hg19/dbsnp.gorz -p chr2 | top 1000000 | group genome -count".formatted(S3_BUCKET),
                true, securityContext, null);
        Assert.assertFalse(systemErrRule.getLog().contains("Not all bytes were read from the S3ObjectInputStream"));
    }

    @Test
    public void testS3Meta() {
        var result = TestUtils.runGorPipe("meta s3://%s/csa_test_data/data_sets/ref/versions/hg19/dbsnp.gorz".formatted(S3_BUCKET), true, securityContext, new String[] {"s3://"});

        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.contains("SOURCE\tTYPE\tS3"));
    }

    @Test
    public void testS3MetaWithMetafile() {
        TestUtils.runGorPipe("gor ../tests/data/gor/genes.gor | top 1 | write s3://%s/gor_unittests/s3write/genes.gorz".formatted(S3_BUCKET), true, securityContext, new String[] {"s3://"});
        var result = TestUtils.runGorPipe("meta s3://%s/gor_unittests/s3write/genes.gorz".formatted(S3_BUCKET), true, securityContext, new String[] {"s3://"});

        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.contains("SOURCE\tTYPE\tS3"));
        Assert.assertTrue(result.contains("GOR\tMD5"));
        Assert.assertTrue(result.contains("GOR\tLINE_COUNT"));
    }

    @Override
    protected SourceReference mkSourceReference(String name) {
        return new SourceReferenceBuilder(name).securityContext(securityContext()).build();
    }

    @Override
    protected long expectedTimeStamp(String s) {
        return  newClient().headObject(HeadObjectRequest.builder()
                .bucket(S3_BUCKET)
                .key("csa_test_data/data_sets/gor_driver_testfiles/" + s)
                .build()).lastModified().toEpochMilli();
    }

    @Override
    protected String securityContext() {
        return securityContext;
    }
}
