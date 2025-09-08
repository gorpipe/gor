package org.gorpipe.s3.driver;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

public class S3MultipartOutputStreamSync extends S3MultipartOutputStream {

    private final S3Client s3Client;

    public S3MultipartOutputStreamSync(S3Client s3Client, String bucket, String key) throws IOException {
        super(bucket, key);
        this.s3Client = s3Client;
    }

    @Override
    protected CreateMultipartUploadResponse sendCreateMultipartUploadRequest(CreateMultipartUploadRequest req) {
        return s3Client.createMultipartUpload(req);
    }

    @Override
    protected UploadPartResponse sendUploadPartRequest(UploadPartRequest req, byte[] data) throws Exception {
        return s3Client.uploadPart(req, software.amazon.awssdk.core.sync.RequestBody.fromBytes(data));
    }

    @Override
    protected CompleteMultipartUploadResponse sendCompleteMultipartUploadRequest(CompleteMultipartUploadRequest req) throws Exception {
        return s3Client.completeMultipartUpload(req);
    }

    @Override
    protected AbortMultipartUploadResponse sendAbortMultipartUploadRequest(AbortMultipartUploadRequest req) {
        return s3Client.abortMultipartUpload(req);
    }
}
