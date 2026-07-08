package org.gorpipe.s3.driver;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Regression tests for how an S3 source reports its url in diagnostics.
 *
 * <p>Reproduces the case behind the "Exists failed for s3:&lt;relative-path&gt;" log lines:
 * a project-relative source reference resolves to a correct bucket/key, but {@link S3Source#getName()}
 * echoes the raw relative url, which renders as a misleading single-colon {@code s3:<path>} in messages.
 * {@link S3Source#getFullS3Url()} must always render the resolved, well-formed {@code s3://bucket/key}.
 */
public class UTestS3SourceName {

    @Test
    public void fullS3UrlResolvesProjectRelativeName() throws Exception {
        String commonRoot = "s3://gregor-object-storage-prd/projects/clin-hg19-prd";
        String relative = "source/cov/.goodcov_8.wgs.gord.buckets/b_459644_xGF9_1/b_459644_xGF9_1.gorz.gori";

        SourceReference ref = new SourceReference(relative, null, commonRoot, null, null, false);
        S3Source source = new S3Source((S3Client) null, ref);

        // getName() echoes the raw, project-relative url -- this is what rendered as the misleading "s3:source/cov/...".
        assertEquals(relative, source.getName());

        // getFullS3Url() must be the resolved, well-formed s3:// url (bucket + key), used in diagnostics.
        assertEquals("s3://gregor-object-storage-prd/projects/clin-hg19-prd/" + relative, source.getFullS3Url());

        // It must never be the malformed single-colon opaque form.
        assertFalse("full url must not be the malformed s3:<relative> form",
                source.getFullS3Url().startsWith("s3:source"));
    }

    @Test
    public void fullS3UrlPreservesAbsoluteUrl() throws Exception {
        String url = "s3://thebucket/the/path.gorz.gori";
        S3Source source = new S3Source((S3Client) null, new SourceReference(url));

        assertEquals(url, source.getFullS3Url());
    }
}
