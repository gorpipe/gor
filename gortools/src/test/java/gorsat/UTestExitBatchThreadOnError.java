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

package gorsat;

import org.gorpipe.model.gor.iterators.RowSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 18/05/2017.
 */
public class UTestExitBatchThreadOnError {
    @Test
    public void testExitBatchThreadOnError() throws InterruptedException {
        String query = "gor ../tests/data/gor/genes.gor | rownum | throwif rownum=21432 | group genome -count";
        RowSource iterator = TestUtils.runGorPipeIterator(query);
        Thread t = new Thread(() -> {
            while (iterator.hasNext()) {
                iterator.next();
            }
            iterator.close();
        });
        t.start();
        t.join(10000);
        Assert.assertFalse("Thread still alive after 10 seconds", t.isAlive());
    }

    @Test
    public void testExitBatchThreadOnBug() throws InterruptedException {
        String query = "gor ../tests/data/gor/genes.gorz | bug process:0.01 | rownum | join -segseg ../tests/data/gor/genes.gorz | bug process:0.01 | group chrom -count";
        RowSource iterator = TestUtils.runGorPipeIterator(query);
        Thread t = new Thread(() -> {
            while (iterator.hasNext()) {
                iterator.next();
            }
            iterator.close();
        });
        t.start();
        t.join(10000);
        Assert.assertFalse("Thread still alive after 10 seconds", t.isAlive());
    }
}
