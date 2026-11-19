/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat.process;

import com.sun.net.httpserver.HttpServer;
import org.gorpipe.gor.cli.render.ReportCommand;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * Tests for the HTTP endpoints of {@link GorTestServer}.
 */
public class UTestGorTestServer {

    private static final String REPORTS = Paths.get("../tests/data/reports").toAbsolutePath().normalize().toString();

    private final HttpClient client = HttpClient.newHttpClient();
    private HttpServer server;
    private String baseUrl;

    @Before
    public void startServer() throws Exception {
        PipeOptions opts = new PipeOptions();
        opts.color_$eq("none");
        server = GorTestServer.create(0, opts);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        if (server != null) server.stop(0);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** What `gor render report <spec>` writes to stdout. */
    private static String renderReportCli(String spec) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            int exitCode = new CommandLine(new ReportCommand()).execute(spec);
            Assert.assertEquals(0, exitCode);
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void testQueryStillAnswers() throws Exception {
        HttpResponse<String> response = post("/query", "norrows 2");
        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(3, response.body().split("\n").length);
    }

    @Test
    public void testRenderMatchesRenderReportCommand() throws Exception {
        String spec = REPORTS + "/test2.yml(top=7)";
        HttpResponse<String> response = post("/render", spec);
        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(renderReportCli(spec), response.body());
    }

    @Test
    public void testRenderNamedReportMatchesRenderReportCommand() throws Exception {
        String spec = REPORTS + "/test3.yml::TestReport(top=2, CASEs=['rs544101329','rs28970552'])";
        HttpResponse<String> response = post("/render", spec);
        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals(renderReportCli(spec), response.body());
    }

    @Test
    public void testRenderOfAMissingReportFails() throws Exception {
        HttpResponse<String> response = post("/render", REPORTS + "/does_not_exist.yml(top=1)");
        Assert.assertEquals(500, response.statusCode());
        Assert.assertFalse(response.body().isEmpty());
    }

    @Test
    public void testRenderOfANonYmlInputIsRejected() throws Exception {
        HttpResponse<String> response = post("/render", "gor x.gor");
        Assert.assertEquals(400, response.statusCode());
    }

    @Test
    public void testRenderNeedsPost() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/render")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(405, response.statusCode());
    }
}
