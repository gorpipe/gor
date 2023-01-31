package gorsat.process;

import org.eclipse.jetty.util.resource.Resource;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.util.StringUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NordFile {
    private List<NordIteratorEntry> entries = new ArrayList<>();
    private NordIteratorEntry firstEntry;
    private Map<String,String> properties = new HashMap<>();
    private Path fileName;
    private boolean useFilter;
    private Set<String> filterEntries;
    private boolean ignoreMissing;

    public void load(FileReader reader,
              Path nordFile,
              boolean useFilter,
              String[] filterEntries,
              boolean ignoreMissingEntries) {

        if (fileName != null) {
            throw new GorResourceException(String.format("Nord file %s has already been parsed.", nordFile),
                    nordFile.toString());
        }

        this.useFilter = useFilter;
        this.fileName = nordFile;
        this.ignoreMissing = ignoreMissingEntries;

        this.filterEntries = new HashSet<>();
        this.filterEntries.addAll(Arrays.asList(filterEntries));

        try (Stream<String> nordStream = reader.iterateFile(nordFile.toString(), 0, false,true)) {
            List<NordIteratorEntry> nordEntries = nordStream
                    .peek(this::processProperty)
                    .filter(x -> !x.startsWith("#"))// Filter out all lines starting with #
                    .map(NordIteratorEntry::parse) // Convert to dictionary entry
                    .peek(this::addFirstEntry)
                    .filter(x -> !this.useFilter || this.filterEntries.contains(x.tag())) // If no filter is set pass everything through or only entries in the filter list
                    .collect(Collectors.toList());

            if (this.filterEntries.size() > 0 && !this.ignoreMissing && nordEntries.size() < this.filterEntries.size()) {
                throw new GorParsingException("Missing entries in dictionary file: "
                        + getMissingEntries(nordEntries, this.filterEntries));
            }

            entries = nordEntries;
        } catch (IOException ioe) {
            throw new GorResourceException("Failed to open nor dictionary file: " + nordFile, nordFile.toString(), ioe);
        }
    }

    public List<NordIteratorEntry> entries() {
        return this.entries;
    }

    public Map<String, String> properties() {
        return this.properties;
    }

    public Path fileName() {
        return this.fileName;
    }

    public boolean ignoreMissing() {
        return this.ignoreMissing;
    }

    public String[] filterEntries() {
        var result = new String[this.filterEntries.size()];
        this.filterEntries.toArray(result);
        return result;
    }

    public boolean useFilter() {
        return this.useFilter;
    }

    public NordIteratorEntry firstEntry() {
        return this.firstEntry;
    }

    public static NordFile fromList(String[] files) {
        if (files.length == 0) {
            throw new GorResourceException("No files supplied to nord file", "");
        }

        var nordFile = new NordFile();
        nordFile.entries = Stream.of(files).map(x -> x+"\tA").map(NordIteratorEntry::parse).collect(Collectors.toList());
        nordFile.fileName = Path.of(".");
        nordFile.filterEntries = new HashSet<>();
        nordFile.firstEntry = nordFile.entries.get(0);
        nordFile.useFilter = false;
        nordFile.ignoreMissing = false;
        return nordFile;
    }

    private void processProperty(String headerEntry) {
        if (StringUtil.isEmpty(headerEntry) || !headerEntry.startsWith("##"))
            return;

        String[] entries = headerEntry.split("=");

        if (entries.length >= 2) {
            properties.put(entries[0].substring(2), headerEntry.substring(entries[0].length()+1) );
        }
    }

    private void addFirstEntry(NordIteratorEntry entry) {
        if (firstEntry == null) firstEntry = entry;
    }

    private String getMissingEntries(List<NordIteratorEntry> nordEntries, Set<String> filterEntriesSet) {
        Set<String> missingEntries = new HashSet<>(filterEntriesSet);
        nordEntries.stream().map(NordIteratorEntry::tag).toList().forEach(missingEntries::remove);
        return String.join(",", missingEntries);
    }
}
