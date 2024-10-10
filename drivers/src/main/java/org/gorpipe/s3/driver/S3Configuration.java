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

package org.gorpipe.s3.driver;

import org.aeonbits.owner.Config;
import org.gorpipe.base.config.annotations.Documentation;
import org.gorpipe.base.config.converters.DurationConverter;

import java.time.Duration;

public interface S3Configuration extends Config {

    @Documentation("Connection timeout to use for S3 (E.g. '2 minutes')")
    @Key("gor.s3.conn.timeout")
    @DefaultValue("2 seconds")
    @ConverterClass(DurationConverter.class)
    Duration connectionTimeout();

    @Documentation("The default timeout for reading from a connected socket.")
    @Key("gor.s3.socket.timeout")
    @DefaultValue("5 minutes")
    @ConverterClass(DurationConverter.class)
    Duration socketTimeout();

    @Documentation("S3 connection pool size")
    @Key("gor.s3.conn.pool.size")
    @DefaultValue("10000")
    int connectionPoolSize();

    @Documentation("S3 max driver retry")
    @Key("gor.s3.conn.retries")
    @DefaultValue("3")
    int connectionRetries();

    @Documentation("S3 validate after inactivity (millis)")
    @Key("gor.s3.validate.after.inactivity")
    @DefaultValue("100")
    int validateAfterInactivityMillis();

    @Documentation("S3 endpoint url")
    @Key("gor.s3.endpoint")
    @DefaultValue("")
    String s3Endpoint();
}
