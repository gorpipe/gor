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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.BaseTable;
import org.gorpipe.gor.table.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * Class do acquire named locks.  The locks can be used for both inter process and inter thread locking.
 * The lock is not reentrent.
 * <p>
 * We use exclusive lock file for locking.    The file is opened with CREATE_NEW which throws error if the while exists.
 * <p>
 * Processes must have access to the same file system.
 * <p>
 * Created by gisli on 08/08/16.
 */
public class ExclusiveFileTableLock extends TableLock {

    private static final Logger log = LoggerFactory.getLogger(ExclusiveFileTableLock.class);

    private static final long OLDEST_LOCK_DATE_POSSIBLE = 1388534400;  // Start of year 2014.
    private static final Duration EXCL_DEFAULT_RESERVE_LOCK_PERIOD = Duration.ofMillis(Integer.valueOf(System.getProperty("gor.table.lock.exclusive.lock_period", "43200000"))); // 12 hours.

    private final Duration checkForLockPeriod = Duration.ofMillis(Integer.valueOf(System.getProperty("gor.table.lock.exclusive.check_period", "100")));  // Time between check for lock (if waiting.)

    private Path lockPath;
    private FileChannel fc;
    private RenewableLockHelper lockHelper;
    protected int thisLockCount;

    /**
     * Create lock
     *
     * @param table table to create lock on.
     * @param name  name of lock.
     */
    public ExclusiveFileTableLock(BaseTable table, String name) {
        super(table, name);
        this.lockPath = table.getFolderPath().resolve(String.format("%s.%s.excl.lock", table.getName(), name));
    }

    @Override
    protected boolean doLock(Duration timeout) {
        if (!isShared() && getReadHoldCount() > 0) {
            TableLockLogMessage  msg = new TableLockLogMessage("ExclusiveFileTableLock in invalid state - Must release all read locks hold by thread before acquiring write lock.");
            throw new GorSystemException(msg.toString(), null);
        }

        if (!isValid())  {
            try {
                 createLock(timeout);
            } catch (Exception e) {
                throw new GorSystemException(new TableLockLogMessage("Error while getting lock").toString(), e);
            }
            log.debug("{}", new TableLockLogMessage("Got process lock"));
        } else {
            log.trace("{}", new TableLockLogMessage("Process lock already created"));
        }

        // Validate and update status

        if (isValid()) {
            log.trace("{}", new TableLockLogMessage("Incrementing counter"));
            thisLockCount++;
            return true;
        } else {
            // Did not get the lock.  If got the thread lock we must clean up.
            TableLockLogMessage  msg = new TableLockLogMessage("Did not acquire lock, timed out or interrupted on thread lock.");

            // If with timeout we warn, else we assume it is normal not to get the lock.
            if (timeout.toMillis() != 0) {
                log.warn("{}", msg);
            } else {
                log.debug("{}", msg);
            }
            return false;
        }
    }

    @Override
    protected void doRelease() {
        if (this.thisLockCount == 0) {
            // We don't have any lock.
            log.debug("{}", new TableLockLogMessage("Trying to release when we dont have the lock"));
            return;
        }

        thisLockCount--;
        log.debug("Lockcount on releasee: " + thisLockCount);
        if (thisLockCount == 0) {
            deleteLock();
        }
    }

    @Override
    public boolean isValid() {
        return lockHelper != null && lockHelper.reservedTo() >= System.currentTimeMillis()
                && fc != null && fc.isOpen();
    }

    @Override
    public int getReadHoldCount() {
        return isShared() ? thisLockCount : 0;
    }

    @Override
    public int getWriteHoldCount() {
        return isShared() ? 0 : thisLockCount;
    }

    @Override
    public long reservedTo() {
        return lockHelper != null ? lockHelper.reservedTo() : -1;
    }

    public Path getLockPath() {
        return this.lockPath;
    }

