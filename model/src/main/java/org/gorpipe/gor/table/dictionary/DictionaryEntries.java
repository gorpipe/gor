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

package org.gorpipe.gor.table.dictionary;

import com.google.common.collect.*;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.table.TableInfo;
import org.gorpipe.gor.table.TableHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that handles the loading caching of and working with table entries.
 *
 * <p>Thread-safety: safe for concurrent <b>reads</b> on a shared instance, including
 * concurrent lazy first-load. Loaders and mutators ({@code insert}/{@code delete}/
 * {@code clear}/{@code loadLinesAndUpdateIndices}/{@code updateTagMap}/{@code updateContentMap})
 * are {@code synchronized}, and the lazily-published state fields are {@code volatile} so the
 * unsynchronized read accessors observe fully-constructed data. Mutation must be externally
 * serialized and must NOT run concurrently with reads on the same instance (structural
 * modification of the backing list would break in-flight iteration); share a read-only instance
 * for concurrent reads and use a separate instance for writes.
 *
 * <p>The public read accessors ({@code getEntries()}/{@code getAllActiveTags()}) return
 * <b>unmodifiable</b> views so callers cannot structurally modify the shared internal state;
 * copy the result if a mutable list is needed (see {@code selectAll()}).
 */
public class DictionaryEntries<T extends DictionaryEntry> implements IDictionaryEntries<T> {
    private static final Logger log = LoggerFactory.getLogger(DictionaryEntries.class);
    private final IDictionaryEntryFactory<T> factory;

    // volatile: lazily built under synchronized builders, but read by unsynchronized accessors
    // (e.g. getEntries()/isLoaded()); volatile gives those reads safe publication.
    private volatile List<T> rawLines;

    // For indices we use hashed values.  Insert into the dict is much much faster, and it takes a lot less space.  Getting data takes
    // a little bit longer as you could get small list of values you need to loop through.
    private volatile ListMultimap<Integer, T> tagHashToLines;  // tags here means aliases and tags.
    private volatile ListMultimap<Integer, T> contentHashToLines;

    private volatile Multiset<String> activeTags;
    private int deletedEntriesCount = 0;  // only mutated/read under (or after) a synchronized builder.
    private final TableInfo table;

    private volatile boolean dataLoaded = false;
    private volatile boolean tagHashLoaded = false;
    private volatile boolean contentMapLoaded = false;

    /**
     * Construct new dict file from the given path and chromosome cache.
     * <p>
     * It is not simple to create objects of type T.  For now we pass in the class so we can do that.
     * Another option would be using google TypeToken, but we had problem getting that working.
     */
    public DictionaryEntries(TableInfo table, IDictionaryEntryFactory<T> factory) {
        this.table = table;
        this.factory = factory;
    }

    @Override
    synchronized public void insert(T line, boolean hasUniqueTags) {
        // Update the line to make it complete and check if double entries are allowed.
        T oldLine;

        if (hasUniqueTags) {
            oldLine = findLineWithTag(line);
        } else {
            oldLine = findLine(line);
        }

        if (oldLine != null) {
            // Updating existing line.
            delete(oldLine, true);
        }

        loadedEntries().add(line);

        addEntryToContentMap(line);
        addEntryToTagMap(line);
    }

    /**
     * Remove line from the table.
     *
     * @param lineToRemove template of the line to remove.
     * @param keepIfBucket if true and the line has bucket we mark the line deleted, else we just remove the line.
     */
    @Override
    synchronized public void delete(T lineToRemove, boolean keepIfBucket) {
        T line = this.findLine(lineToRemove);
        if (line != null) {
            loadedEntries().remove(line);
            removeEntryFromContentMap(line);
            removeEntryFromTagMap(line);

            if (line.hasBucket() && keepIfBucket) {
                // NOTE: the deleted flag is part of the hashCode so we remove and add again if we change it.
                T entry = (T) DictionaryEntry.copy(line);
                entry.setDeleted(true);
                loadedEntries().add(entry);
                addEntryToContentMap(entry);
                addEntryToTagMap(entry);
            }
        }
    }

