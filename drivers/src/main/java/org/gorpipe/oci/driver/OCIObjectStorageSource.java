package org.gorpipe.oci.driver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.model.Range;
import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.requests.*;
import com.oracle.bmc.objectstorage.responses.DeleteObjectResponse;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import org.gorpipe.base.streams.LimitedOutputStream;
import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.binsearch.GorIndexType;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.RequestRange;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Represents an object in OCI.
 */
public class OCIObjectStorageSource implements StreamSource {
    final static Logger log = LoggerFactory.getLogger(OCIObjectStorageSource.class);

    private static final boolean USE_META_CACHE = true ;
    private final SourceReference sourceReference;
    private final OCIUrl url;
    private final String bucket;
    private final String key;
    private final String namespace;
    private final ObjectStorageAsync client;
    private StreamSourceMetadata meta;

    private static final Cache<String, StreamSourceMetadata> metadataCache
            = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(5, TimeUnit.MINUTES).build();

    private final static int MAX_CHUNKS = 10000;
    private int writeChunkSize = Integer.parseInt(System.getProperty("gor.s3.write.chunksize", String.valueOf(1 << 26)));

    /**
     * Create source
     *
     * @param sourceReference contains oci url
     */
    public OCIObjectStorageSource(ObjectStorageAsync client, SourceReference sourceReference) throws MalformedURLException {
        this.client = client;
        this.sourceReference = sourceReference;
        this.url = OCIUrl.parse(sourceReference);
        this.namespace = url.getNamespace();
        this.bucket = url.getBucket();
        this.key = url.getPath();
    }

    @Override
    public InputStream open() {
        return open(null);
    }

    @Override
    public InputStream open(long start) {
        return open(RequestRange.fromFirstLength(start, getSourceMetadata().getLength()));
    }

    @Override
    public InputStream open(long start, long minLength) {
        return open(RequestRange.fromFirstLength(start, minLength));
    }

