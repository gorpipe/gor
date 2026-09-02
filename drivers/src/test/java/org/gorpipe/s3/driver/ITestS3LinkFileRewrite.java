package org.gorpipe.s3.driver;

import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.ExtendedRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryStreamSourceWrapper;
import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * End-to-end regression test for ENGKNOW-3722, against real S3.
 *
 * <p>Reads a versioned {@code .link} file through the production wrapper chain
 * ({@code ExtendedRangeWrapper} over {@code RetryStreamSourceWrapper} over {@link S3Source}), then has
 * a second writer replace the object with a much smaller one, then reads it again in the same JVM.
 *
 * <p>The rewrite goes through the raw {@link S3Client} on purpose. {@code S3Source.getOutputStream}
 * calls {@code invalidateMeta()}, which would clear this JVM's cached length and hide the bug; in
 * production the writer is a different pod, so every other reader keeps the pre-rewrite length. Those
 * readers then clamp their next range to the old, larger length, request bytes at or past the new end
 * of the object, and get {@code 416 Range Not Satisfiable}.
 *
 * <p>Before the fix the second read fails with
 * {@code byte range bytes=<n>-<m> cannot be satisfied from object with content length <n>}.
 */
@Category(IntegrationTests.class)
public class ITestS3LinkFileRewrite {

    private static final String BUCKET = "gdb-unit-test-data";
    private static final String REGION = "eu-west-1";
    private static final String KEY_PREFIX = "csa_test_data/data_sets/gor_driver_testfiles/engknow3722/";

    private static String s3Key;
    private static String s3Secret;

    @BeforeClass
    public static void setUpClass() {
        var props = DriverUtils.getDriverProperties();
        s3Key = props.getProperty("S3_KEY");
        s3Secret = props.getProperty("S3_SECRET");
    }

    private static S3Client newClient() {
        return S3Client.builder()
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.builder().accessKeyId(s3Key).secretAccessKey(s3Secret).build()))
                .build();
    }

    /** A many-versioned link file, the shape that versioned-link GC later compacts. */
    private static String manyVersionsLinkBody(int entries) {
        var sb = new StringBuilder();
        sb.append("## SERIAL = ").append(entries).append('\n');
        sb.append("## VERSION = 1\n");
        sb.append("#FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO\n");
        var base = Instant.parse("2026-01-01T00:00:00.000Z");
        for (int i = 1; i <= entries; i++) {
            sb.append("source/versions/generation_").append(i).append(".gorz\t")
                    .append(base.plusSeconds(i)).append("\tMD5SUM").append(i).append("\t")
                    .append(i).append("\t\n");
        }
        return sb.toString();
    }

    /** What the link file looks like after compaction: a single surviving entry. */
    private static String compactedLinkBody() {
        return """
                ## SERIAL = 1
                ## VERSION = 1
                #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                source/versions/compacted.gorz\t2026-02-01T00:00:00.000Z\tMD5SUMC\t1\t
                """;
    }

    private static void put(S3Client client, String key, String body) {
        client.putObject(PutObjectRequest.builder().bucket(BUCKET).key(key).build(),
                RequestBody.fromString(body));
    }

    /** Reads the link file through the same wrapper chain {@code StreamSourceProvider.wrap} builds. */
    private static String latestEntryUrl(S3Client client, String url) throws Exception {
        // Short retry budget: this test is about whether the read succeeds, not about how long the
        // retry ladder takes. UTestS3RetryHandler covers the fail-fast classification.
        StreamSource wrapped = new ExtendedRangeWrapper(
                new RetryStreamSourceWrapper(new S3RetryHandler(100, 2000),
                        new S3Source(client, new SourceReference(url))),
                65536, 8 * 1024 * 1024);
        try (wrapped) {
            return LinkFile.load(wrapped).getLatestEntryUrl();
        }
    }

    @Test
    public void linkFileCompactedByAnotherWriterIsStillReadable() throws Exception {
        var key = KEY_PREFIX + UUID.randomUUID() + "/dbsnp_test.gorz.link";
        var url = "s3://" + BUCKET + "/" + key;

        try (var client = newClient()) {
            var large = manyVersionsLinkBody(1200);
            put(client, key, large);

            // First read: seeds this JVM's S3 metadata cache with the pre-compaction length.
            assertTrue("first read must resolve the newest generation",
                    latestEntryUrl(client, url).endsWith("generation_1200.gorz"));

            // Another writer compacts the link file. Much smaller object, same key, and this JVM's
            // cached length is not invalidated.
            put(client, key, compactedLinkBody());
            assertTrue("the compacted link file must be smaller than the original",
                    compactedLinkBody().length() < large.length());

            assertTrue("a link file rewritten smaller must still be readable",
                    latestEntryUrl(client, url).endsWith("compacted.gorz"));
        } finally {
            try (var client = newClient()) {
                client.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET).key(key).build());
            }
        }
    }
}
