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

package org.gorpipe.util.collection;

import java.io.Serializable;
import java.util.List;

/**
 * IntHashSet provides an hash set implementation for integer primitives,
 * providing near O(1) performance add and contains but worse on remove. Typically the
 * remove can take O(1) but worst case (but very unlikley) is close to O(N)
 * Note that this class doesn't support multithreading.
 *
 * @version $Id $
 */
public class IntHashSet implements Serializable {
    /**
     * Construct an IntHashSet with the default size.
     */
    public IntHashSet() {
        this(64, 0.75f);
    }

    /**
     * Construct an IntHashSet from the specified integer values
     *
     * @param values The values to contain in the set
     */
    public IntHashSet(int... values) {
        this(initialSize(64, (int) Math.ceil(values.length / 0.75f)), 0.75f);
        add(values);
    }

    /**
     * Copy constructor to create a new IntHashSet from the source IntHashSet
     *
     * @param source
     */
    public IntHashSet(IntHashSet source) {
        this.lengthminusone = source.lengthminusone;
        this.count = source.count;
        this.loadfactor = source.loadfactor;
        this.map = new int[source.map.length];
        System.arraycopy(source.map, 0, map, 0, source.map.length);
        this.used = new int[source.used.length];
        System.arraycopy(source.used, 0, used, 0, source.used.length);
    }

