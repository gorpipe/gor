package org.gorpipe.s3.driver;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * exists() on a missing key issued a HEAD every time (misses were not cached). The negative cache
 * is opt-in (ENGKNOW-3723) and must never hide an object this process has just written.
 */
public class UTestS3SourceNegativeMetadataCache {

    private static final String TTL_PROP = "gor.s3.meta.cache.negative.ttl";

    @After
    public void clearProperty() {
        System.clearProperty(TTL_PROP);
    }

    private static NoSuchKeyException notFound() {
        return (NoSuchKeyException) NoSuchKeyException.builder().statusCode(404).message("Not Found").build();
    }

    private static String uniqueUrl() {
        return "s3://thebucket/dir/" + UUID.randomUUID() + ".gorz";
    }

    private static S3Source source(S3Client client, String url) throws Exception {
        return new S3Source(client, new SourceReference(url));
    }

    @Test
    public void offByDefault() throws Exception {
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(notFound());
        String url = uniqueUrl();

        assertFalse(source(client, url).exists());
        assertFalse(source(client, url).exists());

        verify(client, times(2)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    public void missIsCachedWhenEnabled() throws Exception {
        System.setProperty(TTL_PROP, "30");
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(notFound());
        String url = uniqueUrl();

        assertFalse(source(client, url).exists());
        assertFalse(source(client, url).exists());

        verify(client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    public void expiredMissIsReloaded() throws Exception {
        System.setProperty(TTL_PROP, "1");
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(notFound());
        String url = uniqueUrl();

        assertFalse(source(client, url).exists());
        Thread.sleep(1100);
        assertFalse(source(client, url).exists());

        verify(client, times(2)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    public void writeInvalidatesNegativeEntry() throws Exception {
        System.setProperty(TTL_PROP, "30");
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(notFound())
                .thenReturn(HeadObjectResponse.builder().contentLength(1L).lastModified(Instant.now()).build());
        String url = uniqueUrl();

        assertFalse(source(client, url).exists());
        // Directory creation is the simplest write that goes through S3Source without multipart mocking.
        try {
            source(client, url).createDirectory();
        } catch (GorResourceException ignored) {
            // Formatting the returned path needs a real S3 filesystem; the put and the invalidation
            // (placed directly after putObject) have already happened.
        }
        assertTrue("an object written by this process must not be hidden by a cached miss",
                source(client, url).exists());
    }
}
