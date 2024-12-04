package org.gorpipe.oci.driver;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;


public class UTestOCIUrl {

    @Rule
    public final ProvideSystemProperty endpoint = new ProvideSystemProperty("gor.oci.endpoint", "https://namespace.objectstorage.region.oci.customer-oci.com");


    @Test
    public void testParseOCIGlobalUrl() throws MalformedURLException {
        OCIUrl url = OCIUrl.parse("oci://thebucket/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
    }

    @Test
    public void testParseS3GlobalUrl() throws MalformedURLException {
        OCIUrl url = OCIUrl.parse("s3://thebucket/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
        assertEquals(OCIUrl.DEFAULT_OCI_ENDPOINT, url.getEndpoint());
        assertEquals(URI.create(OCIUrl.DEFAULT_OCI_ENDPOINT).getHost().split("\\.", 2)[0],
                url.getNamespace());
    }

    @Test
    public void testParseOCIHttpUrl() throws MalformedURLException {
        OCIUrl url = OCIUrl.parse("https://namespace.objectstorage.region.oci.customer-oci.com/n/namespace/b/thebucket/o/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
        assertEquals("namespace.objectstorage.region.oci.customer-oci.com", url.getEndpoint());
        assertEquals("namespace", url.getNamespace());
    }

    @Test
    public void testParseS3PathStyleHttpUrl() throws MalformedURLException {
        OCIUrl url = OCIUrl.parse("https://namespace.objectstorage.region.oci.customer-oci.com/thebucket/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
        assertEquals("namespace.objectstorage.region.oci.customer-oci.com", url.getEndpoint());
        assertEquals("namespace", url.getNamespace());
    }

    @Test
    public void testParseS3HostVirtualHttpUrl() throws MalformedURLException {
        OCIUrl url = OCIUrl.parse("https://thebucket.namespace.objectstorage.region.oci.customer-oci.com/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
        assertEquals("namespace.objectstorage.region.oci.customer-oci.com", url.getEndpoint());
        assertEquals("namespace", url.getNamespace());
    }

    @Test
    public void testParseBadUrl() {
        assertThrows(MalformedURLException.class, () -> OCIUrl.parse("unknown://theprovider/the/path.dat"));
        assertThrows(MalformedURLException.class, () -> OCIUrl.parse("https:/theprovider/the/path.dat"));
    }

    @Test
    public void testParseSourceReference() throws MalformedURLException {
        SourceReference sourceRef = new SourceReference("oci://thebucket/the/path.dat");
        OCIUrl url = OCIUrl.parse(sourceRef);
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
    }

    @Test
    public void testGetters() throws MalformedURLException {
        OCIUrl url = OCIUrl.parse("oci://thebucket/the/path.dat");
        assertEquals("thebucket", url.getBucket());
        assertEquals("thebucket", url.getLookupKey());
        assertEquals("the/path.dat", url.getPath());
        assertEquals(OCIUrl.DEFAULT_OCI_ENDPOINT, url.getEndpoint());
        assertEquals(URI.create("oci://thebucket/the/path.dat"), url.getOriginalUrl());
    }

}
