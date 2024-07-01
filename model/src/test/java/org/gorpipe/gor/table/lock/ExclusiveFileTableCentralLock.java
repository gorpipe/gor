package org.gorpipe.gor.table.lock;

import org.gorpipe.gor.table.Table;

import java.nio.file.Path;

/**
 * Version of hte ExclusiveFileTableLock that stored the locks in central location.
 * Used for testing.
 */
public class ExclusiveFileTableCentralLock extends ExclusiveFileTableLock {
    /**
     * Create lock
     *
     * @param table table to create lock on.
     * @param name  name of lock.
     */
    public ExclusiveFileTableCentralLock(Table table, String name) {
        this(table, name, Path.of(System.getProperty("java.io.tmpdir")));
    }

    public ExclusiveFileTableCentralLock(Table table, String name, Path lockRootPath) {
        super(table, name);
        this.lockPath = Path.of(lockRootPath.toString(), String.format("%s.%s.excl.lock", table.getName(), name));
    }
}
