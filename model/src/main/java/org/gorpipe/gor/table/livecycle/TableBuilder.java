package org.gorpipe.gor.table.livecycle;

import org.gorpipe.gor.model.FileReader;

import java.net.URI;
import java.nio.file.Path;

public class TableBuilder<B extends TableBuilder<B>>  {

    public String path;
    public Boolean useHistory;
    public Boolean validateFiles;
    public FileReader fileReader;
    public String id;
    public Boolean useEmbededHeader = null;

    public TableBuilder(String path) {
        this.path = path;
    }

    public TableBuilder(Path path) {
        this(path.toString());
    }

    public TableBuilder(URI path) {
        this(path.toString());
    }

    protected final B self() {
        return (B) this;
    }

    public B fileReader(FileReader val) {
        this.fileReader = val;
        return self();
    }

    public B useHistory(boolean useHistory) {
        this.useHistory = useHistory;
        return self();
    }

    public B validateFiles(boolean val) {
        this.validateFiles = val;
        return self();
    }

    public B id(String val) {
        this.id = val;
        return self();
    }

    public B embeddedHeader(boolean val) {
        this.useEmbededHeader = val;
        return self();
    }


}
