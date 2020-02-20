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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class merges gor lines from multiple BufferedReaders. Use with PriorityQueue.
 */
public class GORLine implements Comparable<GORLine> {
    private Path p;
    private BufferedReader br;
    private String currentLine;
    private String currentChr;
    private int currentPos;
    private String name;
    private String header;

    GORLine(String name, Path p) throws IOException {
        this.p = p;
        this.name = name;
        this.next();
    }

    public String toString() {
        if( p != null ) {
            return name == null ? "chr" + currentLine : "chr" + currentLine + "\t" + name;
        } else return currentLine;
    }

    public String getHeader() {
        return header;
    }

    public void close() throws IOException {
        br.close();
    }

    public GORLine next() throws IOException {
        if( br == null ) {
            br = Files.newBufferedReader(p);
            header = br.readLine();
        }
        currentLine = br.readLine();
        if (currentLine != null) {
            int i = currentLine.indexOf('\t');
            int k = currentLine.indexOf('\t', i + 1);
            currentChr = currentLine.substring(0, i);
            currentPos = Integer.parseInt(currentLine.substring(i + 1, k));
            return this;
        }
        close();
        return null;
    }

    @Override
    public int compareTo(GORLine other) {
        int ret = currentChr.compareTo(other.currentChr);
        if (ret == 0)
            return currentPos - other.currentPos;
        return ret;
    }
}
