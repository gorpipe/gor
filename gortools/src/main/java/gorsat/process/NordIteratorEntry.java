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

package gorsat.process;

import org.gorpipe.exceptions.GorDataException;

public class NordIteratorEntry {

    private final String tag;
    private final String filePath;

    public NordIteratorEntry(String filePath, String tag) {
        this.tag = tag;
        this.filePath = filePath;
    }

    public String getTag() {
        return this.tag;
    }

    public String getFilePath() {
        return filePath;
    }

    public static NordIteratorEntry parse(String entry) {
        String[] entries = entry.split("\t");

        if (entries.length < 2) {
            throw new GorDataException("Nord file requires at least two columns, id and file");
        }

        return new NordIteratorEntry(entries[0], entries[1]);
    }

    @Override
    public String toString() {
        return filePath + "\t" + tag;
    }
}
