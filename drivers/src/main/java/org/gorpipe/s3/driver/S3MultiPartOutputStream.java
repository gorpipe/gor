package org.gorpipe.s3.driver;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class S3MultiPartOutputStream extends OutputStream {
    final static int MAX_CAPACITY = 1<<27;
    ByteArrayOutputStream baos = new ByteArrayOutputStream(MAX_CAPACITY);
    String uploadId;
    int k = 1;
    List<PartETag> partETags = new ArrayList<>();
    AmazonS3Client client;
    String bucket;
    String key;

    public S3MultiPartOutputStream(AmazonS3Client client, String bucket, String key) {
        this.client = client;
        this.bucket = bucket;
        this.key = key;
        var multipartUploadRequest = new InitiateMultipartUploadRequest(bucket, key);
        var multipartUploadResult = client.initiateMultipartUpload(multipartUploadRequest);
        uploadId = multipartUploadResult.getUploadId();
    }

    @Override
    public void write(byte[] bb, int off, int len) throws IOException {
        int payloadLen = baos.size();
        if (payloadLen>0 && payloadLen+len>MAX_CAPACITY) {
            writeToS3(false);
        }
        baos.write(bb, off, len);
    }

    @Override
    public void write(byte[] bb) throws IOException {
        int payloadLen = baos.size();
        if (payloadLen>0 && payloadLen+bb.length>MAX_CAPACITY) {
            writeToS3(false);
        }
        baos.write(bb);
    }

    @Override
    public void write(int b) throws IOException {
        baos.write(b);
        if (baos.size()==MAX_CAPACITY) {
            writeToS3(false);
        }
    }

    private void writeToS3(boolean isLastPart) {
        var bb = baos.toByteArray();
        var bais = new ByteArrayInputStream(bb);
        var request = new UploadPartRequest();
        var objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(bb.length);
        request.withLastPart(isLastPart).withPartSize(bb.length).withUploadId(uploadId).withBucketName(bucket).withKey(key).withInputStream(bais).withPartNumber(k).withObjectMetadata(objectMetadata);
        var uploadPartResult = client.uploadPart(request);
        partETags.add(new PartETag(k, uploadPartResult.getETag()));
        k++;
        baos.reset();
    }

    @Override
    public void close() {
        writeToS3(true);
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest();
        completeMultipartUploadRequest.withUploadId(uploadId).withBucketName(bucket).withKey(key).withPartETags(partETags);
        client.completeMultipartUpload(completeMultipartUploadRequest);
    }
}