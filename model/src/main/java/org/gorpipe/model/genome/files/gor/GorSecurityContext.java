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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * GorSecurityContext manages the current values of various Gor security keys that need to be provided
 * when running a gor query.
 *
 * @version $Id$
 */
public class GorSecurityContext {
    /**
     * Shared context for process wide context
     */
    public static final GorSecurityContext shared = new GorSecurityContext();

    private String context = "";
    private Map<String, String> values = Collections.synchronizedMap(new LinkedHashMap<String, String>());
    private static final String defaultKey = "GorDefaultKey";

    /**
     * Construct the security context
     */
    public GorSecurityContext() {
        setDefault("");  // Must allways contain the default key as the first key, even if it is empty
    }

    /**
     * Set the value of the default key
     *
     * @param value
     * @return The context object to allow chaining
     */
    public GorSecurityContext setDefault(String value) {
        return set(defaultKey, value);
    }

    /**
     * @param key   The key to set or null for default keyless item
     * @param value The value to set
     * @return The context object to allow chaining
     */
    public GorSecurityContext set(String key, String value) {
        // Set the new value in the map of all values
        values.put(key, value);

        // Reconstruct the security key from the map
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            final String val = entry.getValue();
            if (entry.getKey().equals(defaultKey)) {
                sb.append(val);
            } else if (val != null && val.length() > 0) {
                if (sb.length() > 0) {
                    sb.append("|||"); // separator for different keys
                }
                sb.append(entry.getKey());
                sb.append('=');
                sb.append(val);
            }
        }

        // Replace the context with the new value
        context = sb.toString();

        return this;
    }

    /**
     * @return The current security context string to use with gor query
     */
    public String get() {
        return context;
    }


    /**
     * @param key The identifying key
     * @return The associated value
     */
    public String get(String key) {
        return values.get(key);
    }
}
