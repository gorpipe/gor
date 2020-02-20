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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.BaseTable;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to acquire named locks.  The locks are reentrant read write locks and can be used for both inter process and inter thread locking.
 * <p>
 * For inter process locking we use FileChannel.lock.
 * <p>
 * Process must have access to the same file system.
 * <p>
 * Created by gisli on 08/08/16.
 */
public class FileTableLock extends ProcessThreadTableLock {

    private static final Logger log = LoggerFactory.getLogger(FileTableLock.class);


    /**
     * Create lock
     *
     * @param table table to create lock on.
     * @param name  name of lock.
     */
    public FileTableLock(BaseTable table, String name) {
        super(table, name);
        this.lockPath = table.getFolderPath().resolve(String.format("%s.%s.lock", table.getName(), name));
    }

    @SuppressWarnings("squid:S2095") //file lock not acquired when using try-with-resources
    @Override
    protected ProcessLock acquireProcessLock(Duration timeout) throws IOException {
        long requestTime = System.currentTimeMillis();
        log.trace("{}", new TableLockLogMessage("About to create/access process lock file"));
        FileChannel fc = FileChannel.open(getLockPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SYNC);
        do {
            FileLock fileLock = fc.tryLock(0L, Long.MAX_VALUE, isShared());
            if (fileLock != null) {
                writeToLockFile(fileLock, "acquired");
                return new ProcessLock() {
                    @Override
                    public boolean isValid() {
                        return fileLock.isValid();
                    }

                    @Override
                    public void renew() {
                        // No special action needed to renew, so just check if valid.
                        if (!fileLock.isValid()) {
                            throw new AcquireLockException("Could not renew lock " + getId() + ".  Lock lost for unknown reason!");
                        }
                    }

                    @Override
                    public long reservedTo() {
                        return Long.MAX_VALUE;
                    }

                    @Override
                    public void release() {
                        super.release();
                        try {
                            writeToLockFile(fileLock, "Releasing");
                            fileLock.release();
                            fileLock.acquiredBy().close();
                        } catch (IOException e) {
                            throw new GorSystemException(e);
                        }
                    }
                };
            }
            log.trace("{}", new TableLockLogMessage(String.format("Waiting for lock (been waiting for %d millis)", System.currentTimeMillis() - requestTime)));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        } while (System.currentTimeMillis() < requestTime + Math.max(timeout.toMillis(), 0));

        return null;
    }

    protected void writeToLockFile(FileLock fl, String message) {
        if (fl != null && fl.channel().isOpen()) {
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
            String desc = String.format("%s - %s:%s(%s) - %s %s %s (process hold counts %d %d)%n", formatter.format(new Date()),
                    ManagementFactory.getRuntimeMXBean().getName(), Thread.currentThread().getName(), Thread.currentThread().getId(),
                    message, getName(), isShared() ? "ReadLock" : "WriteLock", this.lockData.rrwl.getReadLockCount(),
                    this.lockData.rrwl.getWriteHoldCount());
            try {
                FileUtils.writeStringToFile(getLockPath().toFile(), desc, true);
            } catch (Exception e) {
                log.warn("Could not write data to lock file " + getLockPath(), e);
            }
        }
    }


}
