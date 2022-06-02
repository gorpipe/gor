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

import com.google.common.collect.ArrayListMultimap;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that handles the loading caching of and working with table entries.
 */
public class TableEntries<T extends BucketableTableEntry> implements ITableEntries<T> {
    private static final Logger log = LoggerFactory.getLogger(TableEntries.class);
    private final Class<? extends T> clazzOfT;

    private List<T> rawLines;
    // For indices we use hashed values.  Insert into the dict is much much faster, and it takes a lot less space.  Getting data takes
    // a little bit longer as you could get small list of values you need to loop through.
    private ArrayListMultimap<Integer, T> tagHashToLines;  // tags here means aliases and tags.
    private ArrayListMultimap<Integer, T> contentHashToLines;
    private int nextIndexOrderKey = 0;
    private final BaseDictionaryTable<T> table;

    /**
     * Construct new dict file from the given path and chromosome cache.
     * <p>
     * It is not simple to create objects of type T.  For now we pass in the class so we can do that.
     * Another option would be using google TypeToken, but we had problem getting that working.
     */
    public TableEntries(BaseDictionaryTable<T> table, Class<? extends T> clazzOfT) {
        this.table = table;
        this.clazzOfT = clazzOfT;
    }

    @Override
    public void insert(T line, boolean hasUniqueTags) {
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
        public void delete(T lineToRemove, boolean keepIfBucket) {
            T line = this.findLine(lineToRemove);
            if (line != null) {
                if (line.getIndexOrderKey() >= 0 && line.getIndexOrderKey() < getEntries().size()
                        && line.equals(getEntries().get(line.getIndexOrderKey()))) {
                    getEntries().remove(line.getIndexOrderKey());
                } else {
                    getEntries().remove(line);
                }
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
    public void clear() {
        this.rawLines = null;
        clearContentMap();
        clearTagMap();
    }

    @Override
    public List<T> getEntries() {
        if (this.rawLines == null) {
            loadLinesAndUpdateIndices();
        }

        return this.rawLines;
    }

    @Override
    public List<T> getEntries(String... aliasesAndTags) {
        List<T> lines2Search = getEntries();
        if (tagHashToLines == null) {
            updateTagMap();
        }
        if (tagHashToLines != null && aliasesAndTags != null) {
            // If we have tags and tag map to lines we use that to get a better list of lines to search..
            lines2Search = Arrays.stream(aliasesAndTags).flatMap(t -> tagHashToLines.get(t.hashCode()).stream())
                    .sorted(Comparator.comparing(TableEntry::getIndexOrderKey)).distinct().collect(Collectors.toList());
        }
        return lines2Search;
    }

    @Override
    public Iterator<T> iterator() {
        return getEntries().iterator();
    }

    @Override
    public boolean isLoaded() {
        return this.rawLines != null;
    }

    @Override
    public Set<String> getAllActiveTags() {
        // Note:  Slow implementation, could use caching.
        Set<String> allTags = new HashSet<>();
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            T entry = it.next();
            if (!entry.isDeleted()) {
                allTags.addAll(Arrays.asList(entry.getFilterTags())); // Capture all tags found in dictionary files
            }
        }
        return allTags;
    }

    @Override
    public long size() {
        return getEntries().size();
    }

    private void updateContentMap() {
        contentHashToLines = ArrayListMultimap.create(getEntries().size(), 1);
        nextIndexOrderKey = 0;
        for (T entry : getEntries()) {
            addEntryToContentMap(entry);
        }
    }

    private void updateTagMap() {
        tagHashToLines = ArrayListMultimap.create(getEntries().size(), 1);
        for (T entry : getEntries()) {
            addEntryToTagMap(entry);
        }
    }

    private void addEntryToContentMap(T entry) {
        entry.setIndexOrderKey(nextIndexOrderKey++);
        if (contentHashToLines != null) {
            contentHashToLines.put(entry.getSearchHash(), entry);
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
        nextIndexOrderKey = 0;
    }

    private void clearTagMap() {
        tagHashToLines = null;
    }


    private void loadLinesAndUpdateIndices() {
        this.rawLines = this.loadLines();
        this.updateContentMap();

        log.trace("Loaded {} entries into table {}", this.rawLines.size(), table.getName());
    }

    /**
     * Parse the dictionary lines.
     */
    private List<T> loadLines() {
        log.debug("Loading lines for {}", table.getName());

        boolean hasNeverBeenSavedProperly = TableHeader.NO_SERIAL.equals(table.getHeader().getProperty(TableHeader.HEADER_SERIAL_KEY, TableHeader.NO_SERIAL));
        boolean needsRelativize = hasNeverBeenSavedProperly || true;  // Because manual editing is common we need to force it for now.

        try {
            List<T> newRawLines = new ArrayList<>();
            if (table.getFileReader().exists(table.getPath().toString())) {
                try (BufferedReader br = table.getFileReader().getReader(table.getPath().toString())) {
                    String line;
                    // Remove the header.  For large file it helps not having to check for the header on each line.
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.length() != 0 && !TableHeader.isHeaderLine(line)) {
                            break;
                        }
                    }
                    // Read rest of file.
                    try {
                        Method parseEntryMethod = clazzOfT.getMethod("parseEntry", String.class, URI.class, boolean.class);
                        while (line != null) {
                            line = line.trim();
                            final T entry = (T) parseEntryMethod.invoke(null, line, table.getRootUri(), needsRelativize);
                            if (entry != null) {
                                newRawLines.add(entry);
                            }

                            line = br.readLine();
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                        throw new GorSystemException("Error Initializing Query, can not create entry of type: " + clazzOfT.getName(), ex);
                    }
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
        String lineKey = line.getKey();
        for (T l : lines2Search) {
            if (lineKey.equals(l.getKey())) {
                return l;
            }
        }

        return null;
    }

    private T findLineWithTag(T line) {
        // Using contentHashMap
        List<T> lines2Search = getEntries();
        if (tagHashToLines != null) {
            lines2Search = new ArrayList<>();
            for (String tag : line.getFilterTags()) {
                lines2Search.addAll(tagHashToLines.get(tag.hashCode()));
            }
        }

        String[] linekey = line.getFilterTags();
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
