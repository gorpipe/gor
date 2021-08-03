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

import org.gorpipe.gor.model.FileReader;

import java.io.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

class PVarWriter implements AutoCloseable {
    private final String fileName;
    private Writer writer;
    private boolean first = true;
    private Map<String,String> chrToNum;
    private Optional<FileReader> fileReader;

    PVarWriter(String fileName) {
        this.fileName = fileName;
        this.fileReader = Optional.empty();
        init();
    }

    PVarWriter(String fileName, FileReader fileReader) {
        this.fileName = fileName;
        this.fileReader = Optional.ofNullable(fileReader);
        init();
    }

    void init() {
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
            this.writer = new BufferedWriter(fileReader.isPresent() ? new OutputStreamWriter(fileReader.get().getOutputStream(fileName)) : new FileWriter(this.fileName));
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
