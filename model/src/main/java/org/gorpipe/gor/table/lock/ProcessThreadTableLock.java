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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class to acquire named locks.  The locks are reentrant read write locks and can be used for both inter process and inter thread locking.
 * <p>
 * This class splits the locking mechanism into two parts.
 * <p>
 * 1. Thread locking that handles reentrent and inter thread locking, implmented using ReentrantReaderWriterLock.
 * 2. Inter process locking that is not implemented in this class but in subclasses by implementing the acquireProcessLock method.
 * <p>
 * Note:
 * We got 4 differnt lock counts:
 * 1. Count per TableLock object: thisLockCount  (if it is a read or write lock depends on the type).  Note
 * 2. Count per thread (across multiple table locks):  this.lockData.rrwl.getWriteHoldCount() + this.lockData.rrwl.getReadHoldCount()
 * 3. Count per process:  this.lockData.rrwl.getWriteHoldCount() + this.lockData.rrwl.getReadLockCount()
 * 4. Count for all processes (machines).  ??
 * <p>
 * <p>
 * Created by gisli on 08/08/16.
 */
public abstract class ProcessThreadTableLock extends TableLock {

    private static final Logger log = LoggerFactory.getLogger(ProcessThreadTableLock.class);

    // Helper class to hold the process lock.
    protected static class TableLockData {
        final ReentrantReadWriteLock rrwl;
        ProcessLock processLock;

        TableLockData(ReentrantReadWriteLock rrwl, ProcessLock processLock) {
            this.rrwl = rrwl;
            this.processLock = processLock;
        }
    }

    // Map of the process locks.
    private static final ConcurrentHashMap<String, TableLockData> lockMap = new ConcurrentHashMap<>();

    protected Path lockPath;
    protected TableLockData lockData;
    protected int thisLockCount;   // Count lock counts per this TableLock (Note we can have multiple Table locks per thread).

    /**
     * Create lock
     *
     * @param table table to create lock on.
     * @param name  name of lock.
     */
    public ProcessThreadTableLock(BaseTable table, String name) {
        super(table, name);
        this.lockData = lockMap.computeIfAbsent(getId(), k -> new TableLockData(new ReentrantReadWriteLock(), null));
    }

    /**
     * Acquire process lock.
     *
     * @param timeout
     * @return a valid process lock object or null if no lock was acquired.
     * @throws IOException
     */
    protected abstract ProcessLock acquireProcessLock(Duration timeout) throws IOException;

