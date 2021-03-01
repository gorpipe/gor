/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.model.gor.iterators.RowSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 20/01/2017.
 */
public class UTestJoinICIROptions {
    private void testJoinICIR(String query1, String query2, int expected) {
        try (RowSource iterator1 = TestUtils.runGorPipeIterator(query1)) {
            try (RowSource iterator2 = TestUtils.runGorPipeIterator(query2)) {
                int count = 0;
                boolean pih = iterator1.hasNext();
                while (pih) {
                    Assert.assertEquals(pih, iterator2.hasNext());
                    Assert.assertEquals(iterator1.next(), iterator2.next());
                    count++;
                    pih = iterator1.hasNext();
                }
                Assert.assertEquals(pih, iterator2.hasNext());
                Assert.assertEquals(expected, count);
            }
        }
    }

    @Test
    public void testIC() {
        String query1 = "gor ../tests/data/gor/genes.gorz | join -segsnp ../tests/data/gor/dbsnp_test.gorz -ic | group chrom -gc overlapcount -count";
        String query2 = "gor ../tests/data/gor/genes.gorz | join -segsnp -l -e 0 <(../tests/data/gor/dbsnp_test.gorz | calc present 1) | group 1 -gc 3,gene_symbol -sum -ic present | rename sum_present OverlapCount | group chrom -gc overlapcount -count";
        testJoinICIR(query1, query2, 27);
    }

    @Test
    public void testIR() {
        String query1 = "gor ../tests/data/gor/genes.gorz | segspan | join -segsnp ../tests/data/gor/dbsnp_test.gorz | select 1,pos- | distinct | group chrom -count";
        String query2 = "gor ../tests/data/gor/genes.gorz | join -segsnp ../tests/data/gor/dbsnp_test.gorz -ir | group chrom -count";

        testJoinICIR(query1, query2, 2);
    }

    @Test
    public void testIREnsgenes() {
        String query1 = "gor ../tests/data/gor/genes.gorz | top 5000 |join -segsnp -xl gene_symbol -xr gene_symbol ../tests/data/gor/ensgenes_exons.gorz -ir | sort 3000000 | group chrom -count";
        String query2 = "gor ../tests/data/gor/genes.gorz | top 5000|join -segsnp -xl gene_symbol -xr gene_symbol ../tests/data/gor/ensgenes_exons.gorz | select 1,chromstart- | sort 3000000 | distinct | group chrom -count";

        testJoinICIR(query1, query2, 2);
    }

    @Test
    public void testIRVarjoin() {
        String query1 = "gor -p chr1 ../tests/data/gor/dbsnp_test.gorz | varjoin <(../tests/data/gor/dbsnp_test.gorz | rename #5 hakon | rownum) -ir | group chrom -count";
        String query2 = "gor -p chr1 ../tests/data/gor/dbsnp_test.gorz | group chrom -count";

        testJoinICIR(query1, query2, 1);
    }

    @Test
    public void testICVarjoin() {
        String query1 = "gor -p chr1 ../tests/data/gor/dbsnp_test.gorz | varjoin ../tests/data/gor/dbsnp_test.gorz -ic | group chrom -gc overlapcount -count";
        String query2 = "gor -p chr1 ../tests/data/gor/dbsnp_test.gorz | varjoin -l -e 0 <(../tests/data/gor/dbsnp_test.gorz | calc present 1) | group 1 -gc 3 -sum -ic present | rename sum_present OverlapCount | group chrom -gc overlapcount -count";

        testJoinICIR(query1, query2, 1);
    }
}
