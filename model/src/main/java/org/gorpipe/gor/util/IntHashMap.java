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
package org.gorpipe.gor.util;

import java.io.Serializable;


/**
 * IntHashMap provides an hash map implementation for integer primitives (int onto int),
 * providing near O(1) performance put, get and contains but worse on remove. Typically the
 * remove can take O(1) but worst case (but very unlikley) is close to O(N)
 * Note that this class doesn't support multithreading.
 *
 * @version $Id $
 */
public class IntHashMap implements Cloneable, Serializable {
    /**
     * Construct an IntHashMap with the default size.
     */
    public IntHashMap() {
        this(64);
    }

    private IntHashMap(int size) {
        allocateMap(size);
        this.loadfactor = 0.75f;
    }

    @Override
    public IntHashMap clone() {
        IntHashMap map = new IntHashMap();
        map.allocateMap(this.keys.length);
        map.count = this.count;
        map.loadfactor = this.loadfactor;
        System.arraycopy(this.keys, 0, map.keys, 0, this.keys.length);
        System.arraycopy(this.values, 0, map.values, 0, this.values.length);
        System.arraycopy(this.used, 0, map.used, 0, this.used.length);
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof IntHashMap) && equals((IntHashMap) obj);
    }

    @Override
    public int hashCode() {
        return size();
    }

    /**
     * Check the specified map for keys equality with this
     *
     * @param map The map to check for keys equality
     * @return True if the maps are key equal, else false.
     */
    public boolean equals(IntHashMap map) {
        if (this.size() != map.size())
            return false;

        for (int i = 0; i <= lengthminusone; i++) {
            if (isUsed(i) && !map.containsKey(keys[i])) {
                return false;
            }
        }
        return true;
    }


    /**
     * Clear the contents of the HashMap
     */
    public void clear() {
        allocateMap(64);
    }

    /**
     * Query of empty status of the map
     *
     * @return True if the map is empty, else false
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Query for the number of elements in the hashmap
     *
     * @return The number elements
     */
    public int size() {
        return count;
    }

    /**
     * Copy all keys to an array
     *
     * @return The array with the keys
     */
    public int[] keysToArray() {
        int[] a = new int[size()];
        int i = 0;
        for (int idx = 0; idx < keys.length; idx++) {
            if (isUsed(idx))
                a[i++] = keys[idx];
        }
        return a;
    }

    /**
     * Copy all keys to an array which is order so that the first entry is the key with the lowest value, ..., untill last entry with the highest value
     *
     * @param desc True for descending order (highest first), false for ascending (lowest first).
     * @return The array with the keys
     */
    public int[] keysToArraySortByValue(boolean desc) {
        final int[] a = keysToArray();
        final int[] vals = valuesToArray();
        assert a.length == vals.length;
        for (int i = 0; i < a.length; i++) {
            for (int j = i + 1; j < a.length; j++) {
                if (desc ? vals[j] > vals[i] : vals[i] > vals[j]) {
                    int t = vals[j];
                    vals[j] = vals[i];
                    vals[i] = t;
                    t = a[j];
                    a[j] = a[i];
                    a[i] = t;
                }
            }
        }

        return a;
    }

    /**
     * Copy all values to an array
     *
     * @return The array with the values
     */
    public int[] valuesToArray() {
        int[] a = new int[size()];
        int i = 0;
        for (int idx = 0; idx < values.length; idx++) {
            if (isUsed(idx))
                a[i++] = values[idx];
        }
        return a;
    }

    /**
     * Query if the map contains the specified key
     *
     * @param key The key value to check for containment
     * @return True if the key is in the map, else false
     */
    public boolean containsKey(int key) {
        int index = hash(key) & lengthminusone;
        while (isUsed(index)) {
            // Here it is assumed that the hashmap is never full so we never go full circle
            if (keys[index] == key)
                return true;
            index = (index + 1) & lengthminusone;
        }
        return false;
    }

    /**
     * Query if the map contains the specified value
     *
     * @param value The value to check for containment
     * @return True if the value is in the map, else false
     */
    public boolean containsValue(int value) {
        for (int i = 0; i < values.length; i++) {
            if (isUsed(i) && value == values[i])
                return true;
        }
        return false;
    }

    /**
     * Query the map for the value associated with the specified key.
     * Note: The assumption is made that the key exists in the map. Use containsValue() to ensure key existence.
     *
     * @param key The key to use for lookup
     * @return The value associated with the key, if the key exists. Throws a RuntimeException if the key does not exist in the map.
     */
    public int get(int key) {
        int index = hash(key) & lengthminusone;
        while (isUsed(index)) {
            // Here it is assumed that the hashmap is never full so we never go full circle
            if (keys[index] == key)
                return values[index];
            index = (index + 1) & lengthminusone;
        }
        throw new RuntimeException("Key Not Found");
    }

    /**
     * Query if the map contains the specified key
     *
     * @param key           The key value to check for containment
     * @param notFoundValue The value to return if key is not found. Note that it is in
     *                      the callers responsibility to ensure that the notFoundValue is never actually used
     *                      as an value associated with a key.
     * @return The value associated with the key if it is in the map, else notFoundValue
     */
    public int get(int key, int notFoundValue) {
        int index = hash(key) & lengthminusone;
        while (isUsed(index)) {
            // Here it is assumed that the hashmap is never full so we never go full circle
            if (keys[index] == key)
                return values[index];
            index = (index + 1) & lengthminusone;
        }
        return notFoundValue;
    }

    /**
     * Query if the map contains the specified key
     *
     * @param key           The key value to check for containment
     * @param notFoundValue The value to return if key is not found. Note that it is in
     *                      the callers responsibility to ensure that the notFoundValue is never actually used
     *                      as an value associated with a key.
     * @return The value associated with the key if it is in the map, else notFoundValue
     */
    public long getAsLong(int key, long notFoundValue) {
        int index = hash(key) & lengthminusone;
        while (isUsed(index)) {
            // Here it is assumed that the hashmap is never full so we never go full circle
            if (keys[index] == key)
                return values[index];
            index = (index + 1) & lengthminusone;
        }
        return notFoundValue;
    }

    /**
     * Add the specified value into the map
     *
     * @param key   The key to add
     * @param value The value to associate with the key
     * @return True if the key was inserted into the map, false if it already existed (and the value was overwritten)
     */
    public boolean put(int key, int value) {
        int index = hash(key) & lengthminusone;
        while (isUsed(index)) {
            // Assume the hashmap is never full so we never go full circle
            if (keys[index] == key) {
                values[index] = value;
                return false;
            }
            index = (index + 1) & lengthminusone;
        }

        keys[index] = key;
        values[index] = value;
        setUsed(index);
        count++;

        if (count > lengthminusone * loadfactor)
            grow();
        return true;
    }

    /**
     * Remove the specified value from the map. Note that while on the average an O
     * (1) performance is observed a worst case can approach O(N).
     *
     * @param key The value to remove
     * @return True if the value was removed from the map, false if it did not exist in the map
     */
    public boolean remove(int key) {
        int index = hash(key) & lengthminusone;
        while (isUsed(index) && keys[index] != key) {
            // Here it is assumed that the hashmap is never full so a circle is never reached
            index = (index + 1) & lengthminusone;
        }

        if (isUsed(index) && keys[index] == key) {
            // The value to remove is found, so remove it
            clearUsed(index);
            count--;

            // Assume that all items on the logical right side of index, with hash value
            // larger than index will not be affected with setting the element at index as empty.
            // Check if any element with equal or smaller hash value exist
            int next = (index + 1) & lengthminusone;
            while (isUsed(next)) {
                int nextIdx = hash(keys[next]) & lengthminusone;
                boolean needSwap = next > index ? nextIdx <= index : (nextIdx > next && nextIdx <= index);
                if (needSwap) {
                    // Assume the newly found element can be moved in to the position where
                    // an element was removed since it will hash into a position before or at that.
                    keys[index] = keys[next];
                    setUsed(index);
                    clearUsed(next);
                    index = next;
                }
                next = (next + 1) & lengthminusone;
            }
            return true;
        }

        return false;
    }

    /**
     * Increment the occurrence count of this key, setting it as 1 if it hasn't occurred before
     *
     * @param key The key
     */
    public void increment(int key) {
        put(key, get(key, 0) + 1);
    }

    /**
     * Decrement the occurrence count of this key, if it has occurred before
     *
     * @param key The key
     */
    public void decrement(int key) {
        final int value = get(key, Integer.MIN_VALUE);
        if (value != Integer.MIN_VALUE) {
            put(key, value - 1);
        }
    }
    
    
    /*
            // Assume that all items on the logical right side of index, with hash value
            // larger than index will not be affected with setting the element at index as empty.
            // Check if any element with equal or smaller hash value exist
            int next = (index+1) & lengthminusone;
            while (next > index) {
                if (!isUsed(next))  // Found an empty slot, nothing more to do so return succesfully.
                    return true;
                if ((hash(keys[next]) & lengthminusone) <= index) {
                    // Assume the newly found element can be moved in to the position where
                    // a element was removed since it will hash into a position before or at that.
                    keys[index] = keys[next];
                    setUsed(index);
                    clearUsed(next);
                    index = next;
                }
                next = (next+1) & lengthminusone;
            }
            
            // Here we have reaced over the boundary of the circular array to the beginning
            // and assume that next == 0
            // If none of the elements from 0 to next empty are with hash higher than
            // their position and lower or equal to index, the the job is done.
            // Otherwise move that element to index and keep on checking.
            // Assume there is at least one empty space between 0 and the originaly removed index
            while (isUsed(next)) {
                if (((hash(keys[next]) & lengthminusone) > next) && ((hash(keys[next]) & lengthminusone) <= index)) {
                    keys[index] = keys[next];
                    setUsed(index);
                    clearUsed(next);
                    index = next;
                }
                
                next = (next+1) & lengthminusone;
            }

     */


    private boolean isUsed(int index) {
        // find the integer storing flags for this index, and check if the bit is set
        return (used[index >> 5] & (1 << (index & 31))) != 0;
    }

    private void setUsed(int index) {
        // find the integer storing flags for this index, and set the index bit on
        used[index >> 5] |= (1 << (index & 31));
    }

    private void clearUsed(int index) {
        // find the integer storing flags for this index, and set the index bit off
        used[index >> 5] &= ~(1 << (index & 31));
    }

    private static int hash(int value) {
        // This hashing function was derived from various resources on the internet
        // and interestingly enough seams to be used in jdk 1.4.1 HashMap
        // It appeareantly scramples bits on the lowend quite well
        value += ~(value << 9);
        value ^= (value >>> 14);
        value += (value << 4);
        return value ^ (value >>> 10);
    }

    private void grow() {
        int[] oldKeys = this.keys;
        int[] oldUsed = this.used;
        int[] oldValues = this.values;
        int newsize = keys.length << ((keys.length > 512 * 1024) ? 1 : 3);
        allocateMap(newsize);

        // Not that add calls this method on full map, so care must be taken to ensure
        // that add will not call into grow, while growing
        for (int idx = 0; idx < oldKeys.length; idx++) {
            if ((oldUsed[idx >> 5] & (1 << (idx & 31))) != 0)
                put(oldKeys[idx], oldValues[idx]);
        }
    }

    private void allocateMap(int size) {
        // The map is allways of length 2 pow n , which allows the modulus function
        // to be calculated by bitwize anding the length of the array-1.
        this.count = 0;
        this.keys = new int[size];
        this.values = new int[size];
        this.lengthminusone = keys.length - 1;
        this.used = new int[((size - 1) >> 5) + 1];
    }


    private int lengthminusone; // Last place in the map
    private int count; // Number of elements in the map
    private float loadfactor; // How full can the map grow
    private int[] keys; // The keys in the map (stored in hash order)
    private int[] values; // The values that the keys map to
    private int[] used; // Bit Array where the i-th bit marks the i-th value in the map used or not
}