    @Override
    synchronized public void clear() {
        // We deliberatly set list/maps to null, instead of clearing.  This is because someone could be reading
        // the buffers and we want to let them finish.
        dataLoaded = false;
        this.rawLines = null;
        clearContentMap();
        clearTagMap();
    }

    @Override
    public List<T> getEntries() {
        // Return an unmodifiable view: the backing list is shared across concurrent readers and
        // must not be structurally modified from the outside (see class-level thread-safety note).
        return Collections.unmodifiableList(loadedEntries());
    }

    /**
     * Load the entries if needed and return the live backing list.
     *
     * <p>Reads {@code rawLines} into a single local so the load check and the returned reference are
     * the same read; a separate re-read of the volatile field could observe {@code null} if a
     * concurrent {@code clear()} slipped in (which the contract forbids, but the local read makes the
     * accessor robust anyway). For internal use only — mutators hold the instance lock; read
     * accessors that expose the result to callers must wrap it (see {@link #getEntries()}).
     */
    private List<T> loadedEntries() {
        List<T> lines = this.rawLines;
        if (!this.dataLoaded || lines == null) {
            loadLinesAndUpdateIndices();
            lines = this.rawLines;
        }
        return lines;
    }

    @Override
    public List<T> getEntries(String... aliasesAndTags) {
        List<T> lines2Search = loadedEntries();
        updateTagMap();
        if (tagHashToLines != null && aliasesAndTags != null) {
            // If we have tags and tag map to lines we use that to get a better list of lines to search..
            lines2Search = Arrays.stream(aliasesAndTags).flatMap(t -> tagHashToLines.get(t.hashCode()).stream())
                    .sorted(Comparator.comparing(T::getKey)).distinct()
                    .collect(Collectors.toList());
        }
        return lines2Search;
    }

    @Override
    public Iterator<T> iterator() {
        return getEntries().iterator();
    }

