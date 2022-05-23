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

package org.gorpipe.gor.driver;

import org.aeonbits.owner.Config;
import org.gorpipe.base.config.annotations.ConfigComponent;
import org.gorpipe.base.config.annotations.Documentation;
import org.gorpipe.base.config.bytesize.ByteSize;
import org.gorpipe.base.config.converters.ByteSizeConverter;
import org.gorpipe.base.config.converters.DurationConverter;
import org.gorpipe.base.config.converters.EnhancedBooleanConverter;

import java.time.Duration;

/**
 * Created by stefan on 31.8.2016.
 */
@ConfigComponent("gor")
public interface GorDriverConfig extends Config {

    @Documentation("Whether the new GorDriver framework is enabled.")
    @Key("org.gorpipe.gor.driver")
    @DefaultValue("enabled")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean enabled();

    @Documentation("Whether to enable full range streaming on remote sources.")
    @Key("org.gorpipe.gor.driver.full_range_streaming.remote")
    @DefaultValue("false")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean remoteFullRangeStreamingEnabled();

    @Documentation("Whether to enable full range streaming on local sources.")
    @Key("org.gorpipe.gor.driver.full_range_streaming.local")
    @DefaultValue("false")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean localFullRangeStreamingEnabled();

    @Documentation("Whether to automatically disconnect HTTP connections.")
    @Key("org.gorpipe.gor.driver.http.disconnect")
    @DefaultValue("true")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean disconnectHttpStream();

    @Documentation("Whether to enable automatic retries for failing sources.")
    @Key("org.gorpipe.gor.driver.retries")
    @DefaultValue("enabled")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean retriesEnabled();

    @Documentation("Maximum number of times for a request retry.")
    @Key("org.gorpipe.gor.driver.retries.max_request_retry")
    @DefaultValue("3")
    int maxRequestRetry();

    @Documentation("Maximum number of times to retry a failing read in a source.")
    @Key("org.gorpipe.gor.driver.retries.max_read_retries")
    @DefaultValue("10")
    int maxReadRetries();

    @Documentation("The time to wait before the first retry.")
    @Key("org.gorpipe.gor.driver.retries.initial_sleep")
    @DefaultValue("125 milliseconds")
    @ConverterClass(DurationConverter.class)
    Duration retryInitialSleep();

    @Documentation("The maximum time to wait for retrying.")
    @Key("org.gorpipe.gor.driver.retries.max_sleep")
    @DefaultValue("64 seconds")
    @ConverterClass(DurationConverter.class)
    Duration retryMaxSleep();

    @Documentation("Whether to automatically extend the range of remote source reads when the driver detects sequential reads.")
    @Key("org.gorpipe.gor.driver.extended_range_streaming.remote")
    @DefaultValue("enabled")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean remoteExtendedRangeStreamingEnabled();

    @Documentation("Whether to automatically extend the range of local source reads when the driver detects sequential reads.")
    @Key("org.gorpipe.gor.driver.extended_range_streaming.local")
    @DefaultValue("disabled")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean localExtendedRangeStreamingEnabled();

    @Documentation("The maximum range to which to automatically extend the source reads.")
    @Key("org.gorpipe.gor.driver.extended_range_streaming.max_request_size")
    @DefaultValue("100 mb")
    @ConverterClass(ByteSizeConverter.class)
    ByteSize extendedRangeStreamingMaxRequestSize();

    @Documentation("The range that triggers automatic range extension.")
    @Key("org.gorpipe.gor.driver.extended_range_streaming.seek_threshold")
    @DefaultValue("32 kb")
    @ConverterClass(ByteSizeConverter.class)
    ByteSize extendedRangeStreamingSeekThreshold();

    @Documentation("The maximum size of an index file to cache.")
    @Key("org.gorpipe.gor.driver.index_cache.file_byte_limit")
    @DefaultValue("10 mb")
    @ConverterClass(ByteSizeConverter.class)
    ByteSize maxSizeOfCachedIndexFile();

    @Documentation("Whether to enable caching of remote sources or not by using the File Cache service")
    @Key("org.gorpipe.gor.driver.remote_cache_enabled")
    @DefaultValue("enabled")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean remoteCacheEnabled();

    @Documentation("The url to the file cache service")
    @Key("filecache.service.url")
    @DefaultValue("")
    String fileCacheServiceUrl();

    @Documentation("Base directory for local caching of gor driver sources.")
    @Key("org.gorpipe.gor.driver.local_cache.dir")
    @DefaultValue("${java.io.tmpdir}/local_gordriver_filecache")
    String cacheDir();

    @Documentation("Maximum total size of the local cache directory.")
    @Key("org.gorpipe.gor.driver.local_cache.max_size")
    @DefaultValue("5 GiB")
    @ConverterClass(ByteSizeConverter.class)
    ByteSize maxSize();

    @Documentation("Whether to enable link folders.")
    @Key("GOR_DRIVER_LINK_FOLDERS")
    @DefaultValue("false")
    @ConverterClass(EnhancedBooleanConverter.class)
    boolean supportLinkFolders();

    @Documentation("Plink executable.")
    @Key("org.gorpipe.gor.driver.plink.executable")
    @DefaultValue("plink2")
    String plinkExecutable();
}