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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.gorpipe.model.util.ByteTextBuilder;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.model.gor.RowObj;

/**
 * Description of a data Line. Note we allow it to have public variables to simplify passing data between various code parts.
 * Only implementations of GenomicIterator should ever modify their value.
 */
public class Line extends Row {

    private static final String ERROR_PLACEHOLDER = "Line starting with %s is missing data columns";

    /**
     * The data associated with the position defined by 'pos', i.e. all the other columns than chr and pos
     */
    public ByteTextBuilder[] cols;

    /**
     * The chromosome index
     */
    public int chrIdx;

    /**
     * Default constructor
     */
    public Line() {
    }

    /**
     * Construct a Line from a CharSequence
     * @param contents
     */
    public Line(CharSequence contents) {
        int offset = populateChromPosOnly(contents);

        int length = contents.length();
        int numColumns = 1;
        for(int i = 0; i < length; i++) {
            if(contents.charAt(i) == '\t') {
                numColumns += 1;
            }
        }

        cols = new ByteTextBuilder[numColumns - 2];
        for(int i = 0; i < cols.length; i++) {
            cols[i] = new ByteTextBuilder();
        }
        setData(contents.toString().getBytes(), offset + 1);
    }

    /**
     * Construct a Line object
     *
     * @param cols The columns in the Line
     */
    public Line(ByteTextBuilder[] cols) {
        this.cols = cols;
    }

    public Line(String chr, int pos, int chrIdx, ByteTextBuilder[] cols) {
        super(chr, pos);
        this.cols = cols;
        this.chrIdx = chrIdx;
    }

    /**
     * Construct a Line object
     *
     * @param colcnt The number of columns in the Line
     */
    public Line(int colcnt) {
        this(new ByteTextBuilder[colcnt]);
        for (int i = 0; i < cols.length; i++) {
            cols[i] = new ByteTextBuilder();
        }
    }

    @Override
    public String toString() {
        return getLine().toString();
    }

    /**
     * Set the data columns with the data from the buffer.
     * Assume buffer contains a 'line' of data i.e. columns separated with \t and either ending with \n for the line or all the buffer content is the line
     *
     * @param buf    The buffer to read from
     * @param offset The offset into the buffer to start reading from
     * @return The offset to the next byte after the last read byte from the buffer
     */
    public int setData(Line gorline, byte[] buf, int offset) {
        final int start = offset;
        final int len = buf.length;
        final int colcnt = Math.min(gorline.cols.length, cols.length + 2);
        for (int i = 0; i < 2; i++) {
            int end = offset;
            while (end < len && buf[end] != '\t' && buf[end] != '\n') {
                end++;
            }
            // Ignore \r if preceding a \n
            if (end < len && buf[end] == '\n') {
                if (i != colcnt - 1) {
                    throw new GorDataException(String.format(ERROR_PLACEHOLDER, new String(buf, start, end - start)), -1, "", this.toString());
                }
                gorline.cols[i].set(buf, offset, (end - 1 >= 0 && buf[end - 1] == '\r') ? (end - 1) - offset : end - offset);
            } else {
                gorline.cols[i].set(buf, offset, end - offset);
            }
            offset = end + 1;
        }
        for (int i = 2; i < colcnt; i++) {
            int end = offset;
            while (end < len && buf[end] != '\t' && buf[end] != '\n') {
                end++;
            }
            // Ignore \r if preceding a \n
            if (end < len && buf[end] == '\n') {
                if (i != colcnt - 1) {
                    throw new GorDataException(String.format(ERROR_PLACEHOLDER, new String(buf, start, end - start)), -1, "", this.toString());
                }
                cols[i - 2].set(buf, offset, (end - 1 >= 0 && buf[end - 1] == '\r') ? (end - 1) - offset : end - offset);
            } else {
                cols[i - 2].set(buf, offset, end - offset);
            }
            offset = end + 1;
        }
        return offset;
    }

    /**
     * Copy the column contents from the specified line
     * @param src The source line to copy
     */
    public void copyColumnsFrom(Row src) {
        for (int i = 0; i < cols.length; i++) {
            cols[i].set(src.colAsString(i));
        }
    }

