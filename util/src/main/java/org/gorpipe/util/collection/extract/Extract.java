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

package org.gorpipe.util.collection.extract;

import org.gorpipe.util.collection.IntHashSet;
import org.gorpipe.util.collection.MultiMap;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Extract provides closure like functions for extracting properties for objects in a collection into a collection of properties,
 * i.e. given a object with int field, create an int array of the int field from a collection of such objects.
 *
 * @version $Id$
 */
public class Extract {
    private static final String HEXES = "0123456789ABCDEF";

    private Extract() {
        
    }

    // A set of methods to extract dates as long or as text

    /**
     * @param date The date object to extract long value from
     * @return The long representing the date, or Long.MIN_VALUE if null date
     */
    static long datetime(Date date) {
        return date != null ? date.getTime() : Long.MIN_VALUE;
    }

    /**
     * @param date The date time (as previously gotten by getTime on Date, or Long.MIN_VALUE if date object was null)
     * @return The ISO formatted date string, i.e. YYYY-mm-dd
     */
    public static String dateString(Date date) {
        return dateString(datetime(date));
    }

    /**
     * @param date The date time as a long (as previously gotten by getTime on Date, or Long.MIN_VALUE if date object was null)
     * @return The ISO formatted date string, i.e. YYYY-mm-dd
     */
    static String dateString(long date) {
        if (date == Long.MIN_VALUE) {
            return "";
        }
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(date);
        return dateString(cal);
    }

