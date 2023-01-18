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

package org.gorpipe.gor.model;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.junit.Assert;
import org.junit.Test;

public class UTestGorExceptionMessage {

    @Test
    public void resourceNotFound() {

        try {
            TestUtils.runGorPipeCount("gor #dbsnp# | top 10");
        } catch (GorResourceException e) {
            Assert.assertEquals("Input source does not exist: #dbsnp#", e.getMessage());
        }
    }

    @Test
    public void resourceNotFoundNorGorComparison() {
        String gorMessage = "";
        String norMessage = "";
        try {
            TestUtils.runGorPipeCount("gor #dbsnp# | top 10");
        } catch (GorResourceException e) {
            gorMessage = e.getMessage();
        }
        try {
            TestUtils.runGorPipeCount("nor #dbsnp# | top 10");
        } catch (GorResourceException e) {
            norMessage = e.getMessage();
        }
        Assert.assertEquals(norMessage, gorMessage);
    }

}
