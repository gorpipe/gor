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
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Duration;

/**
 * Class do acquire named locks.  The locks are read write locks and can be used for both interprocess
 * and inter-thread locking.  The lock might or might not be reentrent.
 * <p>
 * Note:
 * 1. Should work both between threads and processes and machines.
 * 2. Read lock must be released before acquiring write lock (in the same thread).
 * <p>
 * Created by gisli on 08/08/16.
 */
public abstract class TableLock implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TableLock.class);

    public enum LockType {READ, WRITE}

    protected final String name;
    protected final String id;
    protected boolean shared = false;

    private Thread shutDownHookThread;

    /**
     * Factory method to create new read locks.
     *
     * @param lockClass type of lock to use.
     * @param table     table to acquire the lock on.
     * @param name      name of the lock.
     * @param timeout   timeout in milliseconds, 0=trylock with no timeout, -1=Wait for ever.
     * @return new read lock object with the given state.
     */
    public static TableLock acquireRead(Class<? extends TableLock> lockClass, BaseDictionaryTable table, String name, Duration timeout) {
        return TableLock.acquire(lockClass, table, name, true, timeout);
    }

    /**
     * Factory method to create new write locks.
     *
     * @param lockClass type of lock to use.
     * @param table     table to acquire the lock on.
     * @param name      name of the lock.
     * @param timeout   timeout in milliseconds, 0=trylock with no timeout, -1=Wait for ever.
     * @return new write lock object with the given state.
     */
    public static TableLock acquireWrite(Class<? extends TableLock> lockClass, BaseDictionaryTable table, String name, Duration timeout) {
        return TableLock.acquire(lockClass, table, name, false, timeout);
    }

    /**
     * Factory method to create new locks.
     *
     * @param lockClass type of lock to use.
     * @param table     table to acquire the lock on.
     * @param name      name of the lock.
     * @param shared    is the lock shared (read lock) or not shared (write lock).
     * @param timeout   timeout in milliseconds, 0=trylock with no timeout, -1=Wait for ever.
     * @return new lock object with the given state.
     */
    private static TableLock acquire(Class<? extends TableLock> lockClass, BaseDictionaryTable table, String name, boolean shared, Duration timeout) {
        table.initialize();
        TableLock lock;
        try {
            lock = lockClass.getDeclaredConstructor(new Class[]{BaseDictionaryTable.class, String.class}).newInstance(table, name);
        } catch (Exception e) {
            throw new RuntimeException("Could not create a new lock object", e);
        }
        lock.lock(shared, timeout);
        return lock;
    }



    /**
     * Construct new table lock object.
     *
     * @param table the table the lock belongs to.
     * @param name  name of the lock.
     */
    public TableLock(BaseDictionaryTable table, String name) {
        this.name = name;
        this.id = String.format("tablelock-%s-%s-%s", table.getFolderPath().toString(), table.getName(), name);

        // Add shutdown hook to do as we can to clean up.
        this.shutDownHookThread = new Thread(() -> release());
        Runtime.getRuntime().addShutdownHook(this.shutDownHookThread);
        log.trace("Added shutdown hook for process lock {}.", this);
    }

    /**
     * Lock this lock.
     *
     * @param shared  should get shared (read lock) lock or not (write lock).
     * @param timeout timeout in milliseconds when getting lock, 0=trylock with no timeout, -1=Wait for ever.
     */
    public void lock(boolean shared, Duration timeout) {
        this.shared = shared;
        // Get the lock.
        boolean didGetLock = doLock(timeout);

        // Validate lock.
        if (!isValid() && didGetLock) {
            throw new RuntimeException("Invalid lock state - got invalid lock but it has not timed out");
        }
    }

    /**
     * Do the actual locking.  Must be overridden be classes implementing locks.
     *
     * @param timeout timeout in milliseconds, 0=trylock with no timeout, -1=Wait for ever.
     * @return true if we got the lock, otherwise false (the lock request timed out).
     */
    protected abstract boolean doLock(Duration timeout);

    /**
     * Release the lock.   Must be overridden be classes implementing locks.
     */
    public void release() {
        doRelease();

        if (this.shutDownHookThread != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutDownHookThread);
                this.shutDownHookThread = null;
            } catch (IllegalStateException ise) {
                // Ignore, happens if we are in 'shutdown'.  No easy way to detect.
            }
            log.trace("Removed shutdown hook for process lock {}.", this);
        }
    }

    /**
     * Do the actual releasing.  Must be overridden be classes implementing locks.
     */
    protected abstract void doRelease();

    /**
     * Check the validity of the lock.   Must be overridden be classes implementing locks.
     *
     * @return true if the lock is valid(active), otherwise false.
     */
    public abstract boolean isValid();


    /**
     * Get number of read locks held by this thread.   Must be overridden be classes implementing locks.
     *
     * @return number of read locks held by this thread.
     */
    public abstract int getReadHoldCount();

    /**
     * Get number of write locks held by this thread.   Must be overridden be classes implementing locks.
     *
     * @return number of write locks held by this thread.
     */
    public abstract int getWriteHoldCount();

    /**
     * Last modified date of this lock   Should be overridden be classes implementing locks.
     * Default implementation gives just current time in milliseconds.
     *
     * @return last time (in millis since epoch) the lock was updated.
     */
    public long lastModified() {
        // Default assume it just canged.
        return System.currentTimeMillis();
    }

    public abstract long reservedTo();

    /**
     * Check the validity of the lock, exception is thrown if the lock is invalid.
     */
    public void assertValid() {
        if (!isValid()) {
            throw new GorSystemException("Invalid lock: " + this.id, null);
        }
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return (isValid() ? "Valid " : "Invalid ") + (isShared() ? "ReadLock" : "WriteLock");
    }

    public boolean isShared() {
        return this.shared;
    }

    public void close() {
        this.release();
    }

    /**
     * Helper class for creating detailed lock log messages.
     */
    public class TableLockLogMessage {
        String message;

        public TableLockLogMessage(String message) {
            this.message = message;
        }

        public String toString() {
            String type = getDescription();
            return String.format("%s for lock %s (%s) in process %s and thread %s(%d) - %s (thread lock count %d %d)", this.message, getName(), getId(),
                    ManagementFactory.getRuntimeMXBean().getName(),
                    Thread.currentThread().getName(), Thread.currentThread().getId(), type, getReadHoldCount(), getWriteHoldCount());
        }
    }
}
