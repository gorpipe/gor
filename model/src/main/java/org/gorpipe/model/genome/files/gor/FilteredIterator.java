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

import org.gorpipe.model.genome.files.gor.filters.RowFilter;

/**
 * A wrapper class which takes an iterator and filters its output.
 *
 * @author Hjalti Thor Isleifsson
 */
class FilteredIterator extends GenomicIteratorAdapterBase {
    private final RowFilter rf;
    private boolean myHasNext = false;
    private Row myNext = null;

    FilteredIterator(GenomicIterator git, RowFilter rf) {
        super(git);
        this.rf = rf;
    }

    @Override
    public boolean seek(String chr, int pos) {
        this.myHasNext = false;
        this.myNext = null;
        final boolean seekSuccess = this.iterator.seek(chr, pos);
        return seekSuccess && this.hasNext();
    }

    @Override
    public Row next() {
        final Row toReturn = this.myNext;
        this.myNext = null;
        this.myHasNext = false;
        return toReturn;
    }

    @Override
    public boolean hasNext() {
        if (this.myHasNext) {
            return true;
        } else {
            while (this.iterator.hasNext()) {
                final Row next = this.iterator.next();
                if (this.rf.test(next)) {
                    this.myHasNext = true;
                    this.myNext = next;
                    break;
                }
            }
            return this.myHasNext;
        }
    }

    @Override
    public boolean next(Line line) {
        throw new UnsupportedOperationException();
    }
}

