package org.gorpipe.s3.driver;

import org.gorpipe.gor.driver.providers.stream.sources.CommonFilesTests;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.SystemErrRule;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.Properties;

public class ITestFilesS3Source extends CommonFilesTests {

    private static String S3_KEY;
    private static String S3_SECRET;
    private static String S3_REGION = "eu-west-1";

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue
            = new ProvideSystemProperty("aws.accessKeyId", S3_KEY);

    @Rule
    public final ProvideSystemProperty otherPropertyIsMissing
            = new ProvideSystemProperty("aws.secretKey", S3_SECRET);

    @Rule
    public final ProvideSystemProperty awsSecretAccessKey
            = new ProvideSystemProperty("aws.secretAccessKey", S3_SECRET);

    @Rule
    public final ProvideSystemProperty awsRegion
            = new ProvideSystemProperty("aws.region", S3_REGION);

    @Rule
    public final ProvideSystemProperty gorSecurityContext
            = new ProvideSystemProperty("gor.security.context", securityContext());

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    public ITestFilesS3Source() throws IOException {
    }


    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");

    }

    @Override
    protected String securityContext() throws IOException {
        return DriverUtils.awsSecurityContext(S3_KEY, S3_SECRET);
    }


    @Override
    protected String getDataName(String name) {
        return "s3://gdb-unit-test-data/csa_test_data/data_sets/bvl_min_gor/" + name;
    }

    @Override
    protected StreamSource createSource(String name) throws IOException {
        return new S3Source(newClient(), new SourceReference(name));
    }

    private S3Client newClient() {
        var credProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.builder().accessKeyId(S3_KEY).secretAccessKey(S3_SECRET).build());
        var builder = S3Client.builder()
                .region(Region.of(S3_REGION))
                .credentialsProvider(credProvider);
        return builder.build();
    }
}
