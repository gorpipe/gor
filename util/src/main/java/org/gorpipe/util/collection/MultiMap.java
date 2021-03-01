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

package org.gorpipe.util.collection;

import java.io.Serializable;
import java.util.*;

/**
 * Maps a single key into a list of values
 *
 * @param <K> The key type
 * @param <V> The value type
 * @version $Id$
 */
public class MultiMap<K, V> implements Serializable {
    private final HashMap<K, ArrayList<V>> map = new HashMap<K, ArrayList<V>>();

    /**
     * @return The number of keys in the map
     */
    public int size() {
        return map.size();
    }

    /**
     * @return The total number of values combined from all keys
     */
    public int countValues() {
        int sum = 0;
        for (ArrayList<V> values : map.values()) {
            sum += values.size();
        }
        return sum;
    }

    /**
     * @return Returns true if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * @param key The key to check for
     * @return True if the key is found in the map, else false
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    /**
     * @return A key with the most values. If multiple such keys are found, anyone of them can be returned.
     */
    public K keyWithMostValues() {
        K key = null;
        int maxSize = -1;
        for (Map.Entry<K, ArrayList<V>> entry : map.entrySet()) {
            if (entry.getValue().size() > maxSize) {
                key = entry.getKey();
                maxSize = entry.getValue().size();
            }
        }

        return key;
    }

    /**
     * @param key
     * @param value
     * @return True if the map contains the provided value, stored with the provided key
     */
    public boolean contains(K key, V value) {
        return get(key).contains(value);
    }

    /**
     * @param key
     * @param value
     * @return An object stored with provided key that is equal to provided value. Null if no such object exists.
     */
    public V get(K key, V value) {
        List<V> list = get(key);
        for (V v : list) {
            if (v.equals(value)) {
                return v;
            }
        }
        return null;
    }

    /**
     * @return The keys in the map
     */
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * @return A collection of all the value lists
     */
    private Collection<ArrayList<V>> values() {
        return map.values();
    }

    /**
     * @return An array list of all entries from all lists stored in the map
     */
    public ArrayList<V> getAllValues() {
        ArrayList<V> ret = new ArrayList<V>();
        for (ArrayList<V> value : values()) {
            ret.addAll(value);
        }
        return ret;
    }

    /**
     * Add a value to the map
     *
     * @param key   The key to use
     * @param value The value to add
     */
    public void put(K key, V value) {
        ArrayList<V> values = map.computeIfAbsent(key, k -> new ArrayList<V>());
        values.add(value);
    }

    /**
     * Get a list of the values added
     *
     * @param key the key
     * @return The values
     */
    public ArrayList<V> get(K key) {
        ArrayList<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<V>();
        }
        return list;
    }

    /**
     * Removes all of the mappings from this map. The map will be empty after this call returns.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Remove the specified key and all associated values
     *
     * @param key The key
     * @return The values
     */
    public ArrayList<V> remove(K key) {
        return map.remove(key);
    }
}