    private IntHashSet(int size, float loadFactor) {
        allocateMap(size);
        this.loadfactor = loadFactor;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null && obj instanceof IntHashSet) && equals((IntHashSet) obj);
    }

    @Override
    public int hashCode() {
        return size();
    }

    /**
     * Check the specified set for equality with this
     *
     * @param set The set to check for equiality
     * @return True if the sets are equal, else false.
     */
    public boolean equals(IntHashSet set) {
        if (this.size() != set.size())
            return false;

        for (int i = 0; i <= lengthminusone; i++) {
            if (isUsed(i) && !set.contains(map[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Query if the set contains the specified value
     *
     * @param value The value to check for containment
     * @return True if the value is in the set, else false
     */
    public boolean contains(int value) {
        int index = hash(value) & lengthminusone;
        while (isUsed(index)) {
            // Here it is assumed that the hashset is never full so we never go full circle
            if (map[index] == value)
                return true;
            index = (index + 1) & lengthminusone;
        }
        return false;
    }

    /**
     * Add the specified value into the set.
     * Note! Using method contains() to check if the provided value is already in the set prior to calling this function is a waste of CPU cycles.
     *
     * @param value The value to add
     * @return True if the value was added, false if it already existed in the map
     */
    public boolean add(int value) {
        int index = hash(value) & lengthminusone;
        while (isUsed(index)) {
            // Assume the hashset is never full so we never go full circle
            if (map[index] == value)
                return false;
            index = (index + 1) & lengthminusone;
        }

        map[index] = value;
        setUsed(index);
        count++;

        if (count > lengthminusone * loadfactor)
            grow();
        return true;
    }

    /**
     * Remove the specified value from the set. Note that while on the average an O
     * (1) performance is observed a worst case can approach O(N).
     *
     * @param value The value to remove
     * @return True if the value was removed from the set, false if it did not exist in the set
     */
    public boolean remove(int value) {
        int index = hash(value) & lengthminusone;
        while (isUsed(index) && map[index] != value) {
            // Here it is assumed that the hashset is never full so a circle is never reached
            index = (index + 1) & lengthminusone;
        }

        if (isUsed(index) && map[index] == value) {
            // The value to remove is found, so remove it
            clearUsed(index);
            count--;

            // Assume that all items on the logical right side of index, with hash value
            // larger than index will not be affected with setting the element at index as empty.
            // Check if any element with equal or smaller hash value exist
            int next = (index + 1) & lengthminusone;
            while (next > index) {
                if (!isUsed(next))  // Found an empty slot, nothing more to do so return succesfully.
                    return true;
                if ((hash(map[next]) & lengthminusone) <= index) {
                    // Assume the newly found element can be moved in to the position where
                    // a element was removed since it will hash into a position before or at that.
                    map[index] = map[next];
                    setUsed(index);
                    clearUsed(next);
                    index = next;
                }
                next = (next + 1) & lengthminusone;
            }

            // Here we have reaced over the boundary of the circular array to the beginning
            // and assume that next == 0
            // If none of the elements from 0 to next empty are with hash higher than
            // their position and lower or equal to index, the the job is done.
            // Otherwise move that element to index and keep on checking.
            // Assume there is at least one empty space between 0 and the originaly removed index
            while (isUsed(next)) {
                if (((hash(map[next]) & lengthminusone) > next) && ((hash(map[next]) & lengthminusone) <= index)) {
                    map[index] = map[next];
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
     * Clear the contents of the HashSet
     */
    public void clear() {
        allocateMap(64);
    }

    /**
     * Add all values from the specifed collection
     *
     * @param values The values to add
     * @return The number of new values added
     */
    public int add(int[] values) {
        int numAdded = 0;
        for (int value : values) {
            if (add(value))
                numAdded++;
        }

        return numAdded;
    }

    /**
     * Add all values from the specified collection
     *
     * @param values The values to add
     * @return The number of new values added
     */
    public int add(List<Integer> values) {
        int numAdded = 0;
        for (int value : values) {
            if (add(value))
                numAdded++;
        }
        return numAdded;
    }

    /**
     * Add all values from an IntArray
     *
     * @param values
     * @return The number of new values
     */
    public int add(IntArray values) {
        return add(values.toArray());
    }

    /**
     * Add all values from the specifed collection
     *
     * @param values The values to add
     * @return The number of new values added
     */
    public int add(IntHashSet values) {
        int numAdded = 0;
        for (int idx = 0; idx < values.map.length; idx++) {
            if (values.isUsed(idx) && add(values.map[idx]))
                numAdded++;
        }

        return numAdded;
    }

    /**
     * Create a union of the specified IntHashSet and this set
     *
     * @param set The specified set to union with this
     * @return A new IntHashSet with the union
     */
    public IntHashSet union(IntHashSet set) {
        IntHashSet union = new IntHashSet(this);
        union.add(set);
        return union;
    }

    /**
     * Create the intersection of the specified set and this
     *
     * @param set The set to create the intersection with
     * @return A new IntHashSet with the intersection
     */
    public IntHashSet intersection(IntHashSet set) {
        IntHashSet inter = new IntHashSet();
        IntHashSet b = set.lengthminusone > this.lengthminusone ? set : this;
        IntHashSet a = b == set ? this : set;
        for (int i = 0; i <= a.lengthminusone; i++) {
            if (a.isUsed(i) && b.contains(a.map[i])) {
                inter.add(a.map[i]);
            }
        }
        return inter;
    }

    /**
     * Remove all values not specified in the values array from this set
     *
     * @param values The value to retain
     */
    public void retainAll(int... values) {
        IntHashSet retainers = new IntHashSet();
        for (int value : values) {
            if (this.contains(value)) {
                retainers.add(value);
            }
        }

        // Copy all values from the temporary map and make ours
        this.lengthminusone = retainers.lengthminusone;
        this.count = retainers.count;
        this.loadfactor = retainers.loadfactor;
        this.map = retainers.map;
        this.used = retainers.used;
    }

    /**
     * Remove All element in the specified set from this set
     *
     * @param set The set to use
     */
    public void removeAll(IntHashSet set) {
        for (int next = set.findNext(0); next != -1; next = set.findNext(next + 1)) {
            this.remove(set.getValue(next));
        }
    }

    /**
     * Create an array of all the elements in the hash set
     *
     * @return An array of the elements in the set
     */
    public int[] toArray() {
        int[] v = new int[size()];
        int i = 0;
        for (int next = findNext(0); next != -1; next = findNext(next + 1)) {
            v[i++] = getValue(next);
        }
        assert i == size();
        return v;
    }

    /**
     * Query of empty status of the set
     *
     * @return True if the set is empty, else false
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * @return The lowest value in the set
     */
    public int min() {
        int min = Integer.MAX_VALUE;
        for (int next = findNext(0); next != -1; next = findNext(next + 1)) {
            final int value = getValue(next);
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    /**
     * @return The highest value in the set
     */
    public int max() {
        int max = Integer.MIN_VALUE;
        for (int next = findNext(0); next != -1; next = findNext(next + 1)) {
            final int value = getValue(next);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     * Query for the number of elements in the hashset
     *
     * @return The number elements
     */
    public int size() {
        return count;
    }

    int findNext(int start) {
        while (start <= lengthminusone) {
            if (isUsed(start))
                return start;
            start++;
        }
        return -1;
    }

    int getValue(int pos) {
        return map[pos];
    }

    private void allocateMap(int size) {
        // The map is always of length 2 pow n , which allows the modulus function
        // to be calculated by bitwise anding the length of the array-1.
        this.count = 0;
        this.map = new int[size];
        this.lengthminusone = map.length - 1;
        this.used = new int[((size - 1) >> 5) + 1];
    }

    private static int initialSize(int curSize, int minNewSize) {
        int newsize = curSize;
        while (newsize < minNewSize) {
            newsize = newsize << ((newsize > 512 * 1024) ? 1 : 3);
        }
        return newsize;
    }

    private void grow() {
        int[] old = this.map;
        int[] oldUsed = this.used;
        int newsize = map.length << ((map.length > 512 * 1024) ? 1 : 3);
        allocateMap(newsize);

        // Not that add calls this method on full map, so care must be taken to ensure
        // that add will not call into grow, while growing
        for (int idx = 0; idx < old.length; idx++) {
            if ((oldUsed[idx >> 5] & (1 << (idx & 31))) != 0)
                add(old[idx]);
        }
    }

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
        // and interestingly enough seams to be used in jdk 1.4.1 HashSet
        // It apparently scrambles bits on the lowend quite well
        value += ~(value << 9);
        value ^= (value >>> 14);
        value += (value << 4);
        return value ^ (value >>> 10);
    }

    private int lengthminusone; // Last place in the map
    private int count; // Number of elements in the map
    private float loadfactor; // How full can the map grow
    private int[] map; // The values in the map
    private int[] used; // Bit Array where the i-th bit marks the i-th value in the set used or not
}
