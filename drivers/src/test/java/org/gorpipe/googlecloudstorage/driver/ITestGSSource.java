package org.gorpipe.googlecloudstorage.driver;

import org.gorpipe.gor.driver.providers.stream.sources.CommonStreamTests;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.ExtendedRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryWrapper;
import org.gorpipe.test.IntegrationTests;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Properties;

@Category(IntegrationTests.class)
@Ignore // TODO: re-enable this as per https://nextcode.atlassian.net/browse/GOR-2169 - stefan 2019-01-23
public class ITestGSSource extends CommonStreamTests {

    private static String GS_CLIENT_ID;
    private static String GS_SECRET;
    private static String GS_REFRESH_TOKEN;
    private static String creds;

    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        GS_CLIENT_ID = props.getProperty("GS_CLIENT_ID");
        GS_SECRET = props.getProperty("GS_SECRET");
        GS_REFRESH_TOKEN = props.getProperty("GS_REFRESH_TOKEN");
        creds = "{\"client_id\": \"" + GS_CLIENT_ID + "\",\"client_secret\": \"" + GS_SECRET + "\",\"refresh_token\": \"" + GS_REFRESH_TOKEN + "\",\"type\": \"authorized_user\"}";
    }

    @Override
    protected String getDataName(String name) {
        return "gs://01_nextcode_gor_integration_test/csa_test_data/data_sets/gor_driver_testfiles/" + name;
    }

    @Override
    protected StreamSource createSource(String name) throws IOException {
        return new GoogleCloudStorageBlobSource(new SourceReference(name), creds);
    }

    @Override
    protected String expectCanonical(StreamSource source, String name) throws IOException {
        return name;
    }

    @Override
    protected void verifyDriverDataSource(String name, DataSource fs) {
        Assert.assertEquals(ExtendedRangeWrapper.class, fs.getClass());
        fs = ((ExtendedRangeWrapper) fs).getWrapped();
        Assert.assertEquals(RetryWrapper.class, fs.getClass());
        fs = ((RetryWrapper) fs).getWrapped();
        Assert.assertEquals(GoogleCloudStorageBlobSource.class, fs.getClass());
    }

    @Override
    protected SourceType expectedSourcetype(StreamSource fs) {
        return GoogleCloudStorageSourceType.GOOGLE_CLOUD_STORAGE_SOURCE_TYPE;
    }

    @Test
    @Override
    public void testGetTimestamp() throws IOException {
        // Skip this test, file uploaded has newer timestamp than local file
    }

    @Test
    @Override
    public void testDriver() throws IOException {
        System.setProperty("gor.google.application.credentials", creds);
        super.testDriver();
    }
}
