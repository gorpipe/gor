package org.gorpipe.gor.model;

import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;

public class EmptyIterator implements GenomicIterator {
    private String header;
    private String[] types;

    public EmptyIterator(String header) {
        this.header = header;
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
        return header;
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
        // not needed
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Row next() {
        return null;
    }
}
