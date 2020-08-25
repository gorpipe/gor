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

package org.gorpipe.gor.monitor;

import org.gorpipe.gor.model.ReadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Messages between gor and its users
 */
public class GorMonitor extends CancelMonitor implements ReadListener, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GorMonitor.class);

    /**
     * Time formatter to log timestamp
     */
    private final DateFormat timestampFormatter = new SimpleDateFormat("m:ss:SSS");

    @Override
    public boolean notify(String chr, int pos, String prevChr) {
        if (!prevChr.equals(chr)) { // Log end of previous chromosome when changing chromosome
            log("GOR progress: " + prevChr + ":" + Integer.MAX_VALUE);
        }
        log("GOR progress: " + chr + ":" + pos);
        return isCancelled();
    }

    /**
     * @param msg log message
     */
    public void log(String msg) {
        final long time = System.currentTimeMillis();
        synchronized (timestampFormatter) {
            String timestamp = timestampFormatter.format(new Date(time));
            log.info("Log: {} {}", timestamp, msg);
        }
    }

    /**
     * @param msg debug message
     */
    public void debug(String msg) {
        final long time = System.currentTimeMillis();
        synchronized (timestampFormatter) {
            String timestamp = timestampFormatter.format(new Date(time));
            log.debug("Log: {} {}", timestamp, msg);
        }
    }

    public void close() {}
}
