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

package org.gorpipe.gor.function;

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.session.GorSession;

/**
 * A function source is an input source written as a function call, name(arg1, arg2, ...), which resolves to a
 * query. It is read as a nested query, i.e. it is accepted wherever &lt;(query) is accepted.
 */
public interface FunctionSource {

    String name();

    /**
     * @param args    the arguments of the function call, trimmed
     * @param session session used to resolve the query
     * @return the query, including its leading input source (e.g. gor or nor), the function source reads from
     */
    String query(String[] args, GorSession session);

    /**
     * @param args    the arguments of the function call, trimmed
     * @param session session used to resolve the pipe steps
     * @return the pipe steps, e.g. for the PIPESTEPS command, the function source resolves to
     */
    default String pipeSteps(String[] args, GorSession session) {
        throw new GorParsingException(name() + " can not be used as pipe steps");
    }
}
