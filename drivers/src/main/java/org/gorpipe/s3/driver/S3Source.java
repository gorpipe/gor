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

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.binsearch.GorIndexType;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.RequestRange;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an object in Amazon S3.
 * Created by villi on 22/08/15.
 */
public class S3Source implements StreamSource {
    private final SourceReference sourceReference;
    private final String bucket;
    private final String key;
    private final AmazonS3Client client;
    private S3SourceMetadata meta;
    private static final Cache<String, S3SourceMetadata> metadataCache = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(2, TimeUnit.HOURS).build();
    private static final Random rnd = new Random();

    private Path path;

    /**
     * Create source
     *
     * @param sourceReference contains S3 url of the form s3://bucket/objectpath
     */
    public S3Source(AmazonS3Client client, SourceReference sourceReference) throws MalformedURLException {
        this(client, sourceReference, S3Url.parse(sourceReference));
    }

    S3Source(AmazonS3Client client, SourceReference sourceReference, S3Url url) {
        this.client = client;
        this.sourceReference = sourceReference;
        this.bucket = url.getBucket();
        this.key = url.getPath();
    }

    @Override
    public InputStream open() throws IOException {
        return open(null);
    }

    @Override
    public InputStream open(long start) throws IOException {
        return open(RequestRange.fromFirstLength(start, getSourceMetadata().getLength()));
    }

    @Override
    public InputStream open(long start, long minLength) throws IOException {
        return open(RequestRange.fromFirstLength(start, minLength));
    }

    @Override
    public OutputStream getOutputStream(boolean append) throws IOException {
        if(append) throw new GorResourceException("S3 write not appendable",bucket+"/"+key);
        return new S3MultiPartOutputStream(client, bucket, key);
    }

    @Override
    public boolean supportsWriting() {
        return true;
    }

    private InputStream open(RequestRange range) throws IOException {
        GetObjectRequest req = new GetObjectRequest(bucket, key);
        if (range!=null) {
            range = range.limitTo(getSourceMetadata().getLength());
            if (range.isEmpty()) return new ByteArrayInputStream(new byte[0]);
            req.setRange(range.getFirst(), range.getLast());
        }
        return openRequest(req);
    }

    private InputStream openRequest(GetObjectRequest request) throws IOException {
        try {
            S3Object object = client.getObject(request);
            return object.getObjectContent();
        } catch(SdkClientException sdkClientException) {
            throw new IOException("Unable to handle S3 request: " + Arrays.stream(request.getRange()).mapToObj(Long::toString).collect(Collectors.joining(",")) + ": " + sourceReference.getUrl(), sdkClientException);
        }
    }

    @Override
    public String getName() {
        return sourceReference.getUrl();
    }

    private S3SourceMetadata loadMetadataFromCache(String bucket, String key) throws ExecutionException {
        meta = metadataCache.get(bucket + key, () -> {
            ObjectMetadata md = client.getObjectMetadata(bucket, key);
            return new S3SourceMetadata(this, md, sourceReference.getLinkLastModified(), sourceReference.getChrSubset());
        });
        return meta;
    }

    @Override
    public S3SourceMetadata getSourceMetadata() throws IOException {
        if (meta == null) {
            try {
                loadMetadataFromCache(bucket, key);
            } catch (ExecutionException | UncheckedExecutionException e) {
                // Need IOException for the retry handler
                throw new IOException(e);
            }
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
    public boolean exists() throws IOException  {
        return fileExists() ? true : Files.exists(getPath());
    }

    @Override
    public boolean fileExists() throws IOException  {
        try {
            // Already in cache, exists
            loadMetadataFromCache(bucket, key);
            return true;
        } catch (ExecutionException | UncheckedExecutionException e) {
            var throwable = e.getCause();
            if (throwable instanceof AmazonS3Exception s3exp) {
                if (s3exp.getStatusCode() == 404) {
                    // The meta data check does not handle the existsance of 'folders' correctly, so if we get
                    // 404 and have a folder we need to use Files.exists (with S3 filesystem path) that handles
                    // S3 'folders'.
                    return false;
                }
            }
            throw new IOException("S3 fileExists failed: " + bucket+key, throwable);
        }
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) throws IOException {
        return Files.createDirectory(getPath()).toString();
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) throws IOException {
        return Files.createDirectories(getPath()).toString();
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(getPath());
    }

    @Override
    public void delete() throws IOException {
        Files.delete(getPath());
    }

    @Override
    public Stream<String> list() throws IOException {
        return Files.list(getPath()).map(Path::toString);
    }

    private Path getPath() {
        if (path == null) {
            FileSystem s3fs;
            try {
                s3fs = FileSystems.getFileSystem(URI.create("s3://" + bucket));
            } catch (ProviderNotFoundException | FileSystemNotFoundException e) {
                s3fs = new S3ClientFileSystemProvider().createFileSystem(URI.create("s3://" + bucket), new HashMap<String, String>(), client);
            }
            path = s3fs.getPath("/" + bucket, key);
        }
        return path;
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
    public void close() throws IOException {
        // No resources to free
    }

    public AmazonS3Client getClient() {
        return client;
    }
}
