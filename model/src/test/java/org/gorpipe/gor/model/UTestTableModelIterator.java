package org.gorpipe.gor.model;

import org.junit.Assert;
import org.junit.Test;

import javax.swing.table.AbstractTableModel;

import static org.junit.Assert.*;

public class UTestTableModelIterator {
    class TestTableModel extends AbstractTableModel {
        // TableModel's column names
        private String[] columnNames = {
                "Chrom", "Pos", "Data"
        };

        // TableModel's data
        private Object[][] data = {
                {"chr1", 1, 1},
                {"chr1", 2, 2},
                {"chr1", 3, 3},
                {"chr2", 1, 21},
                {"chr2", 2, 22},
                {"chr3", 1, 31},
                {"chr3", 2, 32},
        };

        /**
         * Returns the number of rows in the table model.
         */
        public int getRowCount() {
            return data.length;
        }

        /**
         * Returns the number of columns in the table model.
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns the column name for the column index.
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * Returns data type of the column specified by its index.
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        /**
         * Returns the value of a table model at the specified
         * row index and column index.
         */
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data[rowIndex][columnIndex];
        }
    }

    @Test
    public void getHeader() {
        TableModelIterator iterator = new TableModelIterator(new TestTableModel());
        String header = iterator.getHeader();
        String expected = "Chrom\tPos\tData";
        Assert.assertEquals(expected, header);
    }

    @Test
    public void simpleIteration() {
        TableModelIterator iterator = new TableModelIterator(new TestTableModel());

        StringBuilder accumulator = new StringBuilder();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            accumulator.append(row.toString());
            accumulator.append("\n");
        }

        String expected = "chr1\t1\t1\t\n" +
                "chr1\t2\t2\t\n" +
                "chr1\t3\t3\t\n" +
                "chr2\t1\t21\t\n" +
                "chr2\t2\t22\t\n" +
                "chr3\t1\t31\t\n" +
                "chr3\t2\t32\t\n";
        Assert.assertEquals(expected, accumulator.toString());
    }

    @Test
    public void seek() {
        TableModelIterator iterator = new TableModelIterator(new TestTableModel());
        iterator.seek("chr2", 2);

        StringBuilder accumulator = new StringBuilder();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            accumulator.append(row.toString());
            accumulator.append("\n");
        }

        String expected = "chr2\t2\t22\t\n" +
                "chr3\t1\t31\t\n" +
                "chr3\t2\t32\t\n";
        Assert.assertEquals(expected, accumulator.toString());
    }
}