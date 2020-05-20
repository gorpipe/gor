/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat.process;

import org.gorpipe.model.genome.files.gor.FileReader;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.Dictionary;
import org.gorpipe.gor.GorSession;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.RowSource;
import org.gorpipe.model.util.StringUtil;
import gorsat.Iterators.NorInputSource;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Iterator to access data from a nor dictionary file.
 *
 * <p>Nor dictionary file is a file to tag mapping where the iterator represents the data as a continuous stream. The nor
 * dictionary iterator supports filtering from an input filter array. An empty filter array indicates no filter. Results
 * from the nor dictionary iterator contains the original source plus a source column which contains the originating tag
 * for the row. Filters can be silenced and a custom source column name can be applied. All files in the nor dictionary
 * are relative to the dictionary file unless a full path is specified.
 *
 * <p>Nor dictionary supports header which starts with #. All lines starting with # are ignored from the dictionary. All
 * header entries starting with ##[key]=[value] are treated as key value pairs. Nord files support the 'Source' keyword
 * for naming the source column. If no Source reference is set in the nor dictionary file or no source is set with the
 * -s option, the source column is not shown.
 *
 * <p>Example of a nor dictionary:
 *
 *
 * <p><p>##Source=phenotype
 * <p>#Source\tTag\n
 * <p>./nor/file1.tsv\tPatient_1\n
 * <p>./nor/file2.tsv\tPatient_2\n
 * <p>./nor/file3.tsv\tPatient_3\n
 * <p>#./nor/file4.tsv\tPatient_4\n   -> Ignored
 *
 * <p><p>Nor dictionaries do not support bucketization as there only one file open at a time.
 *
 */
public class NordIterator extends RowSource {

    private Map<String,String> properties = new HashMap<>();
    private final boolean useFilter;
    private final Set<String> filterEntries;
    private final String nordFile;
    private String sourceColumnName;
    private String projectRoot = "";
    private FileReader fileReader;
    private final boolean ignoreMissingEntries;
    private final boolean forceReadOfHeader;
    private boolean addSourceColumn;
    private boolean showSourceColumn = true;
    private NorInputSource activeIterator;
    private Iterator<NordIteratorEntry> nordEntriesIterator;
    private NordIteratorEntry activeEntry = null;
    private int headerSize = 0;
    private String nordRoot;

    private static final String DEFAULT_SOURCE_COLUMN_NAME = "Source";
    private static final String SOURCE_PROPERTY_NAME = "Source";

    /**
     * Nor dictionary constructor. Creates an instance of nor dictionary iterator, see class description.
     *
     * @param nordFile              Nor dictionary file path
     * @param filterEntries         Array of tags to be inclusively filtered.
     * @param sourceColumnName      Name of the ouput source column
     * @param ignoreMissingEntries  Indicates if missing entries should be ignored or not. Not ignoring missing entries
     *                              will throw a parsing exception.
     * @param forceReadOfHeader     Force the read of headers in the source files.
     */
    public NordIterator(String nordFile,
                        boolean useFilter,
                        String[] filterEntries,
                        String sourceColumnName,
                        boolean ignoreMissingEntries,
                        boolean forceReadOfHeader) {
        this.nordFile = nordFile;
        this.useFilter = useFilter;
        this.filterEntries = new HashSet<>(Arrays.asList(filterEntries));
        this.ignoreMissingEntries = ignoreMissingEntries;
        this.forceReadOfHeader = forceReadOfHeader;
        this.sourceColumnName = sourceColumnName;
    }

    @Override
    public void close() {
        if (activeIterator != null) {
            activeIterator.close();
            activeIterator = null;
        }
    }

    @Override
    public boolean hasNext() {
        while (activeIterator == null || !activeIterator.hasNext()) {
            // Note that we do this in a loop to handle potentially empty files in the middle of the dict
            if(!prepareNextIterator()) {
                return false;
            }
        }

        return activeIterator != null && activeIterator.hasNext();
    }

    @Override
    public Row next() {
        if (activeIterator != null && activeIterator.hasNext()) {
             String extraColumn = "";
             if (showSourceColumn && addSourceColumn) extraColumn = "\t" + activeEntry.getTag();

             return RowObj.StoR(activeIterator.next() + extraColumn);
        }

        return null;
    }

    @Override
    public void setPosition(String seekChr, int seekPos) {
        throw new GorSystemException("Nor dictionary iterator does not support seek", null);
    }

