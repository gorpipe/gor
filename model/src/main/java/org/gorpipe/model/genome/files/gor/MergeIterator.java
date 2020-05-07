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

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.GorContext;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * MergeIterator merges lines from multiple genomic iterators. All the iterators must have the same
 * layout, and lines are interleaved in genomic order. This is effectively doing a merge-sort on
 * the iterators.
 */
public class MergeIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(MergeIterator.class);

    private static final String DEFAULT_SOURCE_COLUMN_NAME = "Source";
    private List<GenomicIterator> sources;
    /**
     * The queue stores rows from each source. The queue is initialized with one row
     * from each source, and when a row is pulled from the queue a new one is pulled
     * from the source where it came from.
     */
    private PriorityQueue<RowFromIterator> queue;
    /**
     * This flag controls whether a column should be added to each row with the name
     * of the of the source. Note that the source may already have the source column
     * added.
     */
    private boolean insertSource;

    /**
     * Set once queue has been primed.
     */
    private boolean isPrimed = false;

    /**
     * Optional GorMonitor instance, so that cancelling can be done while priming
     */
    private GorMonitor gorMonitor;

    public MergeIterator(List<GenomicIterator> sources, GorOptions options) {
        this(sources, options, null);
    }

    public MergeIterator(List<GenomicIterator> sources, GorOptions options, GorMonitor gm) {
        this.sources = sources;
        insertSource = options.insertSource;
        gorMonitor = gm;

        getHeaderFromSources(options);
    }

    @Override
    public void setContext(GorContext context) {
        statsSenderName = "MergeIterator";
        super.setContext(context);
        addStat("numSources", sources.size());
    }

    private static String[] getHeaderWithOptionalSourceColumn(GorOptions options, GenomicIterator i) {
        String[] header = i.getHeader().split("\t");
        if (options.insertSource) {
            String name = getSourceColumnName(options);
            if (i.isSourceAlreadyInserted()) {
                header = (String[]) ArrayUtils.clone(header);
                header[header.length - 1] = name;
            } else {
                header = (String[]) ArrayUtils.add(header, name);
            }
        }
        return header;
    }

    private static String getSourceColumnName(GorOptions options) {
        return options.sourceColName != null ? options.sourceColName : DEFAULT_SOURCE_COLUMN_NAME;
    }

    @Override
    public boolean seek(String chr, int pos) {
        incStat("seek");

        clearQueue();
        isPrimed = true;
        for (GenomicIterator it : sources) {
            it.seek(chr, pos);
            addNextToQueue(it);
        }

        return !queue.isEmpty();
    }

    @Override
    public boolean hasNext() {
        incStat("hasNext");

        if (!isPrimed) {
            primeQueue(sources);
        }
        return !queue.isEmpty();
    }

    @Override
    public Row next() {
        incStat("next");

        if (!isPrimed) {
            primeQueue(sources);
        }
        RowFromIterator rowFromIterator = queue.poll();
        if (rowFromIterator == null) {
            return null;
        }

        GenomicIterator source = rowFromIterator.source;
        addNextToQueue(source);

        return rowFromIterator.row;
    }

    @Override
    public boolean next(Line line) {
        throw new GorSystemException("next filling Line should not be used from MergeIterator", null);
    }

    @Override
    public void close() {
        for (GenomicIterator it : sources) {
            it.close();
        }
    }

    private void getHeaderFromSources(GorOptions options) {
        String firstName = "";
        for (GenomicIterator it : this.sources) {
            String[] headerWithOptionalSourceColumn = getHeaderWithOptionalSourceColumn(options, it);
            String header = getHeader();
            if (header.length() == 0) {
                setHeader(String.join("\t",headerWithOptionalSourceColumn));
                setColnum(headerWithOptionalSourceColumn.length - 2);
                firstName = it.getSourceName();
            } else {
                String[] headerSplit = header.split("\t");
                if (!areHeadersEqual(headerSplit, headerWithOptionalSourceColumn)) {
                    String message = "Error initializing query: Header for " + it.getSourceName() + " ("
                            + String.join(",", headerWithOptionalSourceColumn)
                            + ") is different from the first opened file "
                            + firstName + " (" + String.join(",", headerSplit) + ")";
                    throw new GorDataException(message);
                }
            }
            it.setColnum(getColnum());
        }
    }

    private boolean areHeadersEqual(String[] first, String[] second) {
        if (first.length != second.length) {
            return false;
        }
        for (int i = 0; i < first.length; i++) {
            if (!first[i].equalsIgnoreCase(second[i])) {
                return false;
            }
        }
        return true;
    }

    private void primeQueue(List<GenomicIterator> sources) {
        isPrimed = true;
        clearQueue();
        int index = 0;
        for (GenomicIterator it : sources) {
            if (gorMonitor != null && gorMonitor.isCancelled()) {
                return;
            }
            it.setSourceIndex(index);
            index++;
            addNextToQueue(it);
        }
    }

    private void clearQueue() {
        if (queue != null) {
            queue.clear();
        } else {
            queue = new PriorityQueue<>(sources.size(), new RowComparator());
        }
    }

    private void addNextToQueue(GenomicIterator it) {
        while (it.hasNext()) {
            Row r = it.next();
            if (r == null) {
                String msg = String.format("Iterator next returned null after hasNext returned true (%s, %s)", it.getClass().getName(), it.getSourceName());
                throw new GorSystemException(msg, null);
            }
            if (insertSource && !it.isSourceAlreadyInserted()) {
                insertOptionalSourceColumn(r, it.getSourceName());
            }
            if (it.isIncluded(r)) {
                queue.add(new RowFromIterator(r, it));
                break;
            }
        }
    }

    private void insertOptionalSourceColumn(Row r, String s) {
        String[] header = getHeader().split("\t");
        if (r.numCols() == header.length) {
            r.setColumn(header.length - 3, s);
        } else {
            r.addSingleColumnToRow(s);
        }
    }

    class RowFromIterator {
        Row row;
        GenomicIterator source;

        RowFromIterator(Row r, GenomicIterator s) {
            row = r;
            source = s;
        }
    }

    class RowComparator implements Comparator<RowFromIterator> {
        @Override
        public int compare(RowFromIterator o1, RowFromIterator o2) {
            int chrCompare = o1.row.chr.compareTo(o2.row.chr);
            if (chrCompare == 0) {
                int posCompare = o1.row.pos - o2.row.pos;
                if (posCompare == 0) {
                    return o1.source.getSourceIndex() - o2.source.getSourceIndex();
                }
                return posCompare;
            }
            return chrCompare;
        }
    }
}
