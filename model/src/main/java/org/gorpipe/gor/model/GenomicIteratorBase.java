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
package org.gorpipe.gor.model;

import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.stats.StatsCollector;
import org.gorpipe.model.gor.Pipes;

import java.util.function.Predicate;

/**
 * GenomicIterator is a seekable iterator on genomic ordered data. It assumes the data source contains
 * chromosome and position information and is sorted on those in ascending order.
 * <p>
 * This is implemented as input into the gor engine.
 *
 * WARNING:  If adding methods to this class those should also be added to GenomicIteratorAdapterBase.
 *
 * @version $Id$
 */
public abstract class GenomicIteratorBase implements GenomicIterator {
    private String header = "";
    private String sourceName = "";

    private boolean sourceAlreadyInserted;
    private int bufferSize = Pipes.rowsToProcessBuffer();

    private GorContext context = null;
    private StatsCollector statsCollector = null;
    private int statsSenderId = -1;
    protected String statsSenderName = "";
    protected String statsSenderAnnotation = "";

    public void initStats(GorContext context, String sender, String annotation) {
        if (context != null) {
            statsCollector = context.getStats();
            if (statsCollector != null && !sender.equals("")) {
                statsSenderId = statsCollector.registerSender(sender, annotation);
            }
        }
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void setBufferSize(int bs) {
        bufferSize = bs;
    }

    @Override
    public void setContext(GorContext context) {
        this.context = context;
        initStats(context, statsSenderName, statsSenderAnnotation);
    }

    public GorContext getContext() {
        return context;
    }

    public void incStat(String name) {
        if (statsCollector != null) {
            statsCollector.inc(statsSenderId, name);
        }
    }

    public void decStat(String name) {
        if (statsCollector != null) {
            statsCollector.dec(statsSenderId, name);
        }
    }

    void addStat(String name, float delta) {
        if (statsCollector != null) {
            statsCollector.add(statsSenderId, name, delta);
        }
    }

    @Override
    public GenomicIterator clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    @Override
    public boolean isSourceAlreadyInserted() {
        return sourceAlreadyInserted;
    }

    @Override
    public void setSourceAlreadyInserted(boolean sourceAlreadyInserted) {
        this.sourceAlreadyInserted = sourceAlreadyInserted;
    }

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public void setHeader(String header) {
        this.header = header;
    }

    @Override
    public void init(GorSession session) {
    }
}