    // Suppress SQ warning about lock not being released.  This class is lock implementation and has separate unlock method.
    @SuppressWarnings("squid:S2222")
    @Override
    protected boolean doLock(Duration timeout) {
        log.trace("Getting lock data for {}", getId());

        // We can not have read lock when getting write lock.  For now just check and fail.
        // Could try to automatically release the read locks, but then we would need to reinstate them
        // when we release the write lock (othewise the code that got those locks could be in trouble).
        if (!isShared() && this.lockData.rrwl.getReadHoldCount() > 0) {
            throw new RuntimeException("FileTableLock in invalid state - Must release all read locks hold by thread before acquiring  write lock.");
        }

        try {
            // Acquire the thread lock.

            Lock threadLock = getThreadLock();
            boolean hasThreadLock = false;
            try {
                if (timeout.toMillis() > 0) {
                    hasThreadLock = threadLock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
                } else if (timeout.toMillis() == 0) {
                    hasThreadLock = threadLock.tryLock();
                } else {
                    threadLock.lockInterruptibly();
                    hasThreadLock = true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("{}", new TableLockLogMessage("Did not get lock, thread interupted"));
            }

            // Acquire the process lock.

            if (hasThreadLock) {
                log.debug("{}", new TableLockLogMessage("Got thread lock"));

                // The thread lock is reentrant so we still need to lock this.lockData before altering it.
                synchronized (this.lockData) {
                    // Can have more than one thread with thread lock (and still no one with the process lock).
                    if (this.lockData.processLock == null || !this.lockData.processLock.isValid()) {
                        this.lockData.processLock = acquireProcessLock(timeout);
                        log.debug("{}", new TableLockLogMessage("Got process lock"));
                    } else {
                        log.trace("{}", new TableLockLogMessage("Process lock already created"));
                    }
                }
            }

            // Validate and update status

            if (hasThreadLock && this.lockData.processLock != null) {
                log.trace("{}", new TableLockLogMessage("Incrementing counter"));
                this.thisLockCount++;
                return true;
            } else {
                TableLockLogMessage msg;
                // Did not get the lock.  If got the thread lock we must clean up.
                if (hasThreadLock) {
                    threadLock.unlock();
                    msg = new TableLockLogMessage("Did not acquire lock, timed out on process lock.");
                } else {
                    msg = new TableLockLogMessage("Did not acquire lock, timed out or interupted on thread lock.");
                }
                // If with timeout we warn, else we assume it is normal not to get the lock.
                if (timeout.toMillis() != 0) {
                    log.warn("{}", msg);
                } else {
                    log.debug("{}", msg);
                }
                return false;
            }
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    public Path getLockPath() {
        return this.lockPath;
    }

    @Override
    public boolean isValid() {
        return this.thisLockCount > 0 && this.lockData.processLock != null && this.lockData.processLock.isValid();
    }

    @Override
    public void assertValid() {
        if (!isValid()) {
            throw new GorSystemException(String.format("Invalid lock: %s (lock count %s, processLock %s, processLock valid %s)",
                    this.id, this.thisLockCount,
                    this.lockData.processLock,
                    this.lockData.processLock != null && this.lockData.processLock.isValid()), null);
        }
    }

    @Override
    public int getReadHoldCount() {
        return this.lockData.rrwl.getReadHoldCount();
    }

    @Override
    public int getWriteHoldCount() {
        return this.lockData.rrwl.getWriteHoldCount();
    }

    @Override
    public long lastModified() {
        return getLockPath().toFile().lastModified();
    }

    @Override
    public long reservedTo() {
        if (this.lockData.processLock != null) {
            return this.lockData.processLock.reservedTo();
        }

        return checkReservedTo();
    }

    /**
     * Check reserved to for this lock.
     */
    protected long checkReservedTo() {
        return -1L;
    }

    @Override
    public void doRelease() {
        log.trace("{}", new TableLockLogMessage("Releasing lock"));

        if (this.thisLockCount == 0) {
            // We don't have any lock.
            log.debug("{}", new TableLockLogMessage("Trying to release when we dont have the lock"));
            return;
        }

        Lock threadLock = getThreadLock();
        threadLock.unlock();
        log.trace("{}", new TableLockLogMessage("Released thread lock"));

        // The thread lock is reentrant so we still need to lock this.data before altering it.
        synchronized (this.lockData) {
            // Can sum up, as only one thread can have the write lock (if we are with a lock no one else can have the write lock).
            if (this.lockData.rrwl.getWriteHoldCount() + this.lockData.rrwl.getReadLockCount() == 0) {
                // The last lock (for all the threads) so we release the process lock too.
                log.trace("{}", new TableLockLogMessage("Releasing process lock"));
                // The last release for the process (and we are the tread holding the lock).
                if (this.lockData.processLock != null) {
                    this.lockData.processLock.release();
                    this.lockData.processLock = null;
                }
                log.debug("{}", new TableLockLogMessage("Released process lock"));
            }
        }

        log.trace("{}", new TableLockLogMessage("Decrementing counter"));
        this.thisLockCount--;
    }

    private Lock getThreadLock() {
        return isShared() ? this.lockData.rrwl.readLock() : this.lockData.rrwl.writeLock();
    }

    // Attempted clean up, ok if does not succeced (as someone else has lock on it).
    // Should be called when we do NOT have the process lock our selves.
    private void tryDeleteLockFile() {
        try {
            if (Files.exists(getLockPath())) {
                try (FileChannel fc = FileChannel.open(getLockPath(), StandardOpenOption.WRITE)) {
                    FileLock lock = fc.tryLock(0, Long.MAX_VALUE, false);
                    if (lock != null && lock.isValid()) {
                        log.debug("{}", new TableLockLogMessage("Deleting process lock file"));
                        // We got exclusive lock, and can hence delete the file.
                        Files.delete(getLockPath());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore, no big deal if the lock file was not deleted.
            log.debug(String.format("Was not able to delete lock file %s, ignored", getLockPath()), e);
        }
    }


}
