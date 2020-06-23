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

package org.gorpipe.base.logging;

import java.lang.management.ManagementFactory;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Created by gisli on 21/02/2017.
 */

public class ProcessIdConverter extends ClassicConverter {
    private static final String PROCESS_ID = getProcessId();

    @Override
    public String convert(final ILoggingEvent event) {
        // for every logging event return processId from mx bean
        // (or better alternative)
        return PROCESS_ID;
    }

    public static String getProcessId() {
        try {
            return ManagementFactory.getRuntimeMXBean().getName().split("[@]", 2)[0];
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
