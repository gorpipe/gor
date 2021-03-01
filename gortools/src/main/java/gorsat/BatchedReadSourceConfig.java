/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
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

package gorsat;

import org.aeonbits.owner.Config;
import org.gorpipe.base.config.annotations.Documentation;
import org.gorpipe.base.config.converters.DurationConverter;

import java.time.Duration;

/**
 * Created by sigmar on 26/04/2017.
 */
public interface BatchedReadSourceConfig extends Config {
    String BATCHEDREADSOURCETIMETRIGGERMS_KEY = "gor.bufferflush.timetrigger";
    String BATCHOFFERTIMEOUTMS_KEY = "gor.batch.offer.timeout";
    String BATCHLOGINTERVALMS_KEY = "gor.batch.log.interval";
    String BATCHMAXGORLINES_KEY = "gor.batch.max.gorlines";
    String BATCHBUFFERFILLTIME_KEY = "gor.bufferfill.time";
    String FRAMEBUFFERSIZE_KEY = "gor.cmd.framebuffer.size";
    String CONNECTIONRETRIES_KEY = "gor.cmd.connection.retries";
    String CONNECTIONRETRYTIME_KEY = "gor.cmd.connection.retry.time";
    String CONNECTIONRETRYEXPONENT_KEY = "gor.cmd.connection.retry.exponent";

    @Config.Key(CONNECTIONRETRIES_KEY)
    @Config.DefaultValue("10")
    @Documentation("Maximum number of connection retries")
    Integer getConnectionRetries();

    @Config.Key(CONNECTIONRETRYEXPONENT_KEY)
    @Config.DefaultValue("0.0")
    @Documentation("Maximum number of connection retries")
    Double getConnectionRetryExponent();

    @Config.Key(CONNECTIONRETRYTIME_KEY)
    @Config.DefaultValue("1s")
    @ConverterClass(DurationConverter.class)
    @Documentation("Connection retry wait time")
    Duration getConnectionRetryTime();

    @Config.Key(FRAMEBUFFERSIZE_KEY)
    @Config.DefaultValue("4000000")
    @Documentation("Limit the size of gorline batch to fit in GRPC/HTTP2 framebuffer")
    Integer getFrameBufferSize();

    @Config.Key(BATCHMAXGORLINES_KEY)
    @Config.DefaultValue("16384")
    @Documentation("Maximum number of gorlines in batch")
    Integer getMaxGorlines();

    @Config.Key(BATCHBUFFERFILLTIME_KEY)
    @Config.DefaultValue("1000ms")
    @ConverterClass(DurationConverter.class)
    @Documentation("Max milliseconds until read buffer is flushed")
    Duration getBufferFillTime();

    @Config.Key(BATCHEDREADSOURCETIMETRIGGERMS_KEY)
    @Config.DefaultValue("100ms")
    @ConverterClass(DurationConverter.class)
    @Documentation("Minimum milliseconds until read buffer is flushed")
    Duration getBufferFlushTimout();

    @Config.Key(BATCHOFFERTIMEOUTMS_KEY)
    @Config.DefaultValue("100ms")
    @ConverterClass(DurationConverter.class)
    @Documentation("Minimum milliseconds until batch offer times out")
    Duration getBatchOfferTimeout();

    @Config.Key(BATCHLOGINTERVALMS_KEY)
    @Config.DefaultValue("3 seconds")
    @ConverterClass(DurationConverter.class)
    @Documentation("Log interval while waiting for a batch")
    Duration getLogInterval();
}
