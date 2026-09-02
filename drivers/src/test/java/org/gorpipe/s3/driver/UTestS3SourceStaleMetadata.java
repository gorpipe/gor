package org.gorpipe.s3.driver;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests how {@link S3Source} recovers when the object length it cached no longer matches the object.
 *
 * <p>Regression coverage for ENGKNOW-3722. {@code S3Source.open(RequestRange)} clamps every ranged
 * GET to {@code getSourceMetadata().getLength()}, which is served from a cache. A {@code .link} file
 * rewritten smaller by another pod (S3 has no append, so {@code LinkFile.save} and versioned-link GC
 * both replace the whole object) leaves readers holding the old, larger length. They then ask for a
 * range starting at or past the new end and S3 answers {@code 416 Range Not Satisfiable} — in
 * production, 220 failed queries in 14 days, each after ~167 s of pointless retries.
 *
 * <p>The lengths below are the real signature from the production logs: a link file that shrank from
 * 98825 to 98674 bytes.
 */
public class UTestS3SourceStaleMetadata {

    private static final long STALE_LENGTH = 98825;
    private static final long ACTUAL_LENGTH = 98674;

    /**
     * What {@code ExtendedRangeStream} actually asks for once {@code StreamUtils.readString} requests
     * 200000 bytes: it drains the first 128 kb request, then reopens at the position it reached with
     * double the length. {@link S3Source} clamps that to the cached length, which is how the
     * production range {@code bytes=98674-98824} arises.
     */
    private static final long REOPEN_LENGTH = 262144;

    private static HeadObjectResponse head(long contentLength) {
        return HeadObjectResponse.builder()
                .contentLength(contentLength)
                .lastModified(Instant.now())
                .build();
    }

    private static S3Exception rangeNotSatisfiable() {
        return (S3Exception) S3Exception.builder()
                .statusCode(416)
                .message(String.format("byte range bytes=%d-%d cannot be satisfied from object with "
                        + "content length %d", ACTUAL_LENGTH, STALE_LENGTH - 1, ACTUAL_LENGTH))
                .build();
    }

    private static ResponseInputStream<GetObjectResponse> body(byte[] content) {
        return new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength((long) content.length).build(),
                new ByteArrayInputStream(content));
    }

    /** A distinct key per test, so the process-wide static metadata cache cannot leak between them. */
    private static S3Source sourceFor(S3Client client) {
        var url = "s3://thebucket/ref/" + UUID.randomUUID() + "/gmb_disease_map.tsv.link";
        try {
            return new S3Source(client, new SourceReference(url));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void rangePastEndOfShrunkObjectReadsAsEndOfFile() throws Exception {
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(head(STALE_LENGTH), head(ACTUAL_LENGTH));
        when(client.getObject(any(GetObjectRequest.class)))
                .thenThrow(rangeNotSatisfiable());

        var source = sourceFor(client);

        try (InputStream is = source.open(ACTUAL_LENGTH, REOPEN_LENGTH)) {
            assertEquals("a range wholly past the end of the rewritten object is end-of-file",
                    -1, is.read());
        }

        verify(client, times(2)).headObject(any(HeadObjectRequest.class));
        verify(client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    public void rangeIsReissuedWhenRefreshedLengthStillCoversIt() throws Exception {
        long grownLength = 200000;
        byte[] expected = new byte[(int) (STALE_LENGTH - ACTUAL_LENGTH)];
        java.util.Arrays.fill(expected, (byte) 'x');

        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(head(STALE_LENGTH), head(grownLength));
        when(client.getObject(any(GetObjectRequest.class)))
                .thenThrow(rangeNotSatisfiable())
                .thenReturn(body(expected));

        var source = sourceFor(client);

        try (InputStream is = source.open(ACTUAL_LENGTH, REOPEN_LENGTH)) {
            assertEquals("the refreshed range must be fetched, not reported as end-of-file",
                    expected.length, is.readAllBytes().length);
        }

        verify(client, times(2)).getObject(any(GetObjectRequest.class));
    }

    /**
     * The metadata cache key was {@code bucket + key} with no separator, so a bucket/key split at a
     * different offset produced the same key: bucket {@code ab<id>} + key {@code c/file.link} and
     * bucket {@code a} + key {@code b<id>c/file.link} both concatenate to {@code ab<id>c/file.link}.
     * Two unrelated objects would then share a cached length -- and an invalidation of one would clear
     * the other.
     */
    @Test
    public void metadataCacheKeysDoNotCollideAcrossTheBucketBoundary() throws Exception {
        var id = UUID.randomUUID().toString().replace("-", "");
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(head(100), head(200));

        var first = new S3Source(client, new SourceReference("s3://ab" + id + "/c/file.link"));
        var second = new S3Source(client, new SourceReference("s3://a/b" + id + "c/file.link"));

        assertEquals(100L, first.getSourceMetadata().getLength().longValue());
        assertEquals("the second object must get its own length, not the first object's",
                200L, second.getSourceMetadata().getLength().longValue());

        verify(client, times(2)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    public void otherS3ErrorsDoNotRefreshMetadata() throws Exception {
        S3Client client = mock(S3Client.class);
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(head(STALE_LENGTH));
        when(client.getObject(any(GetObjectRequest.class)))
                .thenThrow((S3Exception) S3Exception.builder()
                        .statusCode(403).message("Access Denied").build());

        var source = sourceFor(client);

        assertThrows(GorResourceException.class, () -> source.open(0, 100));

        verify(client, times(1)).headObject(any(HeadObjectRequest.class));
        verify(client, times(1)).getObject(any(GetObjectRequest.class));
    }
}
