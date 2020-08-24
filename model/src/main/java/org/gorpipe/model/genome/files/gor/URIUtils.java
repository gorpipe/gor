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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sigmar on 17/08/15.
 */
public class URIUtils {
    public static String getHost(URI uri) {
        String host = uri.getHost();

        // underscore in hostname, http://bugs.java.com/view_bug.do?bug_id=6587184
        if (host == null) {
            String uristr = uri.toString();
            int i = uristr.indexOf("://");
            int k = uristr.indexOf('/', i + 3);
            if (k == -1) {
                k = uristr.indexOf('?', i + 3);
                if (k == -1) {
                    k = uristr.length();
                }
            }
            host = uristr.substring(i + 3, k);
        }
        return host;
    }

    public static Map<String, String> getParams(URI uri) {
        Map<String, String> params = new HashMap<String, String>();
        String query = uri.getQuery();
        if (query != null && query.length() > 0) {
            String[] queryarray = query.split("&");
            for (String param : queryarray) {
                int splitind = param.indexOf('=');
                if (splitind == -1) params.put(param, "");
                else params.put(param.substring(0, splitind), param.substring(splitind + 1));
            }
        }
        return params;
    }
}
