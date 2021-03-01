/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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
package org.gorpipe.querydialogs.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings("javadoc")
public class ValueFormatter {
    public static final String DEFAULT_FORMAT = "default";
    public static final String EMPTY_FORMAT = "empty";
    public static final String KEYWORDS_FORMAT = "keywords";
    public static final String VALUES_FORMAT = "values";

    private final Map<Object, String> formats = new HashMap<Object, String>();

    /**
     * @param formats
     */
    public ValueFormatter(Map<Object, String> formats) {
        this.formats.putAll(formats);
        if (!this.formats.containsKey(DEFAULT_FORMAT)) {
            this.formats.put(DEFAULT_FORMAT, "%s");
        } else if (this.formats.containsKey(this.formats.get(DEFAULT_FORMAT))) {
            this.formats.put(DEFAULT_FORMAT, this.formats.get(this.formats.get(DEFAULT_FORMAT)));
        }
    }

    /**
     * @param key
     * @param values
     * @return the given values formatted according to format identified by given key
     */
    public String format(Object key, Object... values) {
        if (formats.containsKey(key)) {
            String f = formats.get(key);
            if (f == null) return "";
            return String.format(formats.get(key), values);
        }
        return null;
    }

    /**
     * Check if value formatter has empty format defined.
     *
     * @return <code>true</code> if empty format is defined, otherwise <code>false</code>
     */
    public boolean hasEmptyFormat() {
        return formats.containsKey(EMPTY_FORMAT);
    }

}
