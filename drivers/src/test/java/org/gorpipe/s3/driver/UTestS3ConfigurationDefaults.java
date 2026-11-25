package org.gorpipe.s3.driver;

import org.aeonbits.owner.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Default-value regression coverage for the S3 retry knobs added in ENGKNOW-3723. */
public class UTestS3ConfigurationDefaults {

    @Test
    public void logKeyPrefixSegmentsDefaultsTo1() {
        S3Configuration config = ConfigFactory.create(S3Configuration.class);
        assertEquals(1, config.logKeyPrefixSegments());
    }
}