    @Override
    public OutputStream getOutputStream(boolean append) {
        if(append) throw new GorResourceException("OCI object store write not appendable",bucket+"/"+key);
        invalidateMeta();

        long maxFileSize = (long)writeChunkSize * (long)MAX_CHUNKS;

        try {
            StreamBroker broker = new StreamBroker();
            var request =
                    PutObjectRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucket)
                            .objectName(key)
                            .putObjectBody(broker.getInputStream())
                            .build();

            Future<PutObjectResponse> future = client.putObject(request, null);
            broker.getOutputStream().setFuture(future);
            return  new LimitedOutputStream(broker.getOutputStream(), maxFileSize);
        } catch (Exception e) {
            throw new GorResourceException(getName(), getName(), e).retry();
        }
    }

    @Override
    public boolean supportsWriting() {
        return true;
    }

    private InputStream open(RequestRange range) {
        var requestBuilder =
                GetObjectRequest.builder()
                        .namespaceName(namespace)
                        .bucketName(bucket)
                        .objectName(key);

        if (range != null) {
            range = range.limitTo(getSourceMetadata().getLength());
            if (range.isEmpty()) return new ByteArrayInputStream(new byte[0]);
            requestBuilder.range(new Range(range.getFirst(), range.getLast()));
        }

        return openRequest(requestBuilder.build());
    }

    private InputStream openRequest(GetObjectRequest request) {
        try {
            Future<GetObjectResponse> fut = client.getObject(request, null);
            var response = fut.get();
            return response.getInputStream();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        } catch (Exception e) {
            throw new GorResourceException("Failed to open S3 object: " + sourceReference.getUrl(), sourceReference.getUrl(), e).retry();
        }
    }

    @Override
    public String getName() {
        return sourceReference.getUrl();
    }

    private StreamSourceMetadata loadMetadata(String bucket, String key) {
        if (USE_META_CACHE) {
            return loadMetadataFromCache(bucket, key);
        } else {
            return createMetaData(bucket, key);
        }
    }

    private StreamSourceMetadata createMetaData(String bucket, String key) {
        try {
            HeadObjectRequest request =
                    HeadObjectRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucket)
                            .objectName(key)
                            .build();
            HeadObjectResponse getResponse = client.headObject(request, null).get();
            return new StreamSourceMetadata(
                    this,
                    getName(),
                    getResponse.getLastModified().toInstant().toEpochMilli(),
                    getResponse.getContentLength(),
                    null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        } catch (Exception e) {
            throw new GorResourceException("Failed to load metadata for " + bucket + "/" + key, getName(), e).retry();
        }
    }

    private StreamSourceMetadata loadMetadataFromCache(String bucket, String key) {
        try {
            return metadataCache.get(bucket + key, () -> createMetaData(bucket, key));
        } catch (Exception  e) {
            throw new GorResourceException("Failed to load metadata from cache for " + bucket + "/" + key, getName(), e).retry();
        }
    }

    @Override
    public StreamSourceMetadata getSourceMetadata() {
        if (meta == null) {
            meta = loadMetadata(bucket, key);
        }
        return meta;
    }

    @Override
    public SourceReference getSourceReference() {
        return sourceReference;
    }

    @Override
    public DataType getDataType() {
        return DataType.fromFileName(key);
    }

    @Override
    public boolean exists() {
        return metaDataExists() || getName().endsWith("/");
    }

    private boolean metaDataExists()  {
        try {
            // Already in cache, exists
            meta = loadMetadata(bucket, key);
            return true;
        } catch (GorResourceException e) {
            var cause = ExceptionUtilities.getUnderlyingCause(e);
            if (cause instanceof NoSuchKeyException
                || (cause instanceof BmcException be && be.getStatusCode() == 404)) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void delete() {
        if (!exists()) return;

        if (isDirectory()) {
            deleteDirectory();
        } else {
            try {
                DeleteObjectRequest request =
                        DeleteObjectRequest.builder()
                                .namespaceName(namespace)
                                .bucketName(bucket)
                                .objectName(key)
                                .build();
                DeleteObjectResponse getResponse = client.deleteObject(request, null).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GorSystemException(e);
            } catch (Exception e) {
                throw new GorResourceException("Failed to delete " + getName(), getName(), e).retry();
            }
        }

        invalidateMeta();
    }

    private void invalidateMeta() {
        meta = null;
        metadataCache.invalidate(bucket + key);
    }

    @Override
    public SourceType getSourceType() {
        return OCIObjectStorageSourceType.OCI_OBJECT_STORAGE;
    }

    @Override
    public GorIndexType useIndex() {
        return GorIndexType.CHROMINDEX;
    }

    @Override
    public void close() {
        // No resources to free
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) {
        try {
            var folder = PathUtils.markAsFolder(key);
            var request = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .objectName(PathUtils.markAsFolder(key))
                    .putObjectBody(new InputStream() {
                        @Override
                        public int read() {
                            return -1;
                        }
                    })
                    .contentLength(0L)
                    .build();

            client.putObject(request, null).get();
            return folder;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        } catch (Exception e) {
            throw new GorResourceException("Failed to create directory " + getName(), getName(), e).retry();
        }
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) {
        // For now just create the last directory in the path.
        return createDirectory(attrs);
    }

    @Override
    public void deleteDirectory() {
        try {
            walk().forEach(object -> {
                try {
                    var request = DeleteObjectRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucket)
                            .objectName(object)
                            .build();
                    client.deleteObject(request, null).get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GorSystemException(e);
                } catch (Exception e) {
                    throw new GorResourceException("Failed to delete object: " + object, getName(), e).retry();
                }
            });
        } catch (Exception e) {
            throw new GorResourceException("Failed to delete directory: " + getName(), getName(), e).retry();
        }
    }

    @Override
    public boolean isDirectory() {
        return key.endsWith("/") || (exists() && this.meta == null);
    }

    @Override
    public Stream<String> list() {
        try {
            Set<String> objectNames = new HashSet<>();
            String nextStartWith = null;

            var keyAsFolder = PathUtils.markAsFolder(key);

            do {
                var builder = ListObjectsRequest.builder()
                        .namespaceName(namespace)
                        .bucketName(bucket)
                        .prefix(key) // Optional: Use this to filter objects by prefix
                        .start(nextStartWith);
                if (key.endsWith("/")) {
                    // Not strictly necessary but returns less data if deep hierarchy
                    builder.delimiter("/");
                }
                var request = builder.build();

                var response = client.listObjects(request, null).get();

                var prefixes = response.getListObjects().getPrefixes();
                if (prefixes != null) {
                    prefixes.forEach(prefix -> {
                        var splits = prefix.substring(keyAsFolder.length()).split("/");
                        objectNames.add(keyAsFolder + splits[0] + "/");
                    });
                }

                response.getListObjects().getObjects().forEach(obj -> {
                    if (!obj.getName().equals(key)) {
                        var splits = obj.getName().substring(keyAsFolder.length()).split("/");
                        objectNames.add(keyAsFolder + splits[0] + (splits.length > 1 ? "/" : ""));
                    }
                });

                nextStartWith = response.getListObjects().getNextStartWith();
            } while (nextStartWith != null);

            return objectNames.stream();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        } catch (Exception e) {
            throw new GorResourceException("Failed to list objects in bucket " + bucket, getName(), e).retry();
        }
    }

    @Override
    public Stream<String> walk() {
        try {
            Set<String> objectNames = new HashSet<>();
            String nextStartWith = null;

            do {
                var builder = ListObjectsRequest.builder()
                        .namespaceName(namespace)
                        .bucketName(bucket)
                        .prefix(key) // Optional: Use this to filter objects by prefix
                        .start(nextStartWith);

                var request = builder.build();

                var response = client.listObjects(request, null).get();

                response.getListObjects().getObjects().forEach(obj -> {
                    objectNames.add(obj.getName());
                });

                nextStartWith = response.getListObjects().getNextStartWith();
            } while (nextStartWith != null);

            return objectNames.stream();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException(e);
        } catch (Exception e) {
            throw new GorResourceException("Failed to list objects in bucket " + bucket, getName(), e).retry();
        }
    }

}
