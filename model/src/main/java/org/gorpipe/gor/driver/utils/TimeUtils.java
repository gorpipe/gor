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

package org.gorpipe.gor.driver.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A class to simplify unit testing on time dependent functionality. It allows setting a delta to the current
 * system time.
 * <p>
 * Created by stefan on 18.12.2015.
 */
public class TimeUtils implements TimeAdjustable {
    private static final Logger log = LoggerFactory.getLogger(TimeUtils.class);

    private static TimeUtils instance;

    private final AtomicLong delta = new AtomicLong();

    /**
     * Prevent access to the constructor. Everyone should use the static singleton instance.
     */
    private TimeUtils() {
    }

    @Override
    public long getTimeMs() {
        long delta = this.delta.get();
        if (delta != 0) {
            log.trace("Returning wrong time (on purpose). Time delta is: {} ms", delta);
        }
        return System.currentTimeMillis() + delta;
    }

    @Override
    public long addAndGetTimeDelta(long delta) {
        return this.delta.addAndGet(delta);
    }

    @Override
    public void resetTimeDelta() {
        this.delta.set(0);
    }

    @Override
    public boolean hasTimeDelta() {
        return this.delta.get() != 0;
    }

    /**
     * Get the "global" instance of this class. Of course this is not cross-JVM safe.
     *
     * @return a JVM-wide singleton instance of this class.
     */
    public static TimeUtils instance() {
        if (instance == null) {
            instance = new TimeUtils();
        }
        return instance;
    }
}
