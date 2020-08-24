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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public abstract class UTestRowImplementations {
    private static final String TEST_LINE = "chr1\t1\tthis\tis\ta\ttest";
    private static final String TEST_LINE_INT = "chr1\t1\tthis\tis\t42\ttest";
    private static final String TEST_LINE_DOUBLE = "chr1\t1\tthis\tis\t3.14\ttest";
    private static final String TEST_LINE_EMPTY_COLUMN = "chr1\t1\tthis\tis\t\ttest";
    private static final String TEST_LINE_EMPTY_LAST_COLUMN = "chr1\t1\tthis\tis\ta\t";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public abstract Row createRow(CharSequence input);

    @Test
    public void chr() {
        Row r = createRow(TEST_LINE);
        assertEquals("chr1", r.chr);
    }

    @Test
    public void pos() {
        Row r = createRow(TEST_LINE);
        assertEquals(1, r.pos);
    }

    @Test
    public void posWhenPosIsNegative() {
        Row r = createRow("chr1\t-1");
        assertEquals(-1, r.pos);
    }

    @Test
    public void posWhenPosIsInvalid() {
        exception.expect(NumberFormatException.class);
        Row r = createRow("chr1\tx");
        assertEquals(0, r.pos);
    }

    @Test
    public void compareToAgainstSelf() {
        Row r = createRow(TEST_LINE);
        //noinspection EqualsWithItself
        assertEquals(0, r.compareTo(r));
    }

    @Test
    public void compareToWhenRowsAreIdentical() {
        Row r1 = createRow(TEST_LINE);
        Row r2 = createRow("chr1\t1");
        r2.addColumns(4);
        r2.setColumn(0, "this");
        r2.setColumn(1, "is");
        r2.setColumn(2, "a");
        r2.setColumn(3, "test");

        assertEquals(0, r1.compareTo(r2));
        assertEquals(0, r2.compareTo(r1));
    }

    @Test
    public void compareToWhenRowsAreDifferent() {
        final Row r1 = createRow("chr1\t1\tabc");
        final Row r2 = createRow("chr1\t1\tdef");
        assertTrue(r1.compareTo(r2) < 0);
        assertTrue(r2.compareTo(r1) > 0);
    }

    @Test
    public void advancedCompareWhenRowsAreIdenticalWithNoSortInfo() {
        Row r1 = createRow(TEST_LINE);
        Row r2 = createRow("chr1\t1");
        r2.addColumns(4);
        r2.setColumn(0, "this");
        r2.setColumn(1, "is");
        r2.setColumn(2, "a");
        r2.setColumn(3, "test");

        assertEquals(0, r1.advancedCompare(r2, null));
        assertEquals(0, r2.advancedCompare(r1, null));
    }

    @Test
    public void advancedCompareWhenRowsAreDifferent() {
        final Row r1 = createRow("chr1\t1\tabc");
        final Row r2 = createRow("chr2\t2\tdef");
        assertTrue(r1.advancedCompare(r2, null) < 0);
        assertTrue(r2.advancedCompare(r1, null) > 0);
    }

    @Test
    public void equalsReturnsTrueWhenRowsAreIdentical1() {
        Row r1 = createRow(TEST_LINE);
        Row r2 = createRow(TEST_LINE);

        assertEquals(r1, r2);
        assertEquals(r2, r1);
    }

    @Test
    public void equalsReturnsTrueWhenRowsAreIdentical2() {
        Row r1 = createRow(TEST_LINE);
        Row r2 = createRow("chr1\t1");
        r2.addColumns(4);
        r2.setColumn(0, "this");
        r2.setColumn(1, "is");
        r2.setColumn(2, "a");
        r2.setColumn(3, "test");

        assertEquals(r1, r2);
        assertEquals(r2, r1);
    }

    @Test
    public void equalsReturnsFalseWhenRowsAreDifferent() {
        final Row r1 = createRow("chr1\t1\tabc");
        final Row r2 = createRow("chr1\t1\tdef");
        assertNotEquals(r1, r2);
        assertNotEquals(r2, r1);
    }

    @Test
    public void hashCodeReturnsSameHashWhenRowsAreIdentical() {
        Row r1 = createRow(TEST_LINE);
        Row r2 = createRow("chr1\t1");
        r2.addColumns(4);
        r2.setColumn(0, "this");
        r2.setColumn(1, "is");
        r2.setColumn(2, "a");
        r2.setColumn(3, "test");

        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    public void testChromAndPos() {
        Row r = createRow(TEST_LINE);
        assertEquals("chr1", r.chr);
        assertEquals(1, r.pos);
    }

    @Test
    public void eightBitSupported() {
        String input = "chr1\t1\tÞetta er próf - áéíóúýö";
        Row r = createRow(input);
        assertEquals(input, r.getAllCols().toString());
    }

    @Test
    public void utf8Supported() {
        String input = "chr1\t1\tÞetta er próf\t이것은 시험이다.\tこれはテストです\t这是一个测试\tЭто тест";
        Row r = createRow(input);
        assertEquals(input, r.getAllCols().toString());
    }

    @Ignore("This seems to be broken in Line")
    @Test
    public void testToColString() {
        String expected = "(chr1) (1) (this) (is) (a) (test) ";
        Row r = createRow(TEST_LINE);
        assertEquals(expected, r.toColString());
    }

    @Test
    public void testIntValue() {
        Row r = createRow(TEST_LINE);

        int result = r.intValue(1);
        assertEquals(1, result);
    }

    @Test
    public void testColAsInt() {
        Row r = createRow(TEST_LINE_INT);
        int result = r.colAsInt(4);
        assertEquals(42, result);
    }

    @Test
    public void testColAsIntNegative() {
        String input = "chr1\t1\tthis\tis\t-42\ttest";
        Row r = createRow(input);
        int result = r.colAsInt(4);
        assertEquals(-42, result);
    }

    @Test
    public void testColAsIntWhenColumnIsNotANumber() {
        Row r = createRow(TEST_LINE);

        exception.expect(java.lang.NumberFormatException.class);

        r.colAsInt(4);
    }

    @Test
    public void testColAsIntWhenColumnIsNotAnIntNumber() {
        Row r = createRow(TEST_LINE_DOUBLE);

        exception.expect(java.lang.NumberFormatException.class);

        r.colAsInt(4);
    }

    @Test
    public void testColAsIntWhenColumnIsEmpty() {
        Row r = createRow(TEST_LINE_EMPTY_COLUMN);

        int result = r.colAsInt(4);
        assertEquals(0, result);
    }

    @Test
    public void testColAsDouble() {
        Row r = createRow(TEST_LINE_DOUBLE);
        double result = r.colAsDouble(4);
        assertEquals(3.14, result, 1e-8);
    }

    @Test
    public void testColAsDoubleNegative() {
        String input = "chr1\t1\tthis\tis\t-3.14\ttest";
        Row r = createRow(input);
        double result = r.colAsDouble(4);
        assertEquals(-3.14, result, 1e-8);
    }

    @Test
    public void testDoubleValue() {
        Row r = createRow(TEST_LINE_DOUBLE);
        double result = r.doubleValue(4);
        assertEquals(3.14, result, 1e-8);
    }

    @Test
    public void testColAsDoubleWhenColumnIsNotANumber() {
        Row r = createRow(TEST_LINE);

        exception.expect(java.lang.NumberFormatException.class);

        r.colAsDouble(4);
    }

    @Test
    public void testColAsDoubleWhenColumnIsEmpty() {
        Row r = createRow(TEST_LINE_EMPTY_COLUMN);
        double result = r.colAsDouble(4);
        assertTrue(Double.isNaN(result));
    }

    @Test
    public void testLongValue() {
        Row r = createRow(TEST_LINE_INT);
        long result = r.longValue(4);
        assertEquals(42L, result);
    }

    @Test
    public void testColAsLong() {
        Row r = createRow(TEST_LINE_INT);
        long result = r.colAsLong(4);
        assertEquals(42L, result);
    }

    @Test
    public void testColAsLongNegative() {
        String input = "chr1\t1\tthis\tis\t-42\ttest";
        Row r = createRow(input);
        long result = r.colAsLong(4);
        assertEquals(-42L, result);
    }

    @Test
    public void testColAsLongWhenColumnIsNotANumber() {
        Row r = createRow(TEST_LINE);

        exception.expect(java.lang.NumberFormatException.class);

        r.colAsLong(4);
    }

    @Test
    public void testColAsLongWhenColumnIsNotAnIntNumber() {
        Row r = createRow(TEST_LINE_DOUBLE);

        exception.expect(java.lang.NumberFormatException.class);

        r.colAsLong(4);
    }

    @Test
    public void testColAsLongWhenColumnIsEmpty() {
        Row r = createRow(TEST_LINE_EMPTY_COLUMN);
        long result = r.colAsLong(4);
        assertEquals(0L, result);
    }

    @Test
    public void testStringValue() {
        Row r = createRow(TEST_LINE);

        String result = r.stringValue(0);
        assertEquals("chr1", result);
    }

    @Test
    public void testStringValueWhenColumnIsEmpty() {
        Row r = createRow(TEST_LINE_EMPTY_COLUMN);

        String result = r.stringValue(4);
        assertEquals("", result);
    }

    @Test
    public void testStringValueWhenLastColumnIsEmpty() {
        Row r = createRow(TEST_LINE_EMPTY_LAST_COLUMN);

        String result = r.stringValue(5);
        assertEquals("", result);
    }

    @Test
    public void colAsString() {
        Row r = createRow(TEST_LINE);
        CharSequence result = r.colAsString(4);
        assertEquals("a", result.toString());
    }

    @Test
    public void colAsStringIcelandicCharsSupported() {
        String input = "Þetta er próf - áéíóúýö";
        Row r = createRow("chr1\t1\t" + input);
        assertEquals(input, r.colAsString(2).toString());
    }

    @Test
    public void colAsStringUtf8Supported() {
        String input = "Þetta er próf 이것은 시험이다. これはテストです 这是一个测试 Это тест";
        Row r = createRow("chr1\t1\t" + input);
        assertEquals(input, r.colAsString(2).toString());
    }
    @Test
    public void testGetAllCols() {
        Row r = createRow(TEST_LINE);
        String result = r.getAllCols().toString();
        assertEquals(TEST_LINE, result);
    }

    @Test
    public void testOtherCols() {
        Row r = createRow(TEST_LINE);
        String result = r.otherCols();
        assertEquals("this\tis\ta\ttest", result);
    }

    @Test
    public void testColsSliceWhenSingleColumnSliced() {
        Row r = createRow(TEST_LINE);
        String result = r.colsSlice(3, 4).toString();
        assertEquals("is", result);
    }

    @Test
    public void testColsSliceWhenMultipleColumnsSliced() {
        Row r = createRow(TEST_LINE);
        String result = r.colsSlice(3, 6).toString();
        assertEquals("is\ta\ttest", result);
    }

    @Test
    public void testNumCols() {
        Row r = createRow(TEST_LINE);

        int result = r.numCols();
        assertEquals(6, result);
    }

    @Test
    public void testLength() {
        Row r = createRow(TEST_LINE);

        int result = r.length();
        assertEquals(TEST_LINE.length(), result);
    }

    @Test
    public void testSelectedColumnsWithFirstColumn() {
        Row r = createRow(TEST_LINE);

        int[] columns = {0};
        CharSequence result = r.selectedColumns(columns);
        assertEquals("chr1", result);
    }

    @Test
    public void testSelectedColumnsWithMultipleColumns() {
        Row r = createRow(TEST_LINE);

        int[] columns = {0, 2, 3};
        CharSequence result = r.selectedColumns(columns);
        assertEquals("chr1\tthis\tis", result);
    }

    @Ignore("otherColsLength seems to be off by one")
    @Test
    public void testOtherColsLength() {
        Row r = createRow(TEST_LINE);

        int result = r.otherColsLength();
        int expected = r.otherCols().length();
        assertEquals(expected, result);
    }

    @Test
    public void testAddSingleColumnToRow() {
        Row r = createRow(TEST_LINE);

        r.addSingleColumnToRow("bingo");
        assertEquals(7, r.numCols());
        assertEquals("bingo", r.colAsString(6).toString());
    }

    @Test
    public void testAddSingleColumnToRowWhenValueContainsTab() {
        Row r = createRow(TEST_LINE);

        // todo: Should this be allowed? Should we even check for it?
        r.addSingleColumnToRow("bingo\tbongo");
        assertEquals(7, r.numCols());
        assertEquals("bingo\tbongo", r.colAsString(6).toString());
    }

    @Test
    public void testSlicedRow() {
        Row r = createRow(TEST_LINE);

        Row r2 = r.slicedRow(3, 5);
        assertEquals(r.chr, r2.chr);
        assertEquals(r.pos, r2.pos);
        assertEquals("chr1\t1\tis\ta", r2.getAllCols().toString());
    }

    @Test
    public void rowWithSelectedColumns() {
        Row r = createRow(TEST_LINE);

        int[] columns = {0, 1, 2, 4, 5};
        Row r2 = r.rowWithSelectedColumns(columns);
        assertEquals(r.chr, r2.chr);
        assertEquals(r.pos, r2.pos);
        assertEquals("chr1\t1\tthis\ta\ttest", r2.getAllCols().toString());
    }

    @Test
    public void rowWithSelectedColumnsWhenOutOfOrder() {
        Row r = createRow(TEST_LINE);

        int[] columns = {0, 1, 4, 5, 2};
        Row r2 = r.rowWithSelectedColumns(columns);
        assertEquals(r.chr, r2.chr);
        assertEquals(r.pos, r2.pos);
        assertEquals("chr1\t1\ta\ttest\tthis", r2.getAllCols().toString());
    }

    @Test
    public void rowWithSelectedColumnsReplacingPos() {
        final Row r0 = createRow("chr1\t0\t42\tsome data");
        int[] columns = {0, 2, 3};
        final Row row = r0.rowWithSelectedColumns(columns);
        assertEquals("chr1", row.chr);
        assertEquals(42, row.pos);
        assertEquals("chr1\t42\tsome data", row.getAllCols().toString());
    }

    @Test
    public void rowWithSelectedColumnsReplacingChrom() {
        final Row r0 = createRow("chr1\t0\t42\tsome data\tchr21");
        int[] columns = {4, 1, 3};
        final Row row = r0.rowWithSelectedColumns(columns);
        assertEquals("chr21", row.chr);
        assertEquals(0, row.pos);
        assertEquals("chr21\t0\tsome data", row.getAllCols().toString());
    }

    @Test
    public void testSa() {
        Row r = createRow(TEST_LINE);

        int result = r.sa(3);
        assertEquals(14, result);
    }

    @Test
    public void testResizeWhenAddingOneColumn() {
        Row r = createRow(TEST_LINE);
        r.resize(7);

        assertEquals(7, r.numCols());
    }

    @Ignore("It seems it's assumed columns are being added, never removed")
    @Test
    public void testResizeWhenRemovingOneColumn() {
        Row r = createRow(TEST_LINE);
        r.resize(5);

        assertEquals(5, r.numCols());
    }

    @Test
    public void testSetColumnWhenColumnHasJustBeenAdded() {
        Row r = createRow(TEST_LINE);
        r.addColumns(1);
        r.setColumn(4, "bingo");

        String result = r.stringValue(6);
        assertEquals("bingo", result);
    }

    @Test
    public void testSetColumnWhenColumnIsInTheMiddle() {
        Row r = createRow(TEST_LINE);
        r.setColumn(2, "bingo");

        String result = r.getAllCols().toString();
        assertEquals("chr1\t1\tthis\tis\tbingo\ttest", result);
        assertEquals("is", r.stringValue(3));
        assertEquals("bingo", r.stringValue(4));
        assertEquals("test", r.stringValue(5));
    }

    @Test
    public void setColumnsDiscrete() {
        Row r = createRow(TEST_LINE);
        int[] colsToReplace = {2, 4};
        String[] values = {"bingo", "bongo"};

        r.setColumns(colsToReplace, values);
        String result = r.getAllCols().toString();
        assertEquals("chr1\t1\tbingo\tis\tbongo\ttest", result);
        assertEquals("bingo", r.stringValue(2));
        assertEquals("is", r.stringValue(3));
        assertEquals("bongo", r.stringValue(4));
        assertEquals("test", r.stringValue(5));
    }

    @Test
    public void setColumnsLastColumn() {
        Row r = createRow(TEST_LINE);
        int[] colsToReplace = {5};
        String[] values = {"bingo"};

        r.setColumns(colsToReplace, values);
        String result = r.getAllCols().toString();
        assertEquals("chr1\t1\tthis\tis\ta\tbingo", result);
        assertEquals("this", r.stringValue(2));
        assertEquals("is", r.stringValue(3));
        assertEquals("a", r.stringValue(4));
        assertEquals("bingo", r.stringValue(5));
    }

    @Test
    public void setColumnsContiguous() {
        Row r = createRow(TEST_LINE);
        int[] colsToReplace = {2,3,4,5};
        String[] values = {"bingo", "bongo", "foo", "bar"};

        r.setColumns(colsToReplace, values);
        String result = r.getAllCols().toString();
        assertEquals("chr1\t1\tbingo\tbongo\tfoo\tbar", result);
        assertEquals("bingo", r.stringValue(2));
        assertEquals("bongo", r.stringValue(3));
        assertEquals("foo", r.stringValue(4));
        assertEquals("bar", r.stringValue(5));
    }

    @Test
    public void testAddColumns() {
        Row r = createRow(TEST_LINE);
        r.addColumns(1);
        assertEquals(7, r.numCols());
    }

    // After adding column, its value is undefined until setColumn is called.
    // We should still be able to get the contents of the row (for error reporting)
    // without it causing issues.
    @Test
    public void testAddColumnsAddedColumnDoesNotCauseNullException() {
        Row r = createRow(TEST_LINE);
        int lenBefore = r.length();

        r.addColumns(1);
        assertTrue(r.length() >= lenBefore);
        assertTrue(r.getAllCols().length() >= lenBefore);
    }

    @Test
    public void testRemoveColumn() {
        Row r = createRow(TEST_LINE);
        r.removeColumn(4);
        assertEquals(5, r.numCols());
        assertEquals("chr1\t1\tthis\tis\ttest", r.getAllCols().toString());
    }

    @Test
    public void testPeekAtColumn() {
        Row r = createRow(TEST_LINE);
        char result = r.peekAtColumn(4);
        assertEquals('a', result);
    }

    @Test
    public void testRowWithAddedColumnWhenValueIsSingleColumn() {
        Row r = createRow(TEST_LINE);
        Row r2 = r.rowWithAddedColumn("bingo");
        assertEquals("chr1", r.chr);
        assertEquals(1, r.pos);
        assertEquals(7, r2.numCols());
        assertEquals(TEST_LINE + "\tbingo", r2.getAllCols().toString());
    }

    @Test
    public void testRowWithAddedColumnWhenValueIsMultipleColumns() {
        Row r = createRow(TEST_LINE);
        Row r2 = r.rowWithAddedColumn("bingo\tbongo\tfoo\tbar");
        assertEquals("chr1", r.chr);
        assertEquals(1, r.pos);
        assertEquals(10, r2.numCols());
        assertEquals(TEST_LINE + "\tbingo\tbongo\tfoo\tbar", r2.getAllCols().toString());
    }

    @Test
    public void rowWithAddedColumns() {
        Row r = createRow(TEST_LINE);
        String[] addedColumns = {"bingo", "bongo", "foo", "bar"};
        Row r2 = r.rowWithAddedColumns(addedColumns);
        assertEquals("chr1", r.chr);
        assertEquals(1, r.pos);
        assertEquals(10, r2.numCols());
        assertEquals(TEST_LINE + "\tbingo\tbongo\tfoo\tbar", r2.getAllCols().toString());
    }

    @Test
    public void joinedWithSlice() {
        final Row r1 = createRow("chr1\t1\tabc");
        final Row r2 = createRow("chr1\t1\tdef");
        final Row row = r1.joinedWithSlice(r2, 2, 3);
        assertEquals("chr1", row.chr);
        assertEquals(1, row.pos);
        assertEquals("chr1\t1\tabc\tdef", row.getAllCols().toString());
        assertEquals("abc", row.stringValue(2));
        assertEquals("def", row.stringValue(3));
    }

    @Test
    public void joinedWithSlice2() {
        final Row r1 = createRow("chr1\t1\tabc");
        final Row r2 = createRow("chr1\t1\tdef");
        r2.addColumns(2);
        r2.setColumn(1, "bingo");
        r2.setColumn(2, "bongo");

        final Row row = r1.joinedWithSlice(r2, 2, 5);
        assertEquals("chr1", row.chr);
        assertEquals(1, row.pos);
        assertEquals("chr1\t1\tabc\tdef\tbingo\tbongo", row.getAllCols().toString());
        assertEquals("abc", row.stringValue(2));
        assertEquals("def", row.stringValue(3));
        assertEquals("bingo", row.stringValue(4));
        assertEquals("bongo", row.stringValue(5));
    }

    @Test
    public void joinedWithSliceAndAddedColumn() {
        final Row r1 = createRow("chr1\t1\tabc");
        final Row r2 = createRow("chr1\t1\tdef");
        final Row row = r1.joinedWithSliceAndAddedColumn("bingo", r2, 2, 3);
        assertEquals("chr1", row.chr);
        assertEquals(1, row.pos);
        assertEquals("chr1\t1\tabc\tbingo\tdef", row.getAllCols().toString());
        assertEquals("abc", row.stringValue(2));
        assertEquals("bingo", row.stringValue(3));
        assertEquals("def", row.stringValue(4));
    }

    @Test
    public void copyReturnsNonNull() {
        Row r = createRow(TEST_LINE);
        Row r2 = r.copyRow();

        assertNotNull(r2);
    }

    @Test
    public void copyRowsAreEqual() {
        Row r = createRow(TEST_LINE);
        Row r2 = r.copyRow();

        assertEquals(r, r2);
    }

    @Test
    public void copyChangingOriginalDoesNotAffectCopy() {
        Row r = createRow(TEST_LINE);
        Row r2 = r.copyRow();
        r.setColumn(2, "bingo");

        assertEquals(r.chr, r2.chr);
        assertEquals(r.pos, r2.pos);
        assertEquals("chr1\t1\tthis\tis\tbingo\ttest", r.getAllCols().toString());
        assertEquals(TEST_LINE, r2.getAllCols().toString());
    }

    @Test
    public void copyChangingOriginalDoesNotAffectCopyAfterColumnValuesHaveBeenPopulated() {
        Row r = createRow(TEST_LINE);
        r.numCols();
        Row r2 = r.copyRow();
        r.setColumn(2, "bingo");

        assertEquals("chr1\t1\tthis\tis\tbingo\ttest", r.getAllCols().toString());
        assertEquals(TEST_LINE, r2.getAllCols().toString());
    }

    @Test
    public void nanIsReadCorrectly() {
        String input = "chr1\t1\tNaN";
        Row r = createRow(input);
        double v = r.colAsDouble(2);
        assertTrue(Double.isNaN(v));
    }

    @Test
    public void nanIsReadCorrectlyWithLeadingSpaces() {
        String input = "chr1\t1\t  NaN";
        Row r = createRow(input);
        double v = r.colAsDouble(2);
        assertTrue(Double.isNaN(v));
    }

    @Test
    public void infinityIsReadCorrectly() {
        String input = "chr1\t1\tInfinity";
        Row r = createRow(input);
        double v = r.colAsDouble(2);
        assertTrue(Double.isInfinite(v));
        assertTrue(v > 0.0);
    }

    @Test
    public void negativeInfinityIsReadCorrectly() {
        String input = "chr1\t1\t-Infinity";
        Row r = createRow(input);
        double v = r.colAsDouble(2);
        assertTrue(Double.isInfinite(v));
        assertTrue(v < 0.0);
    }

    @Test
    public void infinityIsReadCorrectlyWithLeadingSpaces() {
        String input = "chr1\t1\t    Infinity";
        Row r = createRow(input);
        double v = r.colAsDouble(2);
        assertTrue(Double.isInfinite(v));
        assertTrue(v > 0.0);
    }

    @Test
    public void negativeInfinityIsReadCorrectlyWithLeadingSpaces() {
        String input = "chr1\t1\t    -Infinity";
        Row r = createRow(input);
        double v = r.colAsDouble(2);
        assertTrue(Double.isInfinite(v));
        assertTrue(v < 0.0);
    }
}
