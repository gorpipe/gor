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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import org.gorpipe.gor.table.BaseTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class do acquire named locks.  The locks are reentrant can be used for both inter process and inter thread locking.
 * Within process they are read write locks, but between processes they are limited to single lock object (same for read and write).
 * <p>
 * For inter process locking we use exclusive lock file.    The file is opened with CREATE_NEW which throws error if the while exists.
 * <p>
 * Processes must have access to the same file system.
 * <p>
 * Created by gisli on 08/08/16.
 */
public class ExclusiveFileTableLock extends ProcessThreadTableLock {

    private static final Logger log = LoggerFactory.getLogger(ExclusiveFileTableLock.class);

    private static final long OLDEST_LOCK_DATE_POSSIBLE = 1388534400;  // Start of year 2014.
    private static final Duration EXCL_DEFAULT_RESERVE_LOCK_PERIOD = Duration.ofMillis(Integer.valueOf(System.getProperty("gor.table.lock.exclusive.lock_period", "43200000"))); // 12 hours.

    private Duration checkForLockPeriod = Duration.ofMillis(Integer.valueOf(System.getProperty("gor.table.lock.exclusive.check_period", "100")));  // Time between check for lock (if waiting.)

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

    @SuppressWarnings("squid:S2095") //file lock not acquired when using try-with-resources
    @Override
    protected ProcessLock acquireProcessLock(Duration timeout) throws IOException {
        long requestTime = System.currentTimeMillis();
        // Check for expiration either once, half through the wait or every 1 minutes.
        long checkForExpirationInterval = Math.min(Duration.ofMillis(timeout.toMillis() / 2).toMillis(), Duration.ofMinutes(1).toMillis());
        long checkForExpirationTime = requestTime + checkForExpirationInterval;
        log.debug("{}", new TableLockLogMessage("About to create/access process lock file"));
        do {
            try {
                FileChannel fc = FileChannel.open(getLockPath(),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SYNC);

                // If here we got the lock.

                ProcessLock processLock = new ProcessLock(EXCL_DEFAULT_RESERVE_LOCK_PERIOD) {
                    long reservedTo = calcExpirationTime();

                    @Override
                    public synchronized boolean isValid() {
                        return super.isValid() && fc.isOpen();
                    }

                    @Override
                    public synchronized void renew() {
                        if (!Files.exists(getLockPath())) {
                            throw new AcquireLockException("Could not renew lock as the lock file does not exist!");
                        }

                        if (!isValid()) {
                            throw new AcquireLockException("Could not renew lock as it is not valid!");
                        }

                        reservedTo = calcExpirationTime();
                        try {
                            writeToLockFile(fc, "acquired", reservedTo(), false);
                        } catch (Exception e) {
                            throw new AcquireLockException("Could not renew lock because of an exception!", e);
                        }
                        log.trace("Renewing process lock to {}.", reservedTo);
                    }

                    @Override
                    public synchronized long reservedTo() {
                        return reservedTo;
                    }

                    @Override
                    public synchronized void release() {
                        super.release();
                        try {
                            if (Files.exists(getLockPath()) && isValid()) {
                                writeToLockFile(fc, "Releasing", 0, true);
                                fc.close();
                                log.trace("Deleting process lock file");
                                // We got exclusive lock, and can hence delete the file.
                                Files.delete(getLockPath());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Could not release lock because of an exception!", e);
                        }
                    }
                };

                writeToLockFile(fc, "acquired", processLock.reservedTo(), false);

                return processLock;
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
                    return null;
                }
            }
        } while (System.currentTimeMillis() < requestTime + Math.max(timeout.toMillis(), 0));

        return null;
    }

    @Override
    public long reservedTo() {
        if (this.lockData.processLock != null) {
            return this.lockData.processLock.reservedTo();
        }

        return checkReservedTo();
    }

    @Override
    protected long checkReservedTo() {
        try {
            List<String> lines = Files.readAllLines(getLockPath());
            if (!lines.isEmpty() && lines.get(0) != null && lines.get(0).contains("\t")) {
                return Long.parseLong(lines.get(0).split("\t")[1]);
            }
        } catch (IOException ioe) {
            log.warn("Exception when checking/deleting lockfile.", ioe);
        }

        return -1;
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
