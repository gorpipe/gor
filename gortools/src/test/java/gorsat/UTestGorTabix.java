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

import org.gorpipe.util.collection.extract.Extract;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 26/10/15.
 */
public class UTestGorTabix {

    private void doTest(String gor, String file, int expected) {
        String query = gor + " " + file + " | top 10";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(expected, count);
    }

    @Test
    public void testGorTabix() {
        doTest("gor", "../tests/data/gor/dbsnp_test.gor.gz", 10);
    }

    @Test
    public void testGorTabixSeek() {
        doTest("gor -p chr2:1-", "../tests/data/gor/dbsnp_test.gor.gz", 2);
    }

    @Test
    public void testVcfTabixSeek() {
        doTest("gor -p chr2:1-", "../tests/data/gor/dbsnp_test.gor.gz", 2);
    }

    @Test
    public void testOutput() {
        String query = "gor ../tests/data/gor/dbsnp_test.gor.gz  | top 10";
        String res = TestUtils.runGorPipe(query);
        Assert.assertEquals("C5F078A2E93799FC4B294EAEE835B035", Extract.md5(res));
    }

    @Test
    public void testCompareOutputs(){
        String resGor = TestUtils.runGorPipeNoHeader("gor  ../tests/data/gor/dbsnp_test.gor | top 10");
        String resGorGz = TestUtils.runGorPipeNoHeader("gor ../tests/data/gor/dbsnp_test.gor.gz | top 10");
        Assert.assertEquals(Extract.md5(resGor),Extract.md5(resGorGz));
    }

}
