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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing GOR Dictionary
 * <p>
 * Created by gisli on 23/08/16.
 * <p>
 * Format of the gord dictionary file
 * <p>
 * [<meta-information lines>]
 * [<header line>]
 * <data lines>
 * <p>
 * The format of the meta-information lines is:
 * <p>
 * [## <key>=<value>]
 * ...
 * <p>
 * <p>
 * <key>       Attribute name.  Not case sensitive.
 * <value>     Attribute value.
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
 * [# <column name 1>\t<column name 2>\t<column name 3> ...]
 * <p>
 * <p>
 * Each data line is a tab separated list of columns, described as:
 * <p>
 * <file>[[|<flags>]|<bucket>][\t<alias>[\t<startchrom>\t<startpos>\t<endchrom>\t<endpos>\t[tags]]]
 * <p>
 * where:
 * <p>
 * <file>              Abosolute path or relative (to the location of the dictionary) path to the main data file.
 * <flags>             Comma seprated list of flags applicable to the data file.  Available flags:
 * D - The file is marked as deleted.  This means the file has been deleted and should be
 * ignored when reading from the bucket.
 * <bucket>            Relative path ot the bucket file <file> belongs to.
 * <alias>             Alias for <file>.  The alias specifies "source" value of <file>.  If no tags are specified, <alias> is used
 * as tag for <file>.  Can be empty.
 * <startchrom>        Filter start chromosome, e.g. chr1.  Can be empty, if all range elements are empty.
 * <startpos>          Filter start pos. Can be empty, if all range elements are empty.
 * <endchrom>          Filter stop chromosome, e.g. chr3. Can be empty, if all range elements are empty.
 * <endpos>            Filter stop pos.  Can be empty, if all range elements are empty.
 * <tags>              Comma separated list of tags:  <tagval1>[,<tagval2>...]
 * <p>
 * <p>
 * Notes:
 * 1. The <file> + <filter> is a unique key into the file.  Note, this always exact match, i.e. filter that is a subset of another filter
 * will be treated as different.  This could be improved by banning overlapping filters.
 * 2. If <tags> are specified then they are used for filtering (but the <alias> is not).  If no <tags> are specified <alias> is used as
 * tags for filtering.
 * 3. <file> could be bucket file.
 * 4. Seems in general alias is used for normal files but tags for the bucket files.
 * <p>
 * <p>
 * NOTE:
 * - HG says, either all the files have alias or none of them.
 */
public class DictionaryTable extends BaseTable<DictionaryEntry> {

    private static final Logger log = LoggerFactory.getLogger(DictionaryTable.class);

    private boolean useEmbeddedHeader = false;  // Should the header be embeded in the table file stored in header file the table data dir.

    public DictionaryTable(Path path) {
        super(path);
    }

    public DictionaryTable(Builder builder) {
        super(builder);
        this.useEmbeddedHeader = builder.useEmbededHeader;
    }

    public DictionaryTable(Path path, String tagColumn, boolean useHistory, boolean useEmbeddedHeader,
                           String securityContext, boolean validateFiles) {
        super(path, tagColumn, useHistory, securityContext, validateFiles);
        this.useEmbeddedHeader = useEmbeddedHeader;
    }

    @Override
    protected ITableEntries<DictionaryEntry> createTableEntries(Path path) {
        return new TableEntries<>(path, DictionaryEntry.class);
        // Leave this in here for easy try out.
        //return new TableEntries<>(path, DictionaryRawEntry.class);
    }

    @Override
    public List<? extends DictionaryEntry> getOptimizedLines(Map<Integer, Set<String>> columnTags, boolean allowBucketAccess) {
        final TableAccessOptimizer optimizer = new TableAccessOptimizer(this);
        optimizer.update(this.selectAll(), columnTags, allowBucketAccess);
        return optimizer.getLines();
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

    /**
     * @param content new content, absolute or relative to the table.
     */
    public void insert(String content) {
        insert(new DictionaryEntry.Builder(content, getRootUri()).build());
    }


    @Override
    protected void doSave() {
        log.debug("Saving {} entries for table {}", tableEntries.size(), getName());
        String oldSerial = this.header.getProperty(TableHeader.HEADER_SERIAL_KEY);
        this.header.setProperty(TableHeader.HEADER_SERIAL_KEY, oldSerial != null ? String.valueOf(Long.parseLong(oldSerial) + 1) : "1");
        this.header.setProperty(TableHeader.HEADER_LINE_COUNT_KEY, String.valueOf(tableEntries.size()));
        try {
            Path tempFolder = getFolderPath();

            Path tempDict = Files.createTempFile(tempFolder, getName(), ".gord");
            try (BufferedWriter writer = Files.newBufferedWriter(tempDict)) {
                if (useEmbeddedHeader) {
                    writer.write(this.header.formatHeader());
                }
                Iterator<DictionaryEntry> it = tableEntries.iterator();
                while (it.hasNext()) {
                    writer.write(it.next().formatEntryNoNewLine());
                    writer.newLine();
                }
            }

            if (!useEmbeddedHeader) {
                Path tempHeader = Files.createTempFile(tempFolder, "header", ".tmp");
                try (BufferedWriter writer = Files.newBufferedWriter(tempHeader)) {
                    writer.write(this.header.formatHeader());
                }
                updateFromTempFile(getFolderPath().resolve("header"), tempHeader);
            }

            updateFromTempFile(getPath(), tempDict);
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
        log.debug("Done saving {} entries for table {}", tableEntries.size(), getName());
    }

    /**
     * Create or update dictionary.
     *
     * @param name     name of the dictionary.
     * @param rootPath root path
     * @param data     map with alias to files, to be add to the dictionary.
     * @return new table created with the given data.
     */
    public static DictionaryTable createDictionaryWithData(String name, Path rootPath, Map<String, List<String>> data) {
        Path tablePath = rootPath.resolve(name + ".gord");
        if (Files.exists(tablePath)) {
            throw new GorSystemException("Table already exists:  " + tablePath, null);
        }
        DictionaryTable table = new Builder<>(tablePath).useHistory(true)
                .securityContext("").validateFiles(false).build();
        table.insert(data);
        table.save();
        return table;
    }

    public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> extends BaseTable.Builder<B> {
        boolean useEmbededHeader = false;

        private AbstractBuilder(Path path) {
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
            this(Paths.get(path));
        }

        public Builder(Path path) {
            super(path);
        }

        @Override
        public DictionaryTable build() {
            return new DictionaryTable(this);
        }
    }

    public static Builder newBuilder(Path path) {
        return new Builder<>(path);
    }

    // -----------------------------------------------------------------------------------
    //  Some relics from the old dictionary used by GorOptions.
    // -----------------------------------------------------------------------------------

    public static Set<String> tagset(String alias) {
        return alias != null ? tagset(Collections.singletonList(alias)) : tagset(Collections.emptyList());
    }

    private static Set<String> tagset(List<String> t) {
        return t == null ? new HashSet<>() : Collections.unmodifiableSortedSet(new TreeSet<>(t));
    }

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
