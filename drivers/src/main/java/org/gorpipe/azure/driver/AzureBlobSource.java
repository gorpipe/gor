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

package org.gorpipe.azure.driver;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.core.BaseRequest;
import com.microsoft.azure.storage.core.StorageRequest;
import com.microsoft.azure.storage.core.UriQueryBuilder;
import com.microsoft.azure.storage.core.Utility;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.RequestRange;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

/**
 * Represents an object in Microsoft Azure.
 * Created by simmi on 10/09/15.
 */
public class AzureBlobSource implements StreamSource {
    private final SourceReference sourceReference;
    private final String bucket;
    private final String key;
    CloudBlockBlob cb;
    OperationContext opContext;
    BlobRequestOptions options;

    /**
     * Create source
     *
     * @param sourceReference azure url of the form az://bucket/objectpath
     */
    public AzureBlobSource(SourceReference sourceReference) throws InvalidKeyException, StorageException, URISyntaxException {
        this.sourceReference = sourceReference;
        String[] parsed = AzureBlobHelper.parseUrl(this.sourceReference.getUrl());
        bucket = parsed[0];
        key = parsed[1];
        init();
    }

    /**
     * Create source
     *
     * @param bucket azure bucket name
     * @param key    Object key (path)
     */
    public AzureBlobSource(String bucket, String key, String subset) throws InvalidKeyException, StorageException, URISyntaxException {
        this.bucket = bucket;
        this.key = key;
        this.sourceReference = new SourceReferenceBuilder(AzureBlobHelper.makeUrl(bucket, key))
                .chrSubset(subset).build();
        init();
    }

    private void init() throws URISyntaxException, InvalidKeyException, StorageException {
        CloudBlobClient bcli = AzureBlobHelper.getAzure();
        CloudBlobContainer container = bcli.getContainerReference(bucket);
        cb = container.getBlockBlobReference(key);
        cb.downloadAttributes();

        opContext = new OperationContext();
        opContext.initialize();
        options = new BlobRequestOptions();

        CloudBlobClient blobServiceClient = cb.getServiceClient();
        BlobRequestOptions clientOptions = blobServiceClient.getDefaultRequestOptions();
        BlobType blobtype = cb.getProperties().getBlobType();
        options.setRetryPolicyFactory(clientOptions.getRetryPolicyFactory());
        options.setLocationMode(clientOptions.getLocationMode());
        options.setTimeoutIntervalInMs(clientOptions.getTimeoutIntervalInMs());
        options.setMaximumExecutionTimeInMs(clientOptions.getMaximumExecutionTimeInMs());
        //options.setOperationExpiryTimeInMs(new Date().getTime() + modifiedOptions.getMaximumExecutionTimeInMs());
        options.setConcurrentRequestCount(clientOptions.getConcurrentRequestCount());
        options.setSingleBlobPutThresholdInBytes(clientOptions.getSingleBlobPutThresholdInBytes());
        options.setUseTransactionalContentMD5(clientOptions.getUseTransactionalContentMD5());
        options.setStoreBlobContentMD5(clientOptions.getStoreBlobContentMD5());
        options.setDisableContentMD5Validation(clientOptions.getDisableContentMD5Validation());
        options.setRetryPolicyFactory(new RetryExponentialRetry());
        options.setLocationMode(LocationMode.PRIMARY_ONLY);
        options.setConcurrentRequestCount(1);
        options.setSingleBlobPutThresholdInBytes(32 * Constants.MB);
        options.setUseTransactionalContentMD5(false);
        options.setStoreBlobContentMD5(blobtype == BlobType.BLOCK_BLOB);
        options.setDisableContentMD5Validation(false);
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

    private static void addSnapshot(final UriQueryBuilder builder, final String snapshotVersion)
            throws StorageException {
        if (snapshotVersion != null) {
            builder.add(Constants.QueryConstants.SNAPSHOT, snapshotVersion);
        }
    }

    public static HttpURLConnection getBlob(final URI uri, final BlobRequestOptions blobOptions,
                                            final OperationContext opContext, final AccessCondition accessCondition, final String snapshotVersion,
                                            final Long offset, final Long count, boolean requestRangeContentMD5) throws IOException,
            URISyntaxException, StorageException {
        if (offset != null && requestRangeContentMD5) {
            Utility.assertNotNull("count", count);
            Utility.assertInBounds("count", count, 1, Constants.MAX_BLOCK_SIZE);
        }

        final UriQueryBuilder builder = new UriQueryBuilder();
        addSnapshot(builder, snapshotVersion);
        final HttpURLConnection request = BaseRequest.createURLConnection(uri, blobOptions, builder, opContext);
        request.setRequestMethod(Constants.HTTP_GET);

        if (accessCondition != null) {
            accessCondition.applyConditionToRequest(request);
        }

        if (offset != null) {
            long rangeStart = offset;
            long rangeEnd;
            if (count != null) {
                rangeEnd = offset + count - 1;
                request.setRequestProperty(Constants.HeaderConstants.STORAGE_RANGE_HEADER, String.format(
                        Utility.LOCALE_US, Constants.HeaderConstants.RANGE_HEADER_FORMAT, rangeStart, rangeEnd));
            } else {
                request.setRequestProperty(Constants.HeaderConstants.STORAGE_RANGE_HEADER, String.format(
                        Utility.LOCALE_US, Constants.HeaderConstants.BEGIN_RANGE_HEADER_FORMAT, rangeStart));
            }
        }

        if (offset != null && requestRangeContentMD5) {
            request.setRequestProperty(Constants.HeaderConstants.RANGE_GET_CONTENT_MD5, Constants.TRUE);
        }

        return request;
    }

    private InputStream open(RequestRange range) throws IOException {
        try {
            HttpURLConnection urlc = getBlob(cb.getUri(), options, opContext, null, null, range == null ? null : range.getFirst(), range == null ? null : range.getLast() - range.getFirst() + 1, false);
            StorageRequest.signBlobQueueAndFileRequest(urlc, cb.getServiceClient(), -1L, null);
            return urlc.getInputStream();
        } catch (StorageException e) {
            throw new IOException("Unable to open inputstream on Azure url " + sourceReference.getUrl(), e);
        } catch (URISyntaxException | InvalidKeyException e) {
            throw new RuntimeException("Unable to open inputstream on Azure url " + sourceReference.getUrl(), e);
        }
    }

    @Override
    public String getName() {
        return sourceReference.getUrl();
    }

    @Override
    public StreamSourceMetadata getSourceMetadata() throws IOException {
        long length = cb.getProperties().getLength();
        long lastModified = cb.getProperties().getLastModified().getTime();
        return new StreamSourceMetadata(this, getName(), lastModified, length, cb.getProperties().getEtag(), false);
    }

    @Override
    public SourceReference getSourceReference() {
        return sourceReference;
    }

    @Override
    public DataType getDataType() {
        return DataType.fromFileName(key);
    }

    // TODO: Check for Azure key existence.
    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public SourceType getSourceType() {
        return AzureSourceType.AZURE;
    }

    @Override
    public void close() throws IOException {
        // No resources to free
    }
}
