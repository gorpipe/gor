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

package org.gorpipe.model.genome.files.binsearch;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.gorpipe.model.genome.files.binsearch.PositionCache.*;

public class UTestPositionCache {
    private static final Logger log = LoggerFactory.getLogger(UTestPositionCache.class);

    @Test
    public void test_maxNumberOfPos() {
        Assert.assertEquals(0, getMaxNumberOfPos(0, 0));
        Assert.assertEquals(MAX_NUMBER_OF_POS_PER_GB, getMaxNumberOfPos(0, 1));
        Assert.assertEquals(MAX_NUMBER_OF_POS_PER_GB, getMaxNumberOfPos(0, GB));
        Assert.assertEquals(2 * MAX_NUMBER_OF_POS_PER_GB, getMaxNumberOfPos(0, (long) (1.5 * GB)));
    }

    @Test
    public void test_getBounds() {
        final PositionCache pc = new PositionCache(0, 5,  Integer.MAX_VALUE);
        final StringIntKey gisli = new StringIntKey("gisli", 0);
        final StringIntKey hjalti = new StringIntKey("hjalti", 0);
        final StringIntKey joi = new StringIntKey("joi", 0);
        final StringIntKey simmi = new StringIntKey("simmi", 0);
        final StringIntKey snorri = new StringIntKey("snorri", 0);

        pc.putFilePosition(hjalti, 1);
        pc.putFilePosition(simmi, 3);

        final Position gisli_lower = pc.getLowerBound(gisli);
        final Position gisli_upper = pc.getUpperBound(gisli);

        Assert.assertEquals(0, gisli_lower.fileIdx);
        Assert.assertNull(gisli_lower.key);

        Assert.assertEquals(1, gisli_upper.fileIdx);
        Assert.assertEquals(hjalti, gisli_upper.key);

        final Position hjalti_lower = pc.getLowerBound(hjalti);
        final Position hjalti_upper = pc.getUpperBound(hjalti);

        Assert.assertEquals(0, hjalti_lower.fileIdx);
        Assert.assertNull(hjalti_lower.key);

        Assert.assertEquals(1, hjalti_upper.fileIdx);
        Assert.assertEquals(hjalti, hjalti_upper.key);

        final Position joi_lower = pc.getLowerBound(joi);
        final Position joi_upper = pc.getUpperBound(joi);

        Assert.assertEquals(1, joi_lower.fileIdx);
        Assert.assertEquals(hjalti, joi_lower.key);

        Assert.assertEquals(3, joi_upper.fileIdx);
        Assert.assertEquals(simmi, joi_upper.key);

        final Position simmi_lower = pc.getLowerBound(simmi);
        final Position simmi_upper = pc.getUpperBound(simmi);

        Assert.assertEquals(1, simmi_lower.fileIdx);
        Assert.assertEquals(hjalti, simmi_lower.key);

        Assert.assertEquals(3, simmi_upper.fileIdx);
        Assert.assertEquals(simmi, simmi_upper.key);

        final Position snorri_lower = pc.getLowerBound(snorri);
        final Position snorri_upper = pc.getUpperBound(snorri);

        Assert.assertEquals(3, snorri_lower.fileIdx);
        Assert.assertEquals(simmi, snorri_lower.key);

        Assert.assertEquals(5, snorri_upper.fileIdx);
        Assert.assertNull(snorri_upper.key);
    }

    @Test
    public void test_pruningOfGlobalCache() {
        PositionCache.clearGlobalCache();
        PositionCache.setMaxNumberOfFilesInCache(2);

        final String file1 = "file1";
        final String file2 = "file2";
        final String file3 = "file3";

        final PositionCache cache1 = PositionCache.getFilePositionCache(this, file1, "dummy",0, 0);
        final PositionCache cache2 = PositionCache.getFilePositionCache(this, file2, "dummy", 0, 0);
        final PositionCache cache3 = PositionCache.getFilePositionCache(this, file3, "dummy",0, 0);

        Assert.assertSame(cache3, PositionCache.getFilePositionCache(this, file3, "dummy",0, 0));
        Assert.assertSame(cache2, PositionCache.getFilePositionCache(this, file2, "dummy",0, 0));

        final PositionCache cache1_second = PositionCache.getFilePositionCache(this, file1, "dummy",0, 0);
        Assert.assertNotSame(cache1, cache1_second);

        Assert.assertNotSame(cache3, PositionCache.getFilePositionCache(this, file3, "dummy", 0, 0));

        PositionCache.setMaxNumberOfFilesInCache(PositionCache.DEFAULT_MAX_NUMBER_OF_POS_PER_GB);
    }

