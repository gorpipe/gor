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

package org.gorpipe.model.genome.files.gor;

import junit.framework.TestCase;

/**
 * Test the JoinDataCache
 *
 * @version $Id$
 */
public class UTestGorCommand extends TestCase {

    /**
     * Test simple operations on the data cache
     */
    public void testGetWithoutComments() {

        GorCommand gc = new GorCommand("gor /* this is /*inner*/first comment /* Nested */ */c:/data/test./* comment*/gor");
        assertEquals(gc.getWithoutComments(), "gor c:/data/test.gor");
        assertEquals(gc.posWithComment(3), 3);
        assertEquals(gc.posWithComment(4), 53);
        assertEquals(gc.posWithComment(9), 58);
        assertEquals(gc.posWithComment(14), 63);
        assertEquals(gc.posWithComment(18), 79);
    }

    /**
     * Test SQL hints mechanism
     */
    public void testGetWithoutCommentsWithSqlHints() {
        GorCommand gc = new GorCommand("gor /*+ BROADCAST(a) */ /* this is /*inner*/first comment /* Nested */ */c:/data/test./* comment*/gor");
        assertEquals(gc.getWithoutComments(), "gor /*+ BROADCAST(a) */ c:/data/test.gor");
    }
}
