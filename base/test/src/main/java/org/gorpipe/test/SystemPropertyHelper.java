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

package org.gorpipe.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class to keep track of system properties being set and to either restore or remove them when instructed.
 */
public class SystemPropertyHelper {
    // Use HashMap because we want to be able to store null values
    Map<String, String> oldValues = new HashMap<>();

    /**
     * Sets a system property and maintains the old value of that property to reset when instructed.
     * The
     *
     * @param key
     * @param value
     */
    public void setSystemProperty(String key, String value) {
        String oldValue = System.setProperty(key, value);

        // Only store the "first old value" if called multiple times for the same key.
        oldValues.putIfAbsent(key, oldValue);
    }

    public void setSystemProperties(Map<String, String> properties) {
        for (Map.Entry<String,String> entry : properties.entrySet()) {
            setSystemProperty(entry.getKey(), entry.getValue());
        }
    }

    public void reset() {
        // Loop through all keys that have been set and either reset to old value or remove
        List<String> keys = new ArrayList<>(oldValues.keySet());
        for (String key : keys) {
            String oldValue = oldValues.remove(key);
            if (oldValue == null) {
                System.getProperties().remove(key);
            } else {
                System.setProperty(key, oldValue);
            }
        }
    }
}
