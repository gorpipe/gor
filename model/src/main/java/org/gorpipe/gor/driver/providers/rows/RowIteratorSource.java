package org.gorpipe.gor.driver.providers.rows;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.model.GenomicIterator;

public abstract class RowIteratorSource implements DataSource {
    protected SourceReference sourceReference;

    public RowIteratorSource(SourceReference sourceReference) {
        this.sourceReference = sourceReference;
    }

    public abstract  GenomicIterator open();

    public abstract GenomicIterator open(String filter);

    public abstract boolean supportsFiltering();

    public boolean isDirect() {
        return true;
    }
}
