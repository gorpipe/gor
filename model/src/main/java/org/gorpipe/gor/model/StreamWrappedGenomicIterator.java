package org.gorpipe.gor.model;

import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;

import java.util.Iterator;
import java.util.stream.Stream;

public class StreamWrappedGenomicIterator implements GenomicIterator {
    private String header;
    private String[] types;
    private boolean isFirstLine = true;
    private final boolean hasHeader;
    private final Stream<String> data;
    private final Iterator<String> iterator;

    public StreamWrappedGenomicIterator(Stream<String> data, String header, boolean hasHeader) {
        this.data = data;
        this.iterator = data.iterator();
        this.hasHeader = hasHeader;
        this.header =  header;
    }

    @Override
    public void init(GorSession session) {
        // not needed
    }

    @Override
    public void setContext(GorContext context) {
        // not needed
    }

    @Override
    public String getSourceName() {
        return null;
    }

    @Override
    public void setSourceName(String sourceName) {
        // not needed
    }

    @Override
    public boolean isSourceAlreadyInserted() {
        return false;
    }

    @Override
    public void setSourceAlreadyInserted(boolean sourceAlreadyInserted) {
        // not needed
    }

    @Override
    public String getHeader() {
        if (header != null) {
            return header;
        }
        readHeader();
        return header;
    }

    @Override
    public String[] getAdditionalInfo() {
        return new String[0];
    }

    @Override
    public void setHeader(String header) {
        this.header = header;
    }

    @Override
    public String[] getTypes() {
        return types;
    }

    @Override
    public void setTypes(String[] types) {
        this.types = types;
    }

    @Override
    public boolean seek(String chr, int pos) {
        return false;
    }

    @Override
    public void close() {
        data.close();
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public Row next() {
        readHeader();
        return new RowBase(iterator.next(), true);
    }

    private void readHeader() {
        if (isFirstLine) {
            isFirstLine = false;
            if (hasHeader) {
                header = iterator.next();
            }
        }
    }
}
