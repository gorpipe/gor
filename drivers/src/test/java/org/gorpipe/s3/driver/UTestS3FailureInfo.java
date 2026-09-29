package org.gorpipe.s3.driver;

import org.gorpipe.exceptions.GorResourceException;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

    @Test
    public void s3xUriWithCredentialsNeverLeaksThem() {
        // Format produced by S3Source.getS3Path's non-carlspring-filesystem branch:
        // s3x://[key:secret@]endpoint[:port]/bucket/objectkey
        String[] bucketKey = S3FailureInfo.splitBucketKey("s3x://user:pass@host/bucket/dir/f.gorz");
        assertArrayEquals(new String[]{"bucket", "dir/f.gorz"}, bucketKey);

        String prefix = S3FailureInfo.keyPrefix(bucketKey[1], 2);
        assertFalse(prefix, prefix.contains("user"));
        assertFalse(prefix, prefix.contains("pass"));
        assertFalse(prefix, prefix.contains("@"));
        assertFalse(bucketKey[0], bucketKey[0].contains("user"));
        assertFalse(bucketKey[0], bucketKey[0].contains("pass"));
        assertFalse(bucketKey[0], bucketKey[0].contains("@"));
    }

    @Test
    public void s3xUriWithoutUserinfoTreatsHostAsBucket() {
        // No "@" means no userinfo to strip; with no way to distinguish host from bucket without
        // parsing the s3x scheme specifically, the first path component after the scheme is taken
        // as the bucket -- here that is deterministically "host", not "bucket". This never exposes
        // credentials since there are none in this form.
        assertArrayEquals(new String[]{"host", "bucket/dir/f.gorz"},
                S3FailureInfo.splitBucketKey("s3x://host/bucket/dir/f.gorz"));
    }
}
