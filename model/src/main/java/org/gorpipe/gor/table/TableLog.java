/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.gor.table;

import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to keep track of action on a table and save those to file.
 */
public class TableLog {
    private static final Logger log = LoggerFactory.getLogger(TableLog.class);

    public static final String LOG_FILE = "action.log";
    enum LogAction {
        INSERT,
        DELETE,
        ADDTOBUCKET,
        REMOVEFROMBUCKET
    }

    private final Path logDir;  // Location of log files.
    private final Path logFilePath;
    private final DateTimeFormatter formatter;
    protected List<String> unCommittedActions = Collections.synchronizedList(new ArrayList<>());

    /**
     * Constructor
     * @param logDir    folder to store the log file.
     */
    public TableLog(Path logDir) {
        this.logDir = logDir;
        this.logFilePath =  this.logDir.resolve(LOG_FILE);
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Create new log record after entry is updated.
     * @param action    action performed
     * @param argument  action argument (can be null if all info is in the entry object)
     * @param entry     entry after update.
     */
    public void logAfter(LogAction action, String argument, TableEntry entry) {
        String logLine = String.format("%s\t%s\t%s", LocalDateTime.now().format(formatter), action.toString(), argument);
        logLine += String.format("\t%s", entry.formatEntryNoNewLine());
        logLine += "\n";
        unCommittedActions.add(logLine);
    }

    /**
     * Save the uncommitted log lines to file, and clear the list of uncommitted log lines.
     */
    public void commit() {
        log.debug("Committng {} actions to log file {}", unCommittedActions.size(), logFilePath);
        if (!Files.exists(this.logDir) || !Files.isDirectory(this.logDir)) {
            throw new GorSystemException(String.format("Log '%s'folder does not exits", this.logDir.toString()), null);
        }
        try(Writer destination = Files.newBufferedWriter(this.logFilePath,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC)) {
            for (String logLine : unCommittedActions) {
                destination.write(logLine);
            }
            unCommittedActions.clear();
        } catch (IOException e) {
            throw new GorSystemException(String.format("Could not save table log %s", logFilePath), e);
        }
    }
}
