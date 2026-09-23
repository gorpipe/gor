package org.gorpipe.s3.driver;

import com.github.benmanes.caffeine.cache.Cache;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    public void uploadCloseInvalidatesNegativeEntryCachedDuringUpload() throws Exception {
        System.setProperty(TTL_PROP, "30");
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(notFound())
                .thenThrow(notFound())
                .thenReturn(HeadObjectResponse.builder().contentLength(5L).lastModified(Instant.now()).build());
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-1").build());
        ArgumentCaptor<UploadPartRequest> uploadPartRequestCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        when(client.uploadPart(uploadPartRequestCaptor.capture(), any(RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().eTag("etag-1").build());
        when(client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(CompleteMultipartUploadResponse.builder().build());
        String url = uniqueUrl();

        // A miss is cached first, exactly like the other tests.
        assertFalse(source(client, url).exists());

        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        OutputStream out = source(client, url).getOutputStream(false);
        try {
            // Simulate another reader re-caching a miss while this upload is still in flight: opening
            // the stream already invalidated the earlier entry (see getOutputStream), so this issues a
            // real HEAD and caches a fresh miss - the scenario the close()-time invalidation guards against.
            assertFalse(source(client, url).exists());
            out.write(data, 0, data.length);
        } finally {
            out.close();
        }

        assertEquals("the full write must reach the upload in one call, not byte-by-byte",
                data.length, uploadPartRequestCaptor.getValue().contentLength().longValue());
        assertTrue("closing the output stream must clear a miss cached while the upload was in flight",
                source(client, url).exists());
    }

    @Test
    public void racedNegativeCacheInsertDoesNotThrowClassCastException() throws Exception {
        S3Client client = mock(S3Client.class);
        String url = uniqueUrl();
        S3Source source = source(client, url);

        @SuppressWarnings("unchecked")
        Cache<String, Object> racingCache = mock(Cache.class);
        when(racingCache.getIfPresent(any())).thenReturn(null);
        // Simulates another thread inserting a miss between our getIfPresent() check and this get()
        // call: Caffeine returns the winning entry directly without invoking our loader.
        when(racingCache.get(any(), any())).thenReturn(new S3Source.NegativeMetadata(System.currentTimeMillis() + 30_000L));
        Field cacheField = S3Source.class.getDeclaredField("metadataCache");
        cacheField.setAccessible(true);
        cacheField.set(source, racingCache);

        assertFalse("a racing negative-cache insert must be treated as a cached miss, not a ClassCastException",
                source.exists());
        verify(client, never()).headObject(any(HeadObjectRequest.class));
    }
}