    public void init(GorSession session) {

        this.fileReader = session.getProjectContext().getFileReader();
        this.projectRoot = session.getProjectContext().getRealProjectRoot();

        Path nordPath = Paths.get(this.nordFile);

        // Don't like this, there must be a better way
        if (nordPath.getParent() == null) {
            nordRoot = ".";
        } else {
            nordRoot = nordPath.getParent().toString();
        }

        try (Stream<String> nordStream = this.fileReader.iterateFile(this.nordFile, 0, false)) {
            List<NordIteratorEntry> nordEntries = nordStream
                    .peek(this::processProperty)
                    .filter(x -> !x.startsWith("#"))// Filter out all lines starting with #
                    .map(NordIteratorEntry::parse) // Convert to dictionary entry
                    .filter(x -> !useFilter || filterEntries.contains(x.getTag())) // If no filter is set pass everything through or only entries in the filter list
                    .collect(Collectors.toList());
            nordEntriesIterator = nordEntries.iterator();

            if (filterEntries.size() > 0 && !ignoreMissingEntries && nordEntries.size() < filterEntries.size()) {
                throw new GorParsingException("Missing entries in dictionary file: " + getMissingEntries(nordEntries));
            }
        } catch (IOException ioe) {
            throw new GorResourceException("Failed to open nor dictionary file: " + this.nordFile, this.nordFile, ioe);
        }

        if (StringUtil.isEmpty(this.sourceColumnName)) {
            if (properties.containsKey(SOURCE_PROPERTY_NAME)) {
                this.sourceColumnName = properties.get(SOURCE_PROPERTY_NAME);
            } else {
                showSourceColumn = false;
                this.sourceColumnName = DEFAULT_SOURCE_COLUMN_NAME;
            }
        }

        if(!nordEntriesIterator.hasNext()) {
            getHeaderFromFirstFile();
        }
        prepareNextIterator();
    }

    private void getHeaderFromFirstFile() {
        try (Stream<String> stream = this.fileReader.iterateFile(this.nordFile, 0, false)) {
            final Optional<String> first = stream.filter(x -> !x.startsWith("#")).findFirst();
            if (first.isPresent()) {
                String fileName = NordIteratorEntry.parse(first.get()).getFilePath();
                Path entryPath = Paths.get(fileName);
                if (!entryPath.isAbsolute()) {
                    fileName = Paths.get(this.nordRoot, fileName).toString();
                }
                try (NorInputSource inputSource = new NorInputSource(fileName, this.fileReader, false, this.forceReadOfHeader, 0, false, false)) {
                    getHeaderFromIterator(inputSource);
                }
            }
        } catch (IOException e) {
            throw new GorResourceException("Failed to open nor dictionary file: " + this.nordFile, this.nordFile, e);
        }
    }

    private void processProperty(String headerEntry) {
        if (StringUtil.isEmpty(headerEntry) || !headerEntry.startsWith("##"))
            return;

        String[] entries = headerEntry.split("=");

        if (entries.length >= 2) {
            properties.put(entries[0].substring(2), headerEntry.substring(entries[0].length()+1) );
        }
    }

    private boolean prepareNextIterator() {
        // Close the active file iterator
        close();

        if (nordEntriesIterator.hasNext()) {

            // Read the next nord entry
            activeEntry = nordEntriesIterator.next();

            // Get the file path from entry
            String fileName = activeEntry.getFilePath();
            Path entryPath = Paths.get(fileName);

            if (!entryPath.isAbsolute()) {
                Dictionary.FileReference reference = Dictionary.getDictionaryFileParent(Paths.get(this.projectRoot, this.nordFile), this.projectRoot);
                Dictionary.DictionaryLine line = Dictionary.parseDictionaryLine(activeEntry.toString(), reference);

                if (reference.logical != null)
                    fileName = line.fileRef.logical;
                else
                    fileName = Paths.get(this.nordRoot, fileName).toString();
            }

            // Create the nor iterator
            this.activeIterator = new NorInputSource(fileName, this.fileReader, false, this.forceReadOfHeader, 0, false, false);

            // Test header
            getHeaderFromIterator(this.activeIterator);
            return true;
        } else {
            return false;
        }
    }

    private void getHeaderFromIterator(NorInputSource inputSource) {
        String iteratorHeader = inputSource.getHeader();
        if(iteratorHeader.isEmpty()) {
            throw new GorDataException("Missing header for: " + activeEntry.getTag());
        }

        iteratorHeader = addOptionalSourceColumn(iteratorHeader);

        if (getHeader().isEmpty()) {
            setHeader(iteratorHeader);
            headerSize = iteratorHeader.split("\t").length;
        } else if (iteratorHeader.split("\t").length != headerSize) {
            throw new GorDataException("Header lengths do not match between dictionary files for: " + activeEntry.getTag());
        }
    }

    private String addOptionalSourceColumn(String iteratorHeader) {
        this.addSourceColumn = false;
        if (!iteratorHeader.contains("\t" + this.sourceColumnName)) {
            if (showSourceColumn) iteratorHeader += "\t" + this.sourceColumnName;
            this.addSourceColumn = true;
        }
        return iteratorHeader;
    }

    private String getMissingEntries(List<NordIteratorEntry> nordEntries) {
        Set<String> missingEntries = new HashSet<>(this.filterEntries);
        missingEntries.removeAll(nordEntries.stream().map(NordIteratorEntry::getTag).collect(Collectors.toList()));
        return String.join(",", missingEntries);
    }
}
