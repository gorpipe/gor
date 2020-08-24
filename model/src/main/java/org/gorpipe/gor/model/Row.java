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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.model.gor.RowObj;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;

/**
 * Row represents a single row from a genomic iterator. It is an abstract class, providing the interface
 * for the analysis steps to process the rows. Ideally the methods provided here should be used to
 * modify the rows and produce new ones, rather than building them from scratch with a tab-separated
 * string.
 */
public abstract class Row implements Comparable<Row>,ColumnValueProvider, Serializable {
    /**
     * String representation of the chromosome
     */
    public String chr;

    /**
     * Position within the chromosome
     */
    public int pos;

    /**
     * Indicates that this is just a progress row.
     */
    public boolean isProgress = false;

    /**
     * User data for analysis steps that need to provide extra data attached to the row, usually for
     * more efficient processing.
     */
    public RowObj.BinaryHolder bH;

    public Row() {
        super();
    }
    
    public Row(String chr, int pos) {
        this();
        this.chr = chr;
        this.pos = pos;
    }

    @Override
    public int compareTo(Row that) {
        int chrcmp = this.chr.compareTo(that.chr);
        if (chrcmp == 0) {
            int poscmp = this.pos - that.pos;
            if (poscmp == 0) {
                return this.otherCols().compareTo(that.otherCols());
            } else return poscmp;
        } else return chrcmp;
    }

    public enum SortOrder {
        FORWARD,
        REVERSE
    }

    public enum SortType {
        NUMBERIC,
        STRING
    }

    public static class SortInfo {
        public SortInfo(int i, SortOrder c, SortType t) {
            this.sortColumn = i;
            this.sortOrder = c;
            this.sortType = t;
        }

        int sortColumn;
        SortOrder sortOrder;
        SortType sortType;
    }

    public int advancedCompare(Row that, SortInfo[] sortArray) {
        int chrcmp = this.chr.compareTo(that.chr);
        if (chrcmp == 0) {
            int poscmp = this.pos - that.pos;
            if (poscmp == 0) {
                if (sortArray != null && sortArray.length > 0) {
                    int i = 0;
                    while (i < sortArray.length) {
                        int sortCol = sortArray[i].sortColumn;
                        SortOrder sortOrder = sortArray[i].sortOrder;
                        SortType sortType = sortArray[i].sortType;
                        if (sortType == SortType.NUMBERIC) {
                            int cmp = Double.compare(colAsDouble(sortCol), that.colAsDouble(sortCol));
                            if (cmp != 0) {
                                return sortOrder == SortOrder.REVERSE ? -cmp : cmp;
                            }
                        } else {
                            int cmp = colAsString(sortCol).toString().compareTo(that.colAsString(sortCol).toString());
                            if (cmp != 0) {
                                return sortOrder == SortOrder.REVERSE ? -cmp : cmp;
                            }
                        }
                        i += 1;
                    }
                    return 0;
                } else return this.otherCols().compareTo(that.otherCols());
            } else return poscmp;
        } else return chrcmp;
    }

    @Override
    public boolean equals(Object that) {
        return (that instanceof Row && this.compareTo((Row) that) == 0);
    }

    public boolean atSamePos(Row other) {
        // Position comparison only
        return pos == other.pos && chr.equals(other.chr);
    }

    public boolean atPriorPos(Row other) {
        // Position comparison only
        return (pos < other.pos && chr.equals(other.chr)) || chr.compareTo(other.chr) < 0;
    }

    static int countColumns(CharSequence input) {
        int n = 1;
        int length = input.length();
        for(int i = 0; i < length; i++) {
            if(input.charAt(i) == '\t') {
                n += 1;
            }
        }
        return n;
    }

    public int[] getSplitArray() {
        return null;
    }

    /**
     * String representation of the Row, used for logging and debug output
     * @return String representation of the Row
     */
    public abstract String toColString();

    /**
     * Returns the value of the given column as an integer
     * @param colNum Column index (zero based)
     * @return Column value as integer
     */
    public abstract int colAsInt(int colNum);

    /**
     * Returns the value of the given column as a double
     * @param colNum Column index (zero based)
     * @return Column value as double
     */
    public abstract double colAsDouble(int colNum);

