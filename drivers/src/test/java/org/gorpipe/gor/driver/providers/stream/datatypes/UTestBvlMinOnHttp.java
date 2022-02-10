package org.gorpipe.gor.driver.providers.stream.datatypes;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.nanohttpd.protocols.http.TestFileHttpServer;

public class UTestBvlMinOnHttp extends BvlTestSuite {
    private static int port = -1;
    private static TestFileHttpServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new TestFileHttpServer(46994);
        port = server.getPort();
    }

    @Override
    protected String getSourcePath(String name) {
        return "http://127.0.0.1:" + port + "/bvl_min/" + name;
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
        server = null;
    }
}
