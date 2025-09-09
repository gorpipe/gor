package org.gorpipe.util;

import org.junit.Assert;
import org.junit.Test;

public class UTestDateUtils {

    @Test
    public void testParseDateISOEpochFull() {
        Assert.assertEquals("2023-10-05T14:48:00Z", DateUtils.parseDateISOEpoch("2023-10-05T14:48:00Z", false).toString());
        Assert.assertEquals("2023-10-05T14:48:00Z",  DateUtils.parseDateISOEpoch("2023-10-05T14:48:00Z", true).toString());
    }

    @Test
    public void testParseDateISOEpochDateTimeNoZone() {
        Assert.assertEquals("2023-10-05T14:48:00Z", DateUtils.parseDateISOEpoch("2023-10-05T14:48:00", false).toString());
        Assert.assertEquals("2023-10-05T14:48:00Z", DateUtils.parseDateISOEpoch("2023-10-05T14:48:00", true).toString());
    }

    @Test
    public void testParseDateISOEpochMillis() {
        Assert.assertEquals("2023-11-14T22:13:20Z",  DateUtils.parseDateISOEpoch("1700000000000", false).toString());
        Assert.assertEquals("2023-11-14T22:13:20Z", DateUtils.parseDateISOEpoch("1700000000000", true).toString());
    }

    @Test
    public void testParseDateISOEpochYearOnly() {
        Assert.assertEquals("2023-01-01T00:00:00Z",  DateUtils.parseDateISOEpoch("2023", false).toString());
        Assert.assertEquals("2023-12-31T23:59:59Z",  DateUtils.parseDateISOEpoch("2023", true).toString());
    }

    @Test
    public void testParseDateIsoEpochYearMonthOnly() {
        Assert.assertEquals("2023-06-01T00:00:00Z",  DateUtils.parseDateISOEpoch("2023-06", false).toString());
        Assert.assertEquals("2023-06-30T23:59:59Z",  DateUtils.parseDateISOEpoch("2023-06", true).toString());
        Assert.assertEquals("2023-02-28T23:59:59Z",  DateUtils.parseDateISOEpoch("2023-02", true).toString());
    }

    @Test
    public void testParseDateISOEpochDateOnly() {
        Assert.assertEquals("2023-10-05T00:00:00Z", DateUtils.parseDateISOEpoch("2023-10-05", false).toString());
        Assert.assertEquals("2023-10-05T23:59:59Z",  DateUtils.parseDateISOEpoch("2023-10-05", true).toString());
    }

    @Test
    public void testParseDateIsoEpochHoursOnly() {
        Assert.assertEquals("2023-06-15T06:00:00Z",  DateUtils.parseDateISOEpoch("2023-06-15T06", false).toString());
        Assert.assertEquals("2023-06-15T06:59:59Z",  DateUtils.parseDateISOEpoch("2023-06-15T06", true).toString());
    }

    @Test
    public void testParseDateIsoEpochHoursMinutesOnly() {
        Assert.assertEquals("2023-06-15T06:30:00Z",  DateUtils.parseDateISOEpoch("2023-06-15T06:30", false).toString());
        Assert.assertEquals("2023-06-15T06:30:59Z",  DateUtils.parseDateISOEpoch("2023-06-15T06:30", true).toString());
    }

    @Test
    public void testParseDateInvalidFormat() {
        try {
            DateUtils.parseDateISOEpoch("invalid-date", false);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("is not a valid iso date/epoch");
        }
    }

}