    @SuppressWarnings("squid:S2095") //file lock not acquired when using try-with-resources
    protected void createLock(Duration timeout) throws IOException {
        long requestTime = System.currentTimeMillis();
        // Check for expiration either once, half through the wait or every 1 minutes.
        long checkForExpirationInterval = Math.min(Duration.ofMillis(timeout.toMillis() / 2).toMillis(), Duration.ofMinutes(1).toMillis());
        long checkForExpirationTime = requestTime + checkForExpirationInterval;
        log.debug("{}", new TableLockLogMessage("About to create/access process lock file"));
        do {
            try {
                fc = FileChannel.open(getLockPath(),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SYNC);

                // If here we got the lock.

                lockHelper = new RenewableLockHelper(EXCL_DEFAULT_RESERVE_LOCK_PERIOD) {
                    @Override
                    public synchronized void renew() {
                        if (!Files.exists(getLockPath())) {
                            throw new AcquireLockException("Could not renew lock as the lock file does not exist!");
                        }

                        if (!isValid()) {
                            throw new AcquireLockException("Could not renew lock as it is not valid!");
                        }

                        try {
                            writeToLockFile(fc, "acquired", reservedTo(), false);
                        } catch (Exception e) {
                            throw new AcquireLockException("Could not renew lock because of an exception!", e);
                        }
                        log.trace("Renewing process lock to {}.", reservedTo());
                    }
                };

                writeToLockFile(fc, "acquired", reservedTo(), false);

                return;
            } catch (FileAlreadyExistsException e) {
                // If here we did not get the lock.
                long currentTime = System.currentTimeMillis();
                log.debug("{}", new TableLockLogMessage(String.format("Waiting for lock (been waiting for %d millis)", currentTime - requestTime)));
                if (currentTime >= checkForExpirationTime) {
                    // Check if the lock is expired.
                    checkAndReleaseExpiredLock(currentTime);
                    checkForExpirationTime += checkForExpirationInterval;
                }

                try {
                    Thread.sleep(this.checkForLockPeriod.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } while (System.currentTimeMillis() < requestTime + Math.max(timeout.toMillis(), 0));
    }

    private void deleteLock() {
        try {
            if (Files.exists(getLockPath()) && isValid()) {
                writeToLockFile(fc, "Releasing", 0, true);
                fc.close();
                log.trace("Deleting process lock file");
                // We got exclusive lock, and can hence delete the file.
                Files.delete(getLockPath());
            }

            if (lockHelper != null){
                lockHelper.release();
                lockHelper = null;
            }
        } catch (IOException e) {
            throw new GorSystemException("Could not release lock because of an exception!", e);
        }
    }

    /**
     * Helper method to log info to the lock file itself.
     * NOTE:  will only write out data if we have the lock.
     *
     * @param message    message to write.
     * @param append     if true the message is appened, otherwise it replaces the lockfile content.
     * @param reservedTo epoch reserved to.
     */
    protected void writeToLockFile(FileChannel fc, String message, long reservedTo, boolean append) {
        if (fc.isOpen()) {
            DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss:SSS");
            // Note:  This format is parsed when acquiring locks so be careful to update that parsing logic if format changes.
            String desc = String.format("%s - %s:%s(%s) - %s %s %s\t%d%n", formatter.format(new Date()),
                    ManagementFactory.getRuntimeMXBean().getName(), Thread.currentThread().getName(), Thread.currentThread().getId(),
                    message, getName(), isShared() ? "ReadLock" : "WriteLock", reservedTo);
            log.trace("Writing '{}' to lockfile", desc);
            try {
                if (append) {
                    fc.position(fc.size());
                } else {
                    fc.truncate(0);
                    fc.position(0);
                }
                fc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap(desc)));
                fc.force(true);
            } catch (Exception e) {
                log.warn("Failed updating lock file with status data", e);
            }
        }
    }

    private void checkAndReleaseExpiredLock(long currentTime) {
        try {
            // Check if the lock file is expired.
            List<String> lines = Files.readAllLines(getLockPath());
            if (!lines.isEmpty() && lines.get(0) != null && lines.get(0).contains("\t")) {
                long lockExpirationTime = Long.parseLong(lines.get(0).split("\t")[1]);
                if (lockExpirationTime > OLDEST_LOCK_DATE_POSSIBLE && lockExpirationTime < currentTime) {
                    Files.delete(getLockPath());
                    DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss:SSS");
                    String acquiredDate = lines.get(0).split(" - ")[0];
                    if (log.isWarnEnabled()) {
                        log.warn("Deleted expired lock file {} (acquired {}, expired at {})",
                                getLockPath(), acquiredDate, formatter.format(new Date(lockExpirationTime)));
                    }
                }
            }
        } catch (NoSuchFileException nsfe) {
            // Ignore, the file has been deleted, so just continue trying to get the lock.
            log.debug("Lock was deleted while we were checking if was expired.  Will continue trying ot acquire it.");
        } catch (Exception e1) {
            log.warn("Exception when checking/deleting lockfile.", e1);
        }
    }
}
