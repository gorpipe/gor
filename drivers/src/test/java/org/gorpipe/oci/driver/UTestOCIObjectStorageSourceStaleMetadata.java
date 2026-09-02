package org.gorpipe.oci.driver;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.HeadObjectRequest;
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.junit.Test;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The stale-object-length cases from ENGKNOW-3722, for the native OCI driver.
 *
 * <p>{@link OCIObjectStorageSource} clamps every ranged GET to a cached object length exactly as the
 * S3 driver does, so an object rewritten smaller by another process produces the same deterministic
 * {@code 416}. Its cache has a 5 minute expiry, which narrows the window but does not close it.
 *
 * <p>The cache key is also missing the namespace, so two objects with the same bucket and key in
 * different namespaces share one cached length.
 */
public class UTestOCIObjectStorageSourceStaleMetadata {

    private static final long STALE_LENGTH = 98825;
    private static final long ACTUAL_LENGTH = 98674;
    private static final long REOPEN_LENGTH = 262144;

    private static HeadObjectResponse head(long contentLength) {
        return HeadObjectResponse.builder()
                .contentLength(contentLength)
                .lastModified(new Date())
                .build();
    }

    private static BmcException bmcError(int statusCode, String message) {
        return new BmcException(statusCode, "TestServiceCode", message, "test-opc-request-id");
    }

    private static String nativeUrl(String namespace, String bucket, String path) {
        return String.format("https://%s.objectstorage.us-ashburn-1.oci.customer-oci.com/n/%s/b/%s/o/%s",
                namespace, namespace, bucket, path);
    }

    private static OCIObjectStorageSource sourceFor(ObjectStorageAsync client, String namespace,
                                                    String bucket, String path) {
        try {
            return new OCIObjectStorageSource(client, new SourceReference(nativeUrl(namespace, bucket, path)));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void rangePastEndOfShrunkObjectReadsAsEndOfFile() throws Exception {
        ObjectStorageAsync client = mock(ObjectStorageAsync.class);
        when(client.headObject(any(HeadObjectRequest.class), any()))
                .thenReturn(CompletableFuture.completedFuture(head(STALE_LENGTH)),
                        CompletableFuture.completedFuture(head(ACTUAL_LENGTH)));
        when(client.getObject(any(GetObjectRequest.class), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        bmcError(416, "The requested range is not satisfiable")));

        var source = sourceFor(client, "ns" + UUID.randomUUID(), "thebucket", "ref/" + UUID.randomUUID() + "/gmb_disease_map.tsv.link");

        try (InputStream is = source.open(ACTUAL_LENGTH, REOPEN_LENGTH)) {
            assertEquals("a range wholly past the end of the rewritten object is end-of-file",
                    -1, is.read());
        }

        verify(client, times(2)).headObject(any(HeadObjectRequest.class), any());
    }

    @Test
    public void otherErrorsDoNotRefreshMetadata() throws Exception {
        ObjectStorageAsync client = mock(ObjectStorageAsync.class);
        when(client.headObject(any(HeadObjectRequest.class), any()))
                .thenReturn(CompletableFuture.completedFuture(head(STALE_LENGTH)));
        when(client.getObject(any(GetObjectRequest.class), any()))
                .thenReturn(CompletableFuture.failedFuture(bmcError(403, "Access Denied")));

        var source = sourceFor(client, "ns" + UUID.randomUUID(), "thebucket", "ref/" + UUID.randomUUID() + "/gmb_disease_map.tsv.link");

        assertThrows(GorResourceException.class, () -> source.open(0, 100));

        verify(client, times(1)).headObject(any(HeadObjectRequest.class), any());
    }

    @Test
    public void metadataCacheKeysIncludeTheNamespace() throws Exception {
        var id = UUID.randomUUID().toString().replace("-", "");
        ObjectStorageAsync client = mock(ObjectStorageAsync.class);
        when(client.headObject(any(HeadObjectRequest.class), any()))
                .thenReturn(CompletableFuture.completedFuture(head(100)),
                        CompletableFuture.completedFuture(head(200)));

        var first = sourceFor(client, "nsone" + id, "thebucket", "shared/file.link");
        var second = sourceFor(client, "nstwo" + id, "thebucket", "shared/file.link");

        assertEquals(100L, first.getSourceMetadata().getLength().longValue());
        assertEquals("an object in another namespace must get its own length",
                200L, second.getSourceMetadata().getLength().longValue());

        verify(client, times(2)).headObject(any(HeadObjectRequest.class), any());
    }
}
