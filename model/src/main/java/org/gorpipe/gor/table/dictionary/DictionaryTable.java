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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Class representing GOR Dictionary
 * <p>
 * Created by gisli on 23/08/16.
 * <p>
 * Format of the gord dictionary file
 * <p>
 * {@literal [<meta-information lines>]}
 * {@literal [<header line>]}
 * {@literal <data lines>}
 * <p>
 * The format of the meta-information lines is:
 * <p>
 * {@literal [## <key>=<value>]}
 * ...
 * <p>
 * <p>
 * {@literal <key>       Attribute name.  Not case sensitive.}
 * {@literal <value>     Attribute value.}
 * <p>
 * Reserved key values are:
 * <p>
 * fileformat      - Version of the file format.
 * created         - Creation date of the file
 * build           - Reference data build version.
 * columns         - Column definition for the gor files in the dictionary.
 * sourceColumn    - Name of the source column.
 * <p>
 * Format of the header line is:
 * <p>
 * {@literal [# <column name 1>\t<column name 2>\t<column name 3> ...]}
 * <p>
 * <p>
 * Each data line is a tab separated list of columns, described as:
 * <p>
 * {@literal <file>[[|<flags>]|<bucket>][\t<alias>[\t<startchrom>\t<startpos>\t<endchrom>\t<endpos>\t[tags]]]}
 * <p>
 * where:
 * <p>
 * {@literal <file>              Abosolute path or relative (to the location of the dictionary) path to the main data file.}
 * {@literal <flags>             Comma seprated list of flags applicable to the data file.  Available flags:}
 * D - The file is marked as deleted.  This means the file has been deleted and should be
 * ignored when reading from the bucket.
 * {@literal <bucket>            Relative path ot the bucket file <file> belongs to.}
 * {@literal <alias>             Alias for <file>.  The alias specifies "source" value of <file>.  If no tags are specified, <alias> is used}
 * {@literal as tag for <file>.  Can be empty.}
 * {@literal <startchrom>        Filter start chromosome, e.g. chr1.  Can be empty, if all range elements are empty.}
 * {@literal <startpos>          Filter start pos. Can be empty, if all range elements are empty.}
 * {@literal <endchrom>          Filter stop chromosome, e.g. chr3. Can be empty, if all range elements are empty.}
 * {@literal <endpos>            Filter stop pos.  Can be empty, if all range elements are empty.}
 * {@literal <tags>              Comma separated list of tags:  <tagval1>[,<tagval2>...]}
 * <p>
 * <p>
 * Notes:
 * {@literal 1. The <file> + <filter> is a unique key into the file.  Note, this always exact match, i.e. filter that is a subset of another filter}
 * will be treated as different.  This could be improved by banning overlapping filters.
 * {@literal 2. If <tags> are specified then they are used for filtering (but the <alias> is not).  If no <tags> are specified <alias> is used as}
 * tags for filtering.
 * {@literal 3. <file> could be bucket file.}
 * 4. Seems in general alias is used for normal files but tags for the bucket files.
 * <p>
 * <p>
 * NOTE:
 * - HG says, either all the files have alias or none of them.
 */
public class DictionaryTable extends BaseDictionaryTable<DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(DictionaryTable.class);

    private boolean useEmbeddedHeader = false;  // Should the header be embeded in the table file stored in header file the table data dir.

    private TableAccessOptimizer tableAccessOptimizer;

    public DictionaryTable(URI path) {
        super(path);
    }

    public DictionaryTable(URI path, FileReader fileReader) {
        super(path, fileReader);
    }

    public DictionaryTable(Path path) {
        super(path.toUri());
    }

    public DictionaryTable(String path) {
        super(URI.create(path));
    }

    public DictionaryTable(Builder builder) {
        super(builder);
        if (builder.useEmbededHeader != null) {
            this.useEmbeddedHeader = builder.useEmbededHeader;
        }
    }

    @Override
    protected ITableEntries<DictionaryEntry> createTableEntries() {
        return new TableEntries<>(this, DictionaryEntry.class);
        // Leave this in here for easy try out.
        //return new TableEntries<>(path, DictionaryRawEntry.class);
    }

    @Override
    public List<DictionaryEntry> getOptimizedLines(Set<String> tags, boolean allowBucketAccess, boolean isSilentTagFilter) {
        return getTableAccessOptimizer().getOptimizedEntries(tags, allowBucketAccess, isSilentTagFilter);
    }

    private TableAccessOptimizer getTableAccessOptimizer() {
        if (tableAccessOptimizer == null) {
            tableAccessOptimizer = new DefaultTableAccessOptimizer(this);
        }
        return tableAccessOptimizer;
    }

    @Override
    public void insertEntries(Collection<DictionaryEntry> entries) {
        insert(entries);
    }

    @Override
    public void deleteEntries(Collection<DictionaryEntry> entries) {
        delete(entries);
    }

    @Override
    public void insert(Map<String, List<String>> data) {
        List<DictionaryEntry> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            for (String path : entry.getValue()) {
                lines.add(new DictionaryEntry.Builder<>(path, getRootUri()).alias(entry.getKey()).build());
            }
        }
        insert(lines);
    }

    @Override
    protected void saveTempMainFile() {
        log.debug("Saving {} entries for table {}", tableEntries.size(), getName());

        try {
            String tempDict = getTempMainFileName();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(getFileReader().getOutputStream(tempDict)))) {
                if (useEmbeddedHeader) {
                    writer.write(this.header.formatHeader());
                }
                Iterator<DictionaryEntry> it = tableEntries.iterator();
                while (it.hasNext()) {
                    String line = it.next().formatEntryNoNewLine();
                    writer.write(line);
                    writer.newLine();
                }
            }

            if (!useEmbeddedHeader) {
                URI tempHeader = URI.create(getTempFileName(getFolderUri().resolve("header").toString()));
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(getFileReader().getOutputStream((tempHeader.toString()))))) {
                    writer.write(this.header.formatHeader());
                }
            }
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
        log.debug("Done saving {} entries for table {}", tableEntries.size(), getName());
    }

    @Override
    public void commit() {
        super.commit();
        try {
            if (!useEmbeddedHeader) {
                updateFromTempFile(getFolderUri().resolve("header").toString(), getTempFileName(getFolderUri().resolve("header").toString()));
            }
        } catch (IOException e) {
            throw new GorSystemException("Could not move header", e);
        }
    }

    public boolean hasDeletedEntries() {
        return this.tableEntries.hasDeletedTags();
    }

    public Collection<String> getBucketDeletedFiles(String path) {
        return getTableAccessOptimizer().getBucketDeletedFiles(path);
    }

    public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> extends BaseDictionaryTable.Builder<B> {
        Boolean useEmbededHeader = null;

        private AbstractBuilder(URI path) {
            super(path);
        }

        public B embeddedHeader(boolean val) {
            this.useEmbededHeader = val;
            return self();
        }

        @Override
        public abstract DictionaryTable build();
    }

    public static class Builder<B extends Builder<B>> extends AbstractBuilder<B> {
        public Builder(String path) {
            this(URI.create(path));
        }

        public Builder(Path path) {
            super(path.toUri());
        }

        public Builder(URI path) {
            super(path);
        }

        @Override
        public DictionaryTable build() {
            return new DictionaryTable(this);
        }
    }

    final private static Cache<String, DictionaryTable> dictCache = CacheBuilder.newBuilder().maximumSize(1000).build();   //A map from dictionaries to the cache objects.

    public synchronized static DictionaryTable getDictionaryTable(String path, FileReader fileReader, boolean useCache) throws IOException {
        if (useCache) {
            String uniqueID = fileReader.getFileSignature(path);
            var key = dictCacheKeyFromPathAndRoot(path, fileReader.getCommonRoot());
            if (uniqueID == null || uniqueID.equals("")) {
                dictCache.invalidate(key);
                return new DictionaryTable.Builder<>(path).fileReader(fileReader).id(uniqueID).build();
            } else {
                DictionaryTable dictFromCache = dictCache.getIfPresent(key);
                if (dictFromCache == null || !dictFromCache.getId().equals(uniqueID)) {
                    DictionaryTable newDict = new DictionaryTable.Builder<>(path).fileReader(fileReader).id(uniqueID).build();
                    dictCache.put(key, newDict);
                    return newDict;
                } else {
                    return dictFromCache;
                }
            }
        } else {
            return new DictionaryTable.Builder<>(path).fileReader(fileReader).build();
        }
    }

    private static String dictCacheKeyFromPathAndRoot(String path, String commonRoot) {
        return PathUtils.resolve(commonRoot, path);
    }

    // -----------------------------------------------------------------------------------
    //  Some relics from the old dictionary used by GorOptions.
    // -----------------------------------------------------------------------------------

    public static Map<Integer, Set<String>> tagmap(String alias) {
        return alias != null ? tagmap(Collections.singletonList(alias)) : tagmap(Collections.emptyList());
    }

    private static Map<Integer, Set<String>> tagmap(List<String> t) {
        final HashMap<Integer, Set<String>> tags = new HashMap<>();
        if (t != null) {
            tags.put(3, Collections.unmodifiableSortedSet(new TreeSet<>(t)));
        }
        return tags;
    }

}
