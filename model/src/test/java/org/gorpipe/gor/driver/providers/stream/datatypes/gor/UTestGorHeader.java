package org.gorpipe.gor.driver.providers.stream.datatypes.gor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UTestGorHeader {
    @Test
    public void getColumnsReturnsEmptyForNewInstance() {
        GorHeader gh = new GorHeader();

        String[] columns = gh.getColumns();

        assertEquals(0, columns.length);
    }

    @Test
    public void columnsSetWhenConstructingWithArray() {
        String[] columns = {"Chrom", "Pos", "Data"};
        GorHeader gh = new GorHeader(columns);

        assertEquals(3, columns.length);
        assertEquals("Chrom", columns[0]);
        assertEquals("Pos", columns[1]);
        assertEquals("Data", columns[2]);
    }

    @Test
    public void addColumnWhenAddingSingleColumn() {
        GorHeader gh = new GorHeader();
        gh.addColumn("Chrom");

        String[] columns = gh.getColumns();
        String[] types = gh.getTypes();

        assertEquals(1, columns.length);
        assertEquals(1, types.length);
        assertEquals("Chrom", columns[0]);
        assertEquals("", types[0]);
    }

    @Test
    public void addColumnWithTypesWhenAddingSingleColumn() {
        GorHeader gh = new GorHeader();
        gh.addColumn("Chrom", "S");

        String[] columns = gh.getColumns();
        String[] types = gh.getTypes();

        assertEquals(1, columns.length);
        assertEquals("Chrom", columns[0]);
        assertEquals(1, types.length);
        assertEquals("S", types[0]);
    }

    @Test
    public void addColumnWhenAddingMultipleColumns() {
        GorHeader gh = new GorHeader();
        gh.addColumn("Chrom");
        gh.addColumn("Pos");
        gh.addColumn("Data");

        String[] columns = gh.getColumns();

        assertEquals(3, columns.length);
        assertEquals("Chrom", columns[0]);
        assertEquals("Pos", columns[1]);
        assertEquals("Data", columns[2]);
    }

    @Test
    public void toStringWithSingleColumn() {
        GorHeader gh = new GorHeader();
        gh.addColumn("Chrom");

        String s = gh.toString();
        assertEquals("Chrom", s);
    }
}