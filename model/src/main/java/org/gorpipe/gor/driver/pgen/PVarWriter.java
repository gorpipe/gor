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

package org.gorpipe.gor.driver.pgen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

class PVarWriter implements AutoCloseable {
    private final String fileName;
    private BufferedWriter writer;
    private boolean first = true;
    private Map<String,String> chrToNum;

    PVarWriter(String fileName) {
        this.fileName = fileName;
        chrToNum = new HashMap<>();
        IntStream.range(1,23).forEach(i -> chrToNum.put("chr"+i,""+i));
        chrToNum.put("chrX",Integer.toString(chrToNum.size()+1));
        chrToNum.put("chrY",Integer.toString(chrToNum.size()+1));
        chrToNum.put("chrXY",Integer.toString(chrToNum.size()+1));
        chrToNum.put("chrMT",Integer.toString(chrToNum.size()+1));
        chrToNum.put("chrM",Integer.toString(chrToNum.size()));
    }

    void write(String chr, int pos, CharSequence rsId, CharSequence ref, CharSequence alt) throws IOException {
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

    public String getChrNum(String chr) {
        if(chrToNum.containsKey(chr)) {
            return chrToNum.get(chr);
        } else {
            String ret = Integer.toString(chrToNum.size());
            chrToNum.put(chr, ret);
            return ret;
        }
    }
}
