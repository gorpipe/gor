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

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.upplication.s3fs.S3OutputStream;
import com.upplication.s3fs.S3Path;
import com.upplication.s3fs.util.S3UploadRequest;
import org.gorpipe.base.streams.LimitedOutputStream;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.binsearch.GorIndexType;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.RequestRange;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.table.util.PathUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Represents an object in Amazon S3.
 * Created by villi on 22/08/15.
 */
public class S3Source implements StreamSource {
    private static final boolean USE_META_CACHE = true ;
    private final SourceReference sourceReference;
    private final String bucket;
    private final String key;
    private final AmazonS3 client;
    private S3SourceMetadata meta;
    private static final Cache<String, S3SourceMetadata> metadataCache = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(5, TimeUnit.MINUTES).build();

    private Path path;

    private final static int MAX_S3_CHUNKS = 10000;
    private int writeChunkSize = Integer.parseInt(System.getProperty("gor.s3.write.chunksize", String.valueOf(1 << 26)));

    /**
     * Create source
     *
     * @param sourceReference contains S3 url of the form s3://bucket/objectpath
     */
    public S3Source(AmazonS3 client, SourceReference sourceReference) throws MalformedURLException {
        this(client, sourceReference, S3Url.parse(sourceReference));
    }

    S3Source(AmazonS3 client, SourceReference sourceReference, S3Url url) {
        this.client = client;
        this.sourceReference = sourceReference;
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
        if(append) throw new GorResourceException("S3 write not appendable",bucket+"/"+key);
        invalidateMeta();

        long maxFileSize = (long)writeChunkSize * (long)MAX_S3_CHUNKS;
        // S3OutputStream, uses less resources and is slightly faster than our S3MultiPartOutputStream
        return new LimitedOutputStream(
                new S3OutputStream(client, new S3UploadRequest()
                .setObjectId(new S3ObjectId(bucket, key))
                .setChunkSize(writeChunkSize)
                .setMaxThreads(4))
                , maxFileSize);
//        return new S3MultiPartOutputStream(client, bucket, key);
    }

    @Override
    public boolean supportsWriting() {
        return true;
    }

    private InputStream open(RequestRange range) {
        GetObjectRequest req = new GetObjectRequest(bucket, key);
        if (range!=null) {
            range = range.limitTo(getSourceMetadata().getLength());
            if (range.isEmpty()) return new ByteArrayInputStream(new byte[0]);
            req.setRange(range.getFirst(), range.getLast());
        }
        return openRequest(req);
    }

