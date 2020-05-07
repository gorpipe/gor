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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class UTestSeqBasesGenomicIterator {
    private static final Logger log = LoggerFactory.getLogger(UTestSeqBasesGenomicIterator.class);

    private static final String SEQUENCE = "GATCACAGG";
    private static final int CHROM_COUNT = 26;

    @ClassRule
    public static TemporaryFolder workDir = new TemporaryFolder();

    @BeforeClass
    public static void setup() throws IOException {
        final ChromoCache chromoCache = new ChromoCache();
        final DefaultChromoLookup chromoLookup = new DefaultChromoLookup();

        int id = 1;
        while (id < CHROM_COUNT) {
            final String chr = chromoLookup.idToName(id);
            String filename = chr + ".txt";
            final File file = workDir.newFile(filename);
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                out.write(SEQUENCE.getBytes());
            }
            id = chromoCache.findNextInLexicoOrder(chr);
        }
    }

    @Test
    public void getHeader() {
        try (SeqBasesGenomicIterator iterator = getSeqBasesGenomicIterator()) {
            final String header = iterator.getHeader();
            assertArrayEquals(SeqBasesGenomicIterator.COLS, header.split("\t"));
        }
    }

    @Test
    public void seek() {
        try (SeqBasesGenomicIterator iterator = getSeqBasesGenomicIterator()) {
            final boolean result = iterator.seek("chr3", 3);
            assertTrue(result);
            assertTrue(iterator.hasNext());
            final Row row = iterator.next();
            assertEquals("chr3\t3\tT", row.toString());
        }
    }

    @Test
    public void streamWholeSeq() {
        try (SeqBasesGenomicIterator iterator = getSeqBasesGenomicIterator()) {
            int count = 0;
            while(iterator.hasNext()) {
                iterator.next();
                count++;
            }
            assertEquals(CHROM_COUNT*SEQUENCE.length(), count);
        }
    }

    private SeqBasesGenomicIterator getSeqBasesGenomicIterator() {
        try {
            final String path = workDir.getRoot().getCanonicalPath();
            final DefaultChromoLookup lookup = new DefaultChromoLookup();
            final SeqBasesGenomicIterator iterator = new SeqBasesGenomicIterator(path, lookup, null);
            iterator.init(null);
            iterator.setColnum(1);
            return iterator;
        } catch (IOException e) {
            log.warn("Couldn't create SeqBasesGenomicIterator", e);
            return null;
        }
    }
}