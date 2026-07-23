package org.gorpipe.gor.driver.providers.mdr;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.providers.stream.sources.mdr.MdrServer;
import org.gorpipe.gor.driver.providers.stream.sources.mdr.MdrUrlsResult;
import org.gorpipe.gor.driver.providers.stream.sources.mdr.MdrUrlsResultItem;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.List;

public class UTestMdrServer {

    private static final URI MDR_URL =
            URI.create("mdr://6e8c0000-0000-0000-0000-000000000871/x871.cram?env=prd");
    private static final String DOC_ID = "6e8c0000-0000-0000-0000-000000000871";

    private static MdrUrlsResult result(MdrUrlsResultItem... items) {
        return new MdrUrlsResult("direct", List.of(items));
    }

    private static MdrUrlsResultItem item(String url) {
        return new MdrUrlsResultItem(DOC_ID, "x871.cram", url, null);
    }

    @Test
    public void validResolvedUrlReturnsItem() {
        MdrUrlsResultItem item = item("s3://bucket/x871.cram");
        MdrUrlsResultItem out = MdrServer.validateResolved(result(item), MDR_URL);
        Assert.assertEquals("s3://bucket/x871.cram", out.url());
    }

    @Test
    public void nullUrlThrowsWithDocumentId() {
        GorResourceException e = Assert.assertThrows(GorResourceException.class,
                () -> MdrServer.validateResolved(result(item(null)), MDR_URL));
        Assert.assertTrue("message should name the document id, was: " + e.getMessage(),
                e.getMessage().contains(DOC_ID));
    }

    @Test
    public void blankUrlThrows() {
        Assert.assertThrows(GorResourceException.class,
                () -> MdrServer.validateResolved(result(item("   ")), MDR_URL));
    }

    @Test
    public void nullResultThrows() {
        Assert.assertThrows(GorResourceException.class,
                () -> MdrServer.validateResolved(null, MDR_URL));
    }

    @Test
    public void zeroUrlsThrows() {
        Assert.assertThrows(GorResourceException.class,
                () -> MdrServer.validateResolved(result(), MDR_URL));
    }

    @Test
    public void multipleUrlsThrows() {
        Assert.assertThrows(GorResourceException.class,
                () -> MdrServer.validateResolved(
                        result(item("s3://a"), item("s3://b")), MDR_URL));
    }
}
