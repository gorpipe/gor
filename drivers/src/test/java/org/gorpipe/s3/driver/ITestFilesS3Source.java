package org.gorpipe.s3.driver;

import com.amazonaws.auth.BasicAWSCredentials;
import org.gorpipe.gor.driver.providers.stream.sources.CommonFilesTests;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
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

        Credentials.Builder cb = new Credentials.Builder()
                .service("s3")
                .lookupKey("nextcode-unittest")
                .set(Credentials.Attr.KEY, S3_KEY)
                .set(Credentials.Attr.SECRET, S3_SECRET)
                .set(Credentials.Attr.REGION, "us-west-2");
        BundledCredentials.Builder bb = new BundledCredentials.Builder().addCredentials(cb.build());
        System.setProperty("gor.security.context", bb.build().addToSecurityContext(null));
    }


    @Override
    protected String getDataName(String name) {
        return "s3://nextcode-unittest/csa_test_data/data_sets/bvl_min_gor/" + name;
    }

    @Override
    protected StreamSource createSource(String name) throws IOException {

        return new S3Source(newClient(),
                new SourceReference(name));
    }

    private S3Client newClient() {
        return new S3Client(new BasicAWSCredentials(S3_KEY, S3_SECRET));
    }

}
