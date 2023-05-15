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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.*;
import org.gorpipe.gor.table.livecycle.TableBuilder;
import org.gorpipe.gor.table.livecycle.TableTwoPhaseCommitSupport;
import org.gorpipe.gor.table.util.TableLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;

import static org.gorpipe.gor.table.util.PathUtils.*;

/**
 * DictionaryTable.
 */
public class DictionaryTable extends DictionaryTableReader implements Table<DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(DictionaryTable.class);

    private TableTwoPhaseCommitSupport<DictionaryEntry> lifeCycleSupport;

    public DictionaryTable(String path, FileReader fileReader) {
        super(path, fileReader);
        lifeCycleSupport = new DictionaryTwoPhaseCommitSupport(this);

        reload();
    }

    public DictionaryTable(String path) {
        this(path, null);
    }

    public DictionaryTable(Path path) {
        this(path.toUri().toString(), null);
    }
    
    public DictionaryTable(Builder builder) {
        this(builder.path, builder.fileReader);

        if (builder.validateFiles != null) {
            lifeCycleSupport.setValidateFiles(builder.validateFiles);
        }

        if (builder.useHistory != null) {
            lifeCycleSupport.setUseHistory(builder.useHistory);
        }

        if (builder.id != null) {
            this.id = builder.id;
        }

        if (builder.sourceColumn != null) {
            setSourceColumn(builder.sourceColumn);
        }

        if (builder.uniqueTags != null) {
            setUniqueTags(builder.uniqueTags);
        }

        if (builder.useEmbededHeader != null) {
            this.useEmbeddedHeader = builder.useEmbededHeader;
        }
    }

    @Override
    public void initialize() {
        lifeCycleSupport.initialize();
    }

    @Override
    public void setProperty(String key, String value) {
        lifeCycleSupport.setProperty(key, value);
    }

    @Override
    public void setColumns(String[] columns) {
        if (this.tableEntries.size() > 0 && columns.length != this.header.getColumns().length) {
            throw new GorDataException("Invalid columns - " + String.format("New columns length (%d) does not fit current data column count (%d)",
                    columns.length, this.header.getColumns().length));
        }
        lifeCycleSupport.setColumns(columns);
    }

    @Override
    public void setValidateFiles(boolean validateFiles) {
        lifeCycleSupport.setValidateFiles(validateFiles);
    }

    @Override
    public void setUseHistory(boolean useHistory) {
        lifeCycleSupport.setUseHistory(useHistory);
    }

    @Override
    public void commitRequest() throws GorException {
        lifeCycleSupport.commitRequest();
    }

    @Override
    public void commit() {
        lifeCycleSupport.commit();
    }

    @Override
    public void save() {
        lifeCycleSupport.save();
    }

    @Override
    public void delete() {
        lifeCycleSupport.delete();
    }

    @Override
    public void reload() {
        if (lifeCycleSupport != null) { // Don't call reload if not ready (still constructing).
            super.reload();
        }
    }

    @Override
    protected void loadMeta() {
        super.loadMeta();
        lifeCycleSupport.loadMeta();
    }

    public void setSourceColumn(String sourceColumn) {
        header.setProperty(DictionaryTableMeta.HEADER_SOURCE_COLUMN_KEY, sourceColumn);
    }

    public void setBucketize(boolean bucketize) {
        header.setProperty(DictionaryTableMeta.HEADER_BUCKETIZE_KEY, Boolean.toString(bucketize));
    }

    public void setLineFilter(Boolean lineFilter) {
        header.setProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, lineFilter.toString());
    }

    public void setUniqueTags(boolean hasUniqueTags) {
        header.setProperty(DictionaryTableMeta.HEADER_UNIQUE_TAGS_KEY, Boolean.toString(hasUniqueTags));
    }

    @Override
    public void insert(Collection<DictionaryEntry> lines) {
        int count = 0;
        for (DictionaryEntry line : lines) {
            count++;
            if (count % 1000 == 0) {
                log.info("Inserting line {} of {}", count, lines.size());
            }
            // Validate the new file.
            if (isValidateFiles()) {
                validateFile(getContentReal(line));
            }

            this.tableEntries.insert(line, isHasUniqueTags());
            lifeCycleSupport.logAfter(TableLog.LogAction.INSERT, "", line.formatEntryNoNewLine());
        }
        tableAccessOptimizer = null;
    }

    @Override
    public void insert(String... lines) {
        List<DictionaryEntry> entries = lineStringsToEntries(lines);
        insert(entries);
    }

    
    @Override
    public void delete(Collection<DictionaryEntry> lines) {
        for (DictionaryEntry line : lines) {
            tableEntries.delete(line, true);
            lifeCycleSupport.logAfter(TableLog.LogAction.DELETE, "", line.formatEntryNoNewLine());
        }
        tableAccessOptimizer = null;
    }

    @Override
    public void delete(String... lines) {
        List<DictionaryEntry> entries = lineStringsToEntries(lines);
        delete(entries);
    }

    private List<DictionaryEntry> lineStringsToEntries(String[] lines) {
        List<DictionaryEntry> entries = new ArrayList<>();
        for (String line : lines) {
            line = line.stripLeading();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            entries.add(DictionaryEntry.parseEntry(line, getRootPath(), true));
        }
        return entries;
    }

    /**
     * Remove the selected files from their bucket.
     *
     * @param lines lines to be removed.
     */
    public void removeFromBucket(Collection<DictionaryEntry> lines) {
        try {
            for (DictionaryEntry line : lines) {
                DictionaryEntry lineToRemoveFrom = tableEntries.findLine(line);
                if (lineToRemoveFrom != null) {
                    String bucket = lineToRemoveFrom.getBucket();
                    if (lineToRemoveFrom.isDeleted()) {
                        tableEntries.delete(lineToRemoveFrom, false);
                        lifeCycleSupport.logAfter(TableLog.LogAction.DELETE, bucket, lineToRemoveFrom.formatEntryNoNewLine());
                    } else {
                        lineToRemoveFrom.setBucket("");
                        lifeCycleSupport.logAfter(TableLog.LogAction.REMOVEFROMBUCKET, bucket, lineToRemoveFrom.formatEntryNoNewLine());
                    }
                }
            }
        } catch (Exception e) {
            throw new GorDataException("Entries could not be removed from bucket", e);
        }
    }

    /**
     * Remove the selected files from their bucket.
     *
     * @param lines lines to be removed.
     */
    @SafeVarargs
    public final void removeFromBucket(DictionaryEntry... lines) {
        removeFromBucket(Arrays.asList(lines));
    }

    /**
     * Add the given entries to the given bucket, done after the bucketization has been done.
     *
     * Note:  We are assuming here that the underlying datafiles are not changed (but can be deleted).
     *
     * @param bucket bucket to add to.
     * @param lines  files to select.
     */
    public void addToBucket(String bucket, List<DictionaryEntry> lines) {
        String bucketLogical = relativize(getRootPath(), bucket);
        for (DictionaryEntry line : lines) {
            DictionaryEntry lineToUpdate = tableEntries.findLine(line);
            if (lineToUpdate != null) {
                if (lineToUpdate.hasBucket() && !lineToUpdate.getBucket().equals(bucketLogical)) {
                    throw new GorDataException(String.format("File %s is already in bucket %s and can not be added to bucket %s",
                            line.getContentRelative(), lineToUpdate.getBucket(), bucketLogical));
                }
                lineToUpdate.setBucket(bucketLogical);
                lifeCycleSupport.logAfter(TableLog.LogAction.ADDTOBUCKET, bucketLogical, line.formatEntryNoNewLine());
            } else {
                // No line found, must have been deleted.  To be able to use the bucket we must add a new line.
                DictionaryEntry newDeletedLine = (DictionaryEntry) TableEntry.copy(line);
                newDeletedLine.setDeleted(true);
                newDeletedLine.setBucket(bucketLogical);
                tableEntries.insert(newDeletedLine, false);
                lifeCycleSupport.logAfter(TableLog.LogAction.INSERT, bucketLogical, line.formatEntryNoNewLine());
            }
        }
    }

    @SafeVarargs
    public final void addToBucket(String bucket, DictionaryEntry... lines) {
        addToBucket(bucket, Arrays.asList(lines));
    }

    //@Override
    public void insertEntries(Collection<DictionaryEntry> entries) {
        insert(entries);
    }

    @Override
    public void deleteEntries(Collection<DictionaryEntry> entries) {
        delete(entries);
    }

    /**
     * @param data map with alias to files, to be add to the dictionary.  The files must be normalized and either absolute or
     *             relative to the dictionary root.
     */
    public void insert(Map<String, List<String>> data) {
        List<DictionaryEntry> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            for (String path : entry.getValue()) {
                lines.add(new DictionaryEntry.Builder<>(path, getRootPath()).alias(entry.getKey()).build());
            }
        }
        insert(lines);
    }

    public boolean hasDeletedEntries() {
        return this.tableEntries.hasDeletedTags();
    }

    public Collection<String> getBucketDeletedFiles(String path) {
        return getTableAccessOptimizer().getBucketDeletedFiles(path);
    }

    public static class Builder<B extends Builder<B>> extends TableBuilder<B> {

        protected String sourceColumn;
        protected Boolean uniqueTags;

        public Builder(String path) {
            super(path);
        }

        public Builder(Path path) {
            this(path.toString());
        }

        public Builder(URI path) {
            this(path.toString());
        }

        public B sourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
            return self();
        }

        public B uniqueTags(boolean val) {
            this.uniqueTags = val;
            return self();
        }

        public DictionaryTable build() {
            return new DictionaryTable(this);
        }
    }
}