    /**
     * @param cal The calendar object to format to iso date
     * @return The ISO formatted date string, i.e. YYYY-mm-dd
     */
    private static String dateString(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-" + lpad(cal.get(Calendar.MONTH) + 1) + "-" + lpad(cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * @return The ISO formatted date and time string for current time, i.e. YYYY-mm_dd hh:mm:ss
     */
    public static String datetimeString() {
        return datetimeString(currentTimeMillis());
    }

    /*
      extracted so testing can be done reliably.
     */
    public static CurrentTimeProvider  currentTimeProvider = () -> System.currentTimeMillis();
    protected static long currentTimeMillis() {
        return currentTimeProvider.currentTimeMillis();
    }

    interface CurrentTimeProvider {
        long currentTimeMillis();
    }



    /**
     * @param date The date time as a long (as previously gotten by getTime on Date, or Long.MIN_VALUE if date object was null)
     * @return The ISO formatted date and time string, i.e. YYYY-mm_dd hh:mm:ss
     */
    public static String datetimeString(long date) {
        if (date == Long.MIN_VALUE) {
            return "";
        }

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(date);
        return datetimeString(cal);
    }

    /**
     * @param cal The calendar object format to ISO date and time
     * @return The ISO formatted date and time string, i.e. YYYY-mm_dd hh:mm:ss
     */
    private static String datetimeString(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-" + lpad(cal.get(Calendar.MONTH) + 1) + "-" + lpad(cal.get(Calendar.DAY_OF_MONTH))
                + ' ' + lpad(cal.get(Calendar.HOUR_OF_DAY)) + ':' + lpad(cal.get(Calendar.MINUTE)) + ':' + lpad(cal.get(Calendar.SECOND));
    }

    private static String lpad(int val) {
        return val < 10 ? "0" + val : String.valueOf(val);
    }

    /**
     * @param date The time as a long (as previously gotten by getTime on Date, or Long.MIN_VALUE if date object was null)
     * @return The ISO formatted time string
     */
    static String timeString(long date) {
        if (date == Long.MIN_VALUE) {
            return "";
        }
        return String.format("%tT", new Date(date));
    }

    /**
     * @param duration The duration in ms to format
     * @return A formatted time string of time elapsed
     */
    public static String durationString(long duration) {
        final int hours = (int) (duration / (1000 * 60 * 60));
        final int hdiff = hours * 1000 * 60 * 60;
        final int min = (int) ((duration - hdiff) / (1000 * 60));
        final int mdiff = min * 1000 * 60;
        final int sec = (int) ((duration - hdiff - mdiff) / 1000);
        final int ms = (int) (duration - hdiff - mdiff - (sec * 1000));
        final StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (hours > 0 || min > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(min);
            sb.append("m ");
        }
        sb.append(sec);
        sb.append("s");
        if (duration < 10000) { // No need for millisecond tracking after 5 seconds
            sb.append(' ');
            sb.append(StringUtils.leftPad("" + ms, 3, '0'));
            sb.append("ms.");
        }
        return sb.toString();
    }

    /**
     * @param timestamp The time to calculate duration to current time
     * @return A formatted time string of time elapsed since the specified time
     */
    public static String durationStringSince(long timestamp) {
        return durationString(currentTimeMillis() - timestamp);
    }


// A set of methods to extract value from ResultSets while dealing with null values	

    /**
     * @param rs          The ResultSet to extract value from
     * @param col         The name of the column to extract value for
     * @param valueIfNull The value to extract if null was found in result set
     * @return The value extracted
     * @throws SQLException
     */
    public static int nvl(ResultSet rs, String col, int valueIfNull) throws SQLException {
        final int value = rs.getInt(col);
        return rs.wasNull() ? valueIfNull : value;
    }

    /**
     * @param rs          The ResultSet to extract value from
     * @param col         The name of the column to extract value for
     * @param valueIfNull The value to extract if null was found in result set
     * @return The value extracted
     * @throws SQLException
     */
    public static String nvl(ResultSet rs, String col, String valueIfNull) throws SQLException {
        final String value = rs.getString(col);
        return rs.wasNull() ? valueIfNull : value;
    }

    /**
     * @param value       The value to extract if not null
     * @param valueIfNull The value to extract if value was null
     * @return The value extracted
     */
    public static <T> T nvl(T value, T valueIfNull) {
        return value == null ? valueIfNull : value;
    }

    /**
     * @param value       The value to extract toString from if not null
     * @param valueIfNull The value to extract if value was null or value.toString is null
     * @return The value extracted
     */
    public static String nvlToString(Object value, String valueIfNull) {
        return value == null ? valueIfNull : nvl(value.toString(), valueIfNull);
    }

    /**
     * @param value       The value to extract toString from if not null
     * @param valueIfNull The value to extract if value was null or value.toString is null
     * @return The value extracted
     */
    static String nvlToString(int[] value, String valueIfNull) {
        return value == null ? valueIfNull : nvl(Arrays.toString(value), valueIfNull);
    }

    /**
     * @param values series of int values
     * @return An array containing the series of int values
     */
    static int[] arrayI(int... values) {
        return values;
    }

    /**
     * @param values series of values
     * @return An array containing the series of values
     */
    @SafeVarargs
    public static <T> T[] array(T... values) {
        return values;
    }


    /**
     * Converts any array or iterable collection of a single type to an ArrayList of the same type
     *
     * @param values The array to convert
     * @return The content of the provided array in an ArrayList
     */
    @SafeVarargs
    static <T> ArrayList<T> arrayList(T... values) {
        ArrayList<T> ret = new ArrayList<T>();
        Collections.addAll(ret, values);
        return ret;
    }

    /**
     * @param values series of int values
     * @return An IntHashSet containing the series of int values
     */
    static IntHashSet setI(int... values) {
        return new IntHashSet(values);
    }

    /**
     * @param values series of values
     * @return A HashSet containing the series of values
     */
    @SafeVarargs
    public static <T> HashSet<T> set(T... values) {
        final HashSet<T> set = new HashSet<T>();
        Collections.addAll(set, values);
        return set;
    }

    /**
     * @param column The column to extract set of value from
     * @param values Multi-dimensional array
     * @return A HashSet containing the series of values
     */
    public static <T> HashSet<T> set(int column, T[][] values) {
        final HashSet<T> set = new HashSet<T>();
        for (T[] v : values) {
            set.add(v[column]);
        }
        return set;
    }

    /**
     * @param list List of key value pairs to add to the map
     * @return A HashMap containing the map of keys to values
     */
    @SafeVarargs
    public static <T> HashMap<T, T> mapKeyValues(T... list) {
        if ((list.length & 0x1) == 0x1) {
            throw new IllegalArgumentException("Must provide an equal number of keys and values");
        }
        final HashMap<T, T> map = new HashMap<T, T>();
        for (int i = 0; i < list.length; i += 2) {
            map.put(list[i], list[i + 1]);
        }
        return map;
    }

    /**
     * @param keys   The keys to map to the values
     * @param values series of values
     * @return A HashMap containing the map of keys to values
     */
    public static <K, V> HashMap<K, V> map(K[] keys, V[] values) {
        assert keys.length == values.length;
        final HashMap<K, V> map = new HashMap<K, V>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    /**
     * @param keys   The keys to map to the values
     * @param values series of values
     * @return A HashMap containing the map of keys to values
     */
    public static <K, V> HashMap<K, V> map(Set<K> keys, V[] values) {
        assert keys.size() == values.length;
        final HashMap<K, V> map = new HashMap<K, V>();
        int i = 0;
        for (K key : keys) {
            map.put(key, values[i++]);
        }
        return map;
    }

    /**
     * Extract an int array from the provided objects, using the extract function to get the int value.
     * Note that a value is only extracted for non null objects, resulting in a output array that is smaller that the input array if it contains a null.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return An array of the integers, extracted from non null objects
     */
    public static <T> int[] array(T[] values, GetInt<T> extractor) {
        // Copy values into a resulting array
        final int[] results = new int[values.length];
        int i = 0;
        for (int j = 0; j < values.length; j++) {
            if (values[j] != null) {
                results[i++] = extractor.get(values[j]);
            }
        }

        if (i == results.length) { // Full array, just return it as is
            return results;
        } else { // Copy to new array with only the used values
            final int[] a = new int[i];
            System.arraycopy(results, 0, a, 0, i);
            return a;
        }
    }

    /**
     * Extract an int array from the provided objects, using the extract function to get the int value.
     * Note that a value is only extracted for non null objects, resulting in a output array that is smaller that the input collection if it contains a null.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return An array of the integers, extracted from non null objects
     */
    public static <T> int[] array(Collection<T> values, GetInt<T> extractor) {
        // Copy values into a resulting array
        final int[] results = new int[values.size()];
        int i = 0;
        for (T value : values) {
            if (value != null) {
                results[i++] = extractor.get(value);
            }
        }

        if (i == results.length) { // Full array, just return it as is
            return results;
        } else { // Copy to new array with only the used values
            final int[] a = new int[i];
            System.arraycopy(results, 0, a, 0, i);
            return a;
        }
    }

    /**
     * Extract a set of int values from the provided objects, using the extract function to get the int value.
     * Note that a value is only extracted for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A set of the int values, from non null values
     */
    public static <T> IntHashSet set(T[] values, GetInt<T> extractor) {
        // Copy values into a resulting array
        final IntHashSet results = new IntHashSet();
        for (T value : values) {
            if (value != null) {
                results.add(extractor.get(value));
            }
        }

        return results;
    }

    /**
     * Extract a set of int values from the provided objects, using the extract function to get the int value.
     * Note that a value is only extracted for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A set of the int values, from non null values
     */
    public static <T> IntHashSet set(Collection<T> values, GetInt<T> extractor) {
        // Copy values into a resulting array
        final IntHashSet results = new IntHashSet();
        for (T value : values) {
            if (value != null) {
                results.add(extractor.get(value));
            }
        }

        return results;
    }


    /**
     * Extract a map of int value to a object, for all the provided objects, using the extract function to get the int value.
     * Note that a entry is only added for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A map of the int values to the object, from non null values
     */
    public static <T> HashMap<Integer, T> map(T[] values, GetInt<T> extractor) {
        final HashMap<Integer, T> map = new HashMap<Integer, T>();
        for (T value : values) {
            if (value != null) {
                map.put(extractor.get(value), value);
            }
        }
        return map;
    }

    /**
     * Extract a map of int value to a object, for all the provided objects, using the extract function to get the int value.
     * Note that a entry is only added for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A map of the int values to the object, from non null values
     */
    public static <T> HashMap<Integer, T> map(Collection<T> values, GetInt<T> extractor) {
        final HashMap<Integer, T> map = new HashMap<Integer, T>();
        for (T value : values) {
            if (value != null) {
                map.put(extractor.get(value), value);
            }
        }
        return map;
    }


    /**
     * Extract a map of String value to a object, for all the provided objects, using the extract function to get the String value.
     * Note that a entry is only added for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A map of the String values to the object, from non null values
     */
    static <T> MultiMap<Integer, T> multimap(T[] values, GetInt<T> extractor) {
        final MultiMap<Integer, T> map = new MultiMap<Integer, T>();
        for (T value : values) {
            if (value != null) {
                map.put(extractor.get(value), value);
            }
        }
        return map;
    }

    /**
     * Extract a String array from the provided objects, using the extract function to get the String value.
     * Note that a value is only extracted for non null objects, resulting in a output array that is smaller that the input array if it contains a null.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @param excludes  Array of strings to exclude from the extraction
     * @return An array of the Strings, extracted from non null objects
     */
    public static <T> String[] array(T[] values, GetString<T> extractor, String... excludes) {
        // Copy values into a resulting array
        final String[] results = new String[values.length];
        final Set<String> excludeSet = Extract.set(excludes);
        int i = 0;
        for (int j = 0; j < values.length; j++) {
            if (values[j] != null) {
                final String value = extractor.get(values[j]);
                if (!excludeSet.contains(value)) {
                    results[i++] = value;
                }
            }
        }

        if (i == results.length) { // Full array, just return it as is
            return results;
        } else { // Copy to new array with only the used values
            final String[] a = new String[i];
            System.arraycopy(results, 0, a, 0, i);
            return a;
        }
    }


    /**
     * Extract an String array from the provided objects, using the extract function to get the String value.
     * Note that a value is only extracted for non null objects, resulting in a output array that is smaller that the input collection if it contains a null.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return An array of the Strings, extracted from non null objects
     */
    public static <T> String[] array(Collection<T> values, GetString<T> extractor) {
        // Copy values into a resulting array
        final String[] results = new String[values.size()];
        int i = 0;
        for (T value : values) {
            if (value != null) {
                results[i++] = extractor.get(value);
            }
        }

        if (i == results.length) { // Full array, just return it as is
            return results;
        } else { // Copy to new array with only the used values
            final String[] a = new String[i];
            System.arraycopy(results, 0, a, 0, i);
            return a;
        }
    }

    /**
     * Extract a set of String values from the provided objects, using the extract function to get the String value.
     * Note that a value is only extracted for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A set of the String values, from non null values
     */
    public static <T> HashSet<String> set(T[] values, GetString<T> extractor) {
        // Copy values into a resulting array
        final HashSet<String> results = new HashSet<String>();
        for (T value : values) {
            if (value != null) {
                results.add(extractor.get(value));
            }
        }

        return results;
    }

    /**
     * Extract a set of duplicated String values from the provided objects, using the extract function to get the String value.
     * Note that a value is only extracted for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A set of the String values, from non null values that are duplicated
     */
    public static <T> HashSet<String> duplicates(Collection<T> values, GetString<T> extractor) {
        // Copy values into a resulting array
        final HashSet<String> results = new HashSet<String>();
        final HashSet<String> duplicates = new HashSet<String>();
        for (T value : values) {
            if (value != null) {
                String v = extractor.get(value);
                if (results.contains(v)) {
                    duplicates.add(v);
                } else {
                    results.add(v);
                }
            }
        }

        return duplicates;
    }

    /**
     * Extract a set of duplicated String values from the provided objects, using the extract function to get the String value.
     * Note that a value is only extracted for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A set of the String values, from non null values that are duplicated
     */
    public static <T> HashSet<String> duplicates(T[] values, GetString<T> extractor) {
        // Copy values into a resulting array
        final HashSet<String> results = new HashSet<String>();
        final HashSet<String> duplicates = new HashSet<String>();
        for (T value : values) {
            if (value != null) {
                String v = extractor.get(value);
                if (results.contains(v)) {
                    duplicates.add(v);
                } else {
                    results.add(v);
                }
            }
        }

        return duplicates;
    }


    /**
     * Extract a set of String values from the provided objects, using the extract function to get the String value.
     * Note that a value is only extracted for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A set of the String values, from non null values
     */
    public static <T> HashSet<String> set(Collection<T> values, GetString<T> extractor) {
        // Copy values into a resulting array
        final HashSet<String> results = new HashSet<String>();
        for (T value : values) {
            if (value != null) {
                results.add(extractor.get(value));
            }
        }

        return results;
    }


    /**
     * Extract a map of String value to a object, for all the provided objects, using the extract function to get the String value.
     * Note that a entry is only added for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A map of the String values to the object, from non null values
     */
    public static <T> HashMap<String, T> map(T[] values, GetString<T> extractor) {
        final HashMap<String, T> map = new HashMap<String, T>();
        for (T value : values) {
            if (value != null) {
                map.put(extractor.get(value), value);
            }
        }
        return map;
    }


    /**
     * Extract a map of String value to a object, for all the provided objects, using the extract function to get the String value.
     * Note that a entry is only added for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A map of the String values to the object, from non null values
     */
    public static <T> HashMap<String, T> map(Collection<T> values, GetString<T> extractor) {
        final HashMap<String, T> map = new HashMap<String, T>();
        for (T value : values) {
            if (value != null) {
                map.put(extractor.get(value), value);
            }
        }
        return map;
    }

    /**
     * Extract a map of String value to a object, for all the provided objects, using the extract function to get the String value.
     * Note that a entry is only added for non null objects.
     *
     * @param extractor The extractor function
     * @param values    The values to read from
     * @return A map of the String values to the object, from non null values
     */
    static <T> MultiMap<String, T> multimap(T[] values, GetString<T> extractor) {
        final MultiMap<String, T> map = new MultiMap<String, T>();
        for (T value : values) {
            if (value != null) {
                map.put(extractor.get(value), value);
            }
        }
        return map;
    }

    /**
     * Extract the contents of the collection into the specified array, starting at the specified offset in the resulting array
     *
     * @param array  The array to write into
     * @param offset The position in the array to start writing
     * @param values The values to extract
     */
    public static <T> void into(T[] array, int offset, Collection<T> values) {
        for (T value : values) {
            array[offset++] = value;
        }
    }

    /**
     * Extract a single string with string representation of each object from the specified object array
     *
     * @param separator The separator to use between objects
     * @param array     The array of objects
     * @return The single string created
     */
    @SafeVarargs
    public static <T extends Object> String string(String separator, T... array) {
        final StringBuilder sb = new StringBuilder();
        for (Object t : array) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(t.toString());
        }
        return sb.toString();
    }

    /**
     * Extract a single string with string representation of each value from the specified int array
     *
     * @param separator The separator to use between values
     * @param array     The array of values
     * @return The single string created
     */
    public static String string(String separator, int... array) {
        final StringBuilder sb = new StringBuilder();
        for (int v : array) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(v);
        }
        return sb.toString();
    }


