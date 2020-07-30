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

import org.gorpipe.gor.GorContext;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.gor.iterators.RowSource;

public class OptionEvaluator {
    private final GorContext context;

    public OptionEvaluator(GorContext context) {
        this.context = context;
    }

    String getValue(String query, int column) {
        PipeInstance pipeInstance = new PipeInstance(context);
        pipeInstance.init(query, false, "");
        RowSource iterator = pipeInstance.getRowSource();
        if (iterator.hasNext()) {
            Row row = iterator.next();
            return row.stringValue(column - 1);
        }
        return "";
    }
}