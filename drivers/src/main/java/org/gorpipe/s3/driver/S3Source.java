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

package org.gorpipe.s3.driver;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.carlspring.cloud.storage.s3fs.S3FileSystem;
import org.carlspring.cloud.storage.s3fs.S3Path;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.base.streams.LimitedOutputStream;
import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.binsearch.GorIndexType;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.RequestRange;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.table.util.PathUtils;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Represents an object in Amazon S3.
 * Created by villi on 22/08/15.
 */
public class S3Source implements StreamSource {
    static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(S3Source.class);

    // software.amazon.nio.s3..aws-java-nio-spi-for-s3 only uses credentials from the default provider chain (can not specify creds
    // in the driver as we can for Carlspring).  So we can not use it for multiple accounts (if using key and secret).
    private static final boolean USE_S3_CARLSPRING_FILESYSTEM = Boolean.parseBoolean(System.getProperty("gor.s3.carlspring.filesystem", "true"));
    private static final boolean USE_S3_FILESYSTEM_OUTPUTSTREAM = Boolean.parseBoolean(System.getProperty("gor.s3.filesystem.outputstream", "false"));
    // Use NIO filesystem where possible.  Some S3 operations using the NIO filesystem are not supported by the OCI S3 compatibility layer.
    // TODP:  Maybe we should create a OCIS3CompatSource.
    private static final boolean OCI_S3_COMPATIBLE = Boolean.parseBoolean(System.getProperty("gor.oci.s3.compatible", "true"));

    private static final boolean USE_META_CACHE = Boolean.parseBoolean(System.getProperty("gor.s3.meta.cache", "true")) ;
    private static final boolean USE_META_CACHE_SESSION = Boolean.parseBoolean(System.getProperty("gor.s3.meta.cache.session", "true")) ;

    protected final SourceReference sourceReference;
    protected final String bucket;
    protected final String key;
    protected final S3Client client;
    protected final S3AsyncClient asyncClient;
    private static final Map<S3Client, S3FileSystem> s3fsCache = new ConcurrentHashMap<>();
    private S3SourceMetadata meta;
    private static final Cache<String, Object> staticMetadataCache =
            Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(5, TimeUnit.MINUTES).build();
    private Cache<String, Object> metadataCache;
    private Path path;

    private final static int MAX_S3_CHUNKS = 10000;
    private int writeChunkSize = Integer.parseInt(System.getProperty("gor.s3.write.chunksize", String.valueOf(1 << 26)));

    /**
     * Create source
     *
     * @param sourceReference contains S3 url of the form s3://bucket/objectpath
     */
    public S3Source(S3Client client, SourceReference sourceReference) throws MalformedURLException {
        this(client, null, sourceReference, S3Url.parse(sourceReference));
    }

    /**
     * Create source
     *
     * @param sourceReference contains S3 url of the form s3://bucket/objectpath
     */
    public S3Source(S3Client client, S3AsyncClient asyncClient, SourceReference sourceReference) throws MalformedURLException {
        this(client, asyncClient, sourceReference, S3Url.parse(sourceReference));
    }