    /**
     * Extract a single string with string representation of each object from the specified object array
     *
     * @param separator The separator to use between objects
     * @param col       The array of objects
     * @return The single string created
     */
    public static <T extends Object> String string(String separator, Collection<T> col) {
        final StringBuilder sb = new StringBuilder();
        for (T t : col) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(t.toString());
        }
        return sb.toString();
    }

    /**
     * Extract an array of items from the source array that are not in the minus array
     *
     * @param source The source array
     * @param minus  The items not to include
     * @return The items in source - minus
     */
    @SafeVarargs
    public static <T extends Object> T[] minus(T[] source, T... minus) {
        final ArrayList<T> list = new ArrayList<T>();
        final HashSet<T> set = new HashSet<T>(Arrays.asList(minus));
        for (T t : source) {
            if (!set.contains(t)) {
                list.add(t);
            }
        }

        @SuppressWarnings("unchecked") final T[] copy = (source.getClass() == Object[].class) ? (T[]) new Object[list.size()] : (T[]) Array.newInstance(source.getClass().getComponentType(), list.size());
        return list.toArray(copy);
    }

    /**
     * Extract a new array of distinct object from the source array
     *
     * @param source The source array
     * @return The new array with distinct values from source
     */
    @SafeVarargs
    public static <T extends Object> T[] distinct(T... source) {
        final HashSet<T> set = new HashSet<T>(Arrays.asList(source));
        @SuppressWarnings("unchecked") final T[] copy = (source.getClass() == Object[].class) ? (T[]) new Object[set.size()] : (T[]) Array.newInstance(source.getClass().getComponentType(), set.size());
        return set.toArray(copy);
    }

    /**
     * Extract a HEX string from the specified byte array
     *
     * @param raw The raw bytes
     * @return The hex string
     */
    public static String hex(byte[] raw) {
        assert raw != null;
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    /**
     * Query for the MD5 digest for the specified byte array
     *
     * @param bytes The bytes to find digest of
     * @return The digest bytes
     */
    private static byte[] md5Bytes(byte[] bytes) {
        return md5Bytes(bytes, 0, bytes.length);
    }

    /**
     * Query for the MD5 digest for the specified data in the provided byte array
     *
     * @param bytes  The bytes with the data to find digest of
     * @param offset The offset into the byte array to start
     * @param length The number of bytes to include
     * @return The digest bytes
     */
    private static byte[] md5Bytes(byte[] bytes, int offset, int length) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes, offset, length);
            return md5.digest();
        } catch (NoSuchAlgorithmException ex) {
            // Should never happen
            throw new UnsupportedOperationException("Did not find implementation of MD5 hasing", ex);
        }
    }


    /**
     * Query for the MD5 digest for the specified byte array
     *
     * @param s The text string to digest
     * @return The digest as HEX string
     */
    static byte[] md5Bytes(String s) {
        return md5Bytes(s.getBytes(Charset.forName("UTF-8")));
    }

    /**
     * Query for the MD5 digest for the specified byte array
     *
     * @param bytes The bytes
     * @return The digest as HEX string
     */
    public static String md5(byte[] bytes) {
        return Extract.hex(md5Bytes(bytes));
    }

    /**
     * Query for the MD5 digest for the specified String
     *
     * @param s The text string to digest
     * @return The digest as HEX string
     */
    public static String md5(String s) {
        return md5(s.getBytes(Charset.forName("UTF-8")));
    }

    /**
     * Query for the MD5 digest for the specified Path content
     *
     * @param path The Path object to work with
     * @return The digest as HEX string
     * @throws IOException
     */
    public static String md5(Path path) throws IOException {
        return Files.isDirectory(path) ? digestDir(path,"MD5") : digest(path, "MD5");
    }

    /**
     * Query for the specified digest for the provided Path content
     *
     * @param path The Path object to work with
     * @param type The type of digest to create
     * @return The digest as HEX string
     * @throws IOException
     */
    public static String digestDir(Path path, String type) throws IOException {
        try {
            final MessageDigest md5 = MessageDigest.getInstance(type);
            final byte[] bytes = new byte[8 * 1024];
            Optional<IOException> ioException = Files.walk(path).parallel().filter(p -> !Files.isDirectory(p)).map(f -> {
                try (InputStream in = java.nio.file.Files.newInputStream(f)) {
                    int read;
                    while ((read = in.read(bytes)) > 0) {
                        md5.update(bytes, 0, read);
                    }
                } catch (IOException e) {
                    return e;
                }
                return null;
            }).filter(Objects::nonNull).findFirst();
            if(ioException.isPresent()) throw ioException.get();
            return hex(md5.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Did not find implementation of " + type + " hasing", ex);
        }
    }

    /**
     * Query for the specified digest for the provided Path content
     *
     * @param path The Path object to work with
     * @param type The type of digest to create
     * @return The digest as HEX string
     * @throws IOException
     */
    public static String digest(Path path, String type) throws IOException {
        try {
            final MessageDigest md5 = MessageDigest.getInstance(type);
            final byte[] bytes = new byte[8 * 1024];
            try (InputStream in = java.nio.file.Files.newInputStream(path)) {
                int read;
                while ((read = in.read(bytes)) > 0) {
                    md5.update(bytes, 0, read);
                }
            }
            return hex(md5.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Did not find implementation of " + type + " hasing", ex);
        }
    }
}
