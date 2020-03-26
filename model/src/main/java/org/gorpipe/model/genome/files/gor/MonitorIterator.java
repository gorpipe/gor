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

public class MonitorIterator extends GenomicIterator {
    private GenomicIterator iterator;
    private final ReadListener gorMonitor;
    private final long notifyIntervalMs;
    private long lastNotificationTimeMs;
    private String lastNotifiedChr = null;
    private boolean isCancelled = false;

    private int rowCount = 0;

    public MonitorIterator(GenomicIterator it, ReadListener gm, long intervalMs) {
        iterator = it;
        gorMonitor = gm;
        notifyIntervalMs = intervalMs;
        lastNotificationTimeMs = System.currentTimeMillis();
    }

    @Override
    public String getHeader() {
        return iterator.getHeader();
    }

    @Override
    public boolean seek(String chr, int pos) {
        return iterator.seek(chr, pos);
    }

    @Override
    public boolean hasNext() {
        if(isCancelled) {
            return false;
        } else {
            return iterator.hasNext();
        }
    }

    @Override
    public Row next() {
        Row row = iterator.next();
        if(row != null ) {
            handleNotify(row);
        }
        return row;
    }

    @Override
    public boolean next(Line line) {
        boolean result = iterator.next(line);
        if(result) {
            handleNotify(line);
        }
        return result;
    }

    @Override
    public void close() {
        iterator.close();
    }

    private void handleNotify(Row r) {
        rowCount++;
        if(rowCount % 500 == 0) {
            final long now = System.currentTimeMillis();
            if(now - lastNotificationTimeMs >= notifyIntervalMs) {
                String prevChr = lastNotifiedChr != null ? lastNotifiedChr : r.chr;
                lastNotifiedChr = r.chr;
                lastNotificationTimeMs = now;
                isCancelled = gorMonitor.notify(r.chr, r.pos, prevChr);
            }
        }
    }
}
