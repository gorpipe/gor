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

package org.gorpipe.gor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class UTestGorScriptAnalyzer {
    GorScriptAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new GorScriptAnalyzer();
    }

    @Test
    public void empty() {
        analyzer.parse("");
        Collection<GorScriptTask> tasks = analyzer.getTasks();
        Assert.assertEquals(0, tasks.size());
    }

    @Test
    public void simpleStatement() {
        analyzer.parse("gor test.mem");
        Collection<GorScriptTask> tasks = analyzer.getTasks();
        Assert.assertEquals(1, tasks.size());
    }

    @Test
    public void twoStepScript() {
        analyzer.parse("create x=gor test.mem;gor [x]");
        Collection<GorScriptTask> tasks = analyzer.getTasks();
        Assert.assertEquals(2, tasks.size());
    }

    @Test
    public void nonsenseScript() {
        analyzer.parse("this is not a script");
    }
}