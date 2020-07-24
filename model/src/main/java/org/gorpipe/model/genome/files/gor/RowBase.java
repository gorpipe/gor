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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.model.gor.RowObj;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;

/**
 * Created by sigmar on 27/04/2017.
 */
public class RowBase extends Row implements Serializable {
    /**
     * The contents of the row.
     */
    private CharSequence allCols;

    /**
     * The locations of the tab characters within <i>allCols</i> for quick access to individual columns.
     */
    private int[] splitArray;

    private RowBase() {
    }

    public RowBase(CharSequence input, int numColumns) {
        allCols = input;
        splitArray = createSplitArray(allCols, numColumns);
        chr = RowObj.colString(0, allCols, splitArray).toString();
        pos = RowObj.colInt(1, allCols, splitArray);
    }

    public RowBase(CharSequence input) {
        this(input, countColumns(input));
    }

    public static Row getProgressRow(String chr, int pos) {
        final Row toReturn = new RowBase(chr + "\t" + pos + "\tdummy");
        toReturn.isProgress = true;
        return toReturn;
    }

    public RowBase(String chr, int pos, CharSequence allCols, int[] sa, RowObj.BinaryHolder bH) {
        this.chr = chr;
        this.pos = pos;
        this.allCols = allCols;
        this.splitArray = sa;
        this.bH = bH;
    }

    @Override
    public int sa(int i) {
        return splitArray[i];
    }

    @Override
    public int hashCode() {
        return chr.hashCode() + pos + getAllCols().toString().hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if(that instanceof RowBase) {
            return allCols.toString().equals(((RowBase) that).allCols.toString());
        } else {
            return super.equals(that);
        }
    }

    public CharSequence getAllCols() {
        return allCols;
    }

    @Override
    public int[] getSplitArray() {
        return splitArray;
    }

    @Override
    public void addColumns(int num) {
        resize(splitArray.length + num);
    }

    @Override
    public void removeColumn(int n) {
        final int[] oldSplitArray = this.splitArray;
        this.splitArray = new int[oldSplitArray.length - 1];
        System.arraycopy(oldSplitArray, 0, this.splitArray, 0, n);
        final CharSequence oldSequence = this.allCols;
        this.allCols = new StringBuilder();
        ((StringBuilder) this.allCols).append(oldSequence.subSequence(0, oldSplitArray[n - 1]));
        final int diff = oldSplitArray[n] - oldSplitArray[n - 1];
        ((StringBuilder) this.allCols).append(oldSequence.subSequence(oldSplitArray[n], oldSequence.length()));
        while (n < oldSplitArray.length) {
            this.splitArray[n - 1] = oldSplitArray[n++] - diff;
        }
    }

    @Override
    public void writeRowToStream(OutputStream outputStream) throws IOException {
        outputStream.write(this.allCols.toString().getBytes());
    }

    @Override
    public void writeRow(Writer writer) throws IOException {
        writer.write(this.allCols.toString());
    }

    @Override
    public void resize(int newsize) {
        int[] nsa = new int[newsize];
        System.arraycopy(splitArray, 0, nsa, 0, splitArray.length);
        splitArray = nsa;
    }

    @Override
    public void setColumn(int i, String val) {
        if(splitArray[i+1] < allCols.length()) {
            final int prefixLength = splitArray[i + 1] + 1;
            final int tailLength = splitArray[splitArray.length - 1] - splitArray[i+2] + 1;
            final int newLength = prefixLength + val.length() + 1 + tailLength;
            StringBuilder sb = new StringBuilder(newLength);
            sb.append(allCols, 0, splitArray[i + 1] + 1);
            sb.append(val);
            sb.append(allCols, splitArray[i+2], splitArray[splitArray.length - 1]);
            allCols = sb;

            int newSplitValue = splitArray[i+1] + val.length() + 1;
            int delta = splitArray[i+2] - newSplitValue;
            for(int ix = i + 2; ix < splitArray.length; ix++) {
                splitArray[ix] -= delta;
            }
        } else {
            splitArray[i + 2] = splitArray[i + 1] + val.length() + 1;
            final int newLength = splitArray[i+2];
            StringBuilder sb = new StringBuilder(newLength);
            sb.append(allCols, 0, splitArray[i+1]);
            sb.append('\t');
            sb.append(val);
            allCols = sb;
        }
    }

    @Override
    public void setColumns(int[] colsToReplace, String[] values) {
        StringBuilder sb = new StringBuilder();

        addColumnsBefore(sb, colsToReplace[0]);

        for(int i = 0; i < colsToReplace.length; i++ ) {
            int col = colsToReplace[i];
            String value = values[i];
            sb.append(value);
            if(col < numCols() - 1) {
                sb.append('\t');
            }

            if (i < colsToReplace.length - 1) {
                int nextCol = colsToReplace[i + 1];
                if (nextCol > col + 1) {
                    int start = splitArray[col] + 1;
                    int end = splitArray[nextCol - 1] + 1;
                    sb.append(allCols, start, end);
                }
            }
        }
        addColumnsAfter(sb, colsToReplace[colsToReplace.length - 1]);

        allCols = sb;
        splitArray = createSplitArray(allCols, numCols());
    }