    S3Source(S3Client client, S3AsyncClient asyncClient, SourceReference sourceReference, S3Url url) {
        this.client = client;
        this.asyncClient = asyncClient;
        this.sourceReference = sourceReference;
        this.bucket = url.getBucket();
        this.key = url.getPath();

        if (USE_META_CACHE) {
            if (GorSession.currentSession.get() != null && USE_META_CACHE_SESSION) {
                this.metadataCache = GorSession.currentSession.get().getCache().getS3MetadataCache();
            } else {
                log.warn("No session available, can not use metadata cache for " + bucket + "/" + key);
                this.metadataCache = staticMetadataCache;
            }
        }
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
        if(append) throw new GorResourceException("S3 write not appendable", bucket+"/"+key);
        invalidateMeta();

        long maxFileSize = (long)writeChunkSize * (long)MAX_S3_CHUNKS;
        try {
            OutputStream os;
            if (USE_S3_FILESYSTEM_OUTPUTSTREAM) {
                os = getPath().getFileSystem().provider().newOutputStream(getPath(),
                        append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                os = asyncClient != null ?
                        new S3MultipartOutputStreamAsync(asyncClient, bucket, key) :
                        new S3MultipartOutputStreamSync(client, bucket, key);
            }

            return new LimitedOutputStream(os, maxFileSize);
        } catch (IOException e) {
            throw new GorResourceException(getName(), getName(), e).retry();
        }
    }

    @Override
    public boolean supportsWriting() {
        return true;
    }

    private InputStream open(RequestRange range) {
        var reqBuilder = GetObjectRequest.builder().bucket(bucket).key(key);
        if (range!=null) {
            range = range.limitTo(getSourceMetadata().getLength());
            if (range.isEmpty()) return new ByteArrayInputStream(new byte[0]);
            reqBuilder.range(BytesRange.startLengthtoRange(range.getFirst(), range.getLength()));
        }
        return openRequest(reqBuilder.build());
    }

    private InputStream openWithFileSystem(RequestRange range) {
        Path path = Path.of(URI.create(getName()));
        try {
            var channel = FileChannel.open(path, StandardOpenOption.READ);
            if (range!=null) {
                range = range.limitTo(getSourceMetadata().getLength());
                if (range.isEmpty()) return new ByteArrayInputStream(new byte[0]);
                channel.position(range.getFirst());
            }
            return Channels.newInputStream(channel);
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }

    private InputStream openRequest(GetObjectRequest request) {
        try {
            if (asyncClient != null) {
                return new AbortingInputStream(asyncClient.getObject(request, AsyncResponseTransformer.toBlockingInputStream()).join(), request);
            }
            return new AbortingInputStream(client.getObject(request), request);
        } catch (Exception e) {
            throw new GorResourceException("Failed to open S3 object: " + sourceReference.getUrl(), getPath().toString(), e).retry();
        }
    }

    @Override
    public String getName() {
        return sourceReference.getUrl();
    }

    private S3SourceMetadata loadMetadata(String bucket, String key) {
        if (USE_META_CACHE) {
            return loadMetadataFromCache(bucket, key);
        } else {
            return createMetaData(bucket, key);
        }
    }

    private S3SourceMetadata createMetaData(String bucket, String key) {
        HeadObjectResponse objectMetaResponse;
        try {
            if (asyncClient != null) {
                objectMetaResponse = asyncClient.headObject(b -> b.bucket(bucket).key(key)).join();
            } else {
                objectMetaResponse = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            }
        } catch (Exception e) {
            throw new GorResourceException("Failed to load metadata for " + bucket + "/" + key, getPath().toString(), e).retry();
        }

        return new S3SourceMetadata(this, objectMetaResponse, sourceReference.getLinkLastModified());
    }

    private S3SourceMetadata loadMetadataFromCache(String bucket, String key) {
        try {
            // We can not use the cache if the session is not available, as the cache needs to be cleared when the session ends.
            return (S3SourceMetadata)metadataCache.get(bucket + key, k -> {
                // TODO:  If the object does not exists we don't cache.  This method will throw exception and the loader will exit.
                return createMetaData(bucket, key);
            });
        } catch (Exception e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            throw new GorResourceException("Failed to load metadata from cache for " + bucket + "/" + key, getPath().toString(), cause).retry();
        }
    }

    @Override
    public S3SourceMetadata getSourceMetadata() {
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
        try {
            // Note: fileExists only handles dirs if the end with / (and have explictly been created).
            // Therefor we can either fall back to the much slower Files.exists, just say that all dirs exists in S3
            // (as the don't have to explicitly be created).
            // Note: To do a exists with aws filesystem: return Files.exists(getPath());
            return metaDataExists() || getName().endsWith("/");//|| Files.exists(getPath());
        } catch (Exception e) {
            Credentials cred = getCredentials(sourceReference.getSecurityContext(), "s3", bucket);
            throw new GorResourceException(String.format("Exists failed for %s, region: %s, access key: %s, secret key: %s",
                    getName(), client.serviceClientConfiguration().region(),
                    cred != null ? cred.getOrDefault(Credentials.Attr.KEY, "No key in creds") : "No creds",
                    cred != null ? (!StringUtils.isEmpty(cred.getOrDefault(Credentials.Attr.SECRET, "")) ? "Has secret" : "Empty secret")
                                 : "No creds"),
                    getName(), e).retry();
        }
    }

    private boolean metaDataExists()  {
        try {
            // Already in cache, exists
            getSourceMetadata();
            return true;
        } catch (GorResourceException e) {
            var cause = ExceptionUtilities.getUnderlyingCause(e);
            if (cause instanceof NoSuchKeyException) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) {
        try {
            // Files.createDirectory needs elevated access to list all buckets.
            String directoryKey = this.key.endsWith("/") ?this.key : this.key + "/";
            PutObjectRequest request = PutObjectRequest.builder().bucket(this.bucket)
                    .key(directoryKey)
                    //.cacheControl(s3Path.getFileSystem().getRequestHeaderCacheControlProperty())
                    .contentLength(0L)
                    .build();
            client.putObject(request, RequestBody.fromBytes(new byte[0]));
            return PathUtils.formatUri(getPath().toUri());
        } catch (Exception e) {
            throw new GorResourceException(e.getMessage(), getPath().toString(), e).retry();
        }
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) {
        // Files.createDirectory needs elevated access to list all buckets.
//        try {
//            return PathUtils.formatUri(Files.createDirectories(getPath()).toUri());
//        } catch (IOException e) {
//            throw GorResourceException.fromIOException(e, getPath()).retry();
//        }
        // so for now just create the final directory, not the intermediate ones.
        return createDirectory(attrs);
    }

    @Override
    public boolean isDirectory() {
        if (OCI_S3_COMPATIBLE) {
            return key.endsWith("/") || (exists() && this.meta == null);
        }

        return key.endsWith("/") || Files.isDirectory(getPath());
    }

    @Override
    public void delete() {
        if (OCI_S3_COMPATIBLE) {
            if (!exists()) {
                return;
            }
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            client.deleteObject(deleteObjectRequest);
        } else {
            try {
                // Use if exists for folders (that are not reported existing if empty)
                Files.deleteIfExists(getPath());
            } catch (NoSuchFileException e) {
                // Ignore
            } catch (IOException e) {
                throw GorResourceException.fromIOException(e, getPath()).retry();
            }
        }

        invalidateMeta();
    }

    @Override
    public void deleteDirectory() {
        if (OCI_S3_COMPATIBLE) {
            if (!exists()) {
                return;
            }

            if (!isDirectory()) {
                throw new GorResourceException("the path: " + getPath() + " is not a directory", getPath().toString());
            }

            deleteAllWithPrefix();
        } else {
            delete();
        }

        invalidateMeta();
    }

    private void deleteAllWithPrefix() {
        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(bucket)
                .prefix(key)
                .build();

        ListObjectsResponse objectListing = client.listObjects(listObjectsRequest);

        List<ObjectIdentifier> idsToDelete = new ArrayList<>();
        for (var object : objectListing.contents()) {
            idsToDelete.add(ObjectIdentifier.builder().key(object.key()).build());
        }

        if (!idsToDelete.isEmpty()) {
            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(idsToDelete).build())
                    .build();
            client.deleteObjects(deleteObjectsRequest);
        }
    }

    private void invalidateMeta() {
        meta = null;
        if (metadataCache != null) metadataCache.invalidate(bucket + key);
    }

    public String copy(S3Source dest) throws IOException {
        Files.copy(getPath(), dest.getPath());
        return getName();
    }

    @Override
    public Stream<String> list() {
        try {
            return Files.list(getPath()).map(this::s3SubPathToUriString);
        } catch (Exception e) {
            Credentials cred = getCredentials(sourceReference.getSecurityContext(), "s3", bucket);
            throw new GorResourceException(String.format("List failed for %s, region: %s, access key: %s, secret key: %s",
                    getName(), client.serviceClientConfiguration().region(),
                    cred != null ? cred.getOrDefault(Credentials.Attr.KEY, "No key in creds") : "No creds",
                    cred != null ? (!StringUtils.isEmpty(cred.getOrDefault(Credentials.Attr.KEY, "")) ? "Has secret" : "Empty secret")
                            : "No creds"),
                    getName(), e).retry();
        }
    }

    public static Credentials getCredentials(String securityContext, String service, String key) {
        List<Credentials> creds = BundledCredentials.fromSecurityContext(securityContext).getCredentials(service, key);
        if (!creds.isEmpty()) {
            return creds.get(0);
        }
        return null;
    }

    @Override
    public Stream<String> walk() {
        try {
            return Files.walk(getPath()).map(this::s3SubPathToUriString);
        } catch (IOException e) {
            throw GorResourceException.fromIOException(e, getPath()).retry();
        }
    }

    /**
     * Convert Path that we now is sub-path of this, to URI.
     */
    protected String s3SubPathToUriString(Path p) {
        if (p instanceof S3Path) {
            return String.format("s3://%s/%s", ((S3Path) p).getBucketName(), ((S3Path) p).getKey());
        }
        return PathUtils.formatUri(p.toUri());
    }

    @Override
    public Path getPath() {
        if (path == null) {
            path = getS3Path(bucket, key, client);
        }
        return path;
    }

    private Path getS3Path(String bucket, String key, S3Client client) {
        // NOTE:  We keep the old style of creating our own client objects and then create filesystems using that client.
        //        Could also stop creating our own client and just passing all the necessary information to the filesystem
        //        using S3FileSystemProvider.PROPS_TO_OVERLOAD and S3FileSystemProvider.getFileSystem(URI, Map<String, ?>).
        //        Then we could extract the client from the filesystem.
        if (USE_S3_CARLSPRING_FILESYSTEM) {
            var s3fs = s3fsCache.computeIfAbsent(client, c -> S3ClientFileSystemProvider.getInstance().getFileSystem(c));
            return s3fs.getPath("/" + bucket, key);
        } else {
            Credentials cred = getCredentials(sourceReference.getSecurityContext(), "s3", bucket);
            if (cred.getOrDefault(Credentials.Attr.API_ENDPOINT, "").contains("amazonaws.com")) {
                return Path.of(URI.create("s3://%s/%s".formatted(bucket, key)));
            } else
                // Format: s3x://[key:secret@]endpoint[:port]/bucket/objectkey
                return Path.of(URI.create("s3x://%s:%s@%s/%s/%s".formatted(
                        URLEncoder.encode(cred.getOrDefault(Credentials.Attr.KEY, ""), StandardCharsets.UTF_8),
                        URLEncoder.encode(cred.getOrDefault(Credentials.Attr.SECRET, ""), StandardCharsets.UTF_8),
                        cred.getOrDefault(Credentials.Attr.API_ENDPOINT, "https://").substring("https://".length()),
                        bucket,
                        key)));
        }
    }

    @Override
    public SourceType getSourceType() {
        return S3SourceType.S3;
    }

    @Override
    public GorIndexType useIndex() {
        return GorIndexType.CHROMINDEX;
    }

    @Override
    public void close() {
        // No resources to free
    }

    public S3Client getClient() {
        return client;
    }
}
