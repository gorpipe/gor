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

import org.gorpipe.model.util.Util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for defining a drill in report command.
 *
 * @version $Id$
 */
public final class ReportCommand {
    private static final String COL_PLACEHOLDER = "\\$\\p{Digit}+";
    /**
     * The command name.
     */
    public final String name;
    public final String category;
    /**
     * The command.
     */
    public final String command;
    /**
     * The command MD% digest.
     */
    public final String commandMd5Digest;
    private final List<RequiredColumn> requiredReportColumns;
    private Map<String, String> column2Placeholder;

    /**
     * Constructor.
     *
     * @param name            the command name
     * @param command         the command
     * @param requiredColumns comma separated string of column names. Columns may be defined using regular expressions.
     * @throws Exception
     */
    public ReportCommand(final String name, final String command, final String requiredColumns) throws Exception {
        this(name, "", command, parseRequiredColumns(requiredColumns));
    }

    /**
     * Constructor.
     *
     * @param name            the command name
     * @param category        the command category
     * @param command         the command
     * @param requiredColumns comma separated string of column names. Columns may be defined using regular expressions.
     * @throws Exception
     */
    public ReportCommand(final String name, final String category, final String command, final String requiredColumns) throws Exception {
        this(name, category, command, parseRequiredColumns(requiredColumns));
    }

    /**
     * Constructor.
     *
     * @param name            the command name
     * @param category        the command category
     * @param command         the command
     * @param requiredColumns required columns for command
     * @throws Exception
     */
    public ReportCommand(final String name, final String category, final String command, final List<RequiredColumn> requiredColumns) throws Exception {
        this.name = name;
        this.category = category;
        this.command = command;
        this.requiredReportColumns = requiredColumns;
        this.commandMd5Digest = Util.md5(command);
        mapColumn2Placement();
    }

    /**
     * Get the required columns for the command.
     *
     * @return required columns
     */
    public List<RequiredColumn> getRequiredReportColumns() {
        return Collections.unmodifiableList(requiredReportColumns);
    }

    /**
     * Get the command with substituted values.
     *
     * @param column2Value map from column to value
     * @return the command with substituted values
     */
    public String getCommandWithValues(final Map<String, String> column2Value) {
        String commandWithValues = command;
        if (column2Placeholder.size() > 0) {
            for (String col : column2Placeholder.keySet()) {
                commandWithValues = commandWithValues.replaceAll(Pattern.quote(column2Placeholder.get(col)), column2Value.get(col));
            }
        }
        return commandWithValues;
    }

    /**
     * Get the command name and digest combined in a string, i.e. VEPDrillIn_1g68881376djjj387s.
     *
     * @return the command name and digest combined in a string
     */
    public String getCommandNameAndDigest() {
        return name + "_" + commandMd5Digest;
    }

    private static List<RequiredColumn> parseRequiredColumns(final String requiredColumns) {
        List<RequiredColumn> tmpRequiredReportColumns = new ArrayList<>();
        String[] cols = new String[0];
        if (requiredColumns != null) {
            cols = requiredColumns.split(",");
        }
        for (String col : cols) {
            tmpRequiredReportColumns.add(new RequiredColumn(col));
        }
        return tmpRequiredReportColumns;
    }

    private void mapColumn2Placement() throws Exception {
        column2Placeholder = new HashMap<>();

        final Set<String> placeholdersSet = new HashSet<>();
        if (command != null) {
            final Pattern pattern = Pattern.compile(COL_PLACEHOLDER);
            final Matcher m = pattern.matcher(command);

            while (m.find()) {
                final String placeholder = command.substring(m.start(), m.end());
                placeholdersSet.add(placeholder);
            }
        } else {
            throw new Exception("Command missing for: " + name);
        }

        if (placeholdersSet.size() > requiredReportColumns.size()) {
            throw new Exception("Columns missing for placeholders in command '" + command + "'.\nNumber of placeholders is "
                    + placeholdersSet.size() + ".\nNumber of columns is " + requiredReportColumns.size() + ".");
        }

        int placeholderNum = 1;
        for (RequiredColumn col : requiredReportColumns) {
            final String placeholder = "$" + placeholderNum;
            if (placeholdersSet.contains(placeholder)) {
                column2Placeholder.put(col.notation, placeholder);
            }
            placeholderNum++;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (requiredReportColumns.size() > 0) {
            sb.append(requiredReportColumns.get(0).notation);
            for (int i = 1; i < requiredReportColumns.size(); i++) {
                sb.append(",");
                sb.append(requiredReportColumns.get(i).notation);
            }
        }
        return name + "\t" + sb.toString() + "\t" + command;
    }

    /**
     * @param args the arguments
     */
    public static void main(final String[] args) {
        try {
            final String name = "Test";
            final String columns = "GENE_symbol";
            final String command = "http://www.ncbi.nlm.nih.gov/sites/varvu?gene=$1";
            final ReportCommand reportCommand = new ReportCommand(name, command, columns);
            final List<RequiredColumn> requiredColumns = reportCommand.getRequiredReportColumns();
            for (RequiredColumn requiredColumn : requiredColumns) {
                System.out.println(requiredColumn.notation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