    private void addColumnsBefore(StringBuilder sb, int firstCol) {
        if (firstCol > 0) {
            int prefixEnd = splitArray[firstCol-1];
            sb.append(allCols, 0, prefixEnd);
            sb.append('\t');
        }
    }

    private void addColumnsAfter(StringBuilder sb, int lastCol) {
        final int finalColumn = numCols() - 1;
        if(lastCol < finalColumn) {
            int start = splitArray[lastCol] + 1;
            int end = splitArray[finalColumn];
            sb.append(allCols, start, end);
        }
    }

    @Override
    public int length() {
        return allCols.length();
    }

    @Override
    public String toString() {
        return allCols.toString();
    }

    @Override
    public int otherColsLength() {
        return allCols.length() - splitArray[1];
    }

    @Override
    public int numCols() {
        if (splitArray == null) {
            splitArray = RowObj.splitArray(allCols);
        }
        return splitArray.length;
    }

    @Override
    public String toColString() {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < numCols(); i++) {
            line.append("(").append(RowObj.colString(i, allCols, splitArray)).append(") ");
        }
        return line.toString();
    }

    @Override
    public String otherCols() {
        if (numCols() <= 2 || (splitArray.length == 2 && splitArray[splitArray.length - 1] + 1 >= allCols.length()))
            return "";
        return allCols.subSequence(splitArray[1] + 1, allCols.length()).toString();
    }

    @Override
    public CharSequence colAsString(int n) {
        testColumnIndex(n);
        return RowObj.colString(n, allCols, splitArray);
    }


    @Override
    public int colAsInt(int n) {
        testColumnIndex(n);
        return RowObj.colInt(n, allCols, splitArray);
    }

    @Override
    public double colAsDouble(int n) {
        testColumnIndex(n);
        return RowObj.colDouble(n, allCols, splitArray);
    }

    @Override
    public Long colAsLong(int n) {
        testColumnIndex(n);
        return RowObj.colLong(n, allCols, splitArray);
    }

    @Override
    public char peekAtColumn(int n) {
        testColumnIndex(n);
        return RowObj.peekAtColumn(n, allCols, splitArray);
    }

    @Override
    public String selectedColumns(int[] columns) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < columns.length; i++) {
            addColumnToStringBuilder(sb, columns[i]);
            if(i < columns.length - 1) {
                sb.append("\t");
            }
        }

        return sb.toString();
    }

    @Override
    public CharSequence colsSlice(int m, int n) {
        testColumnRange(m, n);
        if (m == n) return "";
        int start2 = m == 0 ? 0 : splitArray[m - 1] + 1;
        int stop2 = splitArray[n - 1];
        return allCols.subSequence(start2, stop2);
    }

    @Override
    public Row joinedWithSlice(Row other, int startCol, int endCol) {
        if(!(other instanceof RowBase)) {
            return super.joinedWithSlice(other, startCol, endCol);
        }
        final int[] otherSplitArray = ((RowBase) other).splitArray;
        int numCols = endCol - startCol;
        final int[] newSplitArray = Arrays.copyOf(splitArray, splitArray.length + numCols);

        int base = splitArray.length;
        int start = startCol == 0 ? 0 : otherSplitArray[startCol - 1];
        int offset = splitArray[splitArray.length - 1] - start;
        for(int c = 0; c < numCols; c++) {
            newSplitArray[base + c] = otherSplitArray[startCol + c] + offset;
        }

        final StringBuilder sb = new StringBuilder(newSplitArray[newSplitArray.length - 1]);
        sb.append(allCols);

        if(otherSplitArray[endCol - 1] > start) {
            sb.append('\t');
            sb.append(((RowBase) other).allCols, start + 1, otherSplitArray[endCol - 1]);
        }

        return new RowBase(chr, pos, sb, newSplitArray, null);
    }

    @Override
    public Row joinedWithSliceAndAddedColumn(CharSequence addedColumn, Row other, int startCol, int endCol) {
        if(!(other instanceof RowBase)) {
            return super.joinedWithSliceAndAddedColumn(addedColumn, other, startCol, endCol);
        }
        final int[] otherSplitArray = ((RowBase) other).splitArray;
        int numCols = endCol - startCol + 1;
        final int[] newSplitArray = Arrays.copyOf(splitArray, splitArray.length + numCols);

        newSplitArray[splitArray.length] = splitArray[splitArray.length - 1] + addedColumn.length() + 1;

        int base = splitArray.length + 1;
        int offset = newSplitArray[splitArray.length] - otherSplitArray[startCol - 1];
        for(int c = 0; c < numCols - 1; c++) {
            newSplitArray[base + c] = otherSplitArray[startCol + c] + offset;
        }

        final StringBuilder sb = new StringBuilder(allCols);
        sb.append('\t');
        sb.append(addedColumn);
        sb.append('\t');
        sb.append(other.colsSlice(startCol, endCol));

        return new RowBase(chr, pos, sb, newSplitArray, null);
    }

    @Override
    public Row slicedRow(int m, int n) {
        testColumnRange(m, n);
        int start1 = 0;
        int stop1 = splitArray[1];
        int length1 = splitArray[1] + ((numCols() > 2 && m != n) ? 1 : 0); // account for the tab
        int start2 = 0;
        int stop2 = 0;
        if (m != n) {
            start2 = splitArray[m - 1] + 1;
            stop2 = splitArray[n - 1];
        }
        StringBuilder strbuff = new java.lang.StringBuilder(length1 + stop2 - start2);
        int i = start1;
        while (i < stop1) {
            strbuff.append(allCols.charAt(i));
            i += 1;
        }
        if (m != n) {
            strbuff.append('\t');
            i = start2;
            while (i < stop2) {
                strbuff.append(allCols.charAt(i));
                i += 1;
            }
        }
        return new RowBase(chr, pos, strbuff, RowObj.splitArray(strbuff), bH);
    }

    @Override
    public void addSingleColumnToRow(String s) {
        splitArray = Arrays.copyOf(splitArray, splitArray.length + 1);
        splitArray[splitArray.length-1] = splitArray[splitArray.length - 2] + s.length() + 1;
        allCols = allCols + "\t" + s;
    }

    @Override
    public Row rowWithSelectedColumns(int[] columns) {
        final int[] newSplitArray = new int[columns.length];
        int newLength = 0;
        for (final int column : columns) {
            int start = column == 0 ? 0 : splitArray[column - 1];
            int end = splitArray[column];
            newLength += end - start;
        }
        StringBuilder sb = new StringBuilder(newLength);

        for(int i = 0; i < columns.length; i++) {
            addColumnToStringBuilder(sb, columns[i]);
            newSplitArray[i] = sb.length();
            if(i < columns.length - 1) {
                sb.append('\t');
            }
        }

        String newChr = columns[0] == 0 ? chr : sb.substring(0, newSplitArray[0]);
        try {
            int newPos = columns[1] == 1 ? pos : RowObj.colInt(1, sb, newSplitArray);
            return new RowBase(newChr, newPos, sb, newSplitArray, null);
        } catch (NumberFormatException e) {
            throw new GorDataException("Position is invalid", e);
        }

    }

    @Override
    public Row rowWithAddedColumn(CharSequence s) {
        return rowWithAddedColumns(s.toString().split("\t", -1));
    }

    @Override
    public Row rowWithAddedColumns(CharSequence[] cols) {
        final int[] newSplitArray = Arrays.copyOf(splitArray, splitArray.length + cols.length);
        final StringBuilder newStringBuilder = new StringBuilder(allCols);
        final int numCols = numCols();
        int offset = splitArray[numCols-1];
        for(int i = 0; i < cols.length; i++) {
            newStringBuilder.append("\t");
            newStringBuilder.append(cols[i]);
            newSplitArray[numCols+i-1] = offset;
            offset += cols[i].length() + 1;
        }
        newSplitArray[numCols+cols.length-1] = offset;

        return new RowBase(chr, pos, newStringBuilder, newSplitArray, null);
    }

    @Override
    public Row copyRow() {
        RowBase clone = new RowBase();
        clone.chr = chr;
        clone.pos = pos;
        clone.allCols = allCols;
        clone.splitArray = splitArray.clone();
        return clone;
    }

    /**
     * Trim the allCols StringBuilder to minimum size needed to contain the column data
     */
    public void trim() {
        if (allCols instanceof StringBuilder && allCols.length() != ((StringBuilder) allCols).capacity()) {
            final StringBuilder t = new StringBuilder(allCols.length());
            t.append(allCols);
            allCols = t;
        }
    }

    @Override
    public String stringValue(int col) {
        return colAsString(col).toString();
    }

    @Override
    public int intValue(int col) {
        return colAsInt(col);
    }

    @Override
    public double doubleValue(int col) {
        return colAsDouble(col);
    }
    @Override
    public long longValue(int col) {
        return colAsLong(col);
    }

    private void addColumnToStringBuilder(StringBuilder sb, int column) {
        int start = column == 0 ? 0 : splitArray[column - 1] + 1;
        int end = splitArray[column];
        sb.append(allCols, start, end);
    }

    private static int[] createSplitArray(CharSequence input, int numColumns) {
        int[] splitArray = new int[numColumns];
        fillSplitArray(input, splitArray);
        return splitArray;
    }

    private void testColumnIndex(int n) {
        int index = n-1;
        if (index > numCols()) {
            throw new GorDataException("Column " + n + " does not exist", n, toString());
        }
    }

    private void testColumnRange(int m, int n) {
        int index1 = m-1;
        if (n > numCols() || index1 > numCols() || n < m)
            throw new GorDataException("Row.colsSlice: illegal columns " + m + ", " + n, n, toString());
    }
}
