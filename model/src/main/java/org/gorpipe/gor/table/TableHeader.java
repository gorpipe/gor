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

package org.gorpipe.gor.table;

import org.apache.commons.lang.StringUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Helper class the represents table header.
 * <p>
 * Created by gisli on 25/07/16.
 *
 * Note on naming of the table properties:
 *  Default naming of the table properties is the same as env variables (UPPER_SNAKE_CASE).  
 */
public class TableHeader {

    private static final Logger log = LoggerFactory.getLogger(TableHeader.class);

    public static final String NO_SERIAL = "0";

    // Basic properties
    public static final String HEADER_FILE_FORMAT_KEY = "FILE_FORMAT";
    public static final String HEADER_CREATED_KEY = "CREATED";
    public static final String HEADER_SOURCE_COLUMN_KEY = "SOURCE_COLUMN";
    public static final String HEADER_COLUMNS_KEY = "COLUMNS";
    public static final String HEADER_SERIAL_KEY = "SERIAL";
    public static final String HEADER_LINE_COUNT_KEY = "LINE_COUNT";
    public static final String HEADER_USE_HISTORY_KEY = "USE_HISTORY";
    public static final String HEADER_UNIQUE_TAGS_KEY = "UNIQUE_TAGS";
    public static final String HEADER_VALIDATE_FILES_KEY = "VALIDATE_FILES";
    public static final String HEADER_BUCKETIZE_KEY = "BUCKETIZE";

    HashMap<String, String> headerProps;
    private String[] contentColumns;                   // Columns of the content (output).
    private String[] tableColumns;                     // Columns of the table it self.

    /**
     *
     */
    public TableHeader() {
        this.headerProps = new HashMap<>();
        clear();
    }

    /**
     * Check if proper header.
     *
     * @return true if the header is a proper header otherwise false.  Header is proper if it has defined contentColumns and they are not dummy contentColumns.
     */
    public boolean isProper() {
        return this.contentColumns != null && this.contentColumns.length > 2 && !this.contentColumns[2].equalsIgnoreCase("col3");
    }

    /**
     * Check if proper table header.
     *
     * @return true if the table header (heador for the dict filee) is a proper header otherwise false.
     * Header is proper if it has defined at least 2 columns and they are not dummy.
     */
    public boolean isProperTableHeader() {
        return this.tableColumns != null && this.tableColumns.length > 1 && !this.tableColumns[1].equalsIgnoreCase("col2");
    }

    /**
     * Get header property.
     *
     * @param key name of the property.
     * @return the header property identifed with <key>
     */
    public String getProperty(String key) {
        if (HEADER_SOURCE_COLUMN_KEY.equals(key) && !headerProps.containsKey(HEADER_SOURCE_COLUMN_KEY) && isProperTableHeader()) {
            // Special treatment for source column.  It it si missing from standard probs and the header is good
            // we retreive it from the standard column heading.
            return tableColumns[1];
        } else {
            return headerProps.get(key);
        }
    }

    /**
     * Set header property.
     *
     * @param key   name of the property.
     * @param value new value of the property.
     */
    public void setProperty(String key, String value) {
        if (!headerProps.containsKey(key) || !headerProps.get(key).equals(value)) {
            headerProps.put(key, value);
        }
    }

    /**
     * Returns <tt>true</tt> if this header contains the property.
     *
     * @param key The property key.
     * @return <tt>true</tt> if this header contains the property.
     */
    public boolean containsProperty(String key) {
        return headerProps.containsKey(key);
    }

    /**
     * Get the content columns.
     *
     * @return get the content columns.
     */
    public String[] getColumns() {
        return contentColumns;
    }

    /**
     * Set the content columns.
     *
     * @param columns the content columns.
     */
    public void setColumns(String[] columns) {
        this.contentColumns = Stream.of(columns).filter(Objects::nonNull).map(String::trim).filter(h -> h.length() > 0).toArray(size -> new String[size]);
        setProperty(HEADER_COLUMNS_KEY, String.join(",", this.contentColumns));
    }

    /**
     * Set table columns.
     *
     * @param tableColumns
     */
    public void setTableColumns(String[] tableColumns) {
        this.tableColumns = tableColumns;
    }

    public int getLineCount() {
        if (headerProps.containsKey(HEADER_LINE_COUNT_KEY)) {
            return Integer.parseInt(headerProps.get(HEADER_LINE_COUNT_KEY));
        }
        return -1;
    }


    /**
     * Parse header line.
     *
     * @param line line to parse.
     * @return true if the line was a header line.
     */
    public boolean parseLine(String line) {
        if (line == null) {
            return false;
        } else if (line.startsWith("##") && line.contains("=")) {
            String[] lineSplit = line.split("=", 2);
            String propName = StringUtils.strip(lineSplit[0], "\t\n #");
            if (propName.equals(HEADER_COLUMNS_KEY)) {
                setColumns(lineSplit[1].trim().split("[\t,]", -1));
            } else {
                setProperty(propName, lineSplit[1].trim());
            }
            return true;
        } else if (line.startsWith("#")) {
            String columnsString = StringUtils.strip(line, "\n #");
            if (columnsString.length() > 0) {
                this.tableColumns = columnsString.split("[\t,]", -1);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if the given line is a header line.
     *
     * @param line line to check.
     * @return true if the given line is a table header line.
     */
    public static boolean isHeaderLine(String line) {
        return line != null && line.startsWith("#");
    }

    /**
     * Clear the header info.
     */
    public void clear() {
        this.headerProps.clear();
        setProperty(HEADER_SERIAL_KEY, "0");
        this.contentColumns = new String[0];
        this.tableColumns = new String[0];
    }

    /**
     * Format the header for outputting into a file.
     *
     * @return the header formatted for outputting into a table file.
     */
    public String formatHeader() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : this.headerProps.entrySet()) {
            sb.append(String.format("## %s = %s%n", entry.getKey(), entry.getValue()));
        }
        sb.append(String.format("# %s%n", String.join("\t", this.tableColumns)));

        return sb.toString();
    }

    public void load(BaseTable table) {
        log.debug("Parsing header for {}", table.getName());

        try {
            clear();

            // Read both from header file and main file, with main file overriding.

            Path headerPath = table.getFolderPath().resolve("header");
            if (Files.exists(headerPath)) {
                try (Stream<String> stream = Files.lines(headerPath)) {
                    stream.forEach(line -> {
                        line = line.trim();
                        if (isHeaderLine(line)) {
                            parseLine(line);
                        }
                    });
                }
            }

            if (Files.exists(table.getPath())) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(table.getPath().toString())))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.length() > 0) {
                            if (isHeaderLine(line)) {
                                parseLine(line);
                            } else {
                                // Done reading the header.
                                break;
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new GorResourceException("Error Initializing Query. Can not read file " + table.getPath(), table.getPath().toString(), ex);
        }
    }
}
