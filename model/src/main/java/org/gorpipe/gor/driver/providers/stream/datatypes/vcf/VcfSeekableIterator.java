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

package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader;
import org.gorpipe.model.genome.files.binsearch.SeekableIterator;
import org.gorpipe.model.genome.files.binsearch.StringIntKey;
import org.gorpipe.model.genome.files.gor.ContigDataScheme;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import org.gorpipe.model.genome.files.gor.Line;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.gor.RowObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;

public class VcfSeekableIterator extends GenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(VcfSeekableIterator.class);

    private final SeekableIterator seekableIterator;
    private final GorHeader gh;
    private final Comparator<StringIntKey> comparator;
    private final ChrBoundedIterator chrIterator;
    private final ContigDataScheme dataScheme;
    private final ChromoLookup lookup;

    private int idx = 0;

    VcfSeekableIterator(StreamSourceSeekableFile file, Comparator<StringIntKey> comparator, ContigDataScheme dataScheme) {
        try {
            this.seekableIterator = new SeekableIterator(file, null, new StringIntKey(0, 1, comparator), true);
        } catch (IOException e) {
            throw new GorResourceException("Could not create seekable iterator.", e.getMessage(), e);
        }

        this.gh = new GorHeader(this.seekableIterator.getHeader().split("\t"));

        this.comparator = comparator;
        this.dataScheme = dataScheme;
        this.lookup = file.getDataSource().getSourceReference().getLookup();

        this.chrIterator = new ChrBoundedIterator();
        this.chrIterator.moveToNewChr();
    }

    @Override
    public String getHeader() {
        return String.join("\t",this.gh.getColumns());
    }

    @Override
    public boolean seek(String chr, int pos) {
        this.idx = this.dataScheme.id2order(this.lookup.chrToId(chr));
        this.chrIterator.moveToNewChr(chr);
        if (this.chrIterator.seek(pos)) {
            return true;
        } else {
            return this.chrIterator.hasNext();
        }
    }

    @Override
    public Row next() {
        return this.chrIterator.next();
    }

    @Override
    public boolean hasNext() {
        if (this.chrIterator.hasNext()) {
            return true;
        } else if (++this.idx < this.dataScheme.length()) {
            this.chrIterator.moveToNewChr();
            return this.hasNext();
        } else {
            return false;
        }
    }

    @Override
    public boolean next(Line line) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        try {
            this.seekableIterator.close();
        } catch (IOException e) {
            log.warn("Could not close source.");
        }
    }

    private class ChrBoundedIterator {
        private Row current;
        boolean reachedEnd;
        String chr;

        void  moveToNewChr() {
            moveToNewChr(dataScheme.id2chr(dataScheme.order2id(idx)));
        }

        void moveToNewChr(String chr) {
            this.chr = chr;
            try {
                seekableIterator.seek(new StringIntKey(this.chr, 0, comparator));
                this.reachedEnd = !seekableIterator.hasNext();
            } catch (IOException e) {
                throw new GorResourceException("Could not seek to chromosome " + this.chr, e.getMessage(), e);
            }
            this.current = null;
        }

        boolean hasNext() {
            if (this.reachedEnd) {
                return false;
            } else if (this.current != null) {
                return true;
            } else {
                this.getNext();
                return this.hasNext();
            }
        }

        Row next() {
            final Row toReturn = this.current;
            this.current = null;
            return toReturn;
        }

        boolean seek(int pos) {
            try {
                this.current = null;
                seekableIterator.seek(new StringIntKey(this.chr, pos, comparator));
                this.reachedEnd = !seekableIterator.hasNext();
            } catch (IOException e) {
                throw new GorResourceException("Could not seek to position " + this.chr + ":" + pos, e.getMessage(), e);
            }
            return this.hasNext();
        }

        private void getNext() {
            if (seekableIterator.hasNext()) {
                final String next;
                try {
                    next = seekableIterator.getNextAsString();
                } catch (IOException e) {
                    throw new GorResourceException("Failed reading next line.", e.getMessage(), e);
                }
                final int chrEnd = next.indexOf('\t');
                final String nextChr = next.substring(0, chrEnd);
                final int nextChrIdx = lookup.chrToId(nextChr);
                if (nextChrIdx != dataScheme.order2id(idx)) {
                    this.current = null;
                    this.reachedEnd = true;
                } else {
                    this.current = RowObj.apply(this.chr + next.substring(chrEnd, next.length()));
                }
            } else {
                this.current = null;
                this.reachedEnd = true;
            }
        }
    }
}
