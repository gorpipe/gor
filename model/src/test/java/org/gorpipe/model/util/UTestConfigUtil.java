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

package org.gorpipe.model.util;

import org.gorpipe.test.utils.FileTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class UTestConfigUtil {

    protected static File gorPropsFile;

    @Before
    public void setup() throws IOException {
        File scriptFile = new File(ConfigUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        // Check on defaults location for config file
        String fileName = scriptFile.getParent() + "/../config/";
        gorPropsFile = FileTestUtils.createTempFile(new File(fileName), "gor.props.defaults", "PROP1=A\nPROP2=B,C");
    }

    @Test
    public void testConfigUtil() {
        ConfigUtil.loadConfig("gor");
        Properties properties = System.getProperties();
        Assert.assertEquals("A", properties.get("PROP1"));
        Assert.assertEquals("B,C", properties.get("PROP2"));
    }
}