    /**
     * Returns the value of the given column as a long
     * @param colNum Column index (zero based)
     * @return Column value as long
     */
    public abstract Long colAsLong(int colNum);

    /**
     * Returns the value of the given column as a string
     * @param colNum Column index (zero based)
     * @return Column value as string
     */
    public abstract CharSequence colAsString(int colNum);

    /**
     * Returns the value of the column as a byte array.
     */
    public byte[] colAsBytes(int colNum) {
        return colAsString(colNum).toString().getBytes();
    }

    /**
     * Returns the value of all columns except for chromosome and position
     * @return Tab-separated string with the value of all columns except the first two
     */
    public abstract String otherCols();

    /**
     * Returns a tab-separated string with the values from the start column up to and included the stop column
     * @param startCol First column to include (zero based)
     * @param stopCol Last column to include (zero based)
     * @return Tab-separated string
     */
    public abstract CharSequence colsSlice(int startCol, int stopCol);

    /**
     * Returns all the columns (including chromosome and position) as a tab-separated string
     * @return Tab-separated string
     */
    public abstract CharSequence getAllCols();

    /**
     * Returns the number of columns in the row, including chromosome and position
     * @return The number of columns
     */
    public abstract int numCols();

    /**
     * Returns the length of the string representation of the row - equal to getAllCols().length()
     * @return The length of the row
     */
    public abstract int length();

    /**
     * Returns a tab-separated string with the values from the given columns, using zero-based indices
     * @param columnIndices An array of columns to include
     * @return Tab-separated string
     */
    public abstract String selectedColumns(int[] columnIndices);

    /**
     * @deprecated
     */
    @Deprecated
    public abstract int otherColsLength();

    /**
     * Adds a single column to the row, with the given string value. The string must not contain a tab character.
     * @param rowString The value to add to the row
     */
    public abstract void addSingleColumnToRow(String rowString);

    /**
     * Creates a new Row, containing a slice of the current row.
     * @param startCol First column to include
     * @param stopCol Last column to include
     * @return A new Row with a slice of the current row
     */
    public abstract Row slicedRow(int startCol, int stopCol);

    /**
     * Creates a new Row, containing the given columns of the current row.
     * @param columnIndices The column indices (zero based) to include in the row.
     * @return A new Row with the given columns from the current row.
     */
    public abstract Row rowWithSelectedColumns(int[] columnIndices);

    /**
     * @deprecated
     */
    @Deprecated
    public abstract int sa(int i);

    /**
     * This should not be a public method
     * @deprecated
     */
    @Deprecated
    public abstract void resize(int newsize);

    /**
     * @// TODO: 2019-02-27 The indexing here is inconsistent - 0 means first column after pos
     * Set the value of the given column to the given value.
     * @param i The column index to set
     * @param val The value to set the column to
     */
    public abstract void setColumn(int i, String val);

    /**
     * Sets the values of multiple columns - effectively the same as calling setColumn multiple
     * times but is more efficent.
     * Column indices must be in order, but need not be contiguous.
     * @param colsToReplace The columns to replace
     * @param values The values - the number of values must match the number of columns to replace
     */
    public void setColumns(int[] colsToReplace, String[] values) {
        for(int i = 0; i < colsToReplace.length; i++) {
            setColumn(colsToReplace[i] - 2, values[i]);
        }
    }

    /**
     * Add the given number of columns to the row. The columns get empty values - use setColumn
     * to give them values.
     * @param num The number of columns to add
     */
    public abstract void addColumns(int num);

    /**
     * Removes the given column.
     * @param n Index (zero based) of the column to remove
     */
    public abstract void removeColumn(int n);

    /**
     * Returns the first character from the given column
     * @param n Column index (zero based)
     * @return The first character from column n
     */
    public abstract char peekAtColumn(int n);

    /**
     * Creates a new row with all the columns from the original row, with the addition of the
     * given value(s). Note that <i>s</i> may contain tab characters - in that case, multiple
     * columns are added.
     * @param s String representation of value(s) to be added to the new row
     * @return A row with all the columns from the current row, plus <i>s</i>
     */
    public Row rowWithAddedColumn(CharSequence s) {
        return RowObj.apply(getAllCols() + "\t" + s);
    }

