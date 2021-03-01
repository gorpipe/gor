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

package org.gorpipe.base.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.io.IOUtils;

/**
 * Created by gisli on 21/02/2017.
 */

public class HostnameConverter extends ClassicConverter {
    private static final String HOSTNAME = getHostName();

    @Override
    public String convert(final ILoggingEvent event) {
        return HOSTNAME;
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                // Windows will always set the 'COMPUTERNAME' variable
                return System.getenv("COMPUTERNAME");
            } else {
                try {
                    Process p = Runtime.getRuntime().exec("hostname");
                    p.waitFor();
                    return IOUtils.toString(p.getInputStream(), Charset.defaultCharset()).trim();
                } catch (Exception e) {
                    // Ignore
                }

                // Most modern shells (such as Bash or derivatives) sets the
                // HOSTNAME variable so lets try that at last.
                String hostname = System.getenv("HOSTNAME");
                if (hostname != null) {
                    return hostname;
                } else {
                    return "Unknown";
                }
            }
        }
    }
}


