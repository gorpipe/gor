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

package gorsat.process;

import org.gorpipe.model.genome.files.gor.Row;
import gorsat.Commands.Analysis;

import java.util.Arrays;

/**
 * Created by sigmar on 16/08/2016.
 */
public class NorToGorPipeStep extends Analysis {
    String header;
    int[] selcol;

    public NorToGorPipeStep(String header) {
        String[] spl = header.split("\t");
        selcol = new int[spl.length - 2];
        for (int i = 2; i < spl.length; i++) selcol[i - 2] = i;
        this.header = String.join("\t", Arrays.copyOfRange(spl, 2, spl.length));
    }

    @Override
    public void process(Row row) {
        Row r = row.rowWithSelectedColumns(selcol);
        super.process(r);
    }

    @Override
    public String getHeader() {
        return header;
    }
}
