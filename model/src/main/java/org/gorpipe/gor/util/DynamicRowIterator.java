package org.gorpipe.gor.util;

import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;

import java.util.ArrayList;
import java.util.List;

public class DynamicRowIterator extends GenomicIteratorBase {

    int index = 0;
    List<Row> rows = new ArrayList<>();

    public void addRow(Row row) {
        rows.add(row);
    }

    public void addRow(String name, String value) {
        rows.add(new RowBase("chrN\t0\t" + name + "\t" + value));
    }

    @Override
    public boolean seek(String chr, int pos) {
        return false;
    }

    @Override
    public void close() {}

    @Override
    public boolean hasNext() {
        return index < rows.size();
    }

    @Override
    public Row next() {
        return rows.get(index++);
    }

    public void merge(GenomicIteratorBase iterator) {
        while(iterator.hasNext()) {
            rows.add(iterator.next());
        }
    }
}
