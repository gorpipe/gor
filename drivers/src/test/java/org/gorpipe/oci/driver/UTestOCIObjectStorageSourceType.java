package org.gorpipe.oci.driver;

import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.utils.DriverUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;


public class UTestOCIObjectStorageSourceType {

    private static String OCI_TENANT;
    private static String OCI_USER;

    private static String OCI_SIMPLE_PRIVATE_KEY;
    private static String OCI_SIMPLE_FINGERPRINT;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final ProvideSystemProperty endpoint = new ProvideSystemProperty("gor.oci.endpoint", "https://namespace.objectstorage.us-ashburn-1.oci.customer-oci.com");

    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        OCI_TENANT = props.getProperty("OCI_TENANT");
        OCI_USER = props.getProperty("OCI_USER");
        OCI_SIMPLE_PRIVATE_KEY = props.getProperty("OCI_SIMPLE_PRIVATE_KEY");
        OCI_SIMPLE_FINGERPRINT = props.getProperty("OCI_SIMPLE_FINGERPRINT");
    }

    @Test
    public void testResolveGorOciUrl() throws IOException {
        var source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("oci://gdb-gor-test-data-dev/the/path.dat", securityContext(), null, null, null, false));
        assertEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());

        source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("oc://gdb-gor-test-data-dev/the/path.dat", securityContext(), null, null, null, false));
        assertEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());
    }

    @Test
    public void testResolveOciHttpUrl() throws IOException {
        var source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("https://namespace.objectstorage.us-ashburn-1.oci.customer-oci.com/gdb-gor-test-data-dev/the/path.dat", securityContext(), null, null, null,  false));
        assertEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());

        source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("http://namespace.objectstorage.us-ashburn-1.oci.customer-oci.com/gdb-gor-test-data-dev/the/path.dat", securityContext(), null, null, null, false));
        assertEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());
    }

    @Test
    public void testResolveOciNativeHttpUrl() throws IOException {
        var source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("https://namespace.objectstorage.us-ashburn-1.oci.customer-oci.com/n/namespace/b/gdb-gor-test-data-dev/o/the/path.dat", securityContext(), null, null, null, false));
        assertEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());
    }

    @Test
    public void testResolveNonOciHttpUrl() throws IOException {
        var source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("https://some.address.com/the/path.dat", securityContext(), null, null, null,  false));
        assertNotEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());

        source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("https://some.address.com/the/path.dat", securityContext(), null, null, null,  false));
        assertNotEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());
    }

    @Test
    public void testResolveS3HttpUrl() throws IOException {
        var source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("https://s3.us-east1.amazonaws.com/gdb-gor-test-data-dev/the/path.dat", securityContext(), null, null, null,  false));
        assertNotEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());

        source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("http://s3.us-east1.amazonaws.com/gdb-gor-test-data-dev/the/path.dat", securityContext(), null, null, null,  false));
        assertNotEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());
    }

    @Test
    public void testResolveS3OciHttpUrl() throws IOException {
        var source = GorDriverFactory.fromConfig().resolveDataSource(
                new SourceReference("s3://namespace.objectstorage.us-ashburn-1.oci.customer-oci.com/the/path.dat",
                        DriverUtils.createSecurityContext("s3", "namespace.objectstorage.us-ashburn-1.oci.customer-oci.com",
                               Credentials.OwnerType.System, "", "dummy", "dummy",
                                "", ""), null, null, null,  false));
        assertNotEquals(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE, source.getSourceType());
    }

    protected String securityContext()  {
        return DriverUtils.ociSecurityContext(OCI_TENANT, OCI_USER, OCI_SIMPLE_PRIVATE_KEY, OCI_SIMPLE_FINGERPRINT);
    }
}
