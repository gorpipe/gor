package org.gorpipe.gor.table.files;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.Table;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.util.DataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * LinkTable consist of a link and the the actual data.
 *
 * The link is used as the a standard table and is named and located like a standard table.  The actual data might be
 * in S3 or a data folder and should normally not be used directly.
 *
 * The link table is updated by creating new data and then update the link. Using link tables allow us to update data
 * using S3 in simlilar way has is standard practice in NFS (create new temp file and then to an atomic move from the
 * temp file to the data file)
 *
 */
public class DictionaryLinkTable implements Table {

    private static final Logger log = LoggerFactory.getLogger(DictionaryLinkTable.class);

    private URI linkUri;            // Path to the link
    private URI linkedTableURI;     // Content of the link
    private URI replacementUri;     // new content of the link (before commit)

    private DictionaryTable linkedTable;
    private FileReader fileReader;


    public DictionaryLinkTable(URI uri, FileReader inputFileReader) {
        this.linkUri = uri;
        setFileReader(inputFileReader);
        linkedTableURI = readLinkContent(uri, inputFileReader);
        if (linkedTableURI != null) {
            linkedTable = new DictionaryTable(linkedTableURI, fileReader);
        }

    }

    /**
     * Make this link table point to a new external dict.
     * @param uri
     */
    public void link(URI uri) {
        replacementUri = uri;
    }

    public void deleteUnderlying() {
        linkedTable.delete();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPath() {
        return linkedTable.getPath();
    }

    @Override
    public String getFolderPath() {
        return linkedTable.getFolderPath();
    }

    @Override
    public String[] getColumns() {
        return linkedTable.getColumns();
    }

    @Override
    public String getProperty(String key) {
        return linkedTable.getProperty(key);
    }

    @Override
    public void setProperty(String key, String value) {
        linkedTable.setProperty(key, value);
    }

    @Override
    public boolean containsProperty(String key) {
        return linkedTable.containsProperty(key);
    }

    @Override
    public Stream<String> getLines() {
        return linkedTable.getLines();
    }

    @Override
    public void insert(Collection lines) {
        throw new GorSystemException("Can not insert entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void insert(String... lines) {
        throw new GorSystemException("Can not insert entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void delete(Collection lines) {
        throw new GorSystemException("Can not insert entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void delete(String... lines) {
        throw new GorSystemException("Can not insert entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void reload() {
        linkedTable.reload();
    }

    @Override
    public void save() {
        commitRequest();
        commit();
    }

    @Override
    public void delete() {
        deleteUnderlying();
        try {
            linkedTable.getFileReader().delete(linkUri.toString());
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    @Override
    public void deleteEntries(Collection collection) {
        throw new GorSystemException("Can not insert entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void insertEntries(Collection collection) {
        throw new GorSystemException("Can not insert entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void commitRequest() {
        
    }

    @Override
    public void commit() {
        if (replacementUri != null) {
            // TODO:  Update new meta from old meta.

            // Update the link.
            writeLink(replacementUri.toString());

            // Delete the old folder.
            deleteUnderlying();

            linkedTableURI = replacementUri;
            replacementUri = null;
            linkedTable = new DictionaryTable(linkedTableURI, fileReader);
        }
    }

    private void writeLink(String content) {
        try (OutputStreamWriter os = new OutputStreamWriter(linkedTable.getFileReader().getOutputStream(linkUri.toString(), false))) {
            os.write(content);
        } catch (IOException e) {
            throw new GorResourceException("Could not update", content, e);
        }
    }

    @Override
    public void initialize() {
        linkedTable.initialize();
        if (!fileReader.exists(linkUri.toString())) {
            writeLink(linkedTableURI.toString());
        }
    }

    @Override
    public String getRootPath() {
        return linkedTable.getRootPath();
    }

    @Override
    public String getSecurityContext() {
        return linkedTable.getSecurityContext();
    }

    @Override
    public void setColumns(String[] columns) {
        linkedTable.setColumns(columns);
    }

    @Override
    public void setFileReader(FileReader fileReader) {
        this.fileReader = fileReader;
    }

    private static URI readLinkContent(URI uri, FileReader fileReader) {
        try {
            return URI.create(fileReader.readLinkContent(uri.toString()));

        } catch (Exception e) {
            // Does not exist, need to create a dummy one (so we for example can participate in trans).                 
            return URI.create(DataUtil.toFile(String.format("/tmp/dummy_%s", UUID.randomUUID()), DataType.GORD));
        }
    }
}
