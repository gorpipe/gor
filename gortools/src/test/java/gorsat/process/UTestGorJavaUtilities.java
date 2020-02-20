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

package gorsat.process;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class UTestGorJavaUtilities {


    @Test
    public void testProjectIdReplacement() {
        String myCommand1 = GorJavaUtilities.projectIdReplacement("dbscope=project_id#int#1", "#{projectid}");
        String myCommand2 = GorJavaUtilities.projectIdReplacement("dbscope=organization#int#1", "#{projectid}");
        String myCommand3 = GorJavaUtilities.projectIdReplacement("dbscope=project_id#int#1", "");
        String myCommand4 = GorJavaUtilities.projectIdReplacement("dbscope=project_id#int#1,organization_id#int#1", "#{projectid}");
        String myCommand5 = GorJavaUtilities.projectIdReplacement("dbscope=project_id#int#1,organization_id#int#1", "#{organizationId}");
        String myCommand6 = GorJavaUtilities.projectIdReplacement("", "#{projectid}");
        Assert.assertEquals("1", myCommand1);
        Assert.assertEquals("#{projectid}", myCommand2);
        Assert.assertEquals("", myCommand3);
        Assert.assertEquals("1", myCommand4);
        Assert.assertEquals("#{organizationId}", myCommand5);
        Assert.assertEquals("#{projectid}", myCommand6);
    }

    @Test
    public void testProjectReplacement() throws IOException {
        String myCommand1 = GorJavaUtilities.projectReplacement("#{projectid}", "", "", "dbscope=project_id#int#1");
        Assert.assertEquals("1", myCommand1);
    }
}