    @Override
    public Iterator<T> getActiveEntries() {
        updateContentMap();
        return new Iterator<T>() {

            int nextIndex = 0;
            int returnedCount = 0;
            @Override
            public boolean hasNext() {
                return returnedCount < rawLines.size() - deletedEntriesCount;
            }

            @Override
            public T next() {
                while (hasNext() && rawLines.get(nextIndex).isDeleted()) {
                    nextIndex++;
                }
                if (hasNext()) {
                    returnedCount++;
                    return rawLines.get(nextIndex++);
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public boolean isLoaded() {
        return this.rawLines != null;
    }

    @Override
    public Set<String> getAllActiveTags() {
        updateContentMap();
        // Read the volatile once and wrap: elementSet() is a live view of the internal multiset and
        // supports remove()/clear(), so it must be made unmodifiable before it escapes to callers.
        Multiset<String> tags = this.activeTags;
        return tags != null ? Collections.unmodifiableSet(tags.elementSet()) : Collections.emptySet();
    }

    @Override
    public boolean hasDeletedEntries() {
        updateContentMap();
        return deletedEntriesCount > 0;
    }

    @Override
    public int size() {
        return loadedEntries().size();
    }

    @Override
    public int getActiveLinesCount() {
        updateContentMap();
        int size = loadedEntries().size();
        return size - deletedEntriesCount;
    }


    synchronized private void updateContentMap() {
        if (contentMapLoaded) return;
        if (!dataLoaded) {
            loadLinesAndUpdateIndices();
        }
        contentHashToLines = ArrayListMultimap.create(rawLines.size(), 1);
        activeTags = HashMultiset.create();
        deletedEntriesCount = 0;
        for (T entry : rawLines) {
            addEntryToContentMap(entry);
        }
        contentMapLoaded = true;
    }

    synchronized private void updateTagMap() {
        if (tagHashLoaded) return;
        if (!dataLoaded) {
            loadLinesAndUpdateIndices();
        }
        tagHashToLines = ArrayListMultimap.create(rawLines.size(), 1);
        for (T entry : rawLines) {
            addEntryToTagMap(entry);
        }
        tagHashLoaded = true;
    }

    private void addEntryToContentMap(T entry) {
        if (contentHashToLines != null) {
            contentHashToLines.put(entry.getSearchHash(), entry);
        }

        if (!entry.isDeleted()) {
            if (activeTags != null) {
                activeTags.addAll(Arrays.asList(entry.getFilterTags()));
            }
        } else {
            deletedEntriesCount++;
        }
    }

    private void addEntryToTagMap(T entry) {
        if (tagHashToLines != null) {
            for (String tag : entry.getFilterTags()) {
                tagHashToLines.put(tag.hashCode(), entry);
            }
        }
    }

    private void removeEntryFromContentMap(T entry) {
        if (contentHashToLines != null) {
            contentHashToLines.remove(entry.getSearchHash(), entry);
        }
        if (!entry.isDeleted()) {
            if (activeTags != null) {
                Multisets.removeOccurrences(activeTags, Arrays.asList(entry.getFilterTags()));
            }
        } else {
            deletedEntriesCount--;
        }
    }

    private void removeEntryFromTagMap(T entry) {
        if (tagHashToLines != null) {
            for (String tag : entry.getFilterTags()) {
                tagHashToLines.remove(tag.hashCode(), entry);
            }
        }
    }

    private void clearContentMap() {
        contentHashToLines = null;
        activeTags = null;
        deletedEntriesCount = 0;
        contentMapLoaded = false;
    }

    private void clearTagMap() {
        tagHashLoaded = false;
        tagHashToLines = null;
    }

    synchronized private void loadLinesAndUpdateIndices() {
        if (dataLoaded) return;
        log.trace("Loading entries into table {} {}", table.getName(), table);
        this.rawLines = this.loadLines();
        if (table.getColumns().length == 0 && !this.rawLines.isEmpty()) {
            try {
                // Update header from the first file.
                table.updateValidateHeader(this.rawLines.get(0).getContentReal(table.getRootPath()));
            } catch (GorDataException e) {
               // Do nothing
            }
        }
        dataLoaded = true;
        log.trace("Loaded {} entries into table {}", this.rawLines.size(), table.getName());
    }

    /**
     * Parse the dictionary lines.
     */
    private List<T> loadLines() {
        log.debug("Loading lines for {}", table.getName());

        // Relativesing the data is expensive, but because manual editing is common we need to do it if the file has not been
        // saved by table service before.
        String serial = table.getProperty(TableHeader.HEADER_SERIAL_KEY);
        boolean hasNeverBeenSavedProperly = serial == null || TableHeader.NO_SERIAL.equals(serial);
        boolean needsRelativize = hasNeverBeenSavedProperly;

        try {
            List<T> newRawLines =  new ArrayList<>();
            if (table.getFileReader().exists(table.getPath())) {
                try (BufferedReader br = table.getFileReader().getReader(table.getPath())) {
                  br.lines().parallel()
                        .map(l -> {
                            String line = l;
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                return factory.parseEntry(line, table.getRootPath(), needsRelativize);
                            }
                            return null;
                        })
                        .filter(e -> e != null)
                        .forEachOrdered(e -> newRawLines.add(e));
                }
            }
            return newRawLines;

        } catch (IOException ex) {
            throw new GorResourceException("Error Initializing Query, can not read file " + table.getPath(),
                    table.getPath().toString(), ex);
        }
    }

    @Override
    public T findLine(T line) {
        // Using contentHashMap
        List<T> allLines = loadedEntries();
        updateContentMap();
        List<T> lines2Search = contentHashToLines != null ? contentHashToLines.get(line.getSearchHash()) : allLines;

        return lines2Search != null ? lines2Search.stream().filter(l -> l.equals(line)).findFirst().orElse(null) : null;
    }

    private T findLineWithTag(T line) {
        String[] linekey = line.getFilterTags();

        // Using contentHashMap
        List<T> lines2Search = loadedEntries();
        if (tagHashToLines != null) {
            lines2Search = new ArrayList<>();
            for (String tag : linekey) {
                lines2Search.addAll(tagHashToLines.get(tag.hashCode()));
            }
        }
        T match = null;
        for (T l : lines2Search) {
            if (Arrays.equals(linekey, l.getFilterTags())) {
                if (null != match) {
                    throw new GorDataException("One or more entries containing the same tag(s) already exist in table " + table.getName() + ". Unable to use unique tags!");
                }
                match = l;
            }
        }
        return match;
    }


}
