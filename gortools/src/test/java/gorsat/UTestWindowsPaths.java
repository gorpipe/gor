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

import org.gorpipe.exceptions.GorResourceException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 07/02/2017.
 */
public class UTestWindowsPaths {
    @Test
    //Just make sure gor resolves a non existing windows path as a file resulting in FileNotFoundException
    public void testWindowsPath() {
        String query = "gor c:\\windows.gor";

        boolean gotfilenotfounderror = false;
        try {
            TestUtils.runGorPipeCount(query);
        } catch (Exception e) {
            gotfilenotfounderror = e instanceof GorResourceException;
        }
        Assert.assertTrue(gotfilenotfounderror);
    }
}