    /**
     * Set the data columns with the data from the buffer.
     * Assume buffer contains a 'line' of data i.e. columns separated with \t and either ending with \n for the line or all the buffer content is the line
     *
     * @param buf    The buffer to read from
     * @param offset The offset into the buffer to start reading from
     * @return The offset to the next byte after the last read byte from the buffer
     */
    public int setData(byte[] buf, int offset) {
        final int start = offset;
        final int len = buf.length;
        final int colcnt = cols.length;
        for (int i = 0; i < colcnt; i++) {
            int end = offset;
            while (end < len && buf[end] != '\t' && buf[end] != '\n') {
                end++;
            }
            // Ignore \r if preceding a \n
            if (end < len && buf[end] == '\n') {
                if (i != colcnt - 1) {
                    throw new GorDataException(String.format(ERROR_PLACEHOLDER, new String(buf, start, end - start)), -1, "", this.toString());
                }
                cols[i].set(buf, offset, (end - 1 >= 0 && buf[end - 1] == '\r') ? (end - 1) - offset : end - offset);
            } else {
                cols[i].set(buf, offset, end - offset);
            }
            offset = end + 1;
        }
        return offset;
    }

    /**
     * Clear column buffers
     */
    public void clear() {
        int len = cols.length;
        while (len-- > 0) {
            cols[len].clear();
        }
    }

    /**
     * Get the line as a tab delimited string
     *
     * @return The string
     */
    public CharSequence getLine() {
        StringBuilder sb = new StringBuilder(length());
        return getLine(sb);
    }

    /**
     * Get the line as a tab delimited string
     *
     * @param sb The string builder to use
     * @return The string
     */
    public CharSequence getLine(StringBuilder sb) {
        sb.setLength(0);
        sb.append(chr);
        sb.append('\t');
        sb.append(this.pos);
        for (int i = 0; i < cols.length; i++) {
            sb.append('\t');
            cols[i].copy(sb);
        }
        return sb;
    }

    @Override
    public void addColumns(int num) {
        resize(numCols() + num);
    }

    @Override
    public void removeColumn(int n) {
        n -= 2;
        if (n < this.cols.length) {
            final ByteTextBuilder[] oldCols = this.cols;
            this.cols = new ByteTextBuilder[oldCols.length - 1];
            System.arraycopy(oldCols, 0, this.cols, 0, n);
            if (n != this.cols.length) {
                System.arraycopy(oldCols, n + 1, this.cols, n, this.cols.length - n);
            }
        }
    }

    @Override
    public void writeRowToStream(OutputStream outputStream) throws IOException {
        outputStream.write(chr.getBytes());
        outputStream.write('\t');
        outputStream.write(String.valueOf(pos).getBytes());
        if (cols != null) {
            for (ByteTextBuilder builder : cols) {
                outputStream.write('\t');
                outputStream.write(builder.peekAtBuffer(), 0, builder.length());
            }
        }
    }

    @Override
    public void writeRow(Writer writer) throws IOException {
        writer.write(chr);
        writer.write('\t');
        writer.write(String.valueOf(pos));
        if (cols != null) {
            for (ByteTextBuilder builder : cols) {
                writer.write('\t');
                writer.write(builder.toString());
            }
        }
    }

    @Override
    public void resize(int newsize) {
        ByteTextBuilder[] ncols = new ByteTextBuilder[newsize - 2];
        System.arraycopy(cols, 0, ncols, 0, cols.length);
        for(int i = cols.length; i < ncols.length; i++) {
            ncols[i] = new ByteTextBuilder();
        }
        cols = ncols;
    }

    public void set(int i, String val) {
        cols[i].set(val);
    }

    @Override
    public void setColumn(int i, String val) {
        cols[i] = new ByteTextBuilder(val);
    }

