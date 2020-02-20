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

import org.junit.Ignore;
import org.junit.Test;

public class UTestVarJoin {
    @Test
    public void varjoin() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\tPosx\tRefx\tAltx\tDatax\n",
                "chr1\t1\tA\tC\tchr1 1\t1\tA\tC\tRight 1 chr1 1\n",
                "chr2\t23\tT\tC\tchr2 23\t23\tT\tC\tRight 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinAlleleString() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tACA\tCAC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tACA\tCAC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\tPosx\tRefx\tAltx\tDatax\n",
                "chr1\t1\tACA\tCAC\tchr1 1\t1\tACA\tCAC\tRight 1 chr1 1\n",
                "chr2\t23\tT\tC\tchr2 23\t23\tT\tC\tRight 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinRefAltColsNotInDefaultLocations() {
        String[] leftLines = {
                "Chrom\tPos\tData\tRef\tAlt",
                "chr1\t1\tchr1 1\tA\tC",
                "chr2\t23\tchr2 23\tT\tC",
                "chr3\t23\tchr2 23\tT\tC"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tData\tAlt",
                "chr1\t1\tA\tRight 1 chr1 1\tC",
                "chr2\t23\tT\tRight 1 chr2 23\tC",
                "chr2\t23\tT\tRight 1 chr2 23 repeat\tA"};

        String joinQuery = "gor %s | varjoin %s";

        String[] expected = {
                "Chrom\tPos\tData\tRef\tAlt\tPosx\tRefx\tDatax\tAltx\n",
                "chr1\t1\tchr1 1\tA\tC\t1\tA\tRight 1 chr1 1\tC\n",
                "chr2\t23\tchr2 23\tT\tC\t23\tT\tRight 1 chr2 23\tC\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinRefAltColsNotInDefaultLocationsAndNonDefaultNames() {
        String[] leftLines = {
                "Chrom\tPos\tData\tFirst\tSecond",
                "chr1\t1\tchr1 1\tA\tC",
                "chr2\t23\tchr2 23\tT\tC",
                "chr3\t23\tchr2 23\tT\tC"};
        String[] rightLines = {
                "Chrom\tPos\tFirst\tData\tSecond",
                "chr1\t1\tA\tRight 1 chr1 1\tC",
                "chr2\t23\tT\tRight 1 chr2 23\tC",
                "chr2\t23\tT\tRight 1 chr2 23 repeat\tA"};

        String joinQuery = "gor %s | varjoin -ref First -alt Second %s";

        String[] expected = {
                "Chrom\tPos\tData\tFirst\tSecond\tPosx\tFirstx\tDatax\tSecondx\n",
                "chr1\t1\tchr1 1\tA\tC\t1\tA\tRight 1 chr1 1\tC\n",
                "chr2\t23\tchr2 23\tT\tC\t23\tT\tRight 1 chr2 23\tC\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinRefAltColsNotInDefaultLocationsAndNonDefaultNamesDifferentForLeftRight() {
        String[] leftLines = {
                "Chrom\tPos\tData\tFirst\tSecond",
                "chr1\t1\tchr1 1\tA\tC",
                "chr2\t23\tchr2 23\tT\tC",
                "chr3\t23\tchr2 23\tT\tC"};
        String[] rightLines = {
                "Chrom\tPos\tBingo\tData\tBongo",
                "chr1\t1\tA\tRight 1 chr1 1\tC",
                "chr2\t23\tT\tRight 1 chr2 23\tC",
                "chr2\t23\tT\tRight 1 chr2 23 repeat\tA"};

        String joinQuery = "gor %s | varjoin -refl First -altl Second -refr Bingo -altr Bongo %s";

        String[] expected = {
                "Chrom\tPos\tData\tFirst\tSecond\tPosx\tBingo\tDatax\tBongo\n",
                "chr1\t1\tchr1 1\tA\tC\t1\tA\tRight 1 chr1 1\tC\n",
                "chr2\t23\tchr2 23\tT\tC\t23\tT\tRight 1 chr2 23\tC\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinRefAltColsWithDifferentNames() {
        String[] leftLines = {
                "Chrom\tPos\tFirst\tSecond\tData",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin %s";

        String[] expected = {
                "Chrom\tPos\tFirst\tSecond\tData\tPosx\tRef\tAlt\tDatax\n",
                "chr1\t1\tA\tC\tchr1 1\t1\tA\tC\tRight 1 chr1 1\n",
                "chr2\t23\tT\tC\tchr2 23\t23\tT\tC\tRight 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinReducedOutput() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin -r %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\tDatax\n",
                "chr1\t1\tA\tC\tchr1 1\tRight 1 chr1 1\n",
                "chr2\t23\tT\tC\tchr2 23\tRight 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinIncludedOnly() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin -i %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\n",
                "chr1\t1\tA\tC\tchr1 1\n",
                "chr2\t23\tT\tC\tchr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinNegative() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin -n %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\n",
                "chr3\t23\tT\tC\tchr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinLeftJoinStyle() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin -l %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\tPosx\tRefx\tAltx\tDatax\n",
                "chr1\t1\tA\tC\tchr1 1\t1\tA\tC\tRight 1 chr1 1\n",
                "chr2\t23\tT\tC\tchr2 23\t23\tT\tC\tRight 1 chr2 23\n",
                "chr3\t23\tT\tC\tchr2 23\t\t\t\t\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varjoinLeftJoinStyleNonDefaultEmpty() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin -l -e x %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\tPosx\tRefx\tAltx\tDatax\n",
                "chr1\t1\tA\tC\tchr1 1\t1\tA\tC\tRight 1 chr1 1\n",
                "chr2\t23\tT\tC\tchr2 23\t23\tT\tC\tRight 1 chr2 23\n",
                "chr3\t23\tT\tC\tchr2 23\tx\tx\tx\tx\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void varJoinLeftFileHasOverlapCountColumn() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tOverlapCount",
                "chr1\t1\tA\tC\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tT\tC\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin %s -ic";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tOverlapCount\tOverlapCountx\n",
                "chr1\t1\tA\tC\tchr1 1\t1\n",
                "chr2\t23\tT\tC\tchr2 23\t1\n",
                "chr3\t23\tT\tC\tchr2 23\t0\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    // todo: add test for norm/nonorm
    // todo: add test span
    // todo: add test multi-allelic sharing (-as)

    @Ignore("https://nextcode.atlassian.net/browse/GOP-254")
    @Test
    public void varjoinCaseMismatchInAlleles() {
        String[] leftLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\ta\tc\tchr1 1",
                "chr2\t23\tT\tC\tchr2 23",
                "chr3\t23\tT\tC\tchr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRef\tAlt\tData",
                "chr1\t1\tA\tC\tRight 1 chr1 1",
                "chr2\t23\tt\tc\tRight 1 chr2 23",
                "chr2\t23\tT\tA\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | varjoin %s";

        String[] expected = {
                "Chrom\tPos\tRef\tAlt\tData\tPosx\tRefx\tAltx\tDatax\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }
}
