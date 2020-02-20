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

package gorsat.external.plink;

public class AdjustedGORLine implements Comparable<AdjustedGORLine> {
    String currentLine;
    String currentChr;
    int currentPos;

    public AdjustedGORLine(String line) {
        currentLine = line;
        init();
    }

    public String toString() {
        return currentLine;
    }

    public void init() {
        int i = currentLine.indexOf("\t");
        int u = currentLine.indexOf("\t", i + 1);
        currentChr = currentLine.substring(0, i);
        currentPos = Integer.parseInt(currentLine.substring(i + 1, u));
    }

    public int compareTo(AdjustedGORLine other) {
        int ret = currentChr.compareTo(other.currentChr);
        if (ret == 0) ret = currentPos - other.currentPos;
        return ret;
    }
}
