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

package org.gorpipe.gor.driver;

import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;
import org.junit.Test;

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
}
