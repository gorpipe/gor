package org.gorpipe.gor.driver.providers.stream.sources;

import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.sources.http.HTTPSource;
import org.gorpipe.gor.driver.providers.stream.sources.http.HTTPSourceProvider;
import org.gorpipe.gor.driver.providers.stream.sources.http.HTTPSourceType;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.ExtendedRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryStreamSourceWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.nanohttpd.protocols.http.TestFileHttpServer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by villi on 25/08/15.
 */
public class UTestHttpSource extends CommonStreamTests {

    private static int port = -1;
    private static TestFileHttpServer server;

    @Before
    public void setUp() throws Exception {
        server = new TestFileHttpServer(0, workDir.getRoot());
        port = server.getPort();
    }

    @Override
    protected String getDataName(File file) {
        return "http://127.0.0.1:" + port + "/" + file.getName();
    }

    @Override
    protected StreamSource createSource(String name) throws IOException {
        //TODO: the config interface should probably rather be injected
        Set<StreamSourceIteratorFactory> initialFactories = new HashSet<>();
        HTTPSourceProvider httpSourceProvider = new HTTPSourceProvider(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class), null
                , initialFactories);
        HTTPSource httpSource = httpSourceProvider.resolveDataSource(new SourceReference(name));

        return httpSourceProvider.wrap(httpSource);
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
        Assert.assertEquals(HTTPSource.class, fs.getClass());
    }

    @Override
    protected SourceType expectedSourcetype(StreamSource fs) {
        return HTTPSourceType.HTTP;
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
        server = null;
    }
}
