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

package gorsat;

import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.model.gor.iterators.RowSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 13/06/16.
 */
public class UTestGorRank {
    @Test
    public void testGorRank() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | rank 10 POS -rmax 1 | top 10";
        int count = 0;
        int maxRank = 0;

        try (RowSource iterator = TestUtils.runGorPipeIterator(query)) {
            String columnHeaders = iterator.getHeader();
            Assert.assertTrue(columnHeaders.contains("rank_POS"));
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                maxRank = Math.max(maxRank, currentRow.colAsInt(5));
                count++;
            }
        }

        Assert.assertEquals(10, count);
        Assert.assertEquals("Maximum rank possible", 1, maxRank);
    }

    @Test
    public void testGorRankTestRMax() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | rank 1000 POS -rmax 2 | top 10";

        int count = 0;
        int maxRank = 0;

        try (RowSource iterator = TestUtils.runGorPipeIterator(query)) {
            String columnHeaders = iterator.getHeader();
            Assert.assertTrue(columnHeaders.contains("rank_POS"));
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                maxRank = Math.max(maxRank, currentRow.colAsInt(5));
                count++;
            }
        }

        Assert.assertEquals(10, count);
        Assert.assertEquals("Maximum rank possible", 2, maxRank);
    }

    @Test
    public void testGorRankTestRankDistribution() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | rank 1000 POS -q | top 10";

        try (RowSource iterator = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            String columnHeaders = iterator.getHeader();
            Assert.assertTrue(columnHeaders.contains("rank_POS"));
            Assert.assertTrue(columnHeaders.contains("lowOReqRank"));
            Assert.assertTrue(columnHeaders.contains("eqRank"));

            double lowOReqRankMax = 0.0;
            double lowOReqRankMin = 1000.0;
            double eqRankMax = 0.0;
            double eqRankMin = 1000.0;

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                lowOReqRankMax = Math.max(lowOReqRankMax, currentRow.colAsDouble(6));
                lowOReqRankMin = Math.min(lowOReqRankMin, currentRow.colAsDouble(6));
                eqRankMax = Math.max(eqRankMax, currentRow.colAsDouble(7));
                eqRankMin = Math.min(eqRankMin, currentRow.colAsDouble(7));
                count++;
            }

            Assert.assertEquals(10, count);
            Assert.assertEquals("Low OReq rank", 0.5, lowOReqRankMin, 0.01);
            Assert.assertEquals("High OReq rank", 1.0, lowOReqRankMax, 0.01);
            Assert.assertEquals("Low eq rank", 0.5, eqRankMin, 0.01);
            Assert.assertEquals("High eq rank", 1.0, eqRankMax, 0.01);
        }
    }

    @Test
    public void testGorRankTestTotalCount() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | rank 1000 POS -c -rmax 1 | top 10";

        try (RowSource iterator = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            String columnHeaders = iterator.getHeader();
            Assert.assertTrue(columnHeaders.contains("rank_POS"));
            Assert.assertTrue(columnHeaders.contains("binCount"));
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                int countToRankDiff = currentRow.colAsInt(6) - currentRow.colAsInt(5);
                Assert.assertTrue("Count to rank difference", countToRankDiff >= 0 && countToRankDiff <= 1);
                count++;
            }

            Assert.assertEquals(10, count);
        }
    }

    @Test
    public void testGorRankTestReportVAlueAtRAnk1() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | rank chrom POS -b | top 10";

        try (RowSource iterator = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            String columnHeaders = iterator.getHeader();
            Assert.assertTrue(columnHeaders.contains("rank_POS"));
            Assert.assertTrue(columnHeaders.contains("rank1_POS"));
            int lastValue = 0;

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                int currentValue = currentRow.colAsInt(6);

                if (count % 2 == 1) {
                    Assert.assertTrue("Rank position should be the same", currentValue == lastValue);
                }

                lastValue = currentValue;
                count++;
            }

            Assert.assertEquals(10, count);
        }
    }

    @Test
    public void testGorRankWithGroup() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | rank chrom POS -gc #4 | top 10";

        try (RowSource iterator = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            int maxRank = 0;
            String columnHeaders = iterator.getHeader();
            Assert.assertTrue(columnHeaders.contains("rank_POS"));

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                maxRank = Math.max(maxRank, currentRow.colAsInt(5));
                count++;
            }

            Assert.assertEquals(10, count);
            Assert.assertEquals("Maximum rank possible", 2, maxRank);
        }
    }

    @Test
    public void testGorRankWithUniqueGroup() {
        String query = "gor ../tests/data/gor/dbsnp_test.gorz | rank chrom POS -gc #3,#4 | top 10";

        try (RowSource iterator = TestUtils.runGorPipeIterator(query)) {
            int count = 0;
            int maxRank = 0;
            String columnHeaders = iterator.getHeader();
            Assert.assertTrue(columnHeaders.contains("rank_POS"));

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                maxRank = Math.max(maxRank, currentRow.colAsInt(5));
                count++;
            }

            Assert.assertEquals(10, count);
            Assert.assertEquals("Maximum rank possible", 1, maxRank);
        }
    }

    @Test
    public void testGorRankWithNorContextWithError() {
        String query = "nor -h ../tests/config/build37split.txt | rank 1000 POS";
        try {
            String[] res = TestUtils.runGorPipeLines(query);
        } catch (GorParsingException ex) {
            Assert.assertTrue("Should get parsing exception", ex.getMessage().contains("Cannot have binSize"));
        }
    }
}
