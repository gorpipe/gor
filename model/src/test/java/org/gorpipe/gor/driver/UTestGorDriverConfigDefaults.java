package org.gorpipe.gor.driver;

import org.aeonbits.owner.ConfigFactory;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

/** Default-value regression coverage for the S3 retry knobs added in ENGKNOW-3723. */
public class UTestGorDriverConfigDefaults {

    @Test
    public void retryMaxSingleSleepDefaultsTo30Seconds() {
        GorDriverConfig config = ConfigFactory.create(GorDriverConfig.class);
        assertEquals(Duration.ofSeconds(30), config.retryMaxSingleSleep());
    }

    @Test
    public void retryMaxAttemptsDefaultsTo6() {
        GorDriverConfig config = ConfigFactory.create(GorDriverConfig.class);
        assertEquals(6, config.retryMaxAttempts());
    }
}
