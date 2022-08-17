package org.gorpipe.gor.table.files;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
import org.gorpipe.gor.table.dictionary.DictionaryTable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Collection;

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
public class DictionaryLinkTable extends DictionaryTable {

    private URI linkUri;
    private URI replacementUri;


    public DictionaryLinkTable(URI uri, FileReader inputFileReader)  {
        super(readLinkContent(uri, inputFileReader), inputFileReader);
        this.linkUri = uri;
    }

    /**
     * Make this link table point to a new external dict.
     * @param uri
     */
    public void link(URI uri) {
        replacementUri = uri;
    }

    @Override
    public void insert(Collection<DictionaryEntry> lines) {
        throw new GorSystemException("Can not insert entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void delete(Collection<DictionaryEntry> lines) {
        throw new GorSystemException("Can not delete entries into DictionaryLinkTable, use replace(new dict)", null);
    }

    @Override
    public void initialize() {

    }

    @Override
    public void commitRequest() {
        
    }

    @Override
    public void commit() {
        if (replacementUri != null) {
            // TODO:  Update new meta from old meta.

            // Update the link.
            try (OutputStreamWriter os = new OutputStreamWriter(fileReader.getOutputStream(linkUri.toString(), false))) {
                os.write(replacementUri.toString());
            } catch (IOException e) {
                throw new GorResourceException("Could not update", replacementUri.toString(), e);
            }
        }
    }

    private static URI readLinkContent(URI uri, FileReader fileReader) {
        String[] linkContent;
        try {
            linkContent = fileReader.readAll(uri.toString());
        } catch (IOException e) {
            throw new GorResourceException("Could not read link", uri.toString(), e);
        }
        return URI.create(linkContent[0]);
    }
}
