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

package org.gorpipe.gor.table.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by gisli on 17/01/2017.
 */
public abstract class RenewableLockHelper {

    private static final Logger log = LoggerFactory.getLogger(RenewableLockHelper.class);

    public static final Duration DEFAULT_RESERVE_LOCK_PERIOD = Duration.ZERO;  // Zero means there will be no lock update thread.

    // Renew delta, i.e. how long before the lock expiries do we try to renew it, only active if less than 0.5 the reserve period.
    private static final Duration DEFAULT_RENEW_LOCK_DELTA = Duration.ofMillis(Integer.valueOf(System.getProperty("gor.table.lock.process.renew_delta", "86400000"))); // 1 day.  Large as this depends clock syncs.
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    // Duration of the locking period (the renew cycle), if null the period is infinite.
    private final Duration reserveLockPeriod;
    private long reservedTo;
    private ScheduledFuture<?> renewHandle = null;

    public RenewableLockHelper() {
        this(DEFAULT_RESERVE_LOCK_PERIOD);
    }

    public RenewableLockHelper(Duration reserveLockPeriod) {
        this.reserveLockPeriod = reserveLockPeriod;
        reservedTo = calcExpirationTime();
        // Start a renew thread, but only if we the period more than 0.
        if (reserveLockPeriod != null && !reserveLockPeriod.isZero() && !reserveLockPeriod.isNegative()) {
            // The renew period must be a little shorter than the reserve period to make sure we renew before expiration.
            long periodMS = Math.max(reserveLockPeriod.toMillis() / 2, reserveLockPeriod.toMillis() - DEFAULT_RENEW_LOCK_DELTA.toMillis());
            log.debug("Scheduling lock renew to run every {} ms for {}.", periodMS, this);
            this.renewHandle = scheduler.scheduleAtFixedRate(() -> {
                try {
                    renew();
                    reservedTo = calcExpirationTime();
                } catch (Throwable t) {
                    log.error("Could not renew lock because of an exception!", t);
                    // Could not renew the lock, the state of this process is unknown and we should exit.  Users of the lock need to use
                    // shutdown hooks for proper cleaning.
                    // TODO:  Shutdown is very harsh so for now we leave it out and just try to release the lock.  Users should assert the lock state before use.
                    //System.exit(-1);
                    release();
                }
            }, periodMS, periodMS, TimeUnit.MILLISECONDS);
        }
    }

    public abstract void renew();

    synchronized public void release() {
        if (this.renewHandle != null) {
            log.debug("Cancelling renew for {}.", this);
            this.renewHandle.cancel(true);

        }

    }

    public long reservedTo() {
        return reservedTo;
    }

    synchronized protected long calcExpirationTime() {
        if (reserveLockPeriod != null && !reserveLockPeriod.isZero() && !reserveLockPeriod.isNegative()) {
            return System.currentTimeMillis() + reserveLockPeriod.toMillis();
        } else {
            return Long.MAX_VALUE;
        }
    }
}
