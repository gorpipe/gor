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

package org.gorpipe.model.genome.files.gor;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.gorpipe.model.IteratorTestUtilities.countRemainingLines;
import static org.junit.Assert.*;

public class UTestMonitorIterator {
    public class TestMonitor extends GorMonitor {
        int numNotifications = 0;
        boolean isCancelled = false;

        @Override
        public boolean notify(String chr, int pos, String prevChr) {
            numNotifications++;
            return isCancelled;
        }
    }

    @Test
    public void getHeader() throws IOException {
        GorOptions gorOptions = GorOptions.createGorOptions("1.mem");
        GenomicIterator iterator = gorOptions.getIterator();
        String header = iterator.getHeader();

        TestMonitor testMonitor = new TestMonitor();
        MonitorIterator monitorIterator = new MonitorIterator(iterator, testMonitor, 0);

        assertEquals(header, monitorIterator.getHeader());
    }

    @Test
    public void nextTriggersMonitor() throws IOException {
        GorOptions gorOptions = GorOptions.createGorOptions("1.mem");
        GenomicIterator iterator = gorOptions.getIterator();

        TestMonitor testMonitor = new TestMonitor();
        MonitorIterator monitorIterator = new MonitorIterator(iterator, testMonitor, 0);

        while (monitorIterator.hasNext()) {
            monitorIterator.next();
        }

        assertTrue(testMonitor.numNotifications > 0);
    }

    @Test
    public void iterationIsInterruptedWhenMonitorIsCancelled() throws IOException {
        GorOptions gorOptions = GorOptions.createGorOptions("1.mem");
        GenomicIterator iterator = gorOptions.getIterator();

        TestMonitor testMonitor = new TestMonitor();
        MonitorIterator monitorIterator = new MonitorIterator(iterator, testMonitor, 0);

        testMonitor.isCancelled = true;

        int counter = 0;
        while (monitorIterator.hasNext()) {
            monitorIterator.next();
            counter++;
        }

        assertTrue(iterator.hasNext());
        assertTrue(counter < 4000);
    }

    @Test
    public void testCancelling() throws Exception {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final ReadListener listener = (chr, pos, prevChr) -> cancelled.get();
        final GenomicIterator git = GorOptions.createGorOptions("1.mem").getIterator();

        try (final MonitorIterator source = new MonitorIterator(git, listener, 10)) {
            // Note in the following we assume source will read 500 rows between checks of current time to estimate ms between notifications
            assertEquals(490, countRemainingLines(source, 490));
            Assert.assertTrue(source.hasNext());
            cancelled.set(true);
            Thread.sleep(100);
            assertEquals(9, countRemainingLines(source, 9));
            Assert.assertTrue("There should be 500 records prior to cancel taking effect", source.hasNext());
            source.next();
            Assert.assertFalse("There should be not 501th record due to cancel", source.hasNext());
        }
    }
}