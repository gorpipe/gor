package org.gorpipe.s3.driver;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import org.gorpipe.gor.driver.providers.stream.datatypes.BvlTestSuite;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.driver.utils.TestUtils;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.test.IntegrationTests;
import org.junit.*;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Properties;

import static org.gorpipe.utils.DriverUtils.awsSecurityContext;

@Category(IntegrationTests.class)
public class ITestBvlMinOnS3 extends BvlTestSuite {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    private static String S3_KEY;
    private static String S3_SECRET;
    private static String S3_REGION;
    private static String S3_GOOGLE_KEY;
    private static String S3_GOOGLE_SECRET;

    public ITestBvlMinOnS3() throws IOException {
    }

    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");
        S3_REGION = "eu-west-1";
        S3_GOOGLE_KEY = props.getProperty("S3_GOOGLE_KEY");
        S3_GOOGLE_SECRET = props.getProperty("S3_GOOGLE_SECRET");
    }

    @Before
    public void setupTest() {
        System.setProperty("org.gorpipe.gor.driver.retries.initial_sleep", "5 milliseconds");
    }

    @Override
    protected String getSourcePath(String name) {
        return "s3://gdb-unit-test-data/csa_test_data/data_sets/bvl_min_gor/" + name;
    }

    @Override
    protected String securityContext() throws IOException {
        return awsSecurityContext(S3_KEY, S3_SECRET);
    }

    @Test
    public void testWithDefaultCredentials() throws IOException {
        Credentials cred = new Credentials.Builder().service("s3")
                .set(Credentials.Attr.KEY, S3_KEY)
                .set(Credentials.Attr.SECRET, S3_SECRET)
                .set(Credentials.Attr.REGION, S3_REGION)
                .build();
        BundledCredentials creds = new BundledCredentials.Builder().addDefaultCredentials(cred).build();
        String sec = creds.addToSecurityContext(null);

        String source = getSourcePath("derived/raw_bam_to_gor/" + names[0] + ".bam.gor");
        SourceReference ref2 = new SourceReferenceBuilder(source).securityContext(sec).build();
        DataSource s3 = TestUtils.gorDriver.resolveDataSource(ref2);
        Assert.assertTrue(s3.exists());
    }

    @Test
    @Ignore("TODO: re-enable this as per https://nextcode.atlassian.net/browse/GOR-2169 - stefan 2019-01-24")
    public void testWithProviderCredentialsAndEndpoint() throws IOException {
        // TODO: When we have reliable s3 compatible storage account outside AWS (e.g. Google cloud) , we should test against that.
        Credentials cred = new Credentials.Builder().service("s3").lookupKey("google:01_nextcode_gor_integration_test").set(Credentials.Attr.KEY, S3_GOOGLE_KEY).set(Credentials.Attr.SECRET, S3_GOOGLE_SECRET).set(Credentials.Attr.API_ENDPOINT, "https://storage.googleapis.com").build();
        Credentials bogus = new Credentials.Builder().service("s3").lookupKey("google:bla").set(Credentials.Attr.KEY, "DummyKey").set(Credentials.Attr.SECRET, "DummySecret").build();
        BundledCredentials creds = new BundledCredentials.Builder().addCredentials(bogus, cred).build();
        String sec = creds.addToSecurityContext(null);

        String source = "s3://google:01_nextcode_gor_integration_test/csa_test_data/data_sets/bvl_min_gor/derived/raw_bam_to_gor/" + names[0] + ".bam.gor";
        SourceReference ref = new SourceReferenceBuilder(source).securityContext(sec).build();
        DataSource s3 = TestUtils.gorDriver.resolveDataSource(ref);
        Assert.assertTrue(s3.exists());

    }

    @Ignore("This test takes a long time (90s) and is only checking a bad end point (what to we expect to happen?)")
    @Test
    public void testWithProviderCredentialsAndBadEndpoint() throws IOException {
        Credentials cred = new Credentials.Builder().service("s3").lookupKey("aws:gdb-unit-test-data").set(Credentials.Attr.KEY, S3_KEY).set(Credentials.Attr.SECRET, S3_SECRET).set(Credentials.Attr.API_ENDPOINT, "https://bad.endpoint.local").build();
        Credentials bogus = new Credentials.Builder().service("s3").lookupKey("aws:bla").set(Credentials.Attr.KEY, "DummyKey").set(Credentials.Attr.SECRET, "DummySecret").build();
        BundledCredentials creds = new BundledCredentials.Builder().addCredentials(bogus, cred).build();
        String sec = creds.addToSecurityContext(null);

        String source = "s3://aws:gdb-unit-test-data/csa_test_data/data_sets/bvl_min_gor/derived/raw_bam_to_gor/" + names[0] + ".bam.gor";
        SourceReference ref = new SourceReferenceBuilder(source).securityContext(sec).build();
        try {
            DataSource ds = TestUtils.gorDriver.getDataSource(ref);
            ds.exists();
            Assert.fail("Should choke on bad endpoint");
        } catch (Exception e) {
            // Ok
        }
    }

    @Test
    @Ignore("Ignored as part of GOP-1458")
    public void testWithKeysInProperties() throws IOException {
        String source = getSourcePath("derived/raw_bam_to_gor/" + names[0] + ".bam.gor");
        SourceReference ref = new SourceReferenceBuilder(source).build();

        DataSource s3 = TestUtils.gorDriver.resolveDataSource(ref);
        Assert.assertTrue(s3.exists());
    }

    @Test
    @Ignore("This test is not running as a part of moving over to gdb-unit-test-data. This fails if there are aws credentials defined in the environment")
    public void testWithoutContext() {
        String name = names[0];
        String source = getSourcePath("derived/raw_bam_to_gor/" + name + ".bam.gor");

        SourceReference ref = new SourceReferenceBuilder(source).build();

        try {
            DataSource ds = TestUtils.gorDriver.getDataSource(ref);
            Assert.fail("Should not be able to query s3 without security context");
        } catch (GorException io) {
            // Expected
        }
        try {
            GenomicIterator iterator = TestUtils.gorDriver.createIterator(ref);
            StringBuilder builder = new StringBuilder();

            TestUtils.addHeader(builder, iterator);
            TestUtils.addLines(builder, iterator, 5);
            Assert.fail("Should not be able to read data without security context");
        } catch (IOException | GorException io) {
            // Expected
        }
    }

    @Test
    public void testWithTemporaryCredentials() throws IOException {
        // Issue temporary credentials, that will be used to access files later on.
        BasicAWSCredentials stsCapableCredentials = new BasicAWSCredentials(S3_KEY, S3_SECRET);
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(stsCapableCredentials))
                .withRegion(Regions.EU_WEST_1)
                .build();

        GetSessionTokenRequest getSessionTokenRequest = new GetSessionTokenRequest();
        getSessionTokenRequest.setDurationSeconds(7200);

        // get credentials with session tokens
        GetSessionTokenResult sessionTokenResult = stsClient.getSessionToken(getSessionTokenRequest);
        com.amazonaws.services.securitytoken.model.Credentials sessionCredentials = sessionTokenResult.getCredentials();
        Assert.assertNotNull(sessionCredentials.getSessionToken());

        // add temporary credentials to the security context
        Credentials cred = new Credentials.Builder().service("s3")
                .lookupKey("gdb-unit-test-data")
                .set(Credentials.Attr.KEY, sessionCredentials.getAccessKeyId())
                .set(Credentials.Attr.SECRET, sessionCredentials.getSecretAccessKey())
                .set(Credentials.Attr.REGION, "eu-west-1")
                .set(Credentials.Attr.SESSION_TOKEN, sessionCredentials.getSessionToken())
                .build();

        BundledCredentials creds = new BundledCredentials.Builder().addCredentials(cred).build();
        String sec = creds.addToSecurityContext(null);

        // Assert that we can access data
        String source = getSourcePath("derived/raw_bam_to_gor/" + names[0] + ".bam.gor");
        SourceReference ref = new SourceReferenceBuilder(source).securityContext(sec).build();

        DataSource s3 = TestUtils.gorDriver.resolveDataSource(ref);
        Assert.assertTrue(s3.exists());


    }

}
