package org.gorpipe.s3.driver;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.gorpipe.gor.driver.providers.stream.sources.CommonFilesTests;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.SystemErrRule;

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

        return new S3Source(newClient(),
                new SourceReference(name));
    }

    private AmazonS3 newClient() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(S3_KEY, S3_SECRET)))
                .withRegion(S3_REGION)
                .build();
    }

}
