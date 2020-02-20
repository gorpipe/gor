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

package org.gorpipe.gor.cli;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriver;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.driver.meta.SourceReference;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Main class to get meta information about gor files/urls resolved through the
 * gor driver.
 */
public class GorMeta {
    private String file;
    private boolean followLink;
    private GorDriver gorDriver;

    public GorMeta(String file, boolean followLink) {
        this.file = file;
        this.followLink = followLink;
        this.gorDriver = GorDriverFactory.fromConfig();
    }

    public int report(PrintStream out) throws IOException {
        DataSource source = gorDriver.resolveDataSource(new SourceReference(file));
        if (source == null) {
            out.println("Unknown data source " + file);
            return -1;
        }

        while (followLink && source.getDataType() == DataType.LINK) {
            String linksTo = GorDriverFactory.fromConfig().readLink(source);
            source.close();
            source = gorDriver.resolveDataSource(new SourceReference(linksTo));
        }

        SourceMetadata meta = source.getSourceMetadata();

        Map<String, String> map = meta.attributes();
        for (String key : map.keySet()) {
            out.println(key + ": " + map.get(key));
        }
        source.close();
        return 0;
    }

    public static void main(String[] args) throws IOException {
        String file;
        boolean followLink = true;

        if (args.length == 0 || args.length > 2) {
            help();
        }
        if (args.length == 1) {
            file = args[0];
        } else {
            if (args[0].equalsIgnoreCase("-l")) {
                followLink = false;
            } else {
                help();
            }
            file = args[1];
        }
        System.exit(new GorMeta(file, followLink).report(System.out));
    }

    private static void help() {
        System.err.println(
                "Usage: gormeta [-l] <file>\n\n" +
                        "Shows Gor Driver meta information for a file/url\n" +
                        "Follows .link files unless -l flag is given\n" +
                        "Example: gormeta .. file.bam"
        );
        System.exit(-1);
    }
}
