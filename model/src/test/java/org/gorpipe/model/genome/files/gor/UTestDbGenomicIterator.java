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

import org.gorpipe.model.genome.files.gor.*;
import org.gorpipe.test.DbTests;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@Category(DbTests.class)
public class UTestDbGenomicIterator {
    private final int LINE_COUNT = 366;

    @BeforeClass
    public static void initDb() throws IOException, ClassNotFoundException {
        DbSource.initInConsoleApp();
    }

    @Test
    public void getHeader() {
        try (final GenomicIterator iterator = getIterator()) {
            final String[] header = iterator.getHeader().split("\t");
            assertEquals(50, header.length);
        }
    }

    @Test
    public void hasNext_ReturnsTrueAtStart() {
        try (final GenomicIterator iterator = getIterator()) {
            assertTrue(iterator.hasNext());
        }
    }

    @Test
    public void next_FillingLine_FirstLine() {
        try (final GenomicIterator iterator = getIterator()) {
            Line line = new Line(48);
            boolean b = iterator.next(line);
            assertTrue(b);
            assertEquals("chr1\t13896\tC\tA\tIO_GIAB_FATHER\tSampleAnnotation\t\t\t\t2018-11-16 10:10:43.31346\tcd14b640-cbb5-0136-d79a-0242ac11000a\t\t\tWASH7P\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1\t\t\t\t137\tGenome-In-A-Bottle-ACMG", line.getAllCols().toString());
        }
    }

    @Test
    public void next_FirstLine() {
        try (final GenomicIterator iterator = getIterator()) {
            // TODO: Test fails without hasNext (this is a bug)
            iterator.hasNext();

            Row r = iterator.next();
            assertEquals("chr1\t13896\tC\tA\tIO_GIAB_FATHER\tSampleAnnotation\t\t\t\t2018-11-16 10:10:43.31346\tcd14b640-cbb5-0136-d79a-0242ac11000a\t\t\tWASH7P\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1\t\t\t\t137\tGenome-In-A-Bottle-ACMG", r.getAllCols().toString());
        }
    }

    @Test
    @Ignore("This is currently broken in DbGenomicIterator")
    public void hasNext_ShouldNotAdvance() {
        try (final GenomicIterator iterator = getIterator()) {
            for (int i = 0; i < LINE_COUNT + 5; i++) {
                assertTrue(iterator.hasNext());
            }
        }
    }

    @Test
    public void hasNext_ShouldReturnFalseWhenFileIsExhausted() {
        try (final GenomicIterator iterator = getIterator()) {
            int count = 0;
            while(iterator.hasNext()) {
                count++;
                if(count > LINE_COUNT) {
                    break;
                }
                iterator.next();
            }
            assertFalse(iterator.hasNext());
            assertEquals(LINE_COUNT, count);
        }
    }

    @Test
    public void seek_SeekToMiddleReturnsTrueWhenPositionExists() {
        try (final GenomicIterator iterator = getIterator()) {
            boolean result = iterator.seek("chr1", 201331068);
            assertTrue(result);
        }
    }

    @Test
    @Ignore("https://nextcode.atlassian.net/browse/GOP-461")
    public void seek_NextReturningRowWorksAfterSeekToMiddle() {
        try (final GenomicIterator iterator = getIterator()) {
            iterator.seek("chr1", 201331068);

            // Todo: next won't work unless hasNext is called first
            assertTrue(iterator.hasNext());

            Row r = iterator.next();
            assertNotNull(r);
            assertEquals("chr1\t201331068\tA\tG\tIO_GIAB_MOTHER\tSampleAnnotation\t\t\t\t2018-09-18 11:10:38.257882\t65799680-9d61-0136-56f3-0242ac11000a\t\t\tTNNT2\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1\t\t\t\t48507\thfg_default_type_f_mendel_ACMG\n", r.getAllCols().toString());
        }
    }

    @Test
    public void seekFindsSameLinesAsStream() {
        List<Row> foundByStream = new ArrayList<>();
        List<Row> foundAfterSeek = new ArrayList<>();

        try (final GenomicIterator iterator = getIterator()) {
            while(iterator.hasNext()) {
                final Row row = iterator.next();
                if(row.chr.equals("chr1") && row.pos == 201331068) {
                    foundByStream.add(row);
                }
            }
        }

        try (final GenomicIterator iterator = getIterator()) {
            iterator.seek("chr1", 201331068);
            while(iterator.hasNext()) {
                final Row row = iterator.next();
                if(row.chr.equals("chr1") && row.pos == 201331068) {
                    foundAfterSeek.add(row);
                }
            }
        }

        foundByStream.sort(null);
        foundAfterSeek.sort(null);
        assertArrayEquals(foundByStream.toArray(), foundAfterSeek.toArray());
    }

    private GenomicIterator getIterator() {
        final GorOptions gorOptions = GorOptions.createGorOptions("-Z dbscope=project_id#int#1 db://rda:rda.v_variant_annotations");
        GenomicIterator iterator = null;
        iterator = gorOptions.getIterator();
        assertNotNull(iterator);

        iterator.init(null);

        // This is done in LineSource - next doesn't work correctly without this
        iterator.setColnum(iterator.getHeader().split("\t").length - 2);

        return iterator;
    }

}