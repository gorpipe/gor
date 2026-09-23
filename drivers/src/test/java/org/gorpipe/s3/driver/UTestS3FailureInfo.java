package org.gorpipe.s3.driver;

import org.gorpipe.exceptions.GorResourceException;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UTestS3FailureInfo {

    @Test
    public void splitsAllUriForms() {
        assertArrayEquals(new String[]{"b", "dir/sub/f.gorz"}, S3FailureInfo.splitBucketKey("s3://b/dir/sub/f.gorz"));
        assertArrayEquals(new String[]{"b", "dir/f.gorz"}, S3FailureInfo.splitBucketKey("/b/dir/f.gorz"));
        assertArrayEquals(new String[]{"b", "dir/f.gorz"}, S3FailureInfo.splitBucketKey("b/dir/f.gorz"));
        assertArrayEquals(new String[]{"b", ""}, S3FailureInfo.splitBucketKey("s3://b"));
        assertArrayEquals(new String[]{"-", ""}, S3FailureInfo.splitBucketKey(null));
    }

    @Test
    public void keyPrefixNeverIncludesFileName() {
        assertEquals("dir/", S3FailureInfo.keyPrefix("dir/sub/f.gorz", 1));
        assertEquals("dir/sub/", S3FailureInfo.keyPrefix("dir/sub/f.gorz", 2));
        assertEquals("dir/sub/", S3FailureInfo.keyPrefix("dir/sub/f.gorz", 5));
        assertEquals("-", S3FailureInfo.keyPrefix("f.gorz", 1));
        assertEquals("-", S3FailureInfo.keyPrefix("dir/sub/f.gorz", 0));
        assertEquals("-", S3FailureInfo.keyPrefix("", 1));
    }

    @Test
    public void readsStatusAndErrorCode() {
        var s3e = (S3Exception) S3Exception.builder().statusCode(429)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("TooManyRequests").build())
                .build();
        var e = new GorResourceException("Failed to open S3 object", "s3://b/dir/f.gorz", s3e);

        var info = S3FailureInfo.of(e, 1);

        assertEquals("b", info.bucket());
        assertEquals("dir/", info.keyPrefix());
        assertEquals("429", info.status());
        assertEquals("TooManyRequests", info.errorCode());
    }

    @Test
    public void nonS3CauseHasNoStatus() {
        var e = new GorResourceException("io", "s3://b/dir/f.gorz", new java.io.IOException("reset"));
        var info = S3FailureInfo.of(e, 1);
        assertEquals("-1", info.status());
        assertEquals("-", info.errorCode());
    }
}
