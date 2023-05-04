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

package org.gorpipe.gor.driver;

import gorsat.TestUtils;
import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class UTestRow {

    @Test
    public void testRowBase() {
        Row row;
        row = RowObj.StoR("chr1\t117\tcolx\tcolxx\tcolxxx");
        row.removeColumn(2);
        assert row.toString().equals("chr1\t117\tcolxx\tcolxxx");

        row = RowObj.StoR("chr1\t117\tcolx\tcolxx\tcolxxx");
        row.removeColumn(3);
        assert row.toString().equals("chr1\t117\tcolx\tcolxxx");

        row = RowObj.StoR("chr1\t117\tcolx\tcolxx\tcolxxx");
        row.removeColumn(4);
        assert row.toString().equals("chr1\t117\tcolx\tcolxx");
    }

    @Test
    public void testParsingInfValueAtEndOfLine() throws IOException {
        File testFile = Files.createTempFile("genes", DataType.TSV.suffix).toFile();
        testFile.deleteOnExit();
        FileUtils.writeStringToFile(testFile, "#ID1\tSID1\tID2\tSID2\tNSNP\tHETHET\tIBS0\tKINSHIP\n" +
                "GD513848301\tGD513848301\tGD504132801\tGD504132801\t417724\t0.192575\t0.0947516\t-inf", Charset.defaultCharset());

        String query = String.format("nor %s | rename id1 PN | rename id2 PN2 | hide sid1,sid2 | where kinship > 0.0441", testFile);
        TestUtils.runGorPipe(query);
        // No exception
    }
}
