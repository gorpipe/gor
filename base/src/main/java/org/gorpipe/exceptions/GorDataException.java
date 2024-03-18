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

package org.gorpipe.exceptions;

public class GorDataException extends GorUserException {

    private String header;
    private String row;
    private int columnNumber;

    public GorDataException(String message) {
        this(message, -1, "", "", null);
    }

    public GorDataException(String message, Throwable cause) {
        this(message, -1, "", "", cause);
    }

    public GorDataException(String message, Integer columnNumber) {
        this(message, columnNumber, "", "", null);
    }

    public GorDataException(String message, int columnNumber, String header) {
        this(message, columnNumber, header, "", null);
    }

    public GorDataException(String message, int columnNumber, String header, String row) {
        this(message, columnNumber, header, row, null);
    }

    public GorDataException(String message, String row) {
        this(message, 0, "", row, null);
    }

    public GorDataException(String message, int columnNumber, String header, String row, Throwable cause) {
        this(message, columnNumber, header, row, cause, true);
    }

    public GorDataException(String message, int columnNumber, String header, String row, Throwable cause, boolean doFormat) {
        super(doFormat ? format(message, columnNumber, header, row) : message, cause);
        this.header = header;
        this.row = row;
        this.columnNumber = columnNumber;
    }

    public String getHeader() {
        return this.header;
    }

    public String getRow() {
        return this.row;
    }

    public int getColumnNumber() {
        return this.columnNumber;
    }

    static public String format(String message, int columnNumber, String header, String row) {
        StringBuilder builder = new StringBuilder();
        builder.append(message);

        if (!ExceptionUtilities.isNullOrEmpty(header)) {
            builder.append("\n");
            builder.append("Header: ");
            builder.append(header);
        }

        if (!ExceptionUtilities.isNullOrEmpty(row)) {
            builder.append("\n");
            builder.append("Row: ");
            builder.append(row);
        }

        if (columnNumber >= 0) {
            builder.append("\n");
            builder.append("Column: ");
            builder.append(columnNumber);
        }

        return builder.toString();
    }
}