    /**
     * Creates a new row with all the columns from the original row, with the addition of the
     * given value(s). Note that <i>s</i> may contain tab characters - in that case, multiple
     * columns are added. <i>sah</i> contains the locations of the tab characters.
     * @param s String representation of value(s) to be added to the new row
     * @return A row with all the columns from the current row, plus <i>s</i>
     */
    public Row rowWithAddedColumns(CharSequence s) {
        return RowObj.apply(getAllCols() + "\t" + s);
    }

    public Row rowWithAddedColumns(CharSequence[] cols) {
        final String suffix = String.join("\t", cols);
        return RowObj.apply(getAllCols() + "\t" + suffix);
    }
    /**
     * Write the contents of the row to the given writer
     * @param outputStream Destination writer
     * @throws IOException
     */
    public abstract void writeRow(Writer outputStream) throws IOException;

    /**
     * Write the contents of the row to the given output stream
     * @param outputStream Destination stream
     * @throws IOException
     */
    public abstract void writeRowToStream(OutputStream outputStream) throws IOException;

    /**
     * Returns a tab-separated string with the values from the start column up to and including the stop column
     * @param m First column to include (zero based)
     * @param n Last column to include (zero based)
     * @return Tab-separated string
     */
    public CharSequence otherColsSlice(int m, int n) {
        // Slice style range, e.g. 2-3 pick only chr, pos and col2.  2-2 give only chr, pos.
        if (m < 2)
            throw new GorDataException("Row.otherColsSlice: illegal columns " + m + ", " + n, -1, "",toString());
        return colsSlice(m, n);
    }

    /**
     * Creates a new row, joined with another row.
     * @param other Row to join with
     * @param startCol First column to include from other row
     * @param endCol Last column to include from other row
     * @return New row, joined with other row
     */
    public Row joinedWithSlice(Row other, int startCol, int endCol) {
        return rowWithAddedColumn(other.colsSlice(startCol, endCol));
    }

    /**
     * Creates a new row, joined with another row with a column in between.
     * @param addedColumn Column value added before slice from other row
     * @param other Row to join with
     * @param startCol First column to include from other row
     * @param endCol Last column to include from other row
     * @return New row, joined with other row
     */
    public Row joinedWithSliceAndAddedColumn(CharSequence addedColumn, Row other, int startCol, int endCol) {
        return rowWithAddedColumn(addedColumn.toString() + "\t" + other.colsSlice(startCol, endCol));
    }

    /**
     * Creates a copy of the current row.
     * @return A copy of the row
     */
    public Row copyRow() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Populates the chr and pos fields based on the input, and returns the offset into input
     * where pos ended.
     * @param input The row contents as a tab-separated string
     * @return The offset where the position field ended.
     */
    int populateChromPosOnly(CharSequence input) {
        int start = 0;
        int length = input.length();

        // First column is chromosome
        int end = findNextTab(input, start, length);
        chr = input.subSequence(start, end).toString();

        // Second column is position. Manually parse the position rather than using parseInt
        // for performance reasons - we know it's a simple unsigned integer - the simplest
        // case for parsing a number.
        start = end + 1;
        end = start;

        int val = 0;
        boolean isNegative = false;
        if(input.charAt(end) == '-') {
            end++;
            isNegative = true;
        }
        while(end < length && input.charAt(end) != '\t') {
            char digit = input.charAt(end);
            if(digit < '0' || digit > '9') {
                throw new NumberFormatException("Invalid position");
            }
            int next = val * 10 + (digit - '0');
            if (next < val) {
                throw new NumberFormatException("Number too large for position");
            }
            val = next;
            end++;
        }
        pos = val;
        if(isNegative) {
            pos = -pos;
        }

        return end;
    }

    /**
     * Looks for the next tab character in the given character sequence
     * @param contents The character sequence to scan
     * @param start Where to start scanning
     * @param length Where to stop
     * @return The position of the first tab character after start, or end
     */
    private static int findNextTab(CharSequence contents, int start, int length) {
        int end = start;
        while (end < length && contents.charAt(end) != '\t') {
            end++;
        }
        return end;
    }

    static void fillSplitArray(CharSequence input, int[] splitArray) {
        int numColumns = splitArray.length;
        int start = 0;
        int end = input.length();
        for (int i = 0; i < numColumns; i++) {
            start = findNextTab(input, start, end);
            splitArray[i] = start;
            start++;
        }
    }
}
