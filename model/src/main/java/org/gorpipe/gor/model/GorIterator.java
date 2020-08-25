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

import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.monitor.GorMonitor;

import java.io.IOException;
import java.util.Iterator;

/**
 * Interface to a feature data loader
 */
public abstract class GorIterator implements AutoCloseable, Iterator<String> {
    /***/
    public String fileName;
    /***/
    public String filter;
    /***/
    public String chrFrom;
    /***/
    public String chrTo;
    /***/
    public int posFrom;
    /***/
    public int posTo;
    /***/
    public long maxRows = Long.MAX_VALUE;
    /***/
    public int groupSize = -1;

    /***/
    public int paintRange = Integer.MAX_VALUE;

    /***/
    public boolean isCancelled = false;

    /***/
    public String[] chromosomes = null;

    private String command;
    private String commandWithValues;
    private String securityKey;

    public final static String DEFAULT_COMMAND = "gor -p $chr:$pos_from-$pos_to $file | top $range";

    /**
     * Initializes with seek parameters
     *
     * @param fName
     * @param filtr
     * @param cFrom
     * @param pFrom
     * @param cTo
     * @param pTo
     * @param mRows
     * @param grSize
     * @param gm
     * @param secKey
     * @throws IOException
     */
    public void init(String fName, String filtr, String cFrom, int pFrom, String cTo, int pTo, long mRows, int grSize, GorMonitor gm, String secKey) throws IOException {
        commandWithValues = command == null ? DEFAULT_COMMAND : command;
        fileName = fName;
        fileName = fileName.replaceAll("\\\\", "/");
        filter = filtr;
        chrFrom = cFrom;
        chrTo = cTo;
        posFrom = pFrom;
        posTo = pTo;
        maxRows = mRows;
        this.groupSize = grSize;
        isCancelled = false;
        if (commandWithValues != null) {
            commandWithValues = populateCommand(commandWithValues);
        }
        init(commandWithValues, gm, secKey);
    }

    /**
     * @param params
     * @param gm
     * @param sk
     * @throws IOException
     */
    public void init(String params, GorMonitor gm, String sk) throws IOException {
        init(params, gm);
        this.securityKey = sk;
    }

    /**
     * @return SecurityContext
     */
    public String getGSecurityKey() {
        return securityKey;
    }

    /**
     * Initialize the loader
     *
     * @param params GOR Iterator parameter string
     * @param gm     Gor monitor
     * @throws IOException
     */
    public abstract void init(String params, GorMonitor gm) throws IOException;

    /**
     * Initialize the loader
     *
     * @param session Gor session
     */
    public void init(GorSession session) {}

    /**
     * @return The header as tab-delimited string
     */
    public abstract String getHeader();

    /**
     * @return true if the iterator supports seek
     */
    public boolean seekable() {
        return false;
    }

    /**
     * @return all columns of the header in a list
     */
    public String[] getHeaderCols() {
        String[] header = {};
        String h = getHeader();
        if (h != null) {
            header = h.split("\t");
        }
        return header;
    }

    /**
     * @param chr chromosome
     * @param pos position on chromosome
     * @throws IOException
     */
    public abstract void seek(String chr, int pos) throws IOException;

    /**
     * @return next line or null if no more lines
     */
    public abstract String next();

    /**
     * Close all loader resources
     */
    public abstract void close();

    /**
     * @param classParams The class parameters
     */
    public void setCommand(String classParams) {
        this.command = classParams;
    }

    /**
     * @return parameters with values
     */
    public String getParamsWithValues() {
        return commandWithValues;
    }

    /**
     * @param cmd cmd with parameters
     * @return cmd with values for all supported parameters
     */
    public String populateCommand(String cmd) {
        if (paintRange == Integer.MAX_VALUE && chrFrom != null && chrFrom.equals(chrTo)) {
            paintRange = posTo - posFrom;
        }
        return populateCommand(cmd, fileName, filter, chrFrom, posFrom, chrTo, posTo, paintRange);
    }

    /**
     * @param cmv command with values
     */
    protected void setCommandWithValues(String cmv) {
        commandWithValues = cmv;
    }

    /**
     * @param cmd   command with parameters for values
     * @param fName
     * @param filtr
     * @param cFrom
     * @param pFrom
     * @param cTo
     * @param pTo
     * @param range
     * @return Command with values for its parameters
     */
    public static String populateCommand(String cmd, String fName, String filtr, String cFrom, int pFrom, String cTo, int pTo, int range) {
        if (cmd != null) {
            if ((cTo == null && cFrom == null) || !cFrom.equals(cTo)) {
                cmd = cmd.replace("-p", "");
                cmd = cmd.replace("$chr:$pos_from-$pos_to", "");
                cmd = cmd.replace(":$pos_from", "");
                cmd = cmd.replace("$chr", "");
                cmd = cmd.replace("-$pos_to", "");
                cmd = cmd.replace("$pos_to", "");
                cmd = cmd.replace("gor  ", "gor ");
            } else {
                cmd = cmd.replaceAll("\\$chr([_fF]+rom)", cFrom);
                if (cTo != null) cmd = cmd.replaceAll("\\$chr([_tT]+o)", cTo);
                cmd = cmd.replaceAll("\\$chr", cFrom);
                if (pFrom >= 0) cmd = cmd.replaceAll("\\$pos([_fF]+rom)", String.valueOf(pFrom));
                if (pTo >= 0) cmd = cmd.replaceAll("\\$pos([_tT]+o)", String.valueOf(pTo));
                if (pFrom >= 0) cmd = cmd.replaceAll("\\$pos", String.valueOf(pFrom));
            }
            if (filtr != null) cmd = cmd.replaceAll("\\$filter", filtr);
            if (fName != null) cmd = cmd.replaceAll("\\$file[_nN]ame", fName);
            if (fName != null) cmd = cmd.replaceAll("\\$file", fName);
            if (range >= 0) cmd = cmd.replaceAll("\\$range", String.valueOf(range));
        }
        cmd = GorCommand.replaceClientConstants(cmd);
        return cmd;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }


    /**
     * @param cFrom
     * @param pFrom
     * @param cTo
     * @param pTo
     * @return true to inform data loader that is using
     * the iterator to reset the cache and reload data
     */
    @SuppressWarnings("unused")
    public boolean forceReload(String cFrom, int pFrom, String cTo, int pTo) {
        return false;
    }

    /**
     * @return average seek count if supported
     */
    public int getAverageSeekCount() {
        return -1;
    }

}