    @Test
    public void test_doNotCacheFilesWithoutId() {
        final PositionCache pc1 = PositionCache.getFilePositionCache(this, "file1", null, 0, 0);
        final PositionCache pc2 = PositionCache.getFilePositionCache(this, "file1", null, 0, 0);

        Assert.assertNotSame(pc1, pc2);
    }

    @Test
    public void test_detectsIdChange() {
        final PositionCache pc1 = PositionCache.getFilePositionCache(this, "file1", "id1", 0, 0);
        final PositionCache pc2 = PositionCache.getFilePositionCache(this, "file1", "id2", 0, 0);

        Assert.assertNotSame(pc1, pc2);
    }

    @Test
    public void test_removesFromCacheWhenNullId() {
        final PositionCache pc1 = PositionCache.getFilePositionCache(this, "file1", "id1", 0, 0);
        final PositionCache pc2 = PositionCache.getFilePositionCache(this, "file1", null, 0, 0);

        Assert.assertNotSame(pc1, pc2);

        final PositionCache pc3 = PositionCache.getFilePositionCache(this, "file1", "id1", 0, 0);

        Assert.assertNotSame(pc1, pc3);
        Assert.assertNotSame(pc2, pc3);
    }

    @Test
    public void test_putFilePosition() {
        PositionCache pc = new PositionCache(35, 7609973009L, 2000);

        pc.putFilePosition(new StringIntKey("chr1", 1), 1);
        Assert.assertEquals(1, pc.getSize());
    }

    @Test
    public void test_putFilePositionDuplicateEntry() {
        PositionCache pc = new PositionCache(35, 7609973009L, 2000);

        pc.putFilePosition(new StringIntKey("chr1", 1), 1);
        pc.putFilePosition(new StringIntKey("chr1", 1), 1);
        pc.putFilePosition(new StringIntKey("chr1", 1), 1);
        Assert.assertEquals(1, pc.getSize());
    }

    @Test
    public void test_putFilePositionMultipleKeys() {
        PositionCache pc = new PositionCache(35, 7609973009L, 2000);

        pc.putFilePosition(new StringIntKey("chr1", 1), 1);
        pc.putFilePosition(new StringIntKey("chr1", 3), 3);
        pc.putFilePosition(new StringIntKey("chr1", 2), 2);
        Assert.assertEquals(3, pc.getSize());
    }

    @Test
    public void test_removeLeastUsefulKey() {
        PositionCache pc = new PositionCache(35, 7609973009L, 2000);

        pc.putFilePosition(new StringIntKey("chr1", 1), 1);
        pc.putFilePosition(new StringIntKey("chr1", 1000), 1000);
        pc.putFilePosition(new StringIntKey("chr1", 1001), 1001);
        pc.putFilePosition(new StringIntKey("chr1", 1002), 1002);
        pc.putFilePosition(new StringIntKey("chr1", 2000), 2000);

        pc.removeLeastUsefulKey();
        Assert.assertEquals(4, pc.getSize());
    }

    public static void main(String[] args) {
        PositionCache pc = new PositionCache(35, 7609973009L, 2000);
        try (InputStream inputStream = new FileInputStream("/Users/snorris/index-experiments/dbsnp.gorz.gori.full")) {
            long start = System.currentTimeMillis();
            GorIndexFile.load(inputStream, pc);
            long end = System.currentTimeMillis();
            log.debug("Index loaded in {} seconds", (end-start)/1000.0);
            long[] filePositionsInCache = pc.getFilePositionsInCache();
            for (long l : filePositionsInCache) {
                log.debug("{}", l);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