    private InputStream openRequest(GetObjectRequest request) {
        try {
            S3Object object = client.getObject(request);
            return new AbortingInputStream(object.getObjectContent(), request);
        } catch (SdkClientException e) {
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
        try {
            ObjectMetadata md = client.getObjectMetadata(bucket, key);
            return new S3SourceMetadata(this, md, sourceReference.getLinkLastModified(), sourceReference.getChrSubset());
        } catch (SdkClientException e) {
            throw new GorResourceException("Failed to load metadata for " + bucket + "/" + key, getPath().toString(), e).retry();
        }
    }

    private S3SourceMetadata loadMetadataFromCache(String bucket, String key) {
        try {
            return metadataCache.get(bucket + key, () -> {
                // TODO:  If the object does not exists we don't cache.  This method will throw exception and the loader will exit.
                return createMetaData(bucket, key);
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw new GorResourceException("Failed to load metadata from cache for " + bucket + "/" + key, getPath().toString(), e).retry();
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
            // Note: fileExists only handles dirs if the end with /. Therefor we fall back to the much slower Files.exists.
            return fileExists() || Files.exists(getPath());
        } catch (AmazonClientException e) {
            throw new GorResourceException(String.format("Exists failed for %s", getName()),
                    getName(), e).retry();
        }
    }

    private boolean fileExists()  {
        try {
            // Already in cache, exists
            loadMetadata(bucket, key);
            return true;
        } catch (GorResourceException e) {
            return false;
        }
    }

    /*
      Map AmazonS3Exception/ExecutionException/InterruptException correctly.  Map to IOException if retry is wanted.
      Either returns IOException or throws a runtime exception.
     */
    protected IOException mapAndRethrowS3Exceptions(Throwable t, String detail)  {

        // Handle execution exceptions.
        if (t instanceof ExecutionException || t instanceof UncheckedExecutionException) {
            t = t.getCause();
        }

        if (t instanceof AmazonS3Exception) {
            AmazonS3Exception e = (AmazonS3Exception) t;
            detail = detail != null ? detail : e.getMessage();
            if (e.getStatusCode() == 400) {
                throw new GorResourceException(String.format("Bad request for resource. Detail: %s. Original message: %s", detail, e.getMessage()), detail, e);
            } else if (e.getStatusCode() == 401) {
                throw new GorResourceException(String.format("Unauthorized. Detail: %s. Original message: %s", detail, e.getMessage()), detail, e);
            } else if (e.getStatusCode() == 403) {
                throw new GorResourceException(String.format("Access Denied. Detail: %s. Original message: %s", detail, e.getMessage()), detail, e);
            } else if (e.getStatusCode() == 404) {
                throw new GorResourceException(String.format("Not Found. Detail: %s. Original message: %s", detail, e.getMessage()), detail, e);
            } else {
                return new IOException(e);
            }
        } else if (t instanceof SdkClientException) {
            return new IOException(detail, t);
        } else if (t instanceof IOException) {
            return (IOException) t;
        } else {
            return new IOException(t);
        }
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) {
        try {
            return PathUtils.formatUri(Files.createDirectory(getPath()).toUri());
        } catch (IOException e) {
            throw GorResourceException.fromIOException(e, getPath()).retry();
        }
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) {
        try {
            return PathUtils.formatUri(Files.createDirectories(getPath()).toUri());
        } catch (IOException e) {
            throw GorResourceException.fromIOException(e, getPath()).retry();
        }
    }

    @Override
    public boolean isDirectory() {
        return key.endsWith("/") || Files.isDirectory(getPath());
    }

    @Override
    public void delete() {
        try {
            Files.deleteIfExists(getPath());  // Use if exists for folders (that are not reported existing if empty)
        } catch (IOException e) {
            throw GorResourceException.fromIOException(e, getPath()).retry();
        }
        invalidateMeta();
    }

    @Override
    public void deleteDirectory() {
        // Implementation based on S3FileSystemProvider.delete with different logic for deleting the prefix.
        Preconditions.checkArgument(getPath() instanceof S3Path,
                "path must be an instance of %s", S3Path.class.getName());

        if (Files.notExists(getPath())){
            return;
        }

        if (!Files.isDirectory(getPath())){
            throw new GorResourceException("the path: " + getPath() + " is not a directory", getPath().toString());
        }

        deleteAllWithPrefix();

        invalidateMeta();
    }

    private void deleteAllWithPrefix() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(key);

        ObjectListing objectListing = getClient().listObjects(listObjectsRequest);

        while (true) {
            List<DeleteObjectsRequest.KeyVersion> keysToDelete = new ArrayList<>();
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                keysToDelete.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
            }
            if (!keysToDelete.isEmpty()) {
                DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket).withKeys(keysToDelete);
                getClient().deleteObjects(deleteObjectsRequest);
            }
            if (objectListing.isTruncated()) {
                objectListing = getClient().listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
    }

    private void invalidateMeta() {
        meta = null;
        metadataCache.invalidate(bucket + key);
    }

    public String copy(S3Source dest) throws IOException {
        Files.copy(getPath(), dest.getPath());
        return getName();
    }

    @Override
    public Stream<String> list() {
        try {
            return Files.list(getPath()).map(this::s3SubPathToUriString);
        } catch (IOException e) {
            throw GorResourceException.fromIOException(e, getPath()).retry();
        }
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
        return PathUtils.formatUri(p.toUri());
    }

    @Override
    public Path getPath() {
        if (path == null) {
            path = getS3Path(bucket, key, client);
        }
        return path;
    }

    public static Path getS3Path(String bucket, String key, AmazonS3 client) {
        FileSystem s3fs;
        try {
            s3fs = S3ClientFileSystemProvider.getInstance().getFileSystem(client);
        } catch (ProviderNotFoundException | FileSystemNotFoundException e) {
            s3fs = S3ClientFileSystemProvider.getInstance().newFileSystem(client, null);
        }
        return s3fs.getPath("/" + bucket, key);
        //return s3fs.getPath(key);
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

    public AmazonS3 getClient() {
        return client;
    }
}
