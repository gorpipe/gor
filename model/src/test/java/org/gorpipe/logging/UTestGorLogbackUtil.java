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

package org.gorpipe.logging;

import ch.qos.logback.classic.LoggerContext;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UTestGorLogbackUtil {

    @Test
    public void logServiceStatus() {
        Logger logger = LoggerFactory.getLogger(UTestGorLogbackUtil.class);
        GorLogbackUtil.logServiceStatus(logger);
    }

    @Test
    public void initServiceLog() {
        GorLogbackUtil.initServiceLog();
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
        Map<String, String> propertyMap = context.getCopyOfPropertyMap();

        Assert.assertTrue(propertyMap.containsKey("gor_service"));
        Assert.assertTrue(propertyMap.containsKey("processid"));
        Assert.assertTrue(propertyMap.containsKey("csa_env"));
        Assert.assertTrue(propertyMap.containsKey("environment"));
    }
}