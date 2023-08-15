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
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.TableInfo;
import org.gorpipe.gor.table.TableHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that handles the loading caching of and working with table entries.
 *
 * Class is thread safe.
 */
public class TableEntries<T extends DictionaryEntry> implements ITableEntries<T> {
    private static final Logger log = LoggerFactory.getLogger(TableEntries.class);
    private final Class<? extends T> clazzOfT;

    private List<T> rawLines;

    // For indices we use hashed values.  Insert into the dict is much much faster, and it takes a lot less space.  Getting data takes
    // a little bit longer as you could get small list of values you need to loop through.
    private ListMultimap<Integer, T> tagHashToLines;  // tags here means aliases and tags.
    private ListMultimap<Integer, T> contentHashToLines;

    private Multiset<String> activeTags;
    private int deletedEntriesCount = 0;
    private final TableInfo<T> table;

    private boolean dataLoaded = false;
    private boolean tagHashLoaded = false;

    /**
     * Construct new dict file from the given path and chromosome cache.
     * <p>
     * It is not simple to create objects of type T.  For now we pass in the class so we can do that.
     * Another option would be using google TypeToken, but we had problem getting that working.
     */
    public TableEntries(TableInfo<T> table, Class<? extends T> clazzOfT) {
        this.table = table;
        this.clazzOfT = clazzOfT;
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

        getEntries().add(line);

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
            getEntries().remove(line);
            removeEntryFromContentMap(line);
            removeEntryFromTagMap(line);

            if (line.hasBucket() && keepIfBucket) {
                // NOTE: the deleted flag is part of the hashCode so we remove and add again if we change it.
                T entry = (T) TableEntry.copy(line);
                entry.setDeleted(true);
                getEntries().add(entry);
                addEntryToContentMap(entry);
                addEntryToTagMap(entry);
            }
        }
    }

    @Override
    synchronized public void clear() {
        dataLoaded = false;
        this.rawLines = null;
        clearContentMap();
        clearTagMap();
    }

    @Override
    public List<T> getEntries() {
        if (!this.dataLoaded) {
            loadLinesAndUpdateIndices();
        }

        return this.rawLines;
    }

    @Override
    public List<T> getEntries(String... aliasesAndTags) {
        List<T> lines2Search = getEntries();
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
        if (!dataLoaded) {
            loadLinesAndUpdateIndices();
        }
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
        if (!dataLoaded) {
            loadLinesAndUpdateIndices();
        }
        return activeTags.elementSet();
    }

    @Override
    public boolean hasDeletedEntries() {
        return deletedEntriesCount > 0;
    }

    @Override
    public int size() {
        return getEntries().size();
    }

    @Override
    public int getActiveLinesCount() {
        int size = getEntries().size();
        return size - deletedEntriesCount;
    }


    private void updateContentMap() {
        contentHashToLines = ArrayListMultimap.create(rawLines.size(), 1);
        activeTags = HashMultiset.create();

        for (T entry : rawLines) {
            addEntryToContentMap(entry);
        }
    }

    synchronized private void updateTagMap() {
        if (tagHashLoaded) return;
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
    }

    private void clearTagMap() {
        tagHashLoaded = false;
        tagHashToLines = null;
    }

    synchronized private void loadLinesAndUpdateIndices() {
        if (dataLoaded) return;
        log.trace("Loading entries into table {} {}", table.getName(), table);
        this.rawLines = this.loadLines();
        this.updateContentMap();
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

                    Method parseEntryMethod;
                    try {
                        parseEntryMethod = clazzOfT.getMethod("parseEntry", String.class, String.class, boolean.class);
                    } catch (NoSuchMethodException ex) {
                        throw new GorSystemException("Error Initializing Query, can not create entry of type: " + clazzOfT.getName() + ", no parseEntry method!", ex);
                    }
                    br.lines().parallel()
                        .map(l -> {
                            String line = l;
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                try {
                                    return (T) parseEntryMethod.invoke(null, line, table.getRootPath(), needsRelativize);
                                } catch(IllegalAccessException | InvocationTargetException ex){
                                    throw new GorSystemException("Error Initializing Query, can not create entry of type: "
                                            + clazzOfT.getName() + ", error parsing line: \"" + l + "\"", ex);
                                }
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
        List<T> allLines = getEntries();
        List<T> lines2Search = contentHashToLines != null ? contentHashToLines.get(line.getSearchHash()) : allLines;

        return lines2Search != null ? lines2Search.stream().filter(l -> l.equals(line)).findFirst().orElse(null) : null;
    }

    private T findLineWithTag(T line) {
        String[] linekey = line.getFilterTags();

        // Using contentHashMap
        List<T> lines2Search = getEntries();
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
