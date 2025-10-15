package org.gorpipe.gor.util;

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

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

public class UTestCommandSubstitutions {

    @Test
    public void testUpdateMapFromSecurityContext() {
        String myCommand1 = CommandSubstitutions.commandSubstitutions("#{projectid}",
                CommandSubstitutions.updateMapFromSecurityContext("dbscope=project_id#int#1", new HashMap<>()));
        Assert.assertEquals("1", myCommand1);

        String myCommand2 = CommandSubstitutions.commandSubstitutions("#{projectid}",
                CommandSubstitutions.updateMapFromSecurityContext("dbscope=organization#int#1", new HashMap<>()));
        Assert.assertEquals("#{projectid}", myCommand2);

        String myCommand3 = CommandSubstitutions.commandSubstitutions("",
                CommandSubstitutions.updateMapFromSecurityContext("dbscope=project_id#int#1", new HashMap<>()));
        Assert.assertEquals("", myCommand3);

        String myCommand4 = CommandSubstitutions.commandSubstitutions("#{projectid}",
                CommandSubstitutions.updateMapFromSecurityContext("dbscope=project_id#int#1,organization_id#int#1", new HashMap<>()));
        Assert.assertEquals("1", myCommand4);

        String myCommand5 = CommandSubstitutions.commandSubstitutions("#{organizationId}",
                CommandSubstitutions.updateMapFromSecurityContext("dbscope=project_id#int#1,organization_id#int#1", new HashMap<>()));
        Assert.assertEquals("#{organizationId}", myCommand5);

        String myCommand6 = CommandSubstitutions.commandSubstitutions("#{projectid}",
                CommandSubstitutions.updateMapFromSecurityContext("", new HashMap<>()));
        Assert.assertEquals("#{projectid}", myCommand6);
    }

    @Test
    public void testUpdateMapWithProjectInfo() throws IOException {
        String myCommand1 = CommandSubstitutions.commandSubstitutions("#{projectid}",
                CommandSubstitutions.updateMapWithProjectInfo(TestUtils.createSession(true,  "dbscope=project_id#int#1"), new HashMap<>()));
        Assert.assertEquals("1", myCommand1);
    }
}

