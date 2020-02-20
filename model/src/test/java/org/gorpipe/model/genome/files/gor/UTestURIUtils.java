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

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.model.genome.files.gor.URIUtils;
import junit.framework.TestCase;

import java.net.URI;
import java.util.Map;

/**
 * Created by sigmar on 26/08/15.
 */
public class UTestURIUtils extends TestCase {
    public void testURIParse() {
        String uristr = "http://test_server/dir/subdir/file.txt?param=something==&otherparam";
        URI uri = URI.create(uristr);
        Map<String, String> params = URIUtils.getParams(uri);
        String host = URIUtils.getHost(uri);

        assertTrue(host.equals("test_server"));
        assertTrue(params.containsKey("param") && params.containsKey("otherparam"));
        assertTrue(params.get("param").equals("something=="));
        assertTrue(params.get("otherparam").length() == 0);
    }
}
