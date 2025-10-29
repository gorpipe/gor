package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.FileReader;

import java.io.IOException;
import java.util.List;

/**
 * Old link file format, version 0.
 */
public class LinkFileV0 extends LinkFile {

    /**
     * Load from a source, if it exists, otherwise create an empty link file.
     *
     * @param source the source to load from
     */
    public LinkFileV0(StreamSource source) throws IOException {
        super(source, loadContentFromSource(source));
    }

    protected LinkFileV0(StreamSource source, LinkFileMeta meta, String content) {
        super(source, meta, content);
    }

    @Override
    protected String getHeader() {
        return "";
    }

    @Override
    protected List<LinkFileEntry> parseEntries(String content) {
        return LinkFileEntryV0.parse(content);
    }

    @Override
    public LinkFile appendEntry(String link, String md5, String info, FileReader reader) {
        entries.clear(); // V0 does not support multiple entries, so we clear the list
        entries.add(new LinkFileEntryV0(link));
        return this;
    }
}
