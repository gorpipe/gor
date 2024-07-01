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

import org.gorpipe.gor.table.livecycle.TableInfoBase;
import org.gorpipe.gor.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Class to handle transactions on tables.
 *
 * The transaction gets a lock on the table, reloads it and if this is write transactions it will save the table on
 * a commit.
 *
 * NOTE: When a transaction is created it reloads the table from disk, removing any changes to the table rows and
 *       meta data.  So when using transactions on existing tables, first create the transaction and then change
 *       the table.  For new tables changes made to the table before the transaction are saved.
 */
public class TableTransaction implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TableTransaction.class);
    private final TableLock lock;
    private boolean shouldSave = false;
    private Runnable closeHook;

    /**
     * Factory method to create new read transaction.  Similar to acquire lock but it throws exception if lock not acquired and
     * auto loads the table on successful lock.
     *
     * @param lockClass type of lock to use.
     * @param table     table to acquire the lock on.
     * @param name      name of the lock.
     * @param timeout   timeout in milliseconds, 0=trylock with no timeout, -1=Wait for ever.
     * @return new read lock object with the given state.
     * @throws AcquireLockException if we did not get lock.
     */
    public static TableTransaction openReadTransaction(Class<? extends TableLock> lockClass, Table table, String name, Duration timeout) {
        return TableTransaction.openTransaction(lockClass, table, name, true, timeout);
    }

    /**
     * Factory method to create new write transaction.  Similar to acquire lock but it throws exception if lock not acquired, auto loads the table on
     * successful lock an saves it on Close (not release) if commit was done.
     *
     * @param lockClass type of lock to use.
     * @param table     table to acquire the lock on.
     * @param name      name of the lock.
     * @param timeout   timeout in milliseconds, 0=trylock with no timeout, -1=Wait for ever.
     * @return new lock object with the given state.
     * @throws AcquireLockException if we did not get lock.
     */
    public static TableTransaction openWriteTransaction(Class<? extends TableLock> lockClass, Table table, String name, Duration timeout) {
        return TableTransaction.openTransaction(lockClass, table, name, false, timeout);
    }

    /**
     * Factory method to create new transaction.  Similar to acquire lock but it throws exception if lock not acquired, loads the table on successful lock
     * (both read and write), and saves it on Close (not release) (only write ofcourse) if commit was done.
     * Note:  The save is only performed if commit was done and this is the last write lock so be careful if you are mixing acquire and acquireAuto, so for
     * example if the last lock is aquired with aquire you MUST call save you self.
     *
     * @param lockClass type of lock to use.
     * @param table     table to acquire the lock on.
     * @param name      name of the lock.
     * @param shared    is the lock shared (read lock) or not shared (write lock).
     * @param timeout   timeout in milliseconds, 0=trylock with no timeout, -1=Wait for ever.
     * @return new lock object with the given state.
     * @throws AcquireLockException if we did not get lock.
     */
    private static TableTransaction openTransaction(Class<? extends TableLock> lockClass, Table table, String name, boolean shared, Duration timeout) {
        return new TableTransaction(lockClass, table, name, shared, timeout);
    }

    public TableTransaction(Class<? extends TableLock> lockClass, Table table, String name, boolean shared, Duration timeout) {
        if (shared) {
            lock = TableLock.acquireRead(lockClass, table, name, timeout);
        } else {
            lock = TableLock.acquireWrite(lockClass, table, name, timeout);
        }

        if (!lock.isValid()) {
            throw new AcquireLockException(String.format("Getting lock timed out for %s on %s (timeout %s)!", name, table.getName(), timeout.toString()));
        }

        try {
            if (shared == false) {
                if (table instanceof TableInfoBase) {
                    ((TableInfoBase) table).updateNFSFolderMetadata();
                }
                if (this.closeHook == null) {
                    this.closeHook = () -> {
                        if (lock.getWriteHoldCount() == 1 && this.shouldSave) {
                            // Save if this is the last lock to close (which is the first acquired).
                            table.save();
                        }
                    };
                }
            }

            if ((lock.getReadHoldCount() + lock.getWriteHoldCount()) == 1) {
                // First lock.
                table.reload();
            }
        } catch (Exception e) {
            lock.release();
            throw e;
        }
    }

    public TableLock getLock() {
        return this.lock;
    }

    public void commit() {
        this.shouldSave = true;
    }

    public void close() {
        try {
            if (closeHook != null) {
                closeHook.run();
            }
        } finally {
            lock.release();
        }
    }
}
