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

package org.gorpipe.gor.cli.manager;

import org.gorpipe.gor.manager.TableManager;
import org.gorpipe.gor.table.dictionary.BaseDictionaryTable;
import org.gorpipe.gor.table.lock.TableLock;
import picocli.CommandLine;

import java.time.Duration;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "islock",
        description="Test the atate of lock with lock name. Returns 'Locked' og 'Unlocked' based on lock state.",
        header="Test state of names lock.")
public class TestIsLockCommand extends ManagerOptions implements Runnable{

    @CommandLine.Parameters(index = "1",
            arity = "1",
            paramLabel = "LOCKNAME",
            description = "Lock name to test.")
    String lockName;

    @Override
    public void run() {
        Duration lockTimeoutDuration = Duration.ofSeconds(lockTimeout);
        TableManager tm = TableManager.newBuilder().useHistory(!nohistory).lockTimeout(lockTimeoutDuration).build();
        BaseDictionaryTable table = tm.initTable(dictionaryFile.toString());

        try (TableLock lock = TableLock.acquireWrite(tm.getLockType(), table, lockName, Duration.ZERO)) {
            System.out.println(lock.isValid() ? "Unlocked" : "Locked " + lock.reservedTo());
        }
    }
}
