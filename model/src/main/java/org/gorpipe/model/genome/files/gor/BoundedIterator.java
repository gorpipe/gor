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

import org.gorpipe.exceptions.GorSystemException;

/**
 * BoundedIterator wraps an iterator with a range.
 */
public class BoundedIterator extends GenomicIteratorAdapterBase {

    private String startChromosome;
    private String stopChromosome;
    private int startPosition;
    private int stopPosition;

    private Row nextFromIterator;
    private boolean isOutOfRange = false;

    /**
     * Constructs a bounded iterator for a range of chromosomes
     * @param it the source iterator
     * @param chr starting chromosome
     * @param start start position within the starting chromosome
     * @param stopChr stopping chromosome
     * @param stop stop position within the stopping chromosome
     */
    public BoundedIterator(GenomicIterator it, String chr, int start, String stopChr, int stop) {
        super(it);

        final ChromoLookup chromoLookup = it.getLookup() == null ? new DefaultChromoLookup() : it.getLookup();
        startChromosome = chr == null ? null : chromoLookup.idToName(chromoLookup.chrToId(chr));
        startPosition = start;
        stopChromosome = stopChr == null ? null : chromoLookup.idToName(chromoLookup.chrToId(stopChr));
        stopPosition = stop == -1 ? Integer.MAX_VALUE : stop;

        if(startChromosome.equals(stopChromosome) && startPosition == stopPosition) {
            isOutOfRange = true;
        } else if(!iterator.seek(startChromosome, startPosition)) {
            isOutOfRange = true;
        }
    }

    /**
     * Constructs a bounded iterator for a single chromosome
     * @param it the source iterator
     * @param chr the chromosome to limit the iteration to
     * @param start starting position
     * @param stop stopping position
     */
    public BoundedIterator(GenomicIterator it, String chr, int start, int stop) {
        this(it, chr, start, null, stop);
    }

    @Override
    public boolean seek(String chr, int pos) {
        nextFromIterator = null;

        int cmp = chr.compareTo(startChromosome);
        if(cmp < 0) {
            chr = startChromosome;
        }
        if(cmp == 0 && pos < startPosition) {
            pos = startPosition;
        }

        if(isInRange(chr, pos)) {
            isOutOfRange = false;
            return iterator.seek(chr, pos);
        } else {
            isOutOfRange = true;
            return false;
        }
    }

    @Override
    public boolean next(Line line) {
        throw new GorSystemException("next filling Line should not be used from BoundedIterator", null);
    }

    @Override
    public boolean hasNext() {
        if(isOutOfRange) {
            return false;
        }

        if(nextFromIterator != null) {
            // We've already pulled the next row from the iterator and validated it
            return true;
        }
        if(!iterator.hasNext()) {
            // The iterator has no more rows
            return false;
        }
        Row r = iterator.next();
        if(isInRange(r)) {
            nextFromIterator = r;
            return true;
        }

        isOutOfRange = true;
        this.nextFromIterator = null;
        return false;
    }

    private boolean isInRange(Row r) {
        return isInRange(r.chr, r.pos);
    }

    private boolean isInRange(String chr, int pos) {
        if(stopChromosome != null && !startChromosome.equals(stopChromosome)) {
            int cmp = chr.compareTo(startChromosome);
            if(cmp < 0) {
                return false;
            }
            if (cmp == 0 && pos < startPosition) {
                return false;
            }
            cmp = chr.compareTo(stopChromosome);
            if(cmp > 0) {
                return false;
            }
            return cmp != 0 || pos <= stopPosition;

        }
        return chr.equals(startChromosome) && pos >= startPosition && pos <= stopPosition;
    }

    @Override
    public Row next() {
        if(hasNext()) {
            Row r = nextFromIterator;
            nextFromIterator = null;
            return r;
        }
        return null;
    }
}
