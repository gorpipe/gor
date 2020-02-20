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

import org.gorpipe.gor.GorSession;
import org.gorpipe.model.genome.files.gor.QueryEvaluator;
import org.gorpipe.model.gor.iterators.RowSource;

import java.util.LinkedList;
import java.util.List;

public class SessionBasedQueryEvaluator extends QueryEvaluator {

    GorSession session;

    public SessionBasedQueryEvaluator(GorSession session) {
        this.session = session;
    }

    @Override
    public List<String> asList(String query) {
        List<String> result = new LinkedList<>();
        try (PipeInstance pipe = PipeInstance.createGorIterator(session.getGorContext())) {
            pipe.init(query, null);
            RowSource iterator = pipe.theIterator();

            while (iterator.hasNext()) {
                result.add(iterator.next().otherCols());
            }
        }

        return result;
    }

    @Override
    public String asValue(String query) {
        return String.join(",", asList(query));
    }
}
