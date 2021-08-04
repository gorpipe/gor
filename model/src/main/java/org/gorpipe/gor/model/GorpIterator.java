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

package org.gorpipe.gor.model;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Format of a gorp file:
 *
 * source1    startChr1    startPos1    stopChr1    stopPos1
 * source2    startChr2    startPos2    stopChr2    stopPos2
 *    .          .            .            .            .
 *    .          .            .            .            .
 *    .          .            .            .            .
 * sourceN    startChrN    startPosN    stopChrN    stopPosN
 *
 * This iterator takes in a source which is a gorp file i.e. a gor parquet dictionary.
 * The gorp file contains a list of gor ordered parquet files and their corresponding ranges.
 * The GorpIterator provides an iterator interface to the merged content of the parquet files.
 *
 * Notes on implementation and usage:
 *
 * 1) We use a RangeMergeIterator to merge the contents of the parquet files in a memory efficient way
 * with a minimum load on the file system.
 *
 * 2) Since the parquet iterators can have a predicate, we send a progress row every time we have to open the first file
 * starting from a given range. This is so that if this iterator is being used by a merge iterator, it will not block
 * all the other iterators in case there are few rows matching the predicate.
 *
 * 3) So that the progress rows do not pass into the analysis steps, this iterator should used from a MergeIterator
 * (which will filter the progress rows) or one can write
 *
 * {@code final GenomicIterator git = new GorpIterator(ss).filter(r -> !r.isProgress);}
 *
 * @author Hjalti Thor Isleifsson
 */
public class GorpIterator extends GenomicIteratorAdapterBase {
    private final String commonRoot;
    private final String securityContext;
    private final String path;
    private final Path parentDirectory; //The directory in which the dictionary is.

    public GorpIterator(StreamSource ss) {
        this.path = ss.getSourceReference().getUrl();
        this.parentDirectory = Paths.get(this.path).getParent();
        this.commonRoot = ss.getSourceReference().getCommonRoot();
        this.securityContext = ss.getSourceReference().securityContext;
        this.setIterator(createIteratorFromDictionary(ss));
    }

    private GenomicIterator createIteratorFromDictionary(StreamSource ss) {
        try (final Stream<String> lines = new BufferedReader(new InputStreamReader(ss.open())).lines()) {
            final List<SourceRef> srs = lines.map(this::parseLineToSourceRef).filter(l -> l.startChr != null).collect(Collectors.toList());
            return new RangeMergeIterator(srs);
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    private SourceRef parseLineToSourceRef(String line) {
        final String[] columns = line.split("\t");
        final String sourcePath = this.parentDirectory == null ? columns[0] : this.parentDirectory.resolve(columns[0]).toString();
        final String startChr = columns[2];
        final int startPos = Integer.parseInt(columns[3]);
        final String stopChr = columns[4];
        final int stopPos = Integer.parseInt(columns[5]);
        return new SourceRef(sourcePath, null, null, null, startChr, startPos, stopChr, stopPos,
                null, null, false, this.securityContext, this.commonRoot);
    }
}
