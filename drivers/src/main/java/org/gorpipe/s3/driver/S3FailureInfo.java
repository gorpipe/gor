package org.gorpipe.s3.driver;

import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * The parts of an S3 failure worth logging on a retry line. Keys can identify samples, so only a
 * directory prefix of the key is kept, never the file name.
 */
record S3FailureInfo(String bucket, String keyPrefix, String status, String errorCode) {
    static final String NONE = "-";
    static final String NO_STATUS = "-1";

    static S3FailureInfo of(GorException e, int keyPrefixSegments) {
        String uri = e instanceof GorResourceException gre ? gre.getUri() : null;
        String[] bucketKey = splitBucketKey(uri);

        String status = NO_STATUS;
        String errorCode = NONE;
        if (ExceptionUtilities.getUnderlyingCause(e) instanceof S3Exception s3e) {
            status = String.valueOf(s3e.statusCode());
            if (s3e.awsErrorDetails() != null && s3e.awsErrorDetails().errorCode() != null) {
                errorCode = s3e.awsErrorDetails().errorCode();
            }
        }
        return new S3FailureInfo(bucketKey[0], keyPrefix(bucketKey[1], keyPrefixSegments), status, errorCode);
    }

    /**
     * Accepts {@code s3://bucket/key}, {@code /bucket/key}, {@code bucket/key}, and the
     * {@code s3x://[user:pass@]host/bucket/key} form {@code S3Source.getS3Path} produces for
     * non-AWS endpoints. Any URI scheme is stripped, and any userinfo (credentials) before an
     * {@code @} is dropped along with the host, so credentials can never end up in the bucket or
     * key -- and so never on a retry log line.
     */
    static String[] splitBucketKey(String uri) {
        if (uri == null || uri.isBlank()) return new String[]{NONE, ""};
        String s = uri.replaceFirst("^[A-Za-z][A-Za-z0-9+.-]*:/*", "").replaceFirst("^/+", "");

        int firstSlash = s.indexOf('/');
        int at = s.indexOf('@');
        if (at >= 0 && (firstSlash < 0 || at < firstSlash)) {
            // Authority with userinfo (e.g. "user:pass@host"): drop everything up to and
            // including the path separator right after the host, so no credential fragment
            // survives into the bucket or key.
            int afterAuthoritySlash = s.indexOf('/', at);
            s = afterAuthoritySlash < 0 ? "" : s.substring(afterAuthoritySlash + 1);
        }

        int slash = s.indexOf('/');
        if (slash < 0) return new String[]{s.isEmpty() ? NONE : s, ""};
        return new String[]{s.substring(0, slash), s.substring(slash + 1)};
    }

    /** First {@code segments} directory segments of the key, with trailing slash; never the file name. */
    static String keyPrefix(String key, int segments) {
        if (segments <= 0 || key == null || key.isEmpty()) return NONE;
        String[] parts = key.split("/");
        int keep = Math.min(segments, parts.length - 1);
        if (keep <= 0) return NONE;
        return String.join("/", java.util.Arrays.copyOf(parts, keep)) + "/";
    }
}
