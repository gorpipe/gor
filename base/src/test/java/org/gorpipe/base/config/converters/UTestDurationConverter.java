/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.base.config.converters;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.gorpipe.base.config.converters.DurationConverter;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UTestDurationConverter {

    public interface DurationTypesConfig extends Config {
        // empty suffix
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10")
        Duration emptySuffix();

        // no space before suffix
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10ms")
        Duration noSpaceBeforeSuffix();

        // nanoseconds
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 ns")
        Duration nsSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 nano")
        Duration nanoSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 nanos")
        Duration nanosSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 nanosecond")
        Duration nanosecondSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 nanoseconds")
        Duration nanosecondsSuffix();

        // microseconds
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 us")
        Duration usSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 µs")
        Duration µsSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 micro")
        Duration microSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 micros")
        Duration microsSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 microsecond")
        Duration microsecondSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 microseconds")
        Duration microsecondsSuffix();

        // milliseconds
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 ms")
        Duration msSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 milli")
        Duration milliSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 millis")
        Duration millisSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 millisecond")
        Duration millisecondSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 milliseconds")
        Duration millisecondsSuffix();

        // seconds
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 s")
        Duration sSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 second")
        Duration secondSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 seconds")
        Duration secondsSuffix();

        // minutes
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 m")
        Duration mSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 minute")
        Duration minuteSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 minutes")
        Duration minutesSuffix();

        // hours
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 h")
        Duration hSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 hour")
        Duration hourSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 hours")
        Duration hoursSuffix();

        // days
        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 d")
        Duration dSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 day")
        Duration daySuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("10 days")
        Duration daysSuffix();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("PT15M")
        Duration iso8601NoPrefix15Minutes();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("-PT15M")
        Duration iso8601MinusPrefix15Minutes();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("+PT15M")
        Duration iso8601PlusPrefix15Minutes();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("-PT-6H+3M")
        Duration iso8601Complex();

        @ConverterClass(DurationConverter.class)
        @DefaultValue("invalid")
        Duration invalid();
    }

    private static boolean allEqual(Duration compareTo, Collection<Duration> durations) {
        for (Duration duration : durations) {
            if (!compareTo.equals(duration)) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testEmptySuffix() {
        DurationTypesConfig cfg = ConfigFactory.create(DurationTypesConfig.class);
        assertEquals(Duration.of(10, ChronoUnit.MILLIS), cfg.emptySuffix());
    }

    @Test
    public void testNoSpaceBeforeSuffix() {
        DurationTypesConfig cfg = ConfigFactory.create(DurationTypesConfig.class);
        assertEquals(Duration.of(10, ChronoUnit.MILLIS), cfg.noSpaceBeforeSuffix());
    }

    @Test
    public void testSuffixEquality() {
        DurationTypesConfig cfg = ConfigFactory.create(DurationTypesConfig.class);

        allEqual(Duration.of(10, ChronoUnit.NANOS),
                Arrays.asList(cfg.nsSuffix(), cfg.nanoSuffix(), cfg.nanosSuffix(), cfg.nanosecondSuffix(), cfg.nanosecondsSuffix()));

        allEqual(Duration.of(10, ChronoUnit.MICROS),
                Arrays.asList(cfg.usSuffix(), cfg.µsSuffix(), cfg.microSuffix(), cfg.microsSuffix(), cfg.microsecondSuffix(), cfg.microsecondsSuffix()));

        allEqual(Duration.of(10, ChronoUnit.MILLIS),
                Arrays.asList(cfg.msSuffix(), cfg.milliSuffix(), cfg.millisSuffix(), cfg.millisecondSuffix(), cfg.millisecondsSuffix()));

        allEqual(Duration.of(10, ChronoUnit.SECONDS),
                Arrays.asList(cfg.sSuffix(), cfg.secondSuffix(), cfg.secondsSuffix()));

        allEqual(Duration.of(10, ChronoUnit.MINUTES),
                Arrays.asList(cfg.mSuffix(), cfg.minuteSuffix(), cfg.minutesSuffix()));

        allEqual(Duration.of(10, ChronoUnit.HOURS),
                Arrays.asList(cfg.hSuffix(), cfg.hourSuffix(), cfg.hoursSuffix()));

        allEqual(Duration.of(10, ChronoUnit.DAYS),
                Arrays.asList(cfg.dSuffix(), cfg.daySuffix(), cfg.daysSuffix()));
    }

    @Test
    public void testIso8601() {
        DurationTypesConfig cfg = ConfigFactory.create(DurationTypesConfig.class);
        assertEquals(Duration.of(15, ChronoUnit.MINUTES), cfg.iso8601NoPrefix15Minutes());
        assertEquals(cfg.iso8601NoPrefix15Minutes(), cfg.iso8601PlusPrefix15Minutes());
        assertEquals(Duration.of(-15, ChronoUnit.MINUTES), cfg.iso8601MinusPrefix15Minutes());
        assertEquals(Duration.of(6, ChronoUnit.HOURS).minus(3, ChronoUnit.MINUTES), cfg.iso8601Complex());
    }

    @Test
    public void testInvalid() {
        DurationTypesConfig cfg = ConfigFactory.create(DurationTypesConfig.class);

        try {
            cfg.invalid();
            fail("Invalid time string should have thrown exception.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}