    @Override
    public CharSequence colsSlice(int m, int n) {
        if (m == n) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            if (m == 0) {
                sb.append(chr);
                sb.append("\t");
                sb.append(pos);
                m++;
            } else if (m == 1) {
                sb.append(pos);
            } else {
                cols[m - 2].copy(sb);
            }
            for (int i = m + 1; i < n; i++) {
                sb.append("\t");
                cols[i - 2].copy(sb);
            }
            return sb;
        }
    }

    @Override
    public String toColString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numCols(); i++) {
            sb.append("(").append(cols[i].toString()).append(") ");
        }
        return sb.toString();
    }

    @Override
    public int colAsInt(int col) {
        if (col == 1) return pos;
        else return cols[col - 2].toInt();
    }

    @Override
    public double colAsDouble(int col) {
        if (col == 1) return (double) pos;
        else return cols[col - 2].toDouble();
    }

    @Override
    public Long colAsLong(int col) {
        return col == 1 ? (long) pos : cols[col - 2].toLong();
    }

    @Override
    public CharSequence colAsString(int col) {
        if (col == 0) return chr;
        else if (col == 1) return Integer.toString(pos);
        else return cols[col - 2];
    }

    @Override
    public byte[] colAsBytes(int colNum) {
        if (colNum == 0) return chr.getBytes();
        else if (colNum == 1) return Integer.toString(pos).getBytes();
        else return cols[colNum - 2].getBytes();
    }

    @Override
    public int numCols() {
        return cols.length + 2;
    }

    public int numOtherCols() {
        return cols.length;
    }

    @Override
    public int otherColsLength() {
        int length = cols.length - 1;
        for (ByteTextBuilder col : cols) {
            length += col.length();
        }
        return length;
    }

    @Override
    public void addSingleColumnToRow(String s) {
        ByteTextBuilder[] ncols = new ByteTextBuilder[cols.length + 1];
        System.arraycopy(cols, 0, ncols, 0, cols.length);
        ncols[cols.length] = new ByteTextBuilder(s);
        cols = ncols;
    }

    /**
     * Uses log10 to calculate integer string length instead of casting to string
     *
     * @return the String length of the integer pos value
     */
    private int posLen() {
        return (int) Math.log10(Math.max(1, pos)) + 1;
    }

    @Override
    public int length() {
        return chr != null ? chr.length() + posLen() + 2 + otherColsLength() : 0;
    }

    private int selectedColumnsLength(int[] columns) {
        int length = columns.length - 1;
        for (int c : columns) {
            if (c == 0) length += chr.length();
            else if (c == 1) length += posLen();
            else length += cols[c - 2].length();
        }
        return length;
    }

    @Override
    public String selectedColumns(int[] columns) {
        if (columns.length > 0) {
            StringBuilder sb = new StringBuilder(selectedColumnsLength(columns));
            addToStringBuilder(sb, columns[0]);
            for (int i = 1; i < columns.length; i++) {
                sb.append("\t");
                addToStringBuilder(sb, columns[i]);
            }
            return sb.toString();
        }
        return "";
    }

    private void addToStringBuilder(StringBuilder sb, int col) {
        if (col == 0) sb.append(chr);
        else if (col == 1) sb.append(pos);
        else cols[col - 2].copy(sb);
    }

    private Line getDerivedRow() {
        Line other = new Line();
        other.chr = chr;
        other.pos = pos;
        other.chrIdx = chrIdx;
        return other;
    }

    @Override
    public Row slicedRow(int startCol, int stopCol) {
        Line other = getDerivedRow();
        int numOtherCols = stopCol - startCol;
        other.cols = new ByteTextBuilder[numOtherCols];
        System.arraycopy(cols, startCol - 2, other.cols, 0, numOtherCols);
        return other;
    }

    @Override
    public Row rowWithSelectedColumns(int[] columns) {
        StringBuilder sb = new StringBuilder();
        addToStringBuilder(sb, columns[0]);
        for (int i = 1; i < columns.length; i++) {
            sb.append('\t');
            addToStringBuilder(sb, columns[i]);
        }
        return RowObj.apply(sb);

        // TODO create a new line object from preexisting column objects instead
        /*ByteTextBuilder[] ncols = new ByteTextBuilder[columns.length];
        for( int i = 0; i < columns.length; i++ ) ncols[i] = cols[columns[i]];
        return new Line(chr,chrIdx,pos,ncols);*/
    }

    @Override
    public Row rowWithAddedColumn(CharSequence s) {
        String input = s.toString();
        String[] columns = input.split("\t", -1);

        Line other = getDerivedRow();
        other.cols = new ByteTextBuilder[cols.length + columns.length];
        System.arraycopy(cols, 0, other.cols, 0, cols.length);

        int offset = cols.length;
        for(int i = 0; i < columns.length; i++) {
            other.cols[i + offset] = new ByteTextBuilder(columns[i]);
        }

        return other;
    }

    @Override
    public CharSequence getAllCols() {
        return this.getLine();
    }

    private void otherCols(StringBuilder sb) {
        if (cols.length > 0) cols[0].copy(sb);
        for (int i = 1; i < cols.length; i++) {
            sb.append('\t');
            cols[i].copy(sb);
        }
    }

    @Override
    public String otherCols() {
        StringBuilder sb = new StringBuilder(otherColsLength());
        otherCols(sb);
        return sb.toString();
    }

    static int toInt(String text) {
        int nr = 0;
        for (int pos = 0; pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9'; pos++) {
            nr = 10 * nr + text.charAt(pos) - '0';
        }
        return nr;
    }

    /**
     * Parse a integer from the specified position in the String, assuming it ends at the denoted end position.
     *
     * @param s     The string to parse from
     * @param start The start position of the integer text
     * @param stop  The stop position of the integer text
     * @return The integer value parsed from the string.
     */
    public static int parseInt(String s, int start, int stop) {
        int value = 0;
        int i = start;
        while (i + 4 <= stop) {
            value = 10 * value + (s.charAt(i++) - '0');
            value = 10 * value + (s.charAt(i++) - '0');
            value = 10 * value + (s.charAt(i++) - '0');
            value = 10 * value + (s.charAt(i++) - '0');
        }
        while (i < stop) {
            value = 10 * value + (s.charAt(i++) - '0');
        }
        return value;
    }

    @Override
    public int sa(int i) {
        int val = i + chr.length();
        if (i > 0) val += posLen();
        for (int k = 2; k <= i; k++) {
            val += cols[k - 2].length();
        }
        return val;
    }

    @Override
    public int hashCode() {
        return pos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Line) {
            return isEqualLine((Line) obj);
        }
        return false;
    }

    /**
     * @param other Another line object
     * @return True if the content of the two line object is equal, else false
     */
    private boolean isEqualLine(Line other) {
        if (!chr.equals(other.chr) || pos != other.pos || cols.length != other.cols.length) {
            return false;
        }
        for (int i = 0; i < cols.length; i++) {
            if (!cols[i].equals(other.cols[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String stringValue(int col) {
        return col > 1 ? this.cols[col - 2].toString() : (col == 1 ? String.valueOf(this.pos) : this.chr);
    }

    @Override
    public int intValue(int col) {
        if (col > 1) return this.cols[col - 2].toInt();
        if (col == 1) return this.pos;
        throw new GorDataException("Chromosome name can not be converted into integer!", col);
    }

    @Override
    public double doubleValue(int col) {
        if (col > 1) return this.cols[col - 2].toDouble();
        if (col == 1) return this.pos;
        throw new GorDataException("Chromosome name can not be converted into double!", col);
    }

    @Override
    public long longValue(int col) {
        if (col > 1) return this.cols[col - 2].toLong();
        if (col == 1) return this.pos;
        throw new GorDataException("Chromosome name can not be converted into long!", col);
    }

    @Override
    public char peekAtColumn(int col) {
        char result;

        if (col > 1) {
            result = (char)this.cols[col - 2].peekAtBuffer()[0];
        } else if (col == 1) {
            result = String.valueOf(this.pos).charAt(0);
        } else {
            if (this.chr != null && !this.chr.isEmpty())
                result = this.chr.charAt(0);
            else
                result = '?';
        }

        return result;
    }

    @Override
    public Row copyRow() {
        Line clone = new Line();
        clone.chr = chr;
        clone.chrIdx = chrIdx;
        clone.pos = pos;

        clone.cols = cols.clone();

        return clone;
    }
}
