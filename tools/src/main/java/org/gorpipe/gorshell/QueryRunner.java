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

package org.gorpipe.gorshell;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.GorSession;
import org.gorpipe.gor.RequestStats;
import org.gorpipe.model.genome.files.gor.GorMonitor;
import org.gorpipe.model.genome.files.gor.Row;
import gorsat.process.PipeInstance;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class QueryRunner extends Thread {
    private final String query;
    private final LineReader lineReader;
    private final Thread ownerThread;
    private final GorMonitor gorMonitor;
    private boolean timingEnabled = false;
    private boolean fileCacheEnabled = true;
    private boolean requestStatsEnabled = false;

    private long startTime;
    private GorSession gorSession;
    private long initTime;
    private long numRows;
    private long beforeLoopTime;
    private long afterLoopTime;
    private boolean isDone;
    private boolean displayResults = true;

    QueryRunner(String query, LineReader lineReader, Thread owner) {
        this.query = query;
        this.lineReader = lineReader;
        this.ownerThread = owner;
        this.gorMonitor = new GorMonitor();
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis();

        String cwd = System.getProperty("user.dir");
        GorShellSessionFactory sessionFactory = new GorShellSessionFactory(cwd);
        sessionFactory.setFileCacheEnabled(fileCacheEnabled);
        sessionFactory.setRequestStatsEnabled(requestStatsEnabled);

        gorSession = sessionFactory.create();
        gorSession.getSystemContext().setMonitor(gorMonitor);
        gorSession.getEventLogger().query(query);

        try (PipeInstance pipe = PipeInstance.createGorIterator(gorSession.getGorContext())) {
            runPipe(pipe);
        } catch (Exception e) {
            lineReader.printAbove(e.toString());
        }

        isDone = true;
        if (!isCancelled()) {
            ownerThread.interrupt();
        }
    }

    private void runPipe(PipeInstance pipe) throws IOException {
        gorSession.getGorContext().start("");
        pipe.init(query, gorMonitor);
        initTime = System.currentTimeMillis();

        printHeader(pipe);
        printRows(pipe);
        printFooter();

        gorSession.getGorContext().end();
        gorSession.close();

        if (requestStatsEnabled) {
            RequestStats rs = (RequestStats) gorSession.getEventLogger();
            rs.saveToJson();
        }
    }

    private void printHeader(PipeInstance pipe) {
        String header = pipe.theIterator().getHeader();
        if (gorSession.getNorContext() || pipe.isNorContext()) {
            header = skipFirstTwoColumns(header);
        }
        lineReader.printAbove(new AttributedString(header, AttributedStyle.BOLD));
    }

    private void printRows(PipeInstance pipe) {
        boolean isNor = gorSession.getNorContext() || pipe.isNorContext();
        numRows = 0;
        beforeLoopTime = System.currentTimeMillis();
        while (pipe.theIterator().hasNext() && !gorMonitor.isCancelled()) {
            Row row = pipe.theIterator().next();
            if (displayResults) {
                String rowAsString = isNor ? row.otherCols() : row.toString();
                lineReader.printAbove(rowAsString);
            }
            numRows++;
        }
        afterLoopTime = System.currentTimeMillis();
    }

    private void printFooter() {
        String cancelledSuffix = "";
        if (gorMonitor.isCancelled()) {
            cancelledSuffix = " (cancelled)";
        }
        String footer = String.format("%d row(s)%s", numRows, cancelledSuffix);
        lineReader.printAbove(new AttributedString(footer, AttributedStyle.BOLD));
        if (timingEnabled) {
            double initTimeSec = (initTime - startTime) / 1000.0;
            double rowsTimeSec = (afterLoopTime - beforeLoopTime) / 1000.0;
            lineReader.printAbove(String.format("Init: %f, Data: %f", initTimeSec, rowsTimeSec));
        }
    }

    private String skipFirstTwoColumns(String header) {
        int firstTab = header.indexOf('\t');
        int secondTab = header.indexOf('\t', firstTab+1);
        header = header.substring(secondTab+1);
        return header;
    }

    void cancel() {
        gorMonitor.setCancelled(true);
    }

    public boolean isDone() {
        return isDone;
    }

    boolean isCancelled() {
        return gorMonitor.isCancelled();
    }

    void setTimingEnabled(boolean timingEnabled) {
        this.timingEnabled = timingEnabled;
    }

    void setFileCacheEnabled(boolean fileCacheEnabled) {
        this.fileCacheEnabled = fileCacheEnabled;
    }

    void setRequestStatsEnabled(boolean requestStatsEnabled) {
        this.requestStatsEnabled = requestStatsEnabled;
    }

    public void setDisplayResults(boolean displayResults) {
        this.displayResults = displayResults;
    }
}
