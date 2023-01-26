package org.gorpipe.s3.driver;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class S3MultiPartOutputStream extends OutputStream {
    private static final Logger log = LoggerFactory.getLogger(S3MultiPartOutputStream.class);

    final static int MAX_CAPACITY = 1<<26;  //1<<26 Note:1<<24  5<<20 5MiB is the small allowed size.
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
    Future<String> fut;

    public S3MultiPartOutputStream(AmazonS3Client client, String bucket, String key) {
        this.client = client;
        this.bucket = bucket;
        this.key = key;
        executorService = Executors.newSingleThreadExecutor();
        fut = executorService.submit(() -> {
            var multipartUploadRequest = new InitiateMultipartUploadRequest(bucket, key);
            var multipartUploadResult = client.initiateMultipartUpload(multipartUploadRequest);
            return multipartUploadResult.getUploadId();
        });
    }

    @Override
    public void write(byte[] bb, int off, int len) throws IOException {
        int left = len;
        while (left > 0) {
            if (!baos.hasRemaining()) {
                writeToS3(false);
            }
            int nextlen = Math.min(left,baos.remaining());
            baos.put(bb, off+len-left, nextlen);
            left -= nextlen;
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (!baos.hasRemaining()) {
            writeToS3(false);
        }
        baos.put((byte) b);
    }

    private void writeToS3(boolean isLastPart) throws IOException {
        var arr = baos.array();
        var len = baos.position();
        baos.rewind();
        baos = baos == baos1 ? baos2 : baos1;
        uploadId = waitForBatch();
        fut = executorService.submit(() -> {
            var bais = new ByteArrayInputStream(arr,0,len);
            var request = new UploadPartRequest();
            var objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(len);
            request.withLastPart(isLastPart).withPartSize(len).withUploadId(uploadId).withBucketName(bucket).withKey(key).withInputStream(bais).withPartNumber(k).withObjectMetadata(objectMetadata);
            var uploadPartResult = client.uploadPart(request);
            partETags.add(new PartETag(k, uploadPartResult.getETag()));
            k++;
            return uploadId;
        });
    }

    private String waitForBatch() throws IOException {
        try {
            return fut.get();
        } catch (InterruptedException | ExecutionException e) {
            String errorMsg = "Unable to upload multipart to s3 bucket";
            if (uploadId!=null) {
                AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucket, key, uploadId);
                try {
                    client.abortMultipartUpload(abortMultipartUploadRequest);
                } catch (Exception ee) {
                    log.error("S3 write abort failed", ee);
                    errorMsg += ": Could not abort (" + uploadId + ")";
                }
            }
            throw new IOException(errorMsg, e);
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