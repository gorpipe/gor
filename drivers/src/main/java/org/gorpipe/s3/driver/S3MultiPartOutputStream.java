package org.gorpipe.s3.driver;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.gorpipe.exceptions.GorResourceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class S3MultiPartOutputStream extends OutputStream {
    final static int MAX_CAPACITY = 1<<26;
    ByteBuffer baos1 = ByteBuffer.allocate(MAX_CAPACITY);
    ByteBuffer baos2 = ByteBuffer.allocate(MAX_CAPACITY);
    ByteBuffer baos = baos1;
    String uploadId;
    int k = 1;
    List<PartETag> partETags = new ArrayList<>();
    AmazonS3Client client;
    String bucket;
    String key;
    ExecutorService executorService;
    Future<String> fut = null;

    public S3MultiPartOutputStream(AmazonS3Client client, String bucket, String key) {
        this.client = client;
        this.bucket = bucket;
        this.key = key;
        executorService = Executors.newSingleThreadExecutor();
        var multipartUploadRequest = new InitiateMultipartUploadRequest(bucket, key);
        var multipartUploadResult = client.initiateMultipartUpload(multipartUploadRequest);
        uploadId = multipartUploadResult.getUploadId();
    }

    @Override
    public void write(byte[] bb, int off, int len) throws IOException {
        int payloadLen = baos.limit();
        if (payloadLen>0 && payloadLen+len>MAX_CAPACITY) {
            writeToS3(false);
        }
        baos.put(bb, off, len);
    }

    @Override
    public void write(byte[] bb) throws IOException {
        int payloadLen = baos.limit();
        if (payloadLen>0 && payloadLen+bb.length>MAX_CAPACITY) {
            writeToS3(false);
        }
        baos.put(bb);
    }

    @Override
    public void write(int b) throws IOException {
        if (baos.limit()==MAX_CAPACITY) {
            writeToS3(false);
        }
        baos.put((byte) b);
    }

    private void writeToS3(boolean isLastPart) throws IOException {
        var ibaos = baos;
        baos = baos == baos1 ? baos2 : baos1;
        if (fut!=null) waitForBatch();
        fut = executorService.submit(() -> {
            var len = ibaos.limit();
            var bais = new ByteArrayInputStream(ibaos.array(),0,len);
            var request = new UploadPartRequest();
            var objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(len);
            request.withLastPart(isLastPart).withPartSize(len).withUploadId(uploadId).withBucketName(bucket).withKey(key).withInputStream(bais).withPartNumber(k).withObjectMetadata(objectMetadata);
            var uploadPartResult = client.uploadPart(request);
            partETags.add(new PartETag(k, uploadPartResult.getETag()));
            k++;
            ibaos.reset();
            return "";
        });
    }

    private void waitForBatch() throws IOException {
        try {
            fut.get();
        } catch (InterruptedException | ExecutionException e) {
            AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucket, key, uploadId);
            client.abortMultipartUpload(abortMultipartUploadRequest);
            throw new IOException("Unable to upload multipart to s3 bucket", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            writeToS3(true);
            waitForBatch();
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest();
            completeMultipartUploadRequest.withUploadId(uploadId).withBucketName(bucket).withKey(key).withPartETags(partETags);
            client.completeMultipartUpload(completeMultipartUploadRequest);
        } finally {
            executorService.shutdown();
        }
    }
}