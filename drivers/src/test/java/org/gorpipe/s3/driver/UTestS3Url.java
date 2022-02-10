package org.gorpipe.s3.driver;

import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by villi on 04/04/17.
 */
public class UTestS3Url {

    @Test
    public void testStandardUrl() throws Exception {
        S3Url url = S3Url.parse("s3://thebucket/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
    }

    @Test
    public void testUrlWithProvider() throws Exception {
        S3Url url = S3Url.parse("s3://theprovider:thebucket/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("theprovider:thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
    }

    @Test
    public void testBadUrl() throws Exception {
        try {
            S3Url url = S3Url.parse("s3://theprovider:thebucket:badstuff/the/path.dat");
            fail();
        } catch (MalformedURLException e) {
            // Expected
        }
    }
}
