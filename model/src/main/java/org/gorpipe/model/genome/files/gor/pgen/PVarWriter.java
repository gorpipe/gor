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

package org.gorpipe.model.genome.files.gor.pgen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class PVarWriter implements AutoCloseable {
    private final String fileName;
    private BufferedWriter writer;
    private boolean first = true;

    PVarWriter(String fileName) {
        this.fileName = fileName;
    }

    void write(CharSequence chr, int pos, CharSequence rsId, CharSequence ref, CharSequence alt) throws IOException {
        if (this.first) {
            this.writer = new BufferedWriter(new FileWriter(this.fileName));
            this.writer.write("#CHROM\tID\tPOS\tALT\tREF\n");
            this.first = false;
        }
        final String toWrite = String.join("\t", getChrNum(chr), rsId, String.valueOf(pos), alt, ref) + "\n";
        this.writer.write(toWrite);
    }

    @Override
    public void close() throws IOException {
        if (!this.first) {
            this.writer.close();
        }
    }

    static CharSequence getChrNum(CharSequence chr) {
        if (chr.charAt(3) > '9') { //chr = X, Y, XY, MT
            if (chr.length() == 5) {
                if (chr.charAt(4) == 'Y') return "25";
                else return "26";
            } else {
                if (chr.charAt(3) == 'X') return "23";
                else return "24";
            }
        } else {
            return chr.subSequence(3, chr.length());
        }
    }
}
