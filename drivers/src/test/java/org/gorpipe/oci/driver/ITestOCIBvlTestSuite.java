package org.gorpipe.oci.driver;

import org.gorpipe.gor.driver.providers.stream.datatypes.BvlTestSuite;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.experimental.categories.Category;
import java.io.IOException;
import java.util.Properties;

@Category(IntegrationTests.class)
public class ITestOCIBvlTestSuite extends BvlTestSuite {

    private String bucketName = "gdb-gor-test-data-dev";

    private static String OCI_TENANT;
    private static String OCI_USER;

    private static String OCI_SIMPLE_PRIVATE_KEY;
    private static String OCI_SIMPLE_FINGERPRINT;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        OCI_TENANT = props.getProperty("OCI_TENANT");
        OCI_USER = props.getProperty("OCI_USER");
        OCI_SIMPLE_PRIVATE_KEY = props.getProperty("OCI_SIMPLE_PRIVATE_KEY");
        OCI_SIMPLE_FINGERPRINT = props.getProperty("OCI_SIMPLE_FINGERPRINT");
    }

    public ITestOCIBvlTestSuite() throws IOException {
    }

    @Override
    protected String getSourcePath(String name) {
        return String.format("oci://%s/csa_test_data/data_sets/bvl_min_gor/%s", bucketName,  name);
    }

    @Override
    protected String securityContext()  {
        return DriverUtils.ociSecurityContext(OCI_TENANT, OCI_USER, OCI_SIMPLE_PRIVATE_KEY, OCI_SIMPLE_FINGERPRINT);
    }

    @Before
    public void setupTest() {
        System.setProperty("org.gorpipe.gor.driver.retries.initial_sleep", "5 milliseconds");
    }

}
