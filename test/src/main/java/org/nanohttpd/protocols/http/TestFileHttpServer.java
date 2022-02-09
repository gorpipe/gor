package org.nanohttpd.protocols.http;

import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.utils.TestUtils;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by villi on 26/08/15.
 */
public class TestFileHttpServer extends NanoHTTPD {
    private final File serverRoot;

    /**
     * Starts a HTTP server to given port using the directory of included test resources as a server root.
     * <p>
     * Throws an IOException if the socket is already in use
     *
     * @param port
     */
    public TestFileHttpServer(int port) throws IOException {
        this(port, null);
    }

    /**
     * Starts a HTTP server on a given port using the supplied directory as server root.
     *
     * @param port       port to run the HTTP server on or 0 to use any available port
     * @param serverRoot the directory to serve as the root of the HTTP server
     * @throws IOException if the port is already in use
     * @throws IOException if the given server root doesn't exist or isn't a directory
     */
    public TestFileHttpServer(int port, File serverRoot) throws IOException {
        super(port);
        if (serverRoot == null) {
            this.serverRoot = new File(TestUtils.getTestFile("dummy.gor")).getParentFile();
        } else {
            if (!serverRoot.exists() || !serverRoot.isDirectory()) {
                throw new IOException("Server root must exist and be a directory: " + serverRoot.getAbsolutePath());
            }
            this.serverRoot = serverRoot;
        }
    }

    @Override
    public Response serve(String uri, String method, Properties header, Properties parms) {
        return serveFile(uri, header, serverRoot, false);
    }

    public int getPort() {
        return ss.getLocalPort();
    }

    public void stop() {
        StreamUtils.tryClose(ss);
        serverThread.interrupt();
    }
}
