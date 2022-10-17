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

import org.gorpipe.gor.table.Table;

import java.time.Duration;

/**
 * Dummy lock object, that does no locking.  Used to turn off locking.
 * Created by gisli on 26/08/16.
 */
public class NoTableLock extends TableLock {

    public NoTableLock(Table table, String name) {
        super(table, name);
    }

    @Override
    protected boolean doLock(Duration timeout) {
        return false;
    }

    @Override
    public void release() {
        // No op
    }

    @Override
    protected void doRelease() {
        // No op
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public int getReadHoldCount() {
        return 0;
    }

    @Override
    public int getWriteHoldCount() {
        return 1;
    }

    @Override
    public long reservedTo() {
        return 0;
    }

    @Override
    public String getDescription() {
        return "Dummy table lock";
    }
}
