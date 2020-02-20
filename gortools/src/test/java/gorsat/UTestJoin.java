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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class UTestJoin {
    private static final Logger log = LoggerFactory.getLogger(UTestJoin.class);

    @Test
    public void joinSnpSnp() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tRight1\n",
                "chr1\t1\tLeft 1 chr1 1\tRight 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\tRight 1 chr2 23\n",
                "chr2\t23\tLeft 1 chr2 23\tRight 1 chr2 23 repeat\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpToList() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -t -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tRight1\n",
                "chr1\t1\tLeft 1 chr1 1\tRight 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\tRight 1 chr2 23,Right 1 chr2 23 repeat\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpRange() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -p chr1:1-10 -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tRight1\n",
                "chr1\t1\tLeft 1 chr1 1\tRight 1 chr1 1\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpNegative() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr3\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -n -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\n",
                "chr3\t23\tLeft 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpIncludedOnly() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -i -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\n",
                "chr1\t1\tLeft 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpRightIncludedOnly() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -ir -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tRight1\n",
                "chr1\t1\tRight 1 chr1 1\n",
                "chr2\t23\tRight 1 chr2 23\n",
                "chr2\t23\tRight 1 chr2 23 repeat\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpEqui() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1\tData",
                "chr1\t1\tLeft 1 chr1 1\tx",
                "chr1\t1\tLeft 1 chr1 1\ty",
                "chr2\t23\tLeft 1 chr2 23\tx"};
        String[] rightLines = {
                "Chrom\tPos\tRight1\tData",
                "chr1\t1\tRight 1 chr1 1\tx",
                "chr2\t23\tRight 1 chr2 23\tx",
                "chr2\t23\tRight 1 chr2 23 repeat\ty"};

        String joinQuery = "gor %s | join -xl Data -xr Data -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tData\tRight1\tDatax\n",
                "chr1\t1\tLeft 1 chr1 1\tx\tRight 1 chr1 1\tx\n",
                "chr2\t23\tLeft 1 chr2 23\tx\tRight 1 chr2 23\tx\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpCaseInsensitiveEqui() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1\tData",
                "chr1\t1\tLeft 1 chr1 1\tX",
                "chr1\t1\tLeft 1 chr1 1\tY",
                "chr2\t23\tLeft 1 chr2 23\tx"};
        String[] rightLines = {
                "Chrom\tPos\tRight1\tData",
                "chr1\t1\tRight 1 chr1 1\tx",
                "chr2\t23\tRight 1 chr2 23\tx",
                "chr2\t23\tRight 1 chr2 23 repeat\ty"};

        String joinQuery = "gor %s | join -xcis -xl Data -xr Data -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tData\tRight1\tDatax\n",
                "chr1\t1\tLeft 1 chr1 1\tX\tRight 1 chr1 1\tx\n",
                "chr2\t23\tLeft 1 chr2 23\tx\tRight 1 chr2 23\tx\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpFuzz() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t20\tLeft 1 chr2 20"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t24\tRight 1 chr2 24"};

        String joinQuery = "gor %s | join -f 5 -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tPosx\tRight1\n",
                "chr1\t1\tLeft 1 chr1 1\t0\t1\tRight 1 chr1 1\n",
                "chr2\t20\tLeft 1 chr2 20\t3\t23\tRight 1 chr2 23\n",
                "chr2\t20\tLeft 1 chr2 20\t4\t24\tRight 1 chr2 24\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpFuzzMinOnly() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t20\tLeft 1 chr2 20"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t24\tRight 1 chr2 24"};

        String joinQuery = "gor %s | join -f 5 -m -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tPosx\tRight1\n",
                "chr1\t1\tLeft 1 chr1 1\t0\t1\tRight 1 chr1 1\n",
                "chr2\t20\tLeft 1 chr2 20\t3\t23\tRight 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpFuzzNClosest() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t20\tLeft 1 chr2 20"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t19\tRight 1 chr2 19",
                "chr2\t20\tRight 1 chr2 20",
                "chr2\t21\tRight 1 chr2 21",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t24\tRight 1 chr2 24"};

        String joinQuery = "gor %s | join -f 5 -o 2 -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tPosx\tRight1\n",
                "chr1\t1\tLeft 1 chr1 1\t0\t1\tRight 1 chr1 1\n",
                "chr2\t20\tLeft 1 chr2 20\t0\t20\tRight 1 chr2 20\n",
                "chr2\t20\tLeft 1 chr2 20\t-1\t19\tRight 1 chr2 19\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSnpFuzzNClosestToList() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t20\tLeft 1 chr2 20"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t19\tRight 1 chr2 19",
                "chr2\t20\tRight 1 chr2 20",
                "chr2\t21\tRight 1 chr2 21",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t24\tRight 1 chr2 24"};

        String joinQuery = "gor %s | join -f 5 -o 2 -t -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tPosx\tRight1\n",
                "chr1\t1\tLeft 1 chr1 1\t0\t1\tRight 1 chr1 1\n",
                "chr2\t20\tLeft 1 chr2 20\t0,-1\t20,19\tRight 1 chr2 20,Right 1 chr2 19\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSeg() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t20\tRight 1 chr1 1",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t20\t40\tRight 1 chr2 20 40"};

        String joinQuery = "gor %s | join -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tStart\tEnd\tRight1\n",
                "chr1\t10\tLeft 1 chr1 1\t0\t1\t20\tRight 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n",
                "chr2\t23\tLeft 1 chr2 23\t0\t20\t40\tRight 1 chr2 20 40\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSegReducedOutput() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t20\tRight 1 chr1 1",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t20\t40\tRight 1 chr2 20 40"};

        String joinQuery = "gor %s | join -r -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tRight1\n",
                "chr1\t10\tLeft 1 chr1 1\tRight 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\tRight 1 chr2 1 50\n",
                "chr2\t23\tLeft 1 chr2 23\tRight 1 chr2 20 40\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSegToList() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t20\tRight 1 chr1 1",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t20\t40\tRight 1 chr2 20 40"};

        String joinQuery = "gor %s | join -t -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tStart\tEnd\tRight1\n",
                "chr1\t10\tLeft 1 chr1 1\t0\t1\t20\tRight 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\t0,0\t1,20\t50,40\tRight 1 chr2 1 50,Right 1 chr2 20 40\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSegIncludedOnly() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t20\tRight 1 chr1 1",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t20\t40\tRight 1 chr2 20 40"};

        String joinQuery = "gor %s | join -i -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\n",
                "chr1\t10\tLeft 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSegFuzz() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t11\t20\tRight 1 chr1 11 20",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t25\t40\tRight 1 chr2 25 40"};

        String joinQuery = "gor %s | join -f 5 -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tStart\tEnd\tRight1\n",
                "chr1\t10\tLeft 1 chr1 1\t2\t11\t20\tRight 1 chr1 11 20\n",
                "chr2\t23\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n",
                "chr2\t23\tLeft 1 chr2 23\t3\t25\t40\tRight 1 chr2 25 40\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSegFuzzMinOnly() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t11\t20\tRight 1 chr1 11 20",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t25\t40\tRight 1 chr2 25 40"};

        String joinQuery = "gor %s | join -f 5 -m -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tStart\tEnd\tRight1\n",
                "chr1\t10\tLeft 1 chr1 1\t2\t11\t20\tRight 1 chr1 11 20\n",
                "chr2\t23\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSegFuzzNClosest() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t11\t20\tRight 1 chr1 11 20",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t10\t20\tRight 1 chr2 10 20",
                "chr2\t25\t40\tRight 1 chr2 25 40",
                "chr2\t26\t50\tRight 1 chr2 26 50"};

        String joinQuery = "gor %s | join -f 5 -o 2 -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tStart\tEnd\tRight1\n",
                "chr1\t10\tLeft 1 chr1 1\t2\t11\t20\tRight 1 chr1 11 20\n",
                "chr2\t23\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n",
                "chr2\t23\tLeft 1 chr2 23\t-3\t10\t20\tRight 1 chr2 10 20\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSnpSegFuzzNClosestToList() {
        String[] leftLines = {
                "Chrom\tPos\tLeft1",
                "chr1\t10\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t11\t20\tRight 1 chr1 11 20",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t10\t20\tRight 1 chr2 10 20",
                "chr2\t25\t40\tRight 1 chr2 25 40",
                "chr2\t26\t50\tRight 1 chr2 26 50"};

        String joinQuery = "gor %s | join -f 5 -o 2 -t -snpseg %s";

        String[] expected = {
                "Chrom\tPos\tLeft1\tdistance\tStart\tEnd\tRight1\n",
                "chr1\t10\tLeft 1 chr1 1\t2\t11\t20\tRight 1 chr1 11 20\n",
                "chr2\t23\tLeft 1 chr2 23\t0,-3\t1,10\t50,20\tRight 1 chr2 1 50,Right 1 chr2 10 20\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSeg() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t20\tRight 1 chr1 1",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t20\t40\tRight 1 chr2 20 40"
        };

        String joinQuery = "gor %s | join -segseg %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tStartx\tEndx\tRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0\t1\t20\tRight 1 chr1 1\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t20\t40\tRight 1 chr2 20 40\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSegToLines() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t20\tRight 1 chr1 1",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t20\t40\tRight 1 chr2 20 40"
        };

        String joinQuery = "gor %s | join -t -segseg %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tStartx\tEndx\tRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0\t1\t20\tRight 1 chr1 1\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0,0\t1,20\t50,40\tRight 1 chr2 1 50,Right 1 chr2 20 40\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSegIncludedOnly() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t20\tRight 1 chr1 1",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t20\t40\tRight 1 chr2 20 40"
        };

        String joinQuery = "gor %s | join -i -segseg %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\n",
                "chr2\t23\t46\tLeft 1 chr2 23\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSegFuzz() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t8\tRight 1 chr1 1 8",
                "chr1\t1\t20\tRight 1 chr1 1 20",
                "chr1\t22\t40\tRight 1 chr1 22 40",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t10\t20\tRight 1 chr2 10 20"
        };

        String joinQuery = "gor %s | join -f 5 -segseg %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tStartx\tEndx\tRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t-3\t1\t8\tRight 1 chr1 1 8\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0\t1\t20\tRight 1 chr1 1 20\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t3\t22\t40\tRight 1 chr1 22 40\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t-4\t10\t20\tRight 1 chr2 10 20\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSegFuzzMinOnly() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t8\tRight 1 chr1 1 8",
                "chr1\t1\t20\tRight 1 chr1 1 20",
                "chr1\t22\t40\tRight 1 chr1 22 40",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t10\t20\tRight 1 chr2 10 20"
        };

        String joinQuery = "gor %s | join -f 5 -m -segseg %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tStartx\tEndx\tRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0\t1\t20\tRight 1 chr1 1 20\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSegFuzzNClosest() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t8\tRight 1 chr1 1 8",
                "chr1\t1\t20\tRight 1 chr1 1 20",
                "chr1\t22\t40\tRight 1 chr1 22 40",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t10\t20\tRight 1 chr2 10 20"
        };

        String joinQuery = "gor %s | join -f 5 -o 2 -segseg %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tStartx\tEndx\tRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0\t1\t20\tRight 1 chr1 1 20\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t-3\t1\t8\tRight 1 chr1 1 8\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t1\t50\tRight 1 chr2 1 50\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t-4\t10\t20\tRight 1 chr2 10 20\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSegFuzzNClosestToList() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tStart\tEnd\tRight1",
                "chr1\t1\t8\tRight 1 chr1 1 8",
                "chr1\t1\t20\tRight 1 chr1 1 20",
                "chr1\t22\t40\tRight 1 chr1 22 40",
                "chr2\t1\t50\tRight 1 chr2 1 50",
                "chr2\t10\t20\tRight 1 chr2 10 20"
        };

        String joinQuery = "gor %s | join -f 5 -o 2 -t -segseg %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tStartx\tEndx\tRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0,-3\t1,1\t20,8\tRight 1 chr1 1 20,Right 1 chr1 1 8\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0,-4\t1,10\t50,20\tRight 1 chr2 1 50,Right 1 chr2 10 20\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSnp() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tPos\ttRight1",
                "chr1\t15\tRight 1 chr1 15",
                "chr2\t1\tRight 1 chr2 1",
                "chr2\t40\tRight 1 chr2 40"
        };

        String joinQuery = "gor %s | join -segsnp %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tPos\ttRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0\t15\tRight 1 chr1 15\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t40\tRight 1 chr2 40\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSnpToList() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tPos\ttRight1",
                "chr1\t15\tRight 1 chr1 15",
                "chr2\t30\tRight 1 chr2 30",
                "chr2\t40\tRight 1 chr2 40"
        };

        String joinQuery = "gor %s | join -t -segsnp %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tPos\ttRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t0\t15\tRight 1 chr1 15\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0,0\t30,40\tRight 1 chr2 30,Right 1 chr2 40\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSnpIncludedOnly() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tPos\ttRight1",
                "chr1\t15\tRight 1 chr1 15",
                "chr2\t1\tRight 1 chr2 1",
                "chr2\t40\tRight 1 chr2 40"
        };

        String joinQuery = "gor %s | join -i -segsnp %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\n",
                "chr2\t23\t46\tLeft 1 chr2 23\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSnpFuzz() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tPos\ttRight1",
                "chr1\t9\tRight 1 chr1 9",
                "chr2\t1\tRight 1 chr2 1",
                "chr2\t20\tRight 1 chr2 20",
                "chr2\t40\tRight 1 chr2 40",
                "chr2\t50\tRight 1 chr2 50"
        };

        String joinQuery = "gor %s | join -f 5 -segsnp %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tPos\ttRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t-2\t9\tRight 1 chr1 9\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t-4\t20\tRight 1 chr2 20\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t40\tRight 1 chr2 40\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t4\t50\tRight 1 chr2 50\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSnpFuzzMinOnly() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tPos\ttRight1",
                "chr1\t9\tRight 1 chr1 9",
                "chr2\t1\tRight 1 chr2 1",
                "chr2\t20\tRight 1 chr2 20",
                "chr2\t40\tRight 1 chr2 40",
                "chr2\t50\tRight 1 chr2 50"
        };

        String joinQuery = "gor %s | join -f 5 -m -segsnp %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tPos\ttRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t-2\t9\tRight 1 chr1 9\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t40\tRight 1 chr2 40\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSnpFuzzNClosest() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tPos\ttRight1",
                "chr1\t9\tRight 1 chr1 9",
                "chr2\t1\tRight 1 chr2 1",
                "chr2\t20\tRight 1 chr2 20",
                "chr2\t40\tRight 1 chr2 40",
                "chr2\t50\tRight 1 chr2 50"
        };

        String joinQuery = "gor %s | join -f 5 -o 2 -segsnp %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tPos\ttRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t-2\t9\tRight 1 chr1 9\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0\t40\tRight 1 chr2 40\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t-4\t20\tRight 1 chr2 20\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinSegSnpFuzzNClosestToList() {
        String[] leftLines = {
                "Chrom\tStart\tEnd\tLeft1",
                "chr1\t10\t20\tLeft 1 chr1 10 20",
                "chr2\t23\t46\tLeft 1 chr2 23"
        };
        String[] rightLines = {
                "Chrom\tPos\ttRight1",
                "chr1\t9\tRight 1 chr1 9",
                "chr2\t1\tRight 1 chr2 1",
                "chr2\t20\tRight 1 chr2 20",
                "chr2\t40\tRight 1 chr2 40",
                "chr2\t50\tRight 1 chr2 50"
        };

        String joinQuery = "gor %s | join -f 5 -o 2 -t -segsnp %s";

        String[] expected = {
                "Chrom\tStart\tEnd\tLeft1\tdistance\tPos\ttRight1\n",
                "chr1\t10\t20\tLeft 1 chr1 10 20\t-2\t9\tRight 1 chr1 9\n",
                "chr2\t23\t46\tLeft 1 chr2 23\t0,-4\t40,20\tRight 1 chr2 40,Right 1 chr2 20\n"
        };

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinWithIdenticalColumnNamesInLeftAndRightFiles() {
        String[] leftLines = {
                "Chrom\tPos\tData",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tData",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tData\tDatax\n",
                "chr1\t1\tLeft 1 chr1 1\tRight 1 chr1 1\n",
                "chr2\t23\tLeft 1 chr2 23\tRight 1 chr2 23\n",
                "chr2\t23\tLeft 1 chr2 23\tRight 1 chr2 23 repeat\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }

    @Test
    public void joinWithOverlapCountColumnInLeftFile() {
        String[] leftLines = {
                "Chrom\tPos\tOverlapCount",
                "chr1\t1\tLeft 1 chr1 1",
                "chr2\t23\tLeft 1 chr2 23"};
        String[] rightLines = {
                "Chrom\tPos\tRight1",
                "chr1\t1\tRight 1 chr1 1",
                "chr2\t23\tRight 1 chr2 23",
                "chr2\t23\tRight 1 chr2 23 repeat"};

        String joinQuery = "gor %s | join -ic -snpsnp %s";

        String[] expected = {
                "Chrom\tPos\tOverlapCount\tOverlapCountx\n",
                "chr1\t1\tLeft 1 chr1 1\t1\n",
                "chr2\t23\tLeft 1 chr2 23\t2\n"};

        TestUtils.assertJoinQuery(leftLines, rightLines, joinQuery, expected);
    }
}
