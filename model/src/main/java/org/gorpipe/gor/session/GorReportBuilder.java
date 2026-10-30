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

package org.gorpipe.gor.session;

import java.util.Map;

public abstract class GorReportBuilder {
    public abstract String parse(String query);

    /**
     * Renders a report into a query, including its leading input source (e.g. gor or nor).
     *
     * @param resource   yml report builder file or url
     * @param reportName name of the report within the yml, or null for the first report
     * @param parameters report arguments, a parameter without a value selects the query entry to render
     * @return the rendered query
     */
    public String renderQuery(String resource, String reportName, Map<String, String> parameters) {
        throw new UnsupportedOperationException(getClass().getName() + " does not support rendering report queries");
    }
}
