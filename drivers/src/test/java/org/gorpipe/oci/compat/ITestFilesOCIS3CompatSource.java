package org.gorpipe.oci.compat;

import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.CommonFilesTests;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.s3.driver.S3Source;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

@Category(IntegrationTests.class)
public class ITestFilesOCIS3CompatSource extends CommonFilesTests {

    private static String S3_KEY;
    private static String S3_SECRET;
    private static String S3_ENDPOINT;
    private static String S3_REGION = "us-ashburn-1";
    private static final String S3_BUCKET = "gdb-gor-test-data-dev";

    private String securityContext;

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    public ITestFilesOCIS3CompatSource() throws IOException {
    }


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
    protected String securityContext() throws IOException {
        return securityContext;
    }

    @Override
    protected String getDataName(String name) {
        return "s3://%s/csa_test_data/data_sets/bvl_min_gor/%s".formatted(S3_BUCKET, name);
    }


    @Override
    protected StreamSource createSource(String name) throws IOException {
        return new S3Source(newClient(), new SourceReference(name, securityContext(), null, null, null, false));
    }

    private S3Client newClient() {
        System.setProperty("aws.requestChecksumCalculation", "WHEN_REQUIRED");
        System.setProperty("aws.responseChecksumValidation", "WHEN_REQUIRED");

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
}
