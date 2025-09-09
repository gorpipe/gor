package org.gorpipe.s3.driver;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class S3MultipartOutputStreamAsync extends S3MultipartOutputStream {

    private final S3AsyncClient s3Client;

    public S3MultipartOutputStreamAsync(S3AsyncClient s3Client, String bucket, String key) throws IOException {
        super(bucket, key);
        this.s3Client = s3Client;
    }

    @Override
    protected CreateMultipartUploadResponse sendCreateMultipartUploadRequest(CreateMultipartUploadRequest req) throws ExecutionException, InterruptedException {
        return s3Client.createMultipartUpload(req).get();
    }

    @Override
    protected UploadPartResponse sendUploadPartRequest(UploadPartRequest req, byte[] data) throws Exception {
        return s3Client.uploadPart(req, AsyncRequestBody.fromBytes(data)).get();
    }

    @Override
    protected CompleteMultipartUploadResponse sendCompleteMultipartUploadRequest(CompleteMultipartUploadRequest req) throws Exception {
        return s3Client.completeMultipartUpload(req).get();
    }

    @Override
    protected AbortMultipartUploadResponse sendAbortMultipartUploadRequest(AbortMultipartUploadRequest req) {
        return s3Client.abortMultipartUpload(req).join();
    }
}